package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

@Named("bindServiceToApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BindServiceToApplicationStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        Map<String, Object> serviceBindingParameters = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);

        getStepLogger().info(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1, service, app.getName());

        CloudControllerClient client = context.getControllerClient();
        client.bindServiceInstance(app.getName(), service, serviceBindingParameters, getApplicationServicesUpdateCallback(context));

        return StepPhase.DONE;
    }

    private ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(ProcessContext context) {
        return new DefaultApplicationServicesUpdateCallback(context);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_BINDING_SERVICE_TO_APPLICATION,
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND), context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                  .getName());
    }

    public static class DefaultApplicationServicesUpdateCallback implements ApplicationServicesUpdateCallback {

        private final ProcessContext context;

        public DefaultApplicationServicesUpdateCallback(ProcessContext context) {
            this.context = context;
        }

        @Override
        public void onError(CloudOperationException e, String applicationName, String serviceName) {
            List<CloudServiceInstanceExtended> servicesToBind = context.getVariable(Variables.SERVICES_TO_BIND);
            CloudServiceInstanceExtended serviceToBind = findServiceCloudModel(servicesToBind, serviceName);

            if (serviceToBind != null && serviceToBind.isOptional()) {
                context.getStepLogger()
                       .warn(e, Messages.COULD_NOT_BIND_OPTIONAL_SERVICE_TO_APP, serviceName, applicationName);
                return;
            }
            throw new SLException(e, Messages.COULD_NOT_BIND_SERVICE_TO_APP, serviceName, applicationName, e.getMessage());
        }

        private CloudServiceInstanceExtended findServiceCloudModel(List<CloudServiceInstanceExtended> servicesCloudModel,
                                                                   String serviceName) {
            return servicesCloudModel.stream()
                                     .filter(service -> service.getName()
                                                               .equals(serviceName))
                                     .findAny()
                                     .orElse(null);
        }

    }

}
