package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.core.cf.clients.AppBoundServiceInstanceNamesGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceBindingParametersGetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;

@Named("determineApplicationServiceBindingActionsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineApplicationServiceBindingActionsStep extends SyncFlowableStep {

    @Inject
    private TokenService tokenService;
    @Inject
    private WebClientFactory webClientFactory;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws FileStorageException {
        CloudControllerClient client = context.getControllerClient();
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        CloudApplicationExtended appToProcess = context.getVariable(Variables.APP_TO_PROCESS);
        if (serviceBindingToDelete != null) {
            String appName = appToProcess != null ? appToProcess.getName()
                : client.getApplicationName(serviceBindingToDelete.getApplicationGuid());
            String serviceInstanceName = getServiceInstanceToUnbind(context, client, serviceBindingToDelete);
            getStepLogger().debug(Messages.WILL_UNBIND_SERVICE_INSTANCE_0_FROM_APP_1, serviceInstanceName, appName);
            return setBindingForDeletion(context, appName, serviceInstanceName);
        }
        String serviceInstanceToUnbindBind = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        getStepLogger().debug(Messages.DETERMINE_BIND_UNBIND_OPERATIONS_APPLICATION_0_SERVICE_INSTANCE_1, appToProcess.getName(),
                              serviceInstanceToUnbindBind);
        if (!isServicePartFromMta(appToProcess, serviceInstanceToUnbindBind) && !shouldKeepExistingServiceBindings(appToProcess)) {
            return setBindingForDeletion(context, appToProcess.getName(), serviceInstanceToUnbindBind);
        }
        CloudApplication existingApp = client.getApplication(appToProcess.getName());

        ServiceBindingParametersGetter serviceBindingParametersGetter = getServiceBindingParametersGetter(context);
        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromMta(appToProcess,
                                                                                                                  serviceInstanceToUnbindBind);
        if (!doesServiceBindingExist(serviceInstanceToUnbindBind, existingApp.getGuid(), context)) {
            context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, false);
            context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, true);
            context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, bindingParameters);
            getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, appToProcess.getName(),
                                  serviceInstanceToUnbindBind, false, true);
            return StepPhase.DONE;
        }
        getStepLogger().debug(Messages.CHECK_SHOULD_REBIND_APPLICATION_SERVICE_INSTANCE, appToProcess.getName(),
                              serviceInstanceToUnbindBind);

        if (shouldKeepExistingServiceBindings(appToProcess)) {
            getStepLogger().info(Messages.WILL_NOT_REBIND_APP_TO_SERVICE_KEEP_EXISTING_STRATEGY, serviceInstanceToUnbindBind,
                                 appToProcess.getName());
            return keepExistingServiceBindings(context, appToProcess, serviceInstanceToUnbindBind);
        }

        if (shouldRecreateServiceBinding(context)
            || areBindingParametersDifferent(serviceBindingParametersGetter, existingApp, serviceInstanceToUnbindBind, bindingParameters)) {
            context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, true);
            context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, true);
            context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, bindingParameters);
            getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, appToProcess.getName(),
                                  serviceInstanceToUnbindBind, true, true);
            return StepPhase.DONE;
        }
        getStepLogger().info(Messages.WILL_NOT_REBIND_APP_TO_SERVICE_SAME_PARAMETERS, serviceInstanceToUnbindBind, appToProcess.getName());
        return keepExistingServiceBindings(context, appToProcess, serviceInstanceToUnbindBind);
    }

    private String getServiceInstanceToUnbind(ProcessContext context, CloudControllerClient controllerClient,
                                              CloudServiceBinding cloudServiceBinding) {
        String serviceInstanceToUnbindBind = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        if (serviceInstanceToUnbindBind != null) {
            return serviceInstanceToUnbindBind;
        }
        String serviceInstanceToDelete = context.getVariable(Variables.SERVICE_TO_DELETE);
        if (serviceInstanceToDelete != null) {
            return serviceInstanceToDelete;
        }
        return controllerClient.getServiceInstanceName(cloudServiceBinding.getServiceInstanceGuid());
    }

    private StepPhase keepExistingServiceBindings(ProcessContext context, CloudApplicationExtended app, String service) {
        context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, false);
        context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, false);
        getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, app.getName(), service, false, false);
        return StepPhase.DONE;
    }

    private StepPhase setBindingForDeletion(ProcessContext context, String appName, String service) {
        context.setVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP, true);
        context.setVariable(Variables.SHOULD_BIND_SERVICE_TO_APP, false);
        getStepLogger().debug(Messages.CALCULATED_BINDING_OPERATIONS_APPLICATION_SERVICE_INSTANCE, appName, service, true, false);
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

    protected AppBoundServiceInstanceNamesGetter getAppServicesGetter(CloudCredentials credentials, String correlationId) {
        return new AppBoundServiceInstanceNamesGetter(configuration, webClientFactory, credentials, correlationId);
    }

    private boolean doesServiceBindingExist(String serviceName, UUID appGuid, ProcessContext context) {
        String user = context.getVariable(Variables.USER);
        String correlationId = context.getVariable(Variables.CORRELATION_ID);
        var token = tokenService.getToken(user);
        var creds = new CloudCredentials(token, true);

        var serviceNamesGetter = getAppServicesGetter(creds, correlationId);
        List<String> appServiceNames = serviceNamesGetter.getServiceInstanceNamesBoundToApp(appGuid);
        return appServiceNames.contains(serviceName);
    }

    private boolean areBindingParametersDifferent(ServiceBindingParametersGetter serviceBindingParametersGetter, CloudApplication app,
                                                  String serviceName, Map<String, Object> newBindingParameters) {
        Map<String, Object> currentBindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(app,
                                                                                                                                      serviceName);
        return !Objects.equals(currentBindingParameters, newBindingParameters);
    }

    private boolean shouldRecreateServiceBinding(ProcessContext context) {
        return context.getVariable(Variables.SHOULD_RECREATE_SERVICE_BINDING);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        CloudServiceBinding serviceBindingToDelete = context.getVariable(Variables.SERVICE_BINDING_TO_DELETE);
        if (serviceBindingToDelete != null) {
            return MessageFormat.format(Messages.ERROR_WHILE_DETERMINING_BIND_UNBIND_OPERATIONS_OF_APPLICATION_GUID_TO_SERVICE_INSTANCE_GUID,
                                        serviceBindingToDelete.getApplicationGuid(), serviceBindingToDelete.getServiceInstanceGuid());
        }
        return MessageFormat.format(Messages.ERROR_WHILE_DETERMINING_BIND_UNBIND_OPERATIONS_OF_APPLICATION_TO_SERVICE,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName(),
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND));
    }

}
