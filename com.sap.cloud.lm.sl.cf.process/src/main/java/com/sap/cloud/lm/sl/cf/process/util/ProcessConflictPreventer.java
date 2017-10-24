package com.sap.cloud.lm.sl.cf.process.util;

import static java.text.MessageFormat.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessConflictPreventer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessConflictPreventer.class);

    private final OperationDao dao;

    public ProcessConflictPreventer(OperationDao dao) {
        this.dao = dao;
    }

    public synchronized void attemptToAcquireLock(String mtaId, String spaceId, String processId) throws SLException {
        LOGGER.info(format(Messages.ACQUIRING_LOCK, processId, mtaId));

        Operation ongoingProcess = dao.findProcessWithLock(mtaId, spaceId);
        if (ongoingProcess != null) {
            throw new SLException(Messages.CONFLICTING_PROCESS_FOUND, ongoingProcess.getProcessId(), mtaId);
        }
        Operation currentOngoingProcess = dao.findRequired(processId);
        currentOngoingProcess.setMtaId(mtaId);
        currentOngoingProcess.acquiredLock(true);
        dao.merge(currentOngoingProcess);

        LOGGER.info(format(Messages.ACQUIRED_LOCK, processId, mtaId));

    }

    public synchronized void attemptToReleaseLock(String processId) throws SLException {
        Operation ongoingProcess = dao.findRequired(processId);
        String mtaId = ongoingProcess.getMtaId();

        LOGGER.info(format(Messages.RELEASING_LOCK, processId, mtaId));
        ongoingProcess.acquiredLock(false);
        dao.merge(ongoingProcess);
        LOGGER.info(format(Messages.RELEASED_LOCK, processId, mtaId));
    }

}
