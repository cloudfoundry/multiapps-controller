package org.cloudfoundry.multiapps.controller.process.steps;

import static java.text.MessageFormat.format;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ErrorDetails;
import com.sap.cloudfoundry.client.facade.domain.Upload;

public class PollUploadAppStatusExecution implements AsyncExecution {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollUploadAppStatusExecution.class);

    @Override
    public AsyncExecutionState execute(ProcessContext context) {
        CloudApplication application = context.getVariable(Variables.APP_TO_PROCESS);

        context.getStepLogger()
               .debug(Messages.CHECKING_UPLOAD_APP_STATUS, application.getName());

        CloudControllerClient client = context.getControllerClient();

        CloudPackage cloudPackage = context.getVariable(Variables.CLOUD_PACKAGE);
        Upload upload = getUploadStatus(client, cloudPackage.getGuid(), application.getName());
        LOGGER.info(format(Messages.UPLOAD_STATUS_0, upload));
        switch (upload.getStatus()) {
            case FAILED:
            case EXPIRED:
                ErrorDetails errorDetails = upload.getErrorDetails();
                context.getStepLogger()
                       .error(Messages.ERROR_UPLOADING_APP_0_STATUS_1_DESCRIPTION_2, application.getName(), upload.getStatus(),
                              errorDetails.getDescription());
                return AsyncExecutionState.ERROR;
            case READY:
                context.getStepLogger()
                       .debug(Messages.APP_UPLOADED, application.getName());
                return AsyncExecutionState.FINISHED;
            case PROCESSING_UPLOAD:
            case COPYING:
            case AWAITING_UPLOAD:
                return AsyncExecutionState.RUNNING;
            default:
                throw new IllegalStateException(format(Messages.UNKNOWN_UPLOAD_STATUS, upload.getStatus()));
        }
    }

    private Upload getUploadStatus(CloudControllerClient client, UUID packageGuid, String applicationName) {
        try {
            return client.getUploadStatus(packageGuid);
        } catch (CloudOperationException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                LOGGER.error(e.getMessage(), e);
                verifyApplicationExists(client, applicationName);
            }
            throw e;
        }
    }

    private void verifyApplicationExists(CloudControllerClient client, String applicationName) {
        LOGGER.debug(format(Messages.VERIFYING_APPLICATION_0_EXISTS, applicationName));
        client.getApplication(applicationName);
    }

    @Override
    public String getPollingErrorMessage(ProcessContext context) {
        return format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, context.getVariable(Variables.APP_TO_PROCESS)
                                                                        .getName());
    }

}
