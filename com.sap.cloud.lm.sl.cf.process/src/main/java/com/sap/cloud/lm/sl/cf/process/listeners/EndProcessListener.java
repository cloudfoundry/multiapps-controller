package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
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
import com.sap.cloud.lm.sl.slp.util.AbstractProcessComponentUtil;

@Component("endProcessListener")
public class EndProcessListener extends AbstractXS2ProcessExecutionListener {

    private static final long serialVersionUID = 7588205099081551733L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndProcessListener.class);

    @Inject
    private AnalyticsCollector analytics;

    @Inject
    @Named("fileService")
    private AbstractFileService fileService;

    @Inject
    private OperationDao ongoingOperationDao;

    @Inject
    protected CloudFoundryClientProvider clientProvider;

    @Override
    public void writeLogs(DelegateExecution context) {
        AbstractProcessComponentUtil.appendLogs(context, getProcessLoggerProviderFactory());
    }

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException, FileStorageException {
        if (ConfigurationUtil.shouldGatherUsageStatistics()) {
            collectAnalytics(context);
        }
        // TODO send the generated statistics to statistics server

        deleteDeploymentFiles(context);

        removeClientForProcess(context);

        new ProcessConflictPreventer(ongoingOperationDao).attemptToReleaseLock(StepsUtil.getCorrelationId(context));

        setOngoingOperationInFinishedState(StepsUtil.getCorrelationId(context));
    }

    private AnalyticsData collectAnalytics(DelegateExecution context) throws SLException {
        AnalyticsData model = analytics.collectAttributes(context);
        model.setProcessFinalState(State.FINISHED);
        LOGGER.info(JsonUtil.toJson(model, true));
        return model;
    }

    protected void setOngoingOperationInFinishedState(String processInstanceId) throws NotFoundException {
        Operation ongoingOperation = ongoingOperationDao.findRequired(processInstanceId);
        ongoingOperation.setState(State.FINISHED);
        ongoingOperationDao.merge(ongoingOperation);
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
