package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollUploadAppStatusStep extends AsyncStepOperation {

    @Override
    public ExecutionStatus executeOperation(ExecutionWrapper execution) throws SLException {
        execution.getStepLogger().logActivitiTask();

        // Get the next cloud application from the context
        final CloudApplication app = StepsUtil.getApp(execution.getContext());

        try {
            execution.getStepLogger().info(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());
            String status = execution.getContextExtensionDao()
                .find(execution.getContext().getProcessInstanceId(), "uploadState")
                .getValue();
            if (ExecutionStatus.FAILED.name().equalsIgnoreCase(status)) {
                String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                execution.getStepLogger().error(message);
                StepsUtil.setStepPhase(execution, StepPhase.RETRY);
                return ExecutionStatus.FAILED;
            }

            ClientExtensions clientExtensions = execution.getClientExtensions();
            if (clientExtensions == null && ExecutionStatus.SUCCESS.name().equalsIgnoreCase(status)) {
                return ExecutionStatus.SUCCESS;
            }

            String uploadToken = StepsUtil.getUploadToken(execution.getContext());
            if (uploadToken == null) {
                String message = format(Messages.APP_UPLOAD_TIMED_OUT, app.getName());
                execution.getStepLogger().error(message);
                StepsUtil.setStepPhase(execution, StepPhase.RETRY);
                return ExecutionStatus.FAILED;
            }

            UploadInfo uploadInfo = clientExtensions.getUploadProgress(uploadToken);
            switch (uploadInfo.getUploadJobState()) {
                case FAILED: {
                    String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                    execution.getStepLogger().error(message);
                    StepsUtil.setStepPhase(execution, StepPhase.RETRY);
                    return ExecutionStatus.FAILED;
                }
                case FINISHED: {
                    execution.getStepLogger().info(Messages.APP_UPLOADED, app.getName());
                    return ExecutionStatus.SUCCESS;
                }
                case RUNNING:
                case QUEUED:
                case UNKNOWN:
                default: {
                    return ExecutionStatus.RUNNING;
                }
            }
        } catch (SLException e) {
            execution.getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            StepsUtil.setStepPhase(execution, StepPhase.RETRY);
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            StepsUtil.setStepPhase(execution, StepPhase.RETRY);
            throw e;
        }
    }

}
