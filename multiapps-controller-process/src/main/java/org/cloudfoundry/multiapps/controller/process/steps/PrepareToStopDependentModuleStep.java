package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.AdditionalModuleParametersReporter;
import org.cloudfoundry.multiapps.controller.process.util.ApplicationEnvironmentCalculator;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.handlers.HandlerFactory;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("prepareToStopDependentModuleStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class PrepareToStopDependentModuleStep extends SyncFlowableStep {

    @Inject
    private ModuleToDeployHelper moduleToDeployHelper;

    @Inject
    private ApplicationEnvironmentCalculator applicationEnvironmentCalculator;

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        context.setVariable(Variables.KEEP_ORIGINAL_APP_NAMES_AFTER_DEPLOY, true);

        Module applicationModule = findModuleInDeploymentDescriptor(context, getCurrentModuleToStop(context).getName());
        context.setVariable(Variables.MODULE_TO_DEPLOY, applicationModule);
        CloudApplicationExtended modifiedApp = StepsUtil.getApplicationCloudModelBuilder(context)
                                                        .build(applicationModule, moduleToDeployHelper);
        buildAdditionalModuleParametersReporter(context).reportUsageOfAdditionalParameters(applicationModule);
        Staging stagingWithUpdatedHealthCheck = modifyHealthCheckType(modifiedApp.getStaging());
        Map<String, String> calculatedAppEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, modifiedApp);
        modifiedApp = ImmutableCloudApplicationExtended.builder()
                                                       .from(modifiedApp)
                                                       .staging(stagingWithUpdatedHealthCheck)
                                                       .routes(getApplicationRoutes(context, modifiedApp))
                                                       .env(calculatedAppEnv)
                                                       .build();
        context.setVariable(Variables.APP_TO_PROCESS, modifiedApp);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format("Failed to stop dependent module", context.getVariable(Variables.APP_TO_PROCESS)
                                                                              .getName());
    }

    static Module getCurrentModuleToStop(ProcessContext context) {
        List<Module> modules = context.getVariable(Variables.DEPENDENT_MODULES_TO_STOP);
        int index = context.getVariable(Variables.APPS_TO_STOP_INDEX);
        return modules.get(index);
    }

    private Module findModuleInDeploymentDescriptor(ProcessContext context, String module) {
        HandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        return handlerFactory.getDescriptorHandler()
                             .findModule(deploymentDescriptor, module);
    }

    private AdditionalModuleParametersReporter buildAdditionalModuleParametersReporter(ProcessContext context) {
        return new AdditionalModuleParametersReporter(context);
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

    private Set<CloudRoute> getApplicationRoutes(ProcessContext context, CloudApplicationExtended modifiedApp) {
        if (context.getVariable(Variables.USE_IDLE_URIS)) {
            return modifiedApp.getIdleRoutes();
        }
        return modifiedApp.getRoutes();
    }

}
