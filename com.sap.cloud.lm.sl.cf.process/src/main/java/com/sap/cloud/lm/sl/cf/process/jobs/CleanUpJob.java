package com.sap.cloud.lm.sl.cf.process.jobs;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.history.HistoricVariableInstance;
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
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.OperationsHelper;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.FileEntry;
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
    private OperationsHelper operationsHelper;
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

        LOGGER.info("Cleanup Job started by application instance: " + configuration.getApplicationInstanceIndex() + " at: " + Instant.now()
            .toString());

        Date expirationTime = getExpirationTime();
        executeSafely(() -> abortOldOperationsInActiveState(expirationTime));

        executeSafely(() -> cleanUpFinishedOperationsData(expirationTime));

        executeSafely(() -> removeActivitiHistoricData(expirationTime));

        executeSafely(() -> removeOldOrphanedFiles(expirationTime));

        executeSafely(() -> removeExpiredTokens());

        executeSafely(() -> executeDataTerminationJob());

        LOGGER.info("Cleanup Job finished at: " + Instant.now()
            .toString());
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
        LOGGER.info("Will perform clean up for data stored before: " + cleanUpTimestamp.toString());
        return cleanUpTimestamp;
    }

    private void abortOldOperationsInActiveState(Date expirationTime) {
        LOGGER.info("Aborting operations started before: " + expirationTime.toString());
        List<Operation> activeOperations = getOperationsInActiveState(expirationTime);
        List<String> activeOperationsIds = getProcessIds(activeOperations);
        LOGGER.info("Operations to be aborted count: " + activeOperationsIds.size());
        int abortedOperationsCount = tryToAbortOperations(activeOperationsIds);
        LOGGER.info("Aborted operations count: " + abortedOperationsCount);
    }

    private void cleanUpFinishedOperationsData(Date expirationTime) {
        LOGGER.info("Cleaning up data for finished operations started before: " + expirationTime.toString());
        List<Operation> finishedOperations = getNotCleanedFinishedOperations(expirationTime);
        List<String> finishedProcessIds = getProcessIds(finishedOperations);
        LOGGER.debug("Data will be cleaned up for operations with process ids: " + finishedProcessIds);

        removeProgressMessages(finishedProcessIds);
        removeProcessLogs(finishedProcessIds);
        removeFilesForOperations(finishedOperations);
        markOperationsAsCleanedUp(finishedOperations);
    }

    private void removeOldOrphanedFiles(Date expirationTime) {
        LOGGER.info("Deleting old orphaned MTA files modified before: " + expirationTime.toString());
        try {
            List<String> appArchiveIds = activitiFacade.getHistoricVariableInstancesByVariableName(Constants.PARAM_APP_ARCHIVE_ID)
                .stream()
                .map(appArchiveId -> (String) appArchiveId.getValue())
                .collect(Collectors.toList());

            List<FileEntry> orphanedFileEntries = fileService.listByModificationTime(expirationTime)
                .stream()
                .filter(oldFileEntry -> !appArchiveIds.contains(oldFileEntry.getId()))
                .collect(Collectors.toList());

            Map<String, List<String>> spaceToFileIds = orphanedFileEntries.stream()
                .collect(Collectors.groupingBy(FileEntry::getSpace, Collectors.mapping(FileEntry::getId, Collectors.toList())));

            int removedOldOrhpanedFiles = fileService.deleteAllByFileIds(spaceToFileIds);
            LOGGER.info("Deleted old orphaned MTA files: " + removedOldOrhpanedFiles);
        } catch (FileStorageException e) {
            throw new SLException(e, "Deletion of old orphaned MTA files failed");
        }
    }

    private void removeActivitiHistoricData(Date expirationTime) {
        Set<String> historicProcessIds = new HashSet<String>();
        activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(expirationTime)
            .stream()
            .forEach(historicProcessInstance -> addProcessAndSubProcesses(historicProcessIds, historicProcessInstance.getId()));
        LOGGER.info("Historic Processes marked for deletion count: " + historicProcessIds.size());
        historicProcessIds.stream()
            .forEach(historicProcessId -> tryToDeleteHistoricProcessInstance(historicProcessId));
    }

    private void addProcessAndSubProcesses(Set<String> historicProcessIds, String historicProcessId) {
        historicProcessIds.add(historicProcessId);
        List<String> subProcessIds = activitiFacade.getHistoricSubProcessIds(historicProcessId);
        historicProcessIds.addAll(subProcessIds);
    }

    private boolean tryToDeleteHistoricProcessInstance(String processId) {
        try {
            LOGGER.info("Deleting Historic Process with id: " + processId);
            activitiFacade.deleteHistoricProcessInstance(processId);
            LOGGER.info("Successfully deleted Historic Process with id: " + processId);
            return true;
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format("Error when trying to delete historic process with id [{0}]: {1}", processId, e.getMessage()),
                e);
            return false;
        }
    }

    private void removeExpiredTokens() {
        Collection<OAuth2AccessToken> allTokens = tokenStore.findTokensByClientId(SecurityUtil.CLIENT_ID);
        if (CollectionUtils.isEmpty(allTokens)) {
            return;
        }
        LOGGER.info("Removing expired tokens");
        int removedTokens = 0;
        for (OAuth2AccessToken token : allTokens) {
            if (token.isExpired()) {
                tokenStore.removeAccessToken(token);
                removedTokens++;
            }
        }
        LOGGER.info("Removed expired tokens count: " + removedTokens);
    }

    private List<Operation> getNotCleanedFinishedOperations(Date expirationTime) {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inFinalState()
            .isNotCleanedUp()
            .descending()
            .build();
        return dao.find(filter);
    }

    private List<Operation> getOperationsInActiveState(Date expirationTime) throws SLException {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inNonFinalState()
            .descending()
            .build();
        return operationsHelper.findOperations(filter, State.getActiveStates());
    }

    private List<String> getProcessIds(List<Operation> operations) {
        return operations.stream()
            .map(operation -> operation.getProcessId())
            .collect(Collectors.toList());
    }

    private int tryToAbortOperations(List<String> processIds) {
        int count = 0;
        LOGGER.debug("Aborting operations: " + processIds);
        for (String processId : processIds) {
            if (tryToAbortOperation(processId)) {
                count++;
            }
        }
        return count;
    }

    private boolean tryToAbortOperation(String processId) {
        try {
            ActivitiAction abortAction = ActivitiActionFactory.getAction("abort", activitiFacade, null);
            LOGGER.info("Aborting operation with id: " + processId);
            abortAction.executeAction(processId);
            LOGGER.info("Successfully aborted operation with id: " + processId);
            return true;
        } catch (Exception e) {
            LOGGER.error(MessageFormat.format("Error when trying to abort operation with id [{0}]: {1}", processId, e.getMessage()), e);
            return false;
        }
    }

    private void removeProgressMessages(List<String> oldFinishedOperationsIds) {
        int removedProgressMessages = progressMessageService.removeAllByProcessIds(oldFinishedOperationsIds);
        LOGGER.info("Deleted progress messages rows count: " + removedProgressMessages);
    }

    private void removeProcessLogs(List<String> oldFinishedOperationsIds) {
        int removedProcessLogs = processLogsPersistenceService.deleteAllByNamespaces(oldFinishedOperationsIds);
        LOGGER.info("Deleted process logs rows count: " + removedProcessLogs);
    }

    private Map<String, String> mapProcessIdToArchive() {
        List<HistoricVariableInstance> appArchiveIds = activitiFacade
            .getHistoricVariableInstancesByVariableName(Constants.PARAM_APP_ARCHIVE_ID);
        Map<String, String> processIdToAppArchiveId = new HashMap<>();
        for (HistoricVariableInstance appArchiveId : appArchiveIds) {
            processIdToAppArchiveId.put(appArchiveId.getProcessInstanceId(), (String) appArchiveId.getValue());
        }
        return processIdToAppArchiveId;
    }

    private Map<String, List<String>> getSpaceToProcessIds(List<Operation> operations) {
        return operations.stream()
            .collect(Collectors.groupingBy(Operation::getSpaceId, Collectors.mapping(Operation::getProcessId, Collectors.toList())));
    }

    private Map<String, List<String>> getSpaceToFileIds(Map<String, List<String>> spaceToProcessIds,
        Map<String, String> processIdToAppArchiveId) {
        if (spaceToProcessIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> spaceToFileIds = new HashMap<>();
        for (String space : spaceToProcessIds.keySet()) {
            if (spaceToProcessIds.get(space) == null) {
                continue;
            }
            List<String> fileIds = new ArrayList<>();
            for (String processId : spaceToProcessIds.get(space)) {
                if (processIdToAppArchiveId.containsKey(processId)) {
                    fileIds.add(processIdToAppArchiveId.get(processId));
                }
            }
            spaceToFileIds.put(space, fileIds);
        }
        return spaceToFileIds;
    }

    private void removeFilesForOperations(List<Operation> operations) {
        Map<String, String> processIdToAppArchiveId = mapProcessIdToArchive();
        Map<String, List<String>> spaceToProcessIds = getSpaceToProcessIds(operations);
        Map<String, List<String>> spaceToFileIds = getSpaceToFileIds(spaceToProcessIds, processIdToAppArchiveId);

        try {
            Map<String, List<String>> spaceToFileChunkIds = splitAllFilesInChunks(spaceToFileIds);
            int removedOldFiles = fileService.deleteAllByFileIds(spaceToFileChunkIds);
            LOGGER.info("Deleted MTA files: " + removedOldFiles);
        } catch (FileStorageException e) {
            throw new SLException(e);
        }
    }

    public Map<String, List<String>> splitAllFilesInChunks(Map<String, List<String>> spaceToFileIds) {
        Map<String, List<String>> spaceToFileChunks = new HashMap<>();
        for (String space : spaceToFileIds.keySet()) {
            List<String> fileChunksInSpace = NameUtil.splitFilesIds(spaceToFileIds.get(space));
            spaceToFileChunks.put(space, fileChunksInSpace);
        }
        return spaceToFileChunks;
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
            LOGGER.error(MessageFormat.format(LOG_ERROR_MESSAGE_PATTERN, e.getMessage()), e);
        }
    }
}
