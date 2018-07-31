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

import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

@Component
@Order(20)
public class InactiveOperationsCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InactiveOperationsCleaner.class);

    private final OperationDao dao;
    private final ProgressMessageService progressMessageService;
    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public InactiveOperationsCleaner(OperationDao dao, ProgressMessageService progressMessageService,
        ProcessLogsPersistenceService processLogsPersistenceService) {
        this.dao = dao;
        this.progressMessageService = progressMessageService;
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    @Override
    public void execute(Date expirationTime) {
        LOGGER.info(format("Cleaning up data for finished operations started before: {0}", expirationTime));
        List<Operation> finishedOperations = getNotCleanedFinishedOperations(expirationTime);
        List<String> finishedProcessIds = getProcessIds(finishedOperations);
        LOGGER.debug(format("Data will be cleaned up for operations with process ids: {0}", finishedProcessIds));

        removeProgressMessages(finishedProcessIds);
        removeProcessLogs(finishedProcessIds);
        markOperationsAsCleanedUp(finishedOperations);
    }

    private List<Operation> getNotCleanedFinishedOperations(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inFinalState()
            .isNotCleanedUp()
            .descending()
            .build();
        return dao.find(filter);
    }

    private List<String> getProcessIds(List<Operation> operations) {
        return operations.stream()
            .map(Operation::getProcessId)
            .collect(Collectors.toList());
    }

    private void removeProgressMessages(List<String> oldFinishedOperationsIds) {
        int removedProgressMessages = progressMessageService.removeAllByProcessIds(oldFinishedOperationsIds);
        LOGGER.info(format("Deleted progress messages rows count: {0}", removedProgressMessages));
    }

    private void removeProcessLogs(List<String> oldFinishedOperationsIds) {
        int removedProcessLogs = processLogsPersistenceService.deleteAllByNamespaces(oldFinishedOperationsIds);
        LOGGER.info(format("Deleted process logs rows count: {0}", removedProcessLogs));
    }

    private void markOperationsAsCleanedUp(List<Operation> finishedOperations) {
        for (Operation finishedOperation : finishedOperations) {
            finishedOperation.setCleanedUp(true);
            dao.merge(finishedOperation);
        }
    }

}
