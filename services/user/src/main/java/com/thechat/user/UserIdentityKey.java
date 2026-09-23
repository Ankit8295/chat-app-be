package com.thechat.user;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "user_identity_keys")
public class UserIdentityKey implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "wrapped_private_key", nullable = false, columnDefinition = "TEXT")
    private String wrappedPrivateKey;

    @Column(name = "wrap_nonce", nullable = false, columnDefinition = "TEXT")
    private String wrapNonce;

    @Column(name = "kdf_salt", nullable = false, columnDefinition = "TEXT")
    private String kdfSalt;

    @Column(name = "kdf_iterations", nullable = false)
    private int kdfIterations;

    @Column(nullable = false, length = 64)
    private String algorithm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew = true;

    protected UserIdentityKey() {
    }

    public UserIdentityKey(
            UUID userId,
            String publicKey,
            String wrappedPrivateKey,
            String wrapNonce,
            String kdfSalt,
            int kdfIterations,
            String algorithm) {
        this.userId = userId;
        this.publicKey = publicKey;
        this.wrappedPrivateKey = wrappedPrivateKey;
        this.wrapNonce = wrapNonce;
        this.kdfSalt = kdfSalt;
        this.kdfIterations = kdfIterations;
        this.algorithm = algorithm;
        this.isNew = true;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.isNew = false;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getWrappedPrivateKey() {
        return wrappedPrivateKey;
    }

    public String getWrapNonce() {
        return wrapNonce;
    }

    public String getKdfSalt() {
        return kdfSalt;
    }

    public int getKdfIterations() {
        return kdfIterations;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
