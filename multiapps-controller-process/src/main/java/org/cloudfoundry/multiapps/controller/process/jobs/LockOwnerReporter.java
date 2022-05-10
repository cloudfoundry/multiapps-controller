package org.cloudfoundry.multiapps.controller.process.jobs;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLockOwnerEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.LockOwnerService;
import org.flowable.engine.ProcessEngine;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Named
public class LockOwnerReporter {

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
            lockOwnerService.add(ImmutableLockOwnerEntry.builder()
                                                        .lockOwner(lockOwner)
                                                        .timestamp(LocalDateTime.now())
                                                        .build());
            return;
        }

        var lockOwnerEntry = lockOwnerEntries.get(0);
        lockOwnerService.update(lockOwnerEntry, ImmutableLockOwnerEntry.copyOf(lockOwnerEntry)
                                                                       .withTimestamp(LocalDateTime.now()));
    }
}
