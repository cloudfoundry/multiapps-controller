package org.cloudfoundry.multiapps.controller.process.flowable.commands;

import java.text.MessageFormat;
import java.util.List;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClearJobLockOwnersCmd implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearJobLockOwnersCmd.class);

    private final List<Job> lockedJobs;
    private final JobServiceConfiguration jobServiceConfiguration;

    public ClearJobLockOwnersCmd(List<Job> lockedJobs, JobServiceConfiguration jobServiceConfiguration) {
        this.lockedJobs = lockedJobs;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        JobEntityManager jobEntityManager = jobServiceConfiguration.getJobEntityManager();
        for (Job job : lockedJobs) {
            JobEntity runningLockedJob = jobEntityManager.findById(job.getId());
            if (runningLockedJob != null) {
                LOGGER.info(MessageFormat.format(Messages.UNLOCKING_JOB_WITH_ID_0_LOCK_EXPIRATION_TIME_1_CREATION_TIME_2_AND_ELEMENT_NAME_3,
                                                 runningLockedJob.getId(), runningLockedJob.getLockExpirationTime(),
                                                 runningLockedJob.getCreateTime(), runningLockedJob.getElementName()));
                runningLockedJob.setLockOwner(null);
                runningLockedJob.setLockExpirationTime(null);
            }
        }
        return null;
    }

}
