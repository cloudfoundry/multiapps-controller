package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

@Named("unbindServiceFromApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnbindServiceFromApplicationStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().info(Messages.UNBINDING_SERVICE_INSTANCE_FROM_APP, service, app.getName());

        CloudControllerClient client = context.getControllerClient();
        client.unbindServiceInstance(app.getName(), service, getApplicationServicesUpdateCallback(context));
        getStepLogger().infoWithoutProgressMessage(Messages.UNBINDING_SERVICE_INSTANCE_FROM_APP_FINISHED, service, app.getName());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_UNBINDING_SERVICE_INSTANCE_FROM_APPLICATION,
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND), context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                  .getName());
    }

    private ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(ProcessContext context) {
        return new UnbindServiceFromApplicationCallback(context);
    }

    public static class UnbindServiceFromApplicationCallback implements ApplicationServicesUpdateCallback {

        private final ProcessContext context;

        public UnbindServiceFromApplicationCallback(ProcessContext context) {
            this.context = context;
        }

        @Override
        public void onError(CloudOperationException e, String applicationName, String serviceName) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                context.getStepLogger()
                       .warnWithoutProgressMessage(e, Messages.SERVICE_BINDING_BETWEEN_SERVICE_0_AND_APP_1_ALREADY_DELETED, serviceName,
                                                   applicationName);
                return;
            }

            throw e;
        }

    }

}
