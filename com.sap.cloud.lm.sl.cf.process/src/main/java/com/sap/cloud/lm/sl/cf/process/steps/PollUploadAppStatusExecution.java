package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;

import com.sap.cloud.lm.sl.cf.process.message.Messages;

public class PollUploadAppStatusExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());

        execution.getStepLogger()
            .debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());

        CloudControllerClient client = execution.getControllerClient();

        UploadToken uploadToken = StepsUtil.getUploadToken(execution.getContext());
        Upload upload = client.getUploadStatus(uploadToken.getPackageGuid());
        switch (upload.getStatus()) {
            case FAILED:
            case EXPIRED:
                execution.getStepLogger()
                    .debug(Messages.ERROR_UPLOADING_APP_WITH_DETAILS, app.getName(), upload.getStatus(), upload.getErrorDetails()
                        .getDescription());
                execution.getStepLogger()
                    .error(Messages.ERROR_UPLOADING_APP, app.getName());
                return AsyncExecutionState.ERROR;
            case READY:
                execution.getStepLogger()
                    .debug(Messages.APP_UPLOADED, app.getName());
                return AsyncExecutionState.FINISHED;
            case PROCESSING_UPLOAD:
            case COPYING:
            case AWAITING_UPLOAD:
                return AsyncExecutionState.RUNNING;
            default:
                throw new IllegalStateException(format(Messages.UNKNOWN_UPLOAD_STATUS, upload.getStatus()));
        }
    }

    @Override
    public String getPollingErrorMessage(ExecutionWrapper execution) {
        return format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, StepsUtil.getApp(execution.getContext())
            .getName());
    }

}
