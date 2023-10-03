package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.LockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.LockOwnerService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.engine.ProcessEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class LockOwnerReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LockOwnerReporter.class);

    private final ProcessEngine processEngine;
    private final LockOwnerService lockOwnerService;

    @Inject
    public LockOwnerReporter(ProcessEngine processEngine, LockOwnerService lockOwnerService) {
        this.processEngine = processEngine;
        this.lockOwnerService = lockOwnerService;
    }

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void run() {
        var lockOwner = processEngine.getProcessEngineConfiguration()
                                     .getAsyncExecutor()
                                     .getLockOwner();
        var lockOwnerEntries = lockOwnerService.createQuery()
                                               .lockOwner(lockOwner)
                                               .list();
        if (lockOwnerEntries.isEmpty()) {
            createLockOwner(lockOwner);
            return;
        }
        updateLockOwner(lockOwnerEntries);
    }

    private void createLockOwner(String lockOwner) {
        LocalDateTime timeNow = LocalDateTime.now();
        lockOwnerService.add(ImmutableLockOwnerEntry.builder()
                                                    .lockOwner(lockOwner)
                                                    .timestamp(timeNow)
                                                    .build());
        LOGGER.info(MessageFormat.format(Messages.CREATING_A_LOCK_OWNER_WITH_NAME_0_AND_TIMESTAMP_1, lockOwner, timeNow));
    }

    private void updateLockOwner(List<LockOwnerEntry> lockOwnerEntries) {
        var lockOwnerEntry = lockOwnerEntries.get(0);
        LocalDateTime timeNow = LocalDateTime.now();
        lockOwnerService.update(lockOwnerEntry, ImmutableLockOwnerEntry.copyOf(lockOwnerEntry)
                                                                       .withTimestamp(timeNow));
        LOGGER.info(MessageFormat.format(Messages.UPDATING_A_LOCK_OWNER_WITH_NAME_0_AND_TIMESTAMP_1, lockOwnerEntry.getLockOwner(),
                                         timeNow));
        if (lockOwnerEntries.size() > 1) {
            reportForDuplicatedLockOwners(lockOwnerEntries);
        }
    }

    private void reportForDuplicatedLockOwners(List<LockOwnerEntry> lockOwnerEntries) {
        LOGGER.info(Messages.MORE_THAN_ONE_LOCK_OWNER_FOUND);
        lockOwnerEntries.stream()
                        .skip(1)
                        .forEach(lockOwner -> LOGGER.info(MessageFormat.format(Messages.LOCK_OWNER_WITH_NAME_0_ID_1_AND_TIMESTAMP_2_EXISTS_MORE_THAN_ONCE,
                                                                               lockOwner.getLockOwner(), lockOwner.getId(),
                                                                               lockOwner.getTimestamp())));
    }
}
