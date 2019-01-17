package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

public class PollUploadAppStatusExecution implements AsyncExecution {

    @Override
    public AsyncExecutionState execute(ExecutionWrapper execution) {
        CloudApplication app = StepsUtil.getApp(execution.getContext());    

        try {
            execution.getStepLogger()
                .debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());

            CloudControllerClient client = execution.getControllerClient();

            UploadToken uploadToken = StepsUtil.getUploadToken(execution.getContext());
            Upload upload = client.getUploadStatus(uploadToken.getToken());
            switch (upload.getStatus()) {
                case EXPIRED:
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
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            execution.getStepLogger()
                .error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
        } catch (SLException e) {
            execution.getStepLogger()
                .error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
        }
    }
    
    

}
