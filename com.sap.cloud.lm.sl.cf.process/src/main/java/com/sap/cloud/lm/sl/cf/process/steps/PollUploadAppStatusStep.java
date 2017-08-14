package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollUploadAppStatusStep")
public class PollUploadAppStatusStep extends AbstractXS2ProcessStepWithBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollUploadAppStatusStep.class);

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {
        logActivitiTask(context, LOGGER);

        // Get the next cloud application from the context
        final CloudApplication app = StepsUtil.getApp(context);

        try {
            debug(context, format(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName()), LOGGER);
            String status = (String) context.getVariable(getStatusVariable());
            if (ExecutionStatus.FAILED.name().equalsIgnoreCase(status)) {
                String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                error(context, message, LOGGER);
                return ExecutionStatus.LOGICAL_RETRY;
            }

            ClientExtensions clientExtensions = getClientExtensions(context, LOGGER);
            if (clientExtensions == null && ExecutionStatus.SUCCESS.name().equalsIgnoreCase(status)) {
                return ExecutionStatus.SUCCESS;
            }

            String uploadToken = StepsUtil.getUploadToken(context);
            if (uploadToken == null) {
                String message = format(Messages.APP_UPLOAD_TIMED_OUT, app.getName());
                error(context, message, LOGGER);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }

            UploadInfo uploadInfo = clientExtensions.getUploadProgress(uploadToken);
            switch (uploadInfo.getUploadJobState()) {
                case FAILED: {
                    String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                    error(context, message, LOGGER);
                    return ExecutionStatus.LOGICAL_RETRY;
                }
                case FINISHED: {
                    debug(context, format(Messages.APP_UPLOADED, app.getName()), LOGGER);
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
            error(context, format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName()), e, LOGGER);
            throw e;
        } catch (CloudFoundryException e) {
            SLException ex = StepsUtil.createException(e);
            error(context, format(Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName()), ex, LOGGER);
            throw ex;
        }
    }

    @Override
    public String getLogicalStepName() {
        return UploadAppStep.class.getSimpleName();
    }

    @Override
    protected String getIndexVariable() {
        return Constants.VAR_APPS_INDEX;
    }

}
