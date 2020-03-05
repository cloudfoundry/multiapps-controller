package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;

import com.sap.cloud.lm.sl.cf.process.Messages;

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
                ErrorDetails errorDetails = upload.getErrorDetails();
                execution.getStepLogger()
                         .error(Messages.ERROR_UPLOADING_APP_0_STATUS_1_DESCRIPTION_2, app.getName(), upload.getStatus(),
                                errorDetails.getDescription());
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
