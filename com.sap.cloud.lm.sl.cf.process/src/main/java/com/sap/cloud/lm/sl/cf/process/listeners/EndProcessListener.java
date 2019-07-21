package com.sap.cloud.lm.sl.cf.process.listeners;

import java.text.MessageFormat;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.util.RestUtil;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.CollectedDataSender;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component("endProcessListener")
public class EndProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndProcessListener.class);

    @Inject
    private CollectedDataSender dataSender;

    @Inject
    @Named("fileService")
    private FileService fileService;

    @Inject
    private OperationDao operationDao;

    @Inject
    protected CloudControllerClientProvider clientProvider;

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected void notifyInternal(DelegateExecution context) throws FileStorageException {
        if (configuration.shouldGatherUsageStatistics()) {
            sendStatistics(context);
        }

        deleteDeploymentFiles(context);

        removeClientForProcess(context);

        setOperationInFinishedState(StepsUtil.getCorrelationId(context));
    }

    protected void setOperationInFinishedState(String processInstanceId) {
        Operation operation = operationDao.findRequired(processInstanceId);
        LOGGER.info(MessageFormat.format(Messages.PROCESS_0_RELEASING_LOCK_FOR_MTA_1_IN_SPACE_2, operation.getProcessId(),
            operation.getMtaId(), operation.getSpaceId()));
        operation.setState(State.FINISHED);
        operation.setEndedAt(ZonedDateTime.now());
        operation.setAcquiredLock(false);
        operationDao.update(operation);
        LOGGER.debug(MessageFormat.format(Messages.PROCESS_0_RELEASED_LOCK, operation.getProcessId()));
    }

    private void removeClientForProcess(DelegateExecution context) {
        String user = StepsUtil.determineCurrentUser(context, getStepLogger());
        String space = StepsUtil.getSpace(context);
        String org = StepsUtil.getOrg(context);
        String spaceID = StepsUtil.getSpaceId(context);

        clientProvider.releaseClient(user, org, space);
        clientProvider.releaseClient(user, spaceID);
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

    protected void sendStatistics(DelegateExecution context) {
        RestTemplate restTemplate = new RestUtil().createRestTemplate(null, false);
        AnalyticsData collectedData = dataSender.collectAnalyticsData(context, State.FINISHED);
        dataSender.sendCollectedData(restTemplate, dataSender.convertCollectedAnalyticsDataToXml(context, collectedData));
    }

    private boolean shouldKeepFiles(Boolean keepFiles) {
        return keepFiles != null && keepFiles;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
