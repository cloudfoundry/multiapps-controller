package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessConflictPreventer.class);

    private final OperationService operationService;

    public ProcessConflictPreventer(OperationService operationService) {
        this.operationService = operationService;
    }

    public synchronized void acquireLock(String mtaId, String namespace, String spaceId, String processId) {
        LOGGER.info(format(Messages.ACQUIRING_LOCK, processId, mtaId));

        validateNoConflictingOperationsExist(mtaId, namespace, spaceId);
        Operation currentOperation = getOperationByProcessId(processId);
        ImmutableOperation.Builder currentOperationWithAcquiredLock = ImmutableOperation.builder()
                                                                                        .from(currentOperation)
                                                                                        .mtaId(mtaId)
                                                                                        .hasAcquiredLock(true);
        if (StringUtils.isNotEmpty(namespace)) {
            currentOperationWithAcquiredLock.namespace(namespace);
        }

        operationService.update(currentOperation, currentOperationWithAcquiredLock.build());

        LOGGER.info(format(Messages.ACQUIRED_LOCK, processId, StepsUtil.getQualifiedMtaId(mtaId, namespace)));
    }

    private void validateNoConflictingOperationsExist(String mtaId, String namespace, String spaceId) {
        List<Operation> conflictingOperations = findConflictingOperations(mtaId, namespace, spaceId);
        String qualifiedMtaId = StepsUtil.getQualifiedMtaId(mtaId, namespace);

        if (conflictingOperations.size() == 1) {
            Operation conflictingOperation = conflictingOperations.get(0);
            throw new SLException(Messages.CONFLICTING_PROCESS_FOUND, conflictingOperation.getProcessId(), qualifiedMtaId);
        }
        if (conflictingOperations.size() >= 2) {
            List<String> operationIds = getOperationIds(conflictingOperations);
            throw new SLException(Messages.MULTIPLE_OPERATIONS_WITH_LOCK_FOUND, qualifiedMtaId, spaceId, operationIds);
        }
    }

    private List<String> getOperationIds(List<Operation> operations) {
        return operations.stream()
                         .map(Operation::getProcessId)
                         .collect(Collectors.toList());
    }

    private List<Operation> findConflictingOperations(String mtaId, String namespace, String spaceId) {
        return operationService.createQuery()
                               .mtaId(mtaId)
                               .namespace(namespace)
                               .spaceId(spaceId)
                               .acquiredLock(true)
                               .list();
    }

    public void releaseLock(String processInstanceId, Operation.State state) {
        Operation operation = getOperationByProcessId(processInstanceId);
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
                                         operation.getMtaId(), operation.getSpaceId()));
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .hasAcquiredLock(false)
                                      .state(state)
                                      .endedAt(ZonedDateTime.now())
                                      .build();
        operationService.update(operation, operation);
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
    }

    private Operation getOperationByProcessId(String processId) {
        return operationService.createQuery()
                               .processId(processId)
                               .singleResult();
    }
}
