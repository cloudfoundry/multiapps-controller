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

@Component
@Order(20)
public class OperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsCleaner.class);

    private final OperationDao dao;
    private final ActivitiFacade activitiFacade;

    @Inject
    public OperationsCleaner(OperationDao dao, ActivitiFacade activitiFacade) {
        this.dao = dao;
        this.activitiFacade = activitiFacade;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(format("Cleaning up data for finished operations started before: {0}", expirationTime));
        List<Operation> operations = getOperationsToCleanUp(expirationTime);
        for (Operation operation : operations) {
            cleanUpSafely(operation);
        }
        LOGGER.info(format("Cleaned up operations: {0}", operations.size()));
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
        dao.remove(operation.getProcessId());
    }

    private List<Operation> getOperationsToCleanUp(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .build();
        return dao.find(filter);
    }

    private void abortOperation(Operation operation) {
        String processId = operation.getProcessId();
        ActivitiAction abortAction = ActivitiActionFactory.getAction(ActivitiActionFactory.ACTION_ID_ABORT, activitiFacade, null);
        LOGGER.debug(format("Aborting operation \"{0}\"", processId));
        abortAction.executeAction(processId);
    }

}
