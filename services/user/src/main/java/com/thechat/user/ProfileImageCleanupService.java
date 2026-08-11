package com.thechat.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thechat.object_storage.CloudflareR2Client;

@Service
public class ProfileImageCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageCleanupService.class);

    private final ProfileImageRepository profileImageRepository;
    private final UserRepository userRepository;
    private final CloudflareR2Client r2Client;

    public ProfileImageCleanupService(
            ProfileImageRepository profileImageRepository,
            UserRepository userRepository,
            CloudflareR2Client r2Client) {
        this.profileImageRepository = profileImageRepository;
        this.userRepository = userRepository;
        this.r2Client = r2Client;
    }

    /**
     * Drops a user's existing PENDING avatar uploads before issuing a new presign.
     * Does not delete the R2 object when it is the user's current profile image key.
     */
    @Transactional
    public void abandonPendingForUser(UUID userId, String currentImageKey) {
        List<ProfileImage> pending = profileImageRepository
                .findByUser_IdAndStatus(userId, ProfileImageStatus.PENDING);
        if (pending.isEmpty()) {
            return;
        }
        removePendingRows(pending, currentImageKey);
        log.info("Abandoned {} pending profile image(s) for userId={}", pending.size(), userId);
    }

    /**
     * Removes PENDING rows older than {@code cutoff}. Best-effort R2 delete when the
     * object key is not the user's current avatar.
     */
    @Transactional
    public int cleanupStalePending(Instant cutoff) {
        List<ProfileImage> stale = profileImageRepository
                .findByStatusAndCreatedAtBefore(ProfileImageStatus.PENDING, cutoff);
        if (stale.isEmpty()) {
            return 0;
        }

        for (ProfileImage image : stale) {
            UUID userId = image.getUser().getId();
            String currentImageKey = userRepository.findById(userId)
                    .map(AppUser::getImage)
                    .orElse(null);
            removePendingRows(List.of(image), currentImageKey);
        }

        log.info("Cleaned up {} stale pending profile image(s) older than {}", stale.size(), cutoff);
        return stale.size();
    }

    private void removePendingRows(List<ProfileImage> pending, String currentImageKey) {
        List<ProfileImage> toDelete = new ArrayList<>(pending.size());
        for (ProfileImage image : pending) {
            String objectKey = image.getObjectKey();
            if (objectKey != null
                    && !objectKey.isBlank()
                    && (currentImageKey == null || !objectKey.equals(currentImageKey))) {
                deleteObjectBestEffort(objectKey);
            }
            toDelete.add(image);
        }
        profileImageRepository.deleteAll(toDelete);
    }

    private void deleteObjectBestEffort(String objectKey) {
        try {
            r2Client.deleteObject(objectKey);
        } catch (Exception exception) {
            log.warn("Failed to delete abandoned avatar object key={}", objectKey, exception);
        }
    }
}
