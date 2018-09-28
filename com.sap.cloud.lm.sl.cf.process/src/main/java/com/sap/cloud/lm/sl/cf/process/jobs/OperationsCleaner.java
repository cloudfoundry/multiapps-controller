package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.activiti.AbortProcessAction;
import com.sap.cloud.lm.sl.cf.core.activiti.FlowableFacade;
import com.sap.cloud.lm.sl.cf.core.activiti.ProcessAction;
import com.sap.cloud.lm.sl.cf.core.activiti.ProcessActionRegistry;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Component
@Order(10)
public class OperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsCleaner.class);
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final OperationDao dao;
    private final FlowableFacade flowableFacade;
    private final ProgressMessageService progressMessageService;
    private final ProcessActionRegistry processActionRegistry;
    private int pageSize = DEFAULT_PAGE_SIZE;

    @Inject
    public OperationsCleaner(OperationDao dao, FlowableFacade activitiFacade, ProgressMessageService progressMessageService,
        ProcessActionRegistry processActionregistry) {
        this.dao = dao;
        this.flowableFacade = activitiFacade;
        this.progressMessageService = progressMessageService;
        this.processActionRegistry = processActionregistry;
    }

    public OperationsCleaner withPageSize(int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.DELETING_OPERATIONS_STARTED_BEFORE_0, expirationTime));
        int abortedOperations = abortActiveOperations(expirationTime);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.ABORTED_OPERATIONS_0, abortedOperations));
        int deletedOperations = dao.removeExpiredInFinalState(expirationTime);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_OPERATIONS_0, deletedOperations));
    }

    private int abortActiveOperations(Date expirationTime) {
        int abortedOperations = 0;
        int pageIndex = 0;
        while (true) {
            List<Operation> activeOperationsPage = getActiveOperationsPage(expirationTime, pageIndex);
            for (Operation operation : activeOperationsPage) {
                boolean abortWasSuccessful = abortSafely(operation);
                if (abortWasSuccessful) {
                    abortedOperations++;
                }
            }
            if (pageSize > activeOperationsPage.size()) {
                return abortedOperations;
            }
            pageIndex++;
        }
    }

    private List<Operation> getActiveOperationsPage(Date expirationTime, int pageIndex) {
        OperationFilter filter = new OperationFilter.Builder().inNonFinalState()
            .startedBefore(expirationTime)
            .firstElement(pageIndex * pageSize)
            .maxResults(pageSize)
            .orderByProcessId()
            .build();
        return dao.find(filter);
    }

    private boolean abortSafely(Operation operation) {
        try {
            abort(operation);
            return true;
        } catch (Exception e) {
            LOGGER.warn(CleanUpJob.LOG_MARKER, format(Messages.COULD_NOT_ABORT_OPERATION_0, operation.getProcessId()), e);
            return false;
        }
    }

    private void abort(Operation operation) {
        ProcessAction abortAction = processActionRegistry.getAction(AbortProcessAction.ACTION_ID_ABORT);
        String processId = operation.getProcessId();
        LOGGER.debug(CleanUpJob.LOG_MARKER, format(Messages.ABORTING_OPERATION_0, processId));
        abortAction.execute(null, processId);
    }

}
