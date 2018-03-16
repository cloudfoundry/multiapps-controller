package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollUploadAppStatusExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) throws SLException {
        execution.getStepLogger()
            .logActivitiTask();

        // Get the next cloud application from the context
        final CloudApplication app = StepsUtil.getApp(execution.getContext());

        try {
            execution.getStepLogger()
                .info(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());
            String status = execution.getContextExtensionDao()
                .find(execution.getContext()
                    .getProcessInstanceId(), Constants.VAR_UPLOAD_STATE)
                .getValue();
            if (AsyncExecutionState.ERROR.name()
                .equalsIgnoreCase(status)) {
                String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                execution.getStepLogger()
                    .error(message);
                return AsyncExecutionState.ERROR;
            }

            ClientExtensions clientExtensions = execution.getClientExtensions();
            if (clientExtensions == null && AsyncExecutionState.FINISHED.name()
                .equalsIgnoreCase(status)) {
                return AsyncExecutionState.FINISHED;
            }

            String uploadToken = StepsUtil.getUploadToken(execution);
            if (uploadToken == null) {
                String message = format(Messages.APP_UPLOAD_TIMED_OUT, app.getName());
                execution.getStepLogger()
                    .error(message);
                return AsyncExecutionState.ERROR;
            }

            UploadInfo uploadInfo = clientExtensions.getUploadProgress(uploadToken);
            switch (uploadInfo.getUploadJobState()) {
                case FAILED: {
                    String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                    execution.getStepLogger()
                        .error(message);
                    return AsyncExecutionState.ERROR;
                }
                case FINISHED: {
                    execution.getStepLogger()
                        .info(Messages.APP_UPLOADED, app.getName());
                    return AsyncExecutionState.FINISHED;
                }
                case RUNNING:
                case QUEUED:
                case UNKNOWN:
                default: {
                    return AsyncExecutionState.RUNNING;
                }
            }
        } catch (SLException e) {
            execution.getStepLogger()
                .error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            execution.getStepLogger()
                .error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw cfe;
        }
    }

}
