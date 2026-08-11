package com.thechat.user;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProfileImageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageCleanupJob.class);

    private final ProfileImageCleanupService cleanupService;
    private final Duration pendingMaxAge;

    public ProfileImageCleanupJob(
            ProfileImageCleanupService cleanupService,
            @Value("${app.profile-image.pending-max-age:PT1H}") Duration pendingMaxAge) {
        this.cleanupService = cleanupService;
        this.pendingMaxAge = pendingMaxAge;
    }

    @Scheduled(fixedDelayString = "${app.profile-image.cleanup-interval-ms:1800000}")
    public void cleanupStalePendingAvatars() {
        Instant cutoff = Instant.now().minus(pendingMaxAge);
        try {
            int removed = cleanupService.cleanupStalePending(cutoff);
            if (removed > 0) {
                log.info("Pending avatar cleanup removed {} row(s)", removed);
            }
        } catch (Exception exception) {
            log.warn("Pending avatar cleanup job failed", exception);
        }
    }
}
