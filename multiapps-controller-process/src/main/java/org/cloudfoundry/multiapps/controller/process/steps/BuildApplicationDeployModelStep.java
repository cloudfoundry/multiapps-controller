package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ConfigurationEntriesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.HandlerFactory;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.Staging;

@Named("buildApplicationDeployModelStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BuildApplicationDeployModelStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        Module module = context.getVariable(Variables.MODULE_TO_DEPLOY);
        getStepLogger().debug(Messages.BUILDING_CLOUD_APP_MODEL, module.getName());
        Module applicationModule = findModuleInDeploymentDescriptor(context, module.getName());
        context.setVariable(Variables.MODULE_TO_DEPLOY, applicationModule);
        CloudApplicationExtended modifiedApp = StepsUtil.getApplicationCloudModelBuilder(context)
                                                        .build(applicationModule, moduleToDeployHelper);
        Staging stagingWithUpdatedHealthCheck = modifyHealthCheckType(modifiedApp.getStaging());
        modifiedApp = ImmutableCloudApplicationExtended.builder()
                                                       .from(modifiedApp)
                                                       .staging(stagingWithUpdatedHealthCheck)
                                                       .routes(getApplicationRoutes(context, modifiedApp))
                                                       .build();
        context.setVariable(Variables.APP_TO_PROCESS, modifiedApp);
        determineBindingUnbindingServicesStrategy(context, module);
        buildConfigurationEntries(context, modifiedApp);
        context.setVariable(Variables.TASKS_TO_EXECUTE, modifiedApp.getTasks());
        getStepLogger().debug(Messages.CLOUD_APP_MODEL_BUILT);
        return StepPhase.DONE;
    }

    private Module findModuleInDeploymentDescriptor(ProcessContext context, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, module);
    }

    private Staging modifyHealthCheckType(Staging staging) {
        String healthCheckType = staging.getHealthCheckType();
        if (StringUtils.isEmpty(healthCheckType)) {
            getStepLogger().debug(Messages.NOT_SPECIFIED_HEALTH_CHECK_TYPE, HealthCheckType.PORT.getValue());
            return ImmutableStaging.copyOf(staging)
                                   .withHealthCheckType(HealthCheckType.PORT.getValue());
        }
        if (HealthCheckType.NONE.getValue()
                                .equalsIgnoreCase(healthCheckType)) {
            getStepLogger().info(Messages.USING_DEPRECATED_HEALTH_CHECK_TYPE_0_SETTING_TO_1, healthCheckType,
                                 HealthCheckType.PROCESS.getValue());
            return ImmutableStaging.copyOf(staging)
                                   .withHealthCheckType(HealthCheckType.PROCESS.getValue());
        }
        return staging;
    }

    private void determineBindingUnbindingServicesStrategy(ProcessContext context, Module module) {
        boolean parallelBindingUnbindingEnabled = (boolean) module.getParameters()
                                                                  .getOrDefault(SupportedParameters.ENABLE_PARALLEL_SERVICE_BINDINGS, true);
        context.setVariable(Variables.SHOULD_UNBIND_BIND_SERVICES_IN_PARALLEL, parallelBindingUnbindingEnabled);
        if (!parallelBindingUnbindingEnabled) {
            getStepLogger().warn(Messages.SERVICES_WILL_BE_BOUND_UNBOUND_SEQUENTIALLY_TO_APPLICATION_0,
                                 NameUtil.getApplicationName(module));
        }
    }

    private Set<CloudRouteSummary> getApplicationRoutes(ProcessContext context, CloudApplicationExtended modifiedApp) {
        if (context.getVariable(Variables.USE_IDLE_URIS)) {
            return modifiedApp.getIdleRoutes();
        }
        return modifiedApp.getRoutes();
    }

    private void buildConfigurationEntries(ProcessContext context, CloudApplicationExtended app) {
        if (context.getVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES)) {
            context.setVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH, Collections.emptyList());
            return;
        }
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);

        ConfigurationEntriesCloudModelBuilder configurationEntriesCloudModelBuilder = getConfigurationEntriesCloudModelBuilder(context);
        Map<String, List<ConfigurationEntry>> allConfigurationEntries = configurationEntriesCloudModelBuilder.build(deploymentDescriptor);
        List<ConfigurationEntry> updatedModuleNames = allConfigurationEntries.getOrDefault(app.getModuleName(), Collections.emptyList());
        context.setVariable(Variables.CONFIGURATION_ENTRIES_TO_PUBLISH, updatedModuleNames);
        context.setVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES, false);

        getStepLogger().debug(Messages.CONFIGURATION_ENTRIES_TO_PUBLISH, SecureSerialization.toJson(updatedModuleNames));
    }

    private ConfigurationEntriesCloudModelBuilder getConfigurationEntriesCloudModelBuilder(ProcessContext context) {
        String organizationName = context.getVariable(Variables.ORGANIZATION_NAME);
        String spaceName = context.getVariable(Variables.SPACE_NAME);
        String spaceGuid = context.getVariable(Variables.SPACE_GUID);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);
        getStepLogger().infoWithoutProgressMessage("Building configuration entries for org {0}, space {1}, spaceId {2} and namespace {3}!",
                                                   organizationName, spaceName, spaceGuid, namespace);
        return new ConfigurationEntriesCloudModelBuilder(organizationName, spaceName, spaceGuid, namespace);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_BUILDING_CLOUD_APP_MODEL;
    }

}
