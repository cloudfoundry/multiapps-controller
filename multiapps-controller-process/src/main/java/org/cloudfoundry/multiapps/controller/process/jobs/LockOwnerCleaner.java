package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.model.LockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.LockOwnerService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.LockOwnerReleaser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class LockOwnerCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockOwnerCleaner.class);

    private final LockOwnerService lockOwnerService;
    private final LockOwnerReleaser lockOwnerReleaser;

    @Inject
    public LockOwnerCleaner(LockOwnerService lockOwnerService, LockOwnerReleaser lockOwnerReleaser) {
        this.lockOwnerService = lockOwnerService;
        this.lockOwnerReleaser = lockOwnerReleaser;
    }

    @Scheduled(fixedRate = 6, timeUnit = TimeUnit.MINUTES)
    public void run() {
        var timestamp = LocalDateTime.now();
        var staleLockOwners = lockOwnerService.createQuery()
                                              .olderThan(timestamp.minus(Duration.ofMinutes(6)))
                                              .list()
                                              .stream()
                                              .map(LockOwnerEntry::getLockOwner)
                                              .collect(Collectors.toList());
        for (var staleLockOwner : staleLockOwners) {
            releaseStaleLockOwner(staleLockOwner);
        }
        if (!staleLockOwners.isEmpty()) {
            lockOwnerService.createQuery()
                            .withLockOwnerAnyOf(staleLockOwners)
                            .delete();
        }
    }

    private void releaseStaleLockOwner(String staleLockOwner) {
        LOGGER.info(MessageFormat.format(Messages.CLEARING_STALE_LOCK_OWNER, staleLockOwner));
        try {
            lockOwnerReleaser.release(staleLockOwner);
            LOGGER.info(MessageFormat.format(Messages.CLEARED_STALE_LOCK_OWNER, staleLockOwner));
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format(Messages.CLEARING_STALE_FLOWABLE_LOCK_OWNER_0_THREW_AN_EXCEPTION_1, staleLockOwner,
                                              e.getMessage()),
                         e);
        }
    }

}
