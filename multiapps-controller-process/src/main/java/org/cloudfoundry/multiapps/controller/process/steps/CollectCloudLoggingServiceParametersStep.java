package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.auditlogging.CloudLoggingServiceConfigurationAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ResourceType;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.CloudLoggingServiceConfigurationService;
import org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging.UnsentProcessLogsProvider;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.LoggingConfigurationBuilder;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Named("collectCloudLoggingServiceParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectCloudLoggingServiceParametersStep extends SyncFlowableStep {

    private static final String CLOUDFOUNDRY_RESOURCE_TYPE_PREFIX = "org.cloudfoundry.";

    private final TokenService tokenService;
    private final CloudControllerClientFactory clientFactory;
    private final CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService;
    private final ProcessTypeParser processTypeParser;
    private final CloudLoggingServiceConfigurationAuditLog cloudLoggingServiceConfigurationAuditLog;
    private final UnsentProcessLogsProvider unsentProcessLogsProvider;

    public CollectCloudLoggingServiceParametersStep(TokenService tokenService, CloudControllerClientFactory clientFactory,
                                                    CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService,
                                                    ProcessTypeParser processTypeParser,
                                                    CloudLoggingServiceConfigurationAuditLog cloudLoggingServiceConfigurationAuditLog,
                                                    UnsentProcessLogsProvider unsentProcessLogsProvider) {
        this.tokenService = tokenService;
        this.clientFactory = clientFactory;
        this.cloudLoggingServiceConfigurationService = cloudLoggingServiceConfigurationService;
        this.processTypeParser = processTypeParser;
        this.cloudLoggingServiceConfigurationAuditLog = cloudLoggingServiceConfigurationAuditLog;
        this.unsentProcessLogsProvider = unsentProcessLogsProvider;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) throws Exception {
        LoggingConfiguration loggingConfiguration = getLoggingConfiguration(context);
        if (loggingConfiguration == null) {
            return StepPhase.DONE;
        }
        List<OperationLogEntry> operationLogEntries = unsentProcessLogsProvider.getUnsentProcessLogs(loggingConfiguration);

        for (OperationLogEntry operationLogEntry : operationLogEntries) {
            operationLogsExporter.sendLogsToCloudLoggingService(loggingConfiguration, operationLogEntry);
        }
        context.setVariable(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, loggingConfiguration);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_COLLECTING_CLOUD_LOGGING_SERVICE_PARAMETERS;
    }

    private LoggingConfiguration getLoggingConfiguration(ProcessContext context) {
        ProcessType processType = processTypeParser.getProcessType(context.getExecution());
        LoggingConfiguration existingLoggingConfiguration = getExistingLoggingConfiguration(context);

        if (processType.equals(ProcessType.UNDEPLOY)) {
            return processUndeployLoggingConfiguration(context, existingLoggingConfiguration);
        }
        return processDeployLoggingConfiguration(context, existingLoggingConfiguration);
    }

    private LoggingConfiguration getExistingLoggingConfiguration(ProcessContext context) {
        LoggingConfiguration loggingConfiguration = cloudLoggingServiceConfigurationService.getLoggingConfiguration(
            context.getVariable(Variables.SPACE_NAME), context.getVariable(Variables.MTA_ID), context.getVariable(Variables.MTA_NAMESPACE));

        cloudLoggingServiceConfigurationAuditLog.logGetLoggingConfiguration(context.getVariable(Variables.USER),
                                                                            context.getVariable(Variables.SPACE_GUID),
                                                                            loggingConfiguration);
        return loggingConfiguration;
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
            deleteExistingLoggingConfigurationIfExists(context, existingLoggingConfiguration);
            return null;
        }

        LoggingConfiguration newLoggingConfiguration = setExternalLoggingServiceConfigurationIfRequired(context, deploymentDescriptor);
        if (newLoggingConfiguration == null) {
            return null;
        }
        if (existingLoggingConfiguration == null) {
            persistLoggingConfiguration(context, newLoggingConfiguration);
        } else {
            updateLoggingConfiguration(context, existingLoggingConfiguration, newLoggingConfiguration);
        }
        return newLoggingConfiguration;
    }

    private void deleteExistingLoggingConfigurationIfExists(ProcessContext context, LoggingConfiguration existingLoggingConfiguration) {
        if (existingLoggingConfiguration != null) {
            cloudLoggingServiceConfigurationAuditLog.logDeleteLoggingConfiguration(context.getVariable(Variables.USER),
                                                                                   context.getVariable(Variables.SPACE_GUID),
                                                                                   existingLoggingConfiguration);
            cloudLoggingServiceConfigurationService.deleteLoggingConfiguration(existingLoggingConfiguration.getId());
        }
    }

    private boolean isCloudLoggingEnabled(DeploymentDescriptor deploymentDescriptor) {
        return !deploymentDescriptor.getResources()
                                    .isEmpty()
            && deploymentDescriptor.getResources()
                                   .stream()
                                   .anyMatch(CollectCloudLoggingServiceParametersStep::isCloudLoggingServiceResource);
    }

    protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                    DeploymentDescriptor deploymentDescriptor) {
        LoggingConfigurationBuilder builder = new LoggingConfigurationBuilder(clientFactory, context, tokenService);
        Resource resource = findCloudLoggingServiceResource(deploymentDescriptor.getResources());
        return builder.exportOperationLogsToExternalSystem(resource);
    }

    protected LoggingConfiguration setExternalLoggingServiceConfigurationIfRequired(ProcessContext context,
                                                                                    LoggingConfiguration loggingConfiguration) {
        LoggingConfigurationBuilder builder = new LoggingConfigurationBuilder(clientFactory, context, tokenService);
        return builder.exportOperationLogsToExternalSystem(loggingConfiguration, context);
    }

    private void persistLoggingConfiguration(ProcessContext context, LoggingConfiguration newLoggingConfiguration) {
        cloudLoggingServiceConfigurationAuditLog.logCreateLoggingConfiguration(context.getVariable(Variables.USER),
                                                                               context.getVariable(Variables.SPACE_GUID),
                                                                               newLoggingConfiguration);
        cloudLoggingServiceConfigurationService.add(newLoggingConfiguration);
    }

    private void updateLoggingConfiguration(ProcessContext context, LoggingConfiguration existingLoggingConfiguration,
                                            LoggingConfiguration newLoggingConfiguration) {
        cloudLoggingServiceConfigurationAuditLog.logUpdateLoggingConfiguration(context.getVariable(Variables.USER),
                                                                               context.getVariable(Variables.SPACE_GUID),
                                                                               newLoggingConfiguration);
        cloudLoggingServiceConfigurationService.update(existingLoggingConfiguration, newLoggingConfiguration);
    }

    private Resource findCloudLoggingServiceResource(List<Resource> resources) {
        return resources.stream()
                        .filter(CollectCloudLoggingServiceParametersStep::isCloudLoggingServiceResource)
                        .findFirst()
                        .get();
    }

    private static boolean isCloudLoggingServiceResource(Resource resource) {
        ResourceType resourceType = ResourceType.get(stripCloudfoundryPrefix(resource.getType()));
        return ResourceType.CLOUD_LOGGING_SERVICE.equals(resourceType);
    }

    private static String stripCloudfoundryPrefix(String resourceType) {
        return resourceType.replace(CLOUDFOUNDRY_RESOURCE_TYPE_PREFIX, EMPTY);
    }
}
