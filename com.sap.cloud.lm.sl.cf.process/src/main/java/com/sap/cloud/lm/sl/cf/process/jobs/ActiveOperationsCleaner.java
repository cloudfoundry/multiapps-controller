package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiAction;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiActionFactory;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Component
@Order(10)
public class ActiveOperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveOperationsCleaner.class);

    private final OperationDao dao;
    private final ActivitiFacade activitiFacade;

    @Inject
    public ActiveOperationsCleaner(OperationDao dao, ActivitiFacade activitiFacade) {
        this.dao = dao;
        this.activitiFacade = activitiFacade;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(format("Aborting operations started before: {0}", expirationTime));
        List<Operation> activeOperations = getOperationsInActiveState(expirationTime);
        List<String> activeOperationsIds = getProcessIds(activeOperations);
        LOGGER.info(format("Operations to be aborted count: {0}", activeOperationsIds.size()));
        long abortedOperationsCount = tryToAbortOperations(activeOperationsIds);
        LOGGER.info(format("Aborted operations count: {0}", abortedOperationsCount));
    }

    private List<Operation> getOperationsInActiveState(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inNonFinalState()
            .descending()
            .build();
        return dao.find(filter);
    }

    private List<String> getProcessIds(List<Operation> operations) {
        return operations.stream()
            .map(Operation::getProcessId)
            .collect(Collectors.toList());
    }

    private long tryToAbortOperations(List<String> processIds) {
        LOGGER.debug(format("Aborting operations: {0}", processIds));
        return processIds.stream()
            .filter(this::tryToAbortOperation)
            .count();
    }

    private boolean tryToAbortOperation(String processId) {
        try {
            ActivitiAction abortAction = ActivitiActionFactory.getAction("abort", activitiFacade, null);
            LOGGER.info(format("Aborting operation with id: {0}", processId));
            abortAction.executeAction(processId);
            LOGGER.info(format("Successfully aborted operation with id: {0}", processId));
            return true;
        } catch (Exception e) {
            LOGGER.error(format("Error when trying to abort operation with id {0}: {1}", processId, e.getMessage()), e);
            return false;
        }
    }

}
