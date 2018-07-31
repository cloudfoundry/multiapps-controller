package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.activiti.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;

@Component
@Order(30)
public class ActivitiHistoricDataCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiHistoricDataCleaner.class);

    private final ActivitiFacade activitiFacade;

    @Inject
    public ActivitiHistoricDataCleaner(ActivitiFacade activitiFacade) {
        this.activitiFacade = activitiFacade;
    }

    @Override
    public void execute(Date expirationTime) {
        List<HistoricProcessInstance> processesToDelete = activitiFacade
            .getHistoricProcessInstancesFinishedAndStartedBefore(expirationTime);

        LOGGER.info(format("Historic processes marked for deletion count: {0}", processesToDelete.size()));
        processesToDelete.stream()
            .forEach(this::deleteHistoricProcessInstanceSafely);
    }

    private void deleteHistoricProcessInstanceSafely(HistoricProcessInstance historicProcessInstance) {
        String processId = historicProcessInstance.getId();
        try {
            LOGGER.info(format("Deleting historic process with ID \"{0}\"...", processId));
            activitiFacade.deleteHistoricProcessInstance(processId);
            LOGGER.info(format("Successfully deleted historic process with ID \"{0}\"", processId));
        } catch (Exception e) {
            LOGGER.warn(format("Could not delete historic process with ID \"{0}\"", processId), e);
        }
    }

}
