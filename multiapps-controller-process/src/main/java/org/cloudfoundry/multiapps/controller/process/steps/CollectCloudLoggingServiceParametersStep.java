package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.CloudLoggingServiceConfigurationService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.process.util.ExternalLoggingServiceConfigurationsCalculator;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
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
    private CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService;

    @Inject
    private ProcessTypeParser processTypeParser;

    @Inject
    private CloudLoggingServiceConfigurationAuditLog cloudLoggingServiceConfigurationAuditLog;

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        LoggingConfiguration loggingConfiguration = getLoggingConfiguration(context);
        if (loggingConfiguration == null) {
            return StepPhase.DONE;
        }
        List<OperationLogEntry> operationLogEntries = operationLogsExporter.getUnsendProcessLogs(loggingConfiguration);

        for (OperationLogEntry operationLogEntry : operationLogEntries) {
            operationLogsExporter.sendLogsToCloudLoggingService(loggingConfiguration, operationLogEntry);
        }
        context.setVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, loggingConfiguration);
        return StepPhase.DONE;
    }

    private LoggingConfiguration getLoggingConfiguration(ProcessContext context) {
        ProcessType processType = processTypeParser.getProcessType(context.getExecution());
        LoggingConfiguration loggingConfiguration = getExistingLoggingConfiguration(context);

        if (processType.equals(ProcessType.UNDEPLOY)) {
            return processUndeployLoggingConfiguration(context, loggingConfiguration);
        } else {
            return processDeployLoggingConfiguration(context, loggingConfiguration);
        }
    }

    private LoggingConfiguration processUndeployLoggingConfiguration(ProcessContext context,
                                                                     LoggingConfiguration existingLoggingConfiguration) {
        if (existingLoggingConfiguration == null) {
            return null;
        }
        return setExternalLoggingServiceConfigurationIfRequired(context, existingLoggingConfiguration);
    }

    private LoggingConfiguration processDeployLoggingConfiguration(ProcessContext context,
                                                                   LoggingConfiguration existingLoggingConfiguration) {
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        if (!isCloudLoggingEnabled(deploymentDescriptor)) {
            if (existingLoggingConfiguration != null) {
                cloudLoggingServiceConfigurationAuditLog.logDeleteLoggingConfiguration(context.getVariable(Variables.USER),
                                                                                       context.getVariable(Variables.SPACE_GUID),
                                                                                       existingLoggingConfiguration);
                cloudLoggingServiceConfigurationService.deleteCloudLoggingServiceConfiguration(existingLoggingConfiguration.getId());
            }
            return null;
        }

        existingLoggingConfiguration = setExternalLoggingServiceConfigurationIfRequired(context, deploymentDescriptor);
        if (existingLoggingConfiguration == null) {
            return null;
        }
        storeOrUpdateLoggingConfiguration(context, existingLoggingConfiguration, getExistingLoggingConfiguration(context));
        return existingLoggingConfiguration;
    }

    private void storeOrUpdateLoggingConfiguration(ProcessContext context, LoggingConfiguration loggingConfiguration,
                                                   LoggingConfiguration existingLoggingConfiguration) {
        if (existingLoggingConfiguration == null) {
            cloudLoggingServiceConfigurationAuditLog.logCreateLoggingConfiguration(context.getVariable(Variables.USER),
                                                                                   context.getVariable(Variables.SPACE_GUID),
                                                                                   loggingConfiguration);
            cloudLoggingServiceConfigurationService.storeCloudLoggingServiceConfiguration(loggingConfiguration);
        } else {
            cloudLoggingServiceConfigurationAuditLog.logUpdateLoggingConfiguration(context.getVariable(Variables.USER),
                                                                                   context.getVariable(Variables.SPACE_GUID),
                                                                                   loggingConfiguration);
            cloudLoggingServiceConfigurationService.updateCloudLoggingServiceConfiguration(loggingConfiguration);
        }
    }

    private LoggingConfiguration getExistingLoggingConfiguration(ProcessContext context) {

        LoggingConfiguration loggingConfiguration = cloudLoggingServiceConfigurationService.getCloudLoggingServiceConfiguration(
            context.getVariable(Variables.SPACE_NAME), context.getVariable(Variables.MTA_ID), context.getVariable(Variables.MTA_NAMESPACE));

        if (loggingConfiguration != null) {
            cloudLoggingServiceConfigurationAuditLog.logGetLoggingConfiguration(context.getVariable(Variables.USER),
                                                                                context.getVariable(Variables.SPACE_GUID),
                                                                                loggingConfiguration);
        }
        return loggingConfiguration;
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

    protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                    LoggingConfiguration loggingConfiguration) {
        ExternalLoggingServiceConfigurationsCalculator calculator = new ExternalLoggingServiceConfigurationsCalculator(clientFactory,
                                                                                                                       context,
                                                                                                                       tokenService);
        return calculator.exportOperationLogsToExternalSystem(loggingConfiguration, context);
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
