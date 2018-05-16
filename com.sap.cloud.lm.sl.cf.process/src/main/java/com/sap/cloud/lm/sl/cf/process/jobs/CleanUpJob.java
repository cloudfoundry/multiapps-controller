package com.sap.cloud.lm.sl.cf.process.jobs;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.sap.cloud.lm.sl.cf.core.configuration.Environment;
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

        LOGGER.info("Cleanup Job started by application instance: " + getInstanceIndex() + " at: " + Instant.now()
            .toString());

        Date expirationTime = getExpirationTime();
        executeSafely(() -> {
            abortOldOperationsInStateError(expirationTime);
        });

        executeSafely(() -> {
            cleanUpFinishedOperationsData(expirationTime);
        });

        executeSafely(() -> {
            removeActivitiHistoricData(expirationTime);
        });

        executeSafely(() -> {
            removeOldOrphanedFiles(expirationTime);
        });

        executeSafely(() -> {
            removeExpiredTokens();
        });

        executeSafely(() -> {
            executeDataTerminationJob();
        });

        LOGGER.info("Cleanup Job finished at: " + Instant.now()
            .toString());
    }

    private String getInstanceIndex() {
        Environment env = new Environment();
        return env.getString("CF_INSTANCE_INDEX");
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

    private void abortOldOperationsInStateError(Date expirationTime) {
        LOGGER.info("Aborting operations started before: " + expirationTime.toString());
        List<Operation> activeOperationsInStateError = getActiveOperationsInStateError(expirationTime);
        List<String> operationsInErrorIds = getProcessIds(activeOperationsInStateError);
        executeAbortOperationAction(operationsInErrorIds);
        LOGGER.info("Aborted operations count : " + operationsInErrorIds.size());
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
        activitiFacade.getHistoricProcessInstancesFinishedAndStartedBefore(expirationTime)
            .stream()
            .forEach(historicProcessInstance -> activitiFacade.deleteHistoricProcessInstance(historicProcessInstance.getId()));
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

    private List<Operation> getActiveOperationsInStateError(Date expirationTime) throws SLException {
        OperationFilter filter = new OperationFilter.Builder().startedBefore(expirationTime)
            .inNonFinalState()
            .descending()
            .build();
        return operationsHelper.findOperations(filter, Arrays.asList(State.ERROR));
    }

    private List<String> getProcessIds(List<Operation> operations) {
        return operations.stream()
            .map(operationInError -> operationInError.getProcessId())
            .collect(Collectors.toList());
    }

    private void executeAbortOperationAction(List<String> processIds) {
        ActivitiAction abortAction = ActivitiActionFactory.getAction("abort", activitiFacade, null);
        for (String processId : processIds) {
            abortAction.executeAction(processId);
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
        Map<String, List<String>> spaceToFileIds = new HashMap<String, List<String>>();
        for (String space : spaceToProcessIds.keySet()) {
            if (spaceToProcessIds.get(space) == null) {
                continue;
            }
            List<String> fileIds = new ArrayList<String>();
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
        Map<String, List<String>> spaceToFileChunks = new HashMap<String, List<String>>();
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
