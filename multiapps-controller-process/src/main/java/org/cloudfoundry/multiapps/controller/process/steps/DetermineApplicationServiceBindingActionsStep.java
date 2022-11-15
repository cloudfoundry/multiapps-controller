package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.core.cf.clients.AppBoundServiceInstanceNamesGetter;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceBindingParametersGetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named("determineApplicationServiceBindingActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineApplicationServiceBindingActionsStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(Messages.DETERMINE_BIND_UNBIND_OPERATIONS_APPLICATION_0_SERVICE_INSTANCE_1, app.getName(), service);

        if (!isServicePartFromMta(app, service) && !shouldKeepExistingServiceBindings(app)) {
            context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, true);
            context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, false);
            getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, app.getName(), service, true, false);
            return StepPhase.DONE;
        }

        CloudControllerClient client = context.getControllerClient();
        CloudApplication existingApp = client.getApplication(app.getName());

        ServiceBindingParametersGetter serviceBindingParametersGetter = getServiceBindingParametersGetter(context);
        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromMta(app, service);
        if (!doesServiceBindingExist(client, service, existingApp.getGuid())) {
            context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, false);
            context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, true);
            context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, bindingParameters);
            getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, app.getName(), service, false, true);
            return StepPhase.DONE;
        }

        getStepLogger().debug(Messages.CHECK_SHOULD_REBIND_APPLICATION_SERVICE_INSTANCE, app.getName(), service);
        if (shouldRebindService(serviceBindingParametersGetter, existingApp, service, bindingParameters)) {
            context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, true);
            context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, true);
            context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, bindingParameters);
            getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, app.getName(), service, true, true);
            return StepPhase.DONE;
        }

        getStepLogger().info(Messages.WILL_NOT_REBIND_APP_TO_SERVICE, service, app.getName());
        context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, false);
        context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, false);
        getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, app.getName(), service, false, false);
        return StepPhase.DONE;
    }

    private boolean isServicePartFromMta(CloudApplicationExtended app, String service) {
        return app.getServices()
                  .contains(service);
    }

    private boolean shouldKeepExistingServiceBindings(CloudApplicationExtended app) {
        AttributeUpdateStrategy appAttributesUpdateBehavior = app.getAttributesUpdateStrategy();
        return appAttributesUpdateBehavior.shouldKeepExistingServiceBindings();
    }

    protected ServiceBindingParametersGetter getServiceBindingParametersGetter(ProcessContext context) {
        return new ServiceBindingParametersGetter(context, fileService, configuration.getMaxManifestSize());
    }

    protected AppBoundServiceInstanceNamesGetter getAppServicesGetter(CloudControllerClient client) {
        return new AppBoundServiceInstanceNamesGetter(client);
    }

    private boolean doesServiceBindingExist(CloudControllerClient client, String service, UUID appGuid) {
        var serviceNamesGetter = getAppServicesGetter(client);
        List<String> appServiceNames = serviceNamesGetter.getServiceInstanceNamesBoundToApp(appGuid);
        return appServiceNames.contains(service);
    }

    private boolean shouldRebindService(ServiceBindingParametersGetter serviceBindingParametersGetter, CloudApplication app,
                                        String serviceName, Map<String, Object> newBindingParameters) {
        Map<String, Object> currentBindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(app,
                                                                                                                                      serviceName);
        return !Objects.equals(currentBindingParameters, newBindingParameters);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_DETERMINE_BIND_UNBIND_OPERATIONS_OF_APPLICATION_TO_SERVICE,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName(),
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND));
    }

}
