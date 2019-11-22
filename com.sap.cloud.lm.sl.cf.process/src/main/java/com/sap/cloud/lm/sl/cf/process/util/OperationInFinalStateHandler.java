package com.sap.cloud.lm.sl.cf.process.util;

import java.text.MessageFormat;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.util.RestUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.LoggingUtil;
import com.sap.cloud.lm.sl.cf.core.util.SafeExecutor;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableOperation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation.State;

@Named
public class OperationInFinalStateHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationInFinalStateHandler.class);

    public static Logger getLogger() {
        return LOGGER;
    }

    @Inject
    private OperationService operationService;
    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private FileService fileService;
    @Inject
    private ApplicationConfiguration configuration;
    @Inject
    private CollectedDataSender dataSender;
    @Inject
    private HistoricOperationEventPersister historicOperationEventPersister;
    private final SafeExecutor safeExecutor = new SafeExecutor();

    public void handle(DelegateExecution context, Operation.State state) {
        LoggingUtil.logWithCorrelationId(StepsUtil.getCorrelationId(context), () -> handleInternal(context, state));
    }

    private void handleInternal(DelegateExecution context, Operation.State state) {
        safeExecutor.execute(() -> {
            if (configuration.shouldGatherUsageStatistics()) {
                sendStatistics(context, state);
            }
        });
        safeExecutor.execute(() -> deleteDeploymentFiles(context));
        safeExecutor.execute(() -> deleteCloudControllerClientForProcess(context));
        safeExecutor.execute(() -> setOperationState(StepsUtil.getCorrelationId(context), state));
    }

    protected void sendStatistics(DelegateExecution context, Operation.State state) {
        RestTemplate restTemplate = new RestUtil().createRestTemplate(null, false);
        AnalyticsData collectedData = dataSender.collectAnalyticsData(context, state);
        dataSender.sendCollectedData(restTemplate, dataSender.convertCollectedAnalyticsDataToXml(context, collectedData));
    }

    protected void deleteDeploymentFiles(DelegateExecution context) throws FileStorageException {
        if (shouldKeepFiles((Boolean) context.getVariable(Constants.PARAM_KEEP_FILES))) {
            return;
        }

        String extensionDescriptorFileIds = (String) context.getVariable(Constants.PARAM_EXT_DESCRIPTOR_FILE_ID);
        String appArchiveFileIds = (String) context.getVariable(Constants.PARAM_APP_ARCHIVE_ID);

        FileSweeper fileSweeper = new FileSweeper(StepsUtil.getSpaceId(context), fileService);
        fileSweeper.sweep(extensionDescriptorFileIds);
        fileSweeper.sweep(appArchiveFileIds);
    }

    private boolean shouldKeepFiles(Boolean keepFiles) {
        return keepFiles != null && keepFiles;
    }

    private void deleteCloudControllerClientForProcess(DelegateExecution context) {
        String user = StepsUtil.determineCurrentUser(context);
        String space = StepsUtil.getSpace(context);
        String org = StepsUtil.getOrg(context);
        String spaceId = StepsUtil.getSpaceId(context);

        clientProvider.releaseClient(user, org, space);
        clientProvider.releaseClient(user, spaceId);
    }

    protected void setOperationState(String processInstanceId, Operation.State state) {
        Operation operation = operationService.createQuery()
                                              .processId(processInstanceId)
                                              .singleResult();
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
                                         operation.getMtaId(), operation.getSpaceId()));
        operation = ImmutableOperation.builder()
                                      .from(operation)
                                      .state(state)
                                      .endedAt(ZonedDateTime.now())
                                      .hasAcquiredLock(false)
                                      .build();
        operationService.update(operation.getProcessId(), operation);
        historicOperationEventPersister.add(processInstanceId, toEventType(state));
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
    }

    private EventType toEventType(State state) {
        return state == Operation.State.FINISHED ? EventType.FINISHED : EventType.ABORTED;
    }

}
