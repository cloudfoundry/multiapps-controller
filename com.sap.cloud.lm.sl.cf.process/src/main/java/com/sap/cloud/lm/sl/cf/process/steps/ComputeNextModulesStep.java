package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerialization;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDependencyChecker;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@Named("computeNextModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextModulesStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION);
        List<Module> allModulesToDeploy = context.getVariable(Variables.MODULES_TO_DEPLOY);
        List<Module> completedModules = context.getVariable(Variables.ITERATED_MODULES_IN_PARALLEL);

        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        ModuleDependencyChecker dependencyChecker = new ModuleDependencyChecker(context.getControllerClient(),
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
