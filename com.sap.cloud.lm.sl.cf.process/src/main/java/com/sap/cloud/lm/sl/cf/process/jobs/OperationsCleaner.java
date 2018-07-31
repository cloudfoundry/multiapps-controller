package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;

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
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

@Component
@Order(20)
public class OperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsCleaner.class);

    private final OperationDao dao;
    private final ActivitiFacade activitiFacade;
    private final ProgressMessageService progressMessageService;
    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public OperationsCleaner(OperationDao dao, ActivitiFacade activitiFacade, ProgressMessageService progressMessageService,
        ProcessLogsPersistenceService processLogsPersistenceService) {
        this.dao = dao;
        this.activitiFacade = activitiFacade;
        this.progressMessageService = progressMessageService;
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(format("Cleaning up data for finished operations started before: {0}", expirationTime));
        List<Operation> operations = getOperationsToCleanUp(expirationTime);
        for (Operation operation : operations) {
            cleanUpSafely(operation);
        }
    }

    private void cleanUpSafely(Operation operation) {
        try {
            cleanUp(operation);
        } catch (Exception e) {
            LOGGER.warn(format("Could not clean up data for operation \"{0}\"", operation.getProcessId()), e);
        }
    }

    private void cleanUp(Operation operation) {
        if (operation.getState() == null) {
            abortOperation(operation);
        }
        removeProgressMessages(operation);
        removeProcessLogs(operation);
        markOperationAsCleanedUp(operation);
    }

    private List<Operation> getOperationsToCleanUp(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().isNotCleanedUp()
            .startedBefore(expirationTime)
            .build();
        return dao.find(filter);
    }

    private void abortOperation(Operation operation) {
        String processId = operation.getProcessId();
        ActivitiAction abortAction = ActivitiActionFactory.getAction("abort", activitiFacade, null);
        LOGGER.info(format("Aborting operation with id: {0}", processId));
        abortAction.executeAction(processId);
        LOGGER.info(format("Successfully aborted operation with id: {0}", processId));
    }

    private void removeProgressMessages(Operation operation) {
        int removedProgressMessages = progressMessageService.removeByProcessId(operation.getProcessId());
        LOGGER.info(format("Deleted progress messages for operation \"{0}\": {1}", operation.getProcessId(), removedProgressMessages));
    }

    private void removeProcessLogs(Operation operation) {
        int removedProcessLogs = processLogsPersistenceService.deleteByNamespace(operation.getProcessId());
        LOGGER.info(format("Deleted process logs for operation \"{0}\": {1}", operation.getProcessId(), removedProcessLogs));
    }

    private void markOperationAsCleanedUp(Operation operation) {
        operation.setCleanedUp(true);
        dao.merge(operation);
    }

}
