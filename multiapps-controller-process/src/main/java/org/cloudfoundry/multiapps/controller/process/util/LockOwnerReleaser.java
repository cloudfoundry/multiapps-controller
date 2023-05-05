package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.controller.process.flowable.commands.ClearJobLockOwnersCmd;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;

@Named("lockOwnerReleaser")
public class LockOwnerReleaser {

    private final ProcessEngine processEngine;

    @Inject
    public LockOwnerReleaser(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public void release(String lockOwner) {
        clearProcessInstanceLockTime(lockOwner);
        clearJobsLockTime(lockOwner);
    }

    private void clearProcessInstanceLockTime(String lockOwner) {
        processEngine.getProcessEngineConfiguration()
                     .getCommandExecutor()
                     .execute(new ClearProcessInstanceLockTimesCmd(lockOwner));
    }

    private void clearJobsLockTime(String lockOwner) {
        JobServiceConfiguration jobServiceConfiguration = processEngine.getProcessEngineConfiguration()
                                                                       .getAsyncExecutor()
                                                                       .getJobServiceConfiguration();
        List<Job> jobs = getStaleJobs(lockOwner);
        if (CollectionUtils.isNotEmpty(jobs)) {
            processEngine.getManagementService()
                         .executeCommand(new ClearJobLockOwnersCmd(jobs, jobServiceConfiguration));
        }
    }

    private List<Job> getStaleJobs(String lockOwner) {
        return processEngine.getManagementService()
                            .createJobQuery()
                            .lockOwner(lockOwner)
                            .list();
    }

}
