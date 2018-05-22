package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Upload;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollUploadAppStatusExecution extends AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) throws SLException {
        execution.getStepLogger().logActivitiTask();

        // Get the next cloud application from the context
        final CloudApplication app = StepsUtil.getApp(execution.getContext());

        try {
            execution.getStepLogger()
                .debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());
            String status = execution.getContextExtensionDao()
                .find(execution.getContext().getProcessInstanceId(), Constants.VAR_UPLOAD_STATE)
                .getValue();
            if (AsyncExecutionState.ERROR.name().equalsIgnoreCase(status)) {
                String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                execution.getStepLogger().error(message);
                return AsyncExecutionState.ERROR;
            }

            String uploadToken = StepsUtil.getUploadToken(execution);
            if (uploadToken == null) {
                String message = format(Messages.APP_UPLOAD_TIMED_OUT, app.getName());
                execution.getStepLogger().error(message);
                return AsyncExecutionState.ERROR;
            }

            CloudFoundryOperations client = execution.getCloudFoundryClient();

            Upload upload = client.getUploadStatus(uploadToken);
            switch (upload.getStatus()) {
                case FAILED:
                    execution.getStepLogger()
                        .error(Messages.ERROR_UPLOADING_APP, app.getName());
                    return AsyncExecutionState.ERROR;
                case FINISHED:
                    execution.getStepLogger()
                        .info(Messages.APP_UPLOADED, app.getName());
                    return AsyncExecutionState.FINISHED;
                case RUNNING:
                    return AsyncExecutionState.RUNNING;
                case QUEUED:
                    return AsyncExecutionState.RUNNING;
                default:
                    throw new IllegalStateException(format(Messages.UNKNOWN_UPLOAD_STATUS, upload.getStatus()));
            }
        } catch (SLException e) {
            execution.getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw cfe;
        }
    }

}
