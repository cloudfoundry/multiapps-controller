package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessConflictPreventer.class);

    private final OperationDao dao;

    public ProcessConflictPreventer(OperationDao dao) {
        this.dao = dao;
    }

    public synchronized void acquireLock(String mtaId, String spaceId, String processId) {
        LOGGER.info(format(Messages.ACQUIRING_LOCK, processId, mtaId));

        validateNoConflictingOperationsExist(mtaId, spaceId);
        Operation currentOperation = dao.findRequired(processId);
        currentOperation.setMtaId(mtaId);
        currentOperation.acquiredLock(true);
        dao.update(currentOperation);

        LOGGER.info(format(Messages.ACQUIRED_LOCK, processId, mtaId));
    }

    private void validateNoConflictingOperationsExist(String mtaId, String spaceId) {
        List<Operation> conflictingOperations = findConflictingOperations(mtaId, spaceId);
        if (conflictingOperations.size() == 1) {
            Operation conflictingOperation = conflictingOperations.get(0);
            throw new SLException(Messages.CONFLICTING_PROCESS_FOUND, conflictingOperation.getProcessId(), mtaId);
        }
        if (conflictingOperations.size() >= 2) {
            List<String> operationIds = getOperationIds(conflictingOperations);
            throw new SLException(Messages.MULTIPLE_OPERATIONS_WITH_LOCK_FOUND, mtaId, spaceId, operationIds);
        }
    }

    private List<String> getOperationIds(List<Operation> operations) {
        return operations.stream()
            .map(Operation::getProcessId)
            .collect(Collectors.toList());
    }

    private List<Operation> findConflictingOperations(String mtaId, String spaceId) {
        OperationFilter filter = new OperationFilter.Builder().mtaId(mtaId)
            .spaceId(spaceId)
            .withAcquiredLock()
            .build();
        return dao.find(filter);
    }

}
