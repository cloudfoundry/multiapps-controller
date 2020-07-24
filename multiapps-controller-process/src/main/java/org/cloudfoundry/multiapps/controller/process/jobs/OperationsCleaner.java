package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.core.persistence.service.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.flowable.AbortProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessAction;
import org.cloudfoundry.multiapps.controller.process.flowable.ProcessActionRegistry;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(10)
public class OperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsCleaner.class);
    private static final int DEFAULT_PAGE_SIZE = 100;

    private final OperationService operationService;
    private final ProcessActionRegistry processActionRegistry;
    private int pageSize = DEFAULT_PAGE_SIZE;

    @Inject
    public OperationsCleaner(OperationService operationService, ProcessActionRegistry processActionRegistry) {
        this.operationService = operationService;
        this.processActionRegistry = processActionRegistry;
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
        int deletedOperations = operationService.createQuery()
                                                .startedBefore(expirationTime)
                                                .inFinalState()
                                                .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.DELETED_OPERATIONS_0, deletedOperations));
    }

    private int abortActiveOperations(Date expirationTime) {
        int abortedOperations = 0;
        for (int pageIndex = 0;; pageIndex++) {
            List<Operation> operationsPage = getOperationsPage(expirationTime, pageIndex);
            for (Operation operation : operationsPage) {
                if (inFinalState(operation)) {
                    continue;
                }
                boolean abortWasSuccessful = abortSafely(operation);
                if (abortWasSuccessful) {
                    abortedOperations++;
                }
            }
            if (pageSize > operationsPage.size()) {
                return abortedOperations;
            }
        }
    }

    private List<Operation> getOperationsPage(Date expirationTime, int pageIndex) {
        return operationService.createQuery()
                               .startedBefore(expirationTime)
                               .offsetOnSelect(pageIndex * pageSize)
                               .limitOnSelect(pageSize)
                               .orderByProcessId(OrderDirection.ASCENDING)
                               .list();
    }

    private boolean inFinalState(Operation operation) {
        return operation.getState() != null;
    }

    private boolean abortSafely(Operation operation) {
        try {
            abort(operation);
            return true;
        } catch (FlowableObjectNotFoundException e) {
            abortOrphanedOperation(operation);
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

    private void abortOrphanedOperation(Operation operation) {
        Operation abortedOperation = ImmutableOperation.builder()
                                                       .from(operation)
                                                       .state(Operation.State.ABORTED)
                                                       .endedAt(ZonedDateTime.now())
                                                       .hasAcquiredLock(false)
                                                       .build();
        operationService.update(operation, abortedOperation);
    }

}
