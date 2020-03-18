package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;

import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PollUploadAppStatusExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        CloudApplication app = execution.getVariable(Variables.APP_TO_PROCESS);

        execution.getStepLogger()
                 .debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());

        CloudControllerClient client = execution.getControllerClient();

        UploadToken uploadToken = execution.getVariable(Variables.UPLOAD_TOKEN);
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
        return format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, execution.getVariable(Variables.APP_TO_PROCESS)
                                                                          .getName());
    }

}
