package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Set<String> historicProcessIds = activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(expirationTime)
            .stream()
            .flatMap(this::getProcessAndSubProcessIdsStream)
            .collect(Collectors.<String> toSet());

        LOGGER.info(format("Historic Processes marked for deletion count: {0}", historicProcessIds.size()));
        historicProcessIds.stream()
            .forEach(this::tryToDeleteHistoricProcessInstance);
    }

    private Stream<String> getProcessAndSubProcessIdsStream(HistoricProcessInstance historicProcessInstance) {
        List<String> processIds = activitiFacade.getHistoricSubProcessIds(historicProcessInstance.getId());
        processIds.add(historicProcessInstance.getId());
        return processIds.stream();
    }

    private boolean tryToDeleteHistoricProcessInstance(String processId) {
        try {
            LOGGER.info(format("Deleting Historic Process with id: {0}", processId));
            activitiFacade.deleteHistoricProcessInstance(processId);
            LOGGER.info(format("Successfully deleted Historic Process with id: {0}", processId));
            return true;
        } catch (Exception e) {
            LOGGER.error(format("Error when trying to delete historic process with id {0}: {1}", processId, e.getMessage()), e);
            return false;
        }
    }

}
