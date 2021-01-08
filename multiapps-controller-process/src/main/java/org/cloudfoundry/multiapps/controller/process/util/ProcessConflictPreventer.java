package org.cloudfoundry.multiapps.controller.process.util;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                                      .cachedState(state)
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
