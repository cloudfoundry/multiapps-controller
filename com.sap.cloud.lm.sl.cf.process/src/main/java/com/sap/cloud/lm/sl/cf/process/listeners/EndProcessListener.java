package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.FileSweeper;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.FileService;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.slp.listener.AbstractSLProcessExecutionListener;
import com.sap.lmsl.slp.SlpTaskState;

@Component("endProcessListener")
public class EndProcessListener extends AbstractSLProcessExecutionListener {

    private static final long serialVersionUID = 7588205099081551733L;

    private static final Logger LOGGER = LoggerFactory.getLogger(EndProcessListener.class);

    @Inject
    private FileService fileService;

    @Inject
    private OngoingOperationDao ongoingOperationDao;

    @Inject
    protected CloudFoundryClientProvider clientProvider;

    @Override
    protected void notifyInternal(DelegateExecution context) throws SLException, FileStorageException {
        deleteDeploymentFiles(context);

        removeClientForProcess(context);

        new ProcessConflictPreventer(ongoingOperationDao).attemptToReleaseLock(context.getProcessInstanceId());

        setOngoingOperationInFinishedState(context.getProcessInstanceId());
    }

    protected void setOngoingOperationInFinishedState(String processInstanceId) throws NotFoundException {
        OngoingOperation ongoingOperation = ongoingOperationDao.find(processInstanceId);
        ongoingOperation.setFinalState(SlpTaskState.SLP_TASK_STATE_FINISHED);
        ongoingOperationDao.merge(ongoingOperation);
    }

    private void removeClientForProcess(DelegateExecution context) throws SLException {
        String user = StepsUtil.determineCurrentUser(context, LOGGER, processLoggerProviderFactory);
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

}
