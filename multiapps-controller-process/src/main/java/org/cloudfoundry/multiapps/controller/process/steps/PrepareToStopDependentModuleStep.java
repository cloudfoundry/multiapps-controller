package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.process.Messages;
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

    private ModuleToDeployHelper moduleToDeployHelper;

    private ApplicationEnvironmentCalculator applicationEnvironmentCalculator;

    @Inject
    public PrepareToStopDependentModuleStep(ModuleToDeployHelper moduleToDeployHelper,
                                            ApplicationEnvironmentCalculator applicationEnvironmentCalculator) {
        this.moduleToDeployHelper = moduleToDeployHelper;
        this.applicationEnvironmentCalculator = applicationEnvironmentCalculator;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        Module applicationModule = findModuleInDeploymentDescriptor(context, getCurrentModuleToStop(context).getName());
        context.setVariable(Variables.MODULE_TO_DEPLOY, applicationModule);
        CloudApplicationExtended modifiedApp = getApplicationCloudModelBuilder(context).build(applicationModule, moduleToDeployHelper);
        Map<String, String> calculatedAppEnv = applicationEnvironmentCalculator.calculateNewApplicationEnv(context, modifiedApp);
        modifiedApp = getCloudApplicationExtended(context, modifiedApp, calculatedAppEnv);
        context.setVariable(Variables.APP_TO_PROCESS, modifiedApp);
        return StepPhase.DONE;
    }

    private CloudApplicationExtended getCloudApplicationExtended(ProcessContext context, CloudApplicationExtended modifiedApp,
                                                                 Map<String, String> calculatedAppEnv) {
        return ImmutableCloudApplicationExtended.builder()
                                                .from(modifiedApp)
                                                .staging(modifiedApp.getStaging())
                                                .routes(getApplicationRoutes(context, modifiedApp))
                                                .env(calculatedAppEnv)
                                                .build();
    }

    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(ProcessContext context) {
        return StepsUtil.getApplicationCloudModelBuilder(context);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_STOPPING_DEPENDENT_MODULES, context.getVariable(Variables.APP_TO_PROCESS)
                                                                                            .getName());
    }

    private Module getCurrentModuleToStop(ProcessContext context) {
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

    private Set<CloudRoute> getApplicationRoutes(ProcessContext context, CloudApplicationExtended modifiedApp) {
        if (Boolean.TRUE.equals(context.getVariable(Variables.USE_IDLE_URIS))) {
            return modifiedApp.getIdleRoutes();
        }
        return modifiedApp.getRoutes();
    }

}
