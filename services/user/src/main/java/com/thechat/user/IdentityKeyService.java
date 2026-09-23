package com.thechat.user;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.user.dto.OwnIdentityKeyResponse;
import com.thechat.user.dto.PublicIdentityKeyResponse;
import com.thechat.user.dto.UpsertIdentityKeyRequest;

@Service
public class IdentityKeyService {

    private static final int MAX_BATCH_IDS = 100;

    private final UserIdentityKeyRepository identityKeyRepository;
    private final UserRepository userRepository;

    public IdentityKeyService(
            UserIdentityKeyRepository identityKeyRepository,
            UserRepository userRepository) {
        this.identityKeyRepository = identityKeyRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public OwnIdentityKeyResponse getOwn(UUID userId) {
        return identityKeyRepository.findById(userId)
                .map(OwnIdentityKeyResponse::from)
                .orElseThrow(IdentityKeyNotFoundException::new);
    }

    @Transactional
    public OwnIdentityKeyResponse upsert(UUID userId, UpsertIdentityKeyRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }

        return identityKeyRepository.findById(userId)
                .map(existing -> {
                    if (!existing.getPublicKey().equals(request.publicKey())) {
                        throw new IdentityKeyConflictException();
                    }
                    return OwnIdentityKeyResponse.from(existing);
                })
                .orElseGet(() -> OwnIdentityKeyResponse.from(identityKeyRepository.save(
                        new UserIdentityKey(
                                userId,
                                request.publicKey(),
                                request.wrappedPrivateKey(),
                                request.wrapNonce(),
                                request.kdfSalt(),
                                request.kdfIterations(),
                                request.algorithm()))));
    }

    @Transactional(readOnly = true)
    public List<PublicIdentityKeyResponse> getPublicKeys(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > MAX_BATCH_IDS) {
            throw new IllegalArgumentException("Too many ids; maximum is " + MAX_BATCH_IDS);
        }
        return identityKeyRepository.findAllByUserIdIn(ids).stream()
                .map(PublicIdentityKeyResponse::from)
                .toList();
    }
}
