package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.activiti.engine.history.HistoricProcessInstance;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiAction;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiActionFactory;
import com.sap.cloud.lm.sl.cf.core.activiti.ActivitiFacade;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.security.data.termination.DataTerminationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

@DisallowConcurrentExecution
public class CleanUpJob implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(CleanUpJob.class);
    private static final String LOG_ERROR_MESSAGE_PATTERN = "[Clean up Job] Error during cleaning up: {0}";

    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private OperationDao dao;
    @Inject
    private ActivitiFacade activitiFacade;
    @Inject
    private DataTerminationService dataTerminationService;
    @Inject
    private AbstractFileService fileService;
    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @Autowired
    @Qualifier("tokenStore")
    TokenStore tokenStore;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        LOGGER.info(format("Cleanup Job started by application instance: {0}  at: {1}", configuration.getApplicationInstanceIndex(),
            Instant.now()));

        Date expirationTime = getExpirationTime();
        executeSafely(() -> abortOldOperationsInActiveState(expirationTime));

        executeSafely(() -> cleanUpFinishedOperationsData(expirationTime));

        executeSafely(() -> removeActivitiHistoricData(expirationTime));

        executeSafely(() -> removeOldFiles(expirationTime));

        executeSafely(() -> removeExpiredTokens());

        executeSafely(() -> executeDataTerminationJob());

        LOGGER.info(format("Cleanup Job finished at: {0}", Instant.now()));
    }

    private void executeDataTerminationJob() {
        if (configuration.getPlatformType() != PlatformType.CF) {
            return;
        }
        LOGGER.warn("Termination of user data started.");
        dataTerminationService.deleteOrphanUserData();
        LOGGER.warn("Termination of user data finished.");
    }

    private Date getExpirationTime() {
        long maxTtlForOldData = configuration.getMaxTtlForOldData();
        Date cleanUpTimestamp = Date.from(Instant.now()
            .minusSeconds(maxTtlForOldData));
        LOGGER.info(format("Will perform clean up for data stored before: {0}", cleanUpTimestamp));
        return cleanUpTimestamp;
    }

    private void abortOldOperationsInActiveState(Date expirationTime) {
        LOGGER.info(format("Aborting operations started before: {0}", expirationTime));
        List<Operation> activeOperations = getOperationsInActiveState(expirationTime);
        List<String> activeOperationsIds = getProcessIds(activeOperations);
        LOGGER.info(format("Operations to be aborted count: {0}", activeOperationsIds.size()));
        long abortedOperationsCount = tryToAbortOperations(activeOperationsIds);
        LOGGER.info(format("Aborted operations count: {0}", abortedOperationsCount));
    }

    private void cleanUpFinishedOperationsData(Date expirationTime) {
        LOGGER.info(format("Cleaning up data for finished operations started before: {0}", expirationTime));
        List<Operation> finishedOperations = getNotCleanedFinishedOperations(expirationTime);
        List<String> finishedProcessIds = getProcessIds(finishedOperations);
        LOGGER.debug(format("Data will be cleaned up for operations with process ids: {0}", finishedProcessIds));

        removeProgressMessages(finishedProcessIds);
        removeProcessLogs(finishedProcessIds);
        markOperationsAsCleanedUp(finishedOperations);
    }

    private void removeOldFiles(Date expirationTime) {
        LOGGER.info(format("Deleting old MTA files modified before: {0}", expirationTime));
        try {
            int removedOldFilesCount = fileService.deleteByModificationTime(expirationTime);
            LOGGER.info(format("Deleted old MTA files: {0}", removedOldFilesCount));
        } catch (FileStorageException e) {
            throw new SLException(e, "Deletion of old MTA files failed");
        }
    }

    private void removeActivitiHistoricData(Date expirationTime) {
        Set<String> historicProcessIds = activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(expirationTime)
            .stream()
            .flatMap(this::getProcessAndSubProcessIdsStream)
            .collect(Collectors.<String> toSet());

        LOGGER.info(format("Historic Processes marked for deletion count: {0}", historicProcessIds.size()));
        historicProcessIds.stream()
            .forEach(this::tryToDeleteHistoricProcessInstance);
    }

    private Stream<String> getProcessAndSubProcessIdsStream(HistoricProcessInstance historicProcessInstance) {
        List<String> processIds = activitiFacade.getHistoricSubProcessIds(historicProcessInstance.getId());
        processIds.add(historicProcessInstance.getId());
        return processIds.stream();
    }

    private boolean tryToDeleteHistoricProcessInstance(String processId) {
        try {
            LOGGER.info(format("Deleting Historic Process with id: {0}", processId));
            activitiFacade.deleteHistoricProcessInstance(processId);
            LOGGER.info(format("Successfully deleted Historic Process with id: {0}", processId));
            return true;
        } catch (Exception e) {
            LOGGER.error(format("Error when trying to delete historic process with id {0}: {1}", processId, e.getMessage()), e);
            return false;
        }
    }

    private void removeExpiredTokens() {
        Collection<OAuth2AccessToken> allTokens = tokenStore.findTokensByClientId(SecurityUtil.CLIENT_ID);
        if (CollectionUtils.isEmpty(allTokens)) {
            return;
        }
        LOGGER.info("Removing expired tokens");
        long removedTokens = allTokens.stream()
            .filter(OAuth2AccessToken::isExpired)
            .peek(tokenStore::removeAccessToken)
            .count();
        LOGGER.info(format("Removed expired tokens count: {0}", removedTokens));
    }

    private List<Operation> getNotCleanedFinishedOperations(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inFinalState()
            .isNotCleanedUp()
            .descending()
            .build();
        return dao.find(filter);
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

    private void executeSafely(Runnable r) {
        try {
            r.run();
        } catch (Exception e) {
            LOGGER.error(format(LOG_ERROR_MESSAGE_PATTERN, e.getMessage()), e);
        }
    }
}
