package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.UploadInfo;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("pollUploadAppStatusStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PollUploadAppStatusStep extends AbstractXS2ProcessStepWithBridge {

    @Override
    protected ExecutionStatus pollStatusInternal(DelegateExecution context) throws SLException {
        getStepLogger().logActivitiTask();

        // Get the next cloud application from the context
        final CloudApplication app = StepsUtil.getApp(context);

        try {
            getStepLogger().debug(Messages.CHECKING_UPLOAD_APP_STATUS, app.getName());
            String status = (String) context.getVariable(getStatusVariable());
            if (ExecutionStatus.FAILED.name().equalsIgnoreCase(status)) {
                String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                getStepLogger().error(message);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }

            ClientExtensions clientExtensions = getClientExtensions(context);
            if (clientExtensions == null && ExecutionStatus.SUCCESS.name().equalsIgnoreCase(status)) {
                return ExecutionStatus.SUCCESS;
            }

            String uploadToken = StepsUtil.getUploadToken(context);
            if (uploadToken == null) {
                String message = format(Messages.APP_UPLOAD_TIMED_OUT, app.getName());
                getStepLogger().error(message);
                setRetryMessage(context, message);
                return ExecutionStatus.LOGICAL_RETRY;
            }

            UploadInfo uploadInfo = clientExtensions.getUploadProgress(uploadToken);
            switch (uploadInfo.getUploadJobState()) {
                case FAILED: {
                    String message = format(Messages.ERROR_UPLOADING_APP, app.getName());
                    getStepLogger().error(message);
                    setRetryMessage(context, message);
                    return ExecutionStatus.LOGICAL_RETRY;
                }
                case FINISHED: {
                    getStepLogger().debug(Messages.APP_UPLOADED, app.getName());
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
            getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            SLException e = StepsUtil.createException(cfe);
            getStepLogger().error(e, Messages.ERROR_CHECKING_UPLOAD_APP_STATUS, app.getName());
            throw e;
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
