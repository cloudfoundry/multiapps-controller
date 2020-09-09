package org.cloudfoundry.multiapps.controller.process.listeners;

import static java.text.MessageFormat.format;

import java.time.Instant;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("expiredJobListener")
public class ExpiredJobListener extends AbstractFlowableEngineEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExpiredJobListener.class);

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    protected void entityDeleted(FlowableEngineEntityEvent event) {
        JobEntity jobEntity = getJobEntity(event);
        if (jobEntity == null) {
            return;
        }

        if (hasJobEntityExpired(jobEntity)) {
            LOGGER.info(format(Messages.JOB_WITH_ID_AND_TASK_NAME_EXPIRED, jobEntity.getProcessInstanceId(), jobEntity.getElementName()));
        }
    }

    private JobEntity getJobEntity(FlowableEngineEntityEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof JobInfoEntity) {
            return (JobEntity) entity;
        }
        return null;
    }

    private boolean hasJobEntityExpired(JobEntity jobEntity) {
        if (jobEntity.getLockExpirationTime() == null) {
            return false;
        }
        return Instant.now()
                      .isAfter(jobEntity.getLockExpirationTime()
                                        .toInstant());
    }

}
