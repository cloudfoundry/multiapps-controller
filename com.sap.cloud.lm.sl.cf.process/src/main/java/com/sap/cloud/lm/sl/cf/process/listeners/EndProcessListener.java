package com.sap.cloud.lm.sl.cf.process.listeners;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.analytics.AnalyticsCollector;
import com.sap.cloud.lm.sl.cf.process.analytics.model.AnalyticsData;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("endProcessListener")
public class EndProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndProcessListener.class);

    @Inject
    private AnalyticsCollector analytics;

    @Inject
    @Named("fileService")
    private AbstractFileService fileService;

    @Inject
    private OperationDao operationDao;

    @Inject
    protected CloudFoundryClientProvider clientProvider;

    @Inject
    private Configuration configuration;

    @Override
    protected void writeLogs(DelegateExecution context) throws IOException, FileStorageException {
        processLoggerProviderFactory.append(context, getLogDir());
    }

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException, FileStorageException {
        if (configuration.shouldGatherUsageStatistics()) {
            collectAnalytics(context);
        }
        // TODO send the generated statistics to statistics server

        deleteDeploymentFiles(context);

        removeClientForProcess(context);

        new ProcessConflictPreventer(operationDao).attemptToReleaseLock(StepsUtil.getCorrelationId(context));

        setOperationInFinishedState(StepsUtil.getCorrelationId(context));
    }

    private AnalyticsData collectAnalytics(DelegateExecution context) throws SLException {
        AnalyticsData model = analytics.collectAttributes(context);
        model.setProcessFinalState(State.FINISHED);
        LOGGER.info(JsonUtil.toJson(model, true));
        return model;
    }

    protected void setOperationInFinishedState(String processInstanceId) throws NotFoundException {
        Operation operation = operationDao.findRequired(processInstanceId);
        operation.setState(State.FINISHED);
        operationDao.merge(operation);
    }

    private void removeClientForProcess(DelegateExecution context) throws SLException {
        String user = StepsUtil.determineCurrentUser(context, getStepLogger());
        String space = StepsUtil.getSpace(context);
        String org = StepsUtil.getOrg(context);

        clientProvider.releaseClient(user, org, space);
    }

    protected void deleteDeploymentFiles(DelegateExecution context) throws SLException, FileStorageException {
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

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}
