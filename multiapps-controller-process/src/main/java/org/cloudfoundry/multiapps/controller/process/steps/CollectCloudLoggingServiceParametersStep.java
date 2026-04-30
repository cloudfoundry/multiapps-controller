package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.ExternalLoggingServiceConfigurationsCalculator;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Named("collectCloudLoggingServiceParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectCloudLoggingServiceParametersStep extends SyncFlowableStep {

    @Inject
    private TokenService tokenService;

    @Inject
    private CloudControllerClientFactory clientFactory;

    @Inject
    private OperationLogsExporter operationLogsExporter;

    @Inject
    private FlowableFacade flowableFacade;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        if (!isCloudLoggingEnabled(deploymentDescriptor)) {
            return StepPhase.DONE;
        }
        LoggingConfiguration loggingConfiguration = setExternalLoggingServiceConfigurationIfRequired(context, deploymentDescriptor);
        List<OperationLogEntry> operationLogEntries = operationLogsExporter.getUnsendProcessLogs(loggingConfiguration);

        for (OperationLogEntry operationLogEntry : operationLogEntries) {
            operationLogsExporter.sendLogsToCloudLoggingService(loggingConfiguration, operationLogEntry);
        }
        context.setVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, loggingConfiguration);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return "Well, failed! Deal with it!";
    }

    protected boolean isRootProcess(DelegateExecution execution) {
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        String processInstanceId = execution.getProcessInstanceId();
        return processInstanceId.equals(correlationId);
    }

    protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                    DeploymentDescriptor deploymentDescriptor) {
        ExternalLoggingServiceConfigurationsCalculator calculator = new ExternalLoggingServiceConfigurationsCalculator(clientFactory,
                                                                                                                       context,
                                                                                                                       tokenService);
        Resource resource = getLoggingServiceResource(deploymentDescriptor.getResources());
        return calculator.exportOperationLogsToExternalSystem(resource);
    }

    private boolean isCloudLoggingEnabled(DeploymentDescriptor deploymentDescriptor) {
        if (deploymentDescriptor.getResources()
                                .isEmpty()) {
            return false;
        }

        return deploymentDescriptor.getResources()
                                   .stream()
                                   .anyMatch(CollectCloudLoggingServiceParametersStep::isCloudLoggingServiceResource);
    }

    private Resource getLoggingServiceResource(List<Resource> resources) {
        return resources.stream()
                        .filter(CollectCloudLoggingServiceParametersStep::isCloudLoggingServiceResource)
                        .findFirst()
                        .get();
    }

    private static boolean isCloudLoggingServiceResource(Resource resource) {
        String resourceType = resource.getType()
                                      .replace("org.cloudfoundry.", EMPTY);
        ResourceType resourceType1 = ResourceType.get(resourceType);
        if (resourceType1 == null) {
            return false;
        }
        return ResourceType.CLOUD_LOGGING_SERVICE.equals(resourceType1);
    }
}
