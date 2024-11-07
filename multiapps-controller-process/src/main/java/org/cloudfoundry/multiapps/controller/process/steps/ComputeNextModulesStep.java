package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ModuleDependencyChecker;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("computeNextModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextModulesStep extends SyncFlowableStep {

    private ModuleToDeployHelper moduleToDeployHelper;

    @Inject
    public ComputeNextModulesStep(ModuleToDeployHelper moduleToDeployHelper) {
        this.moduleToDeployHelper = moduleToDeployHelper;
    }

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION);
        List<Module> allModulesToDeploy = context.getVariable(Variables.MODULES_TO_DEPLOY);
        List<Module> completedModules = context.getVariable(Variables.ITERATED_MODULES_IN_PARALLEL);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        ModuleDependencyChecker dependencyChecker = new ModuleDependencyChecker(context.getControllerClient(),
                                                                                getStepLogger(),
                                                                                moduleToDeployHelper,
                                                                                descriptor.getModules(),
                                                                                allModulesToDeploy,
                                                                                completedModules);
        getStepLogger().debug("Completed modules detected: " + dependencyChecker.getAlreadyDeployedModules());
        getStepLogger().debug("All modules for deploy detected: " + dependencyChecker.getModulesForDeployment());
        getStepLogger().debug("Modules not for deploy detected: " + dependencyChecker.getModulesNotForDeployment());

        // Set next iteration data
        List<Module> modulesForNextIteration = computeApplicationsForNextIteration(allModulesToDeploy, dependencyChecker);
        context.setVariable(Variables.MODULES_TO_ITERATE_IN_PARALLEL, modulesForNextIteration);

        // Mark next iteration data as computed
        context.setVariable(Variables.ITERATED_MODULES_IN_PARALLEL, ListUtils.union(completedModules, modulesForNextIteration));

        getStepLogger().debug(Messages.COMPUTED_NEXT_MODULES_FOR_PARALLEL_ITERATION, SecureSerialization.toJson(modulesForNextIteration));
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION;
    }

    private List<Module> computeApplicationsForNextIteration(List<Module> allModulesToDeploy, ModuleDependencyChecker dependencyChecker) {
        return allModulesToDeploy.stream()
                                 .filter(module -> !dependencyChecker.getAlreadyDeployedModules()
                                                                     .contains(module.getName()))
                                 .filter(dependencyChecker::areAllDependenciesSatisfied)
                                 .collect(Collectors.toList());
    }

}
