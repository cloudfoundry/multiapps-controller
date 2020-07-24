package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

public class PollUploadAppStatusExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplication app = context.getVariable(Variables.APP_TO_PROCESS);

        context.getStepLogger()
               .debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());

        CloudControllerClient client = context.getControllerClient();

        UploadToken uploadToken = context.getVariable(Variables.UPLOAD_TOKEN);
        Upload upload = client.getUploadStatus(uploadToken.getPackageGuid());
        switch (upload.getStatus()) {
            case FAILED:
            case EXPIRED:
                ErrorDetails errorDetails = upload.getErrorDetails();
                context.getStepLogger()
                       .error(Messages.ERROR_UPLOADING_APP_0_STATUS_1_DESCRIPTION_2, app.getName(), upload.getStatus(),
                              errorDetails.getDescription());
                return AsyncExecutionState.ERROR;
            case READY:
                context.getStepLogger()
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
    public String getPollingErrorMessage(ProcessContext context) {
        return format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, context.getVariable(Variables.APP_TO_PROCESS)
                                                                        .getName());
    }

}
