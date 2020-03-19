package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ModuleDependencyChecker;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;

@Named("computeNextModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextModulesStep extends SyncFlowableStep {

    protected final SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION);
        List<Module> allModulesToDeploy = StepsUtil.getModulesToDeploy(context.getExecution());
        List<Module> completedModules = StepsUtil.getIteratedModulesInParallel(context.getExecution());

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
        StepsUtil.setModulesToIterateInParallel(context.getExecution(), modulesForNextIteration);

        // Mark next iteration data as computed
        StepsUtil.setIteratedModulesInParallel(context.getExecution(), ListUtils.union(completedModules, modulesForNextIteration));

        getStepLogger().debug(Messages.COMPUTED_NEXT_MODULES_FOR_PARALLEL_ITERATION, secureSerializer.toJson(modulesForNextIteration));
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION;
    }

    private List<Module> computeApplicationsForNextIteration(List<Module> allModulesToDeploy, ModuleDependencyChecker dependencyChecker) {
        allModulesToDeploy.removeIf(module -> dependencyChecker.getAlreadyDeployedModules()
                                                               .contains(module.getName()));

        return allModulesToDeploy.stream()
                                 .filter(dependencyChecker::areAllDependenciesSatisfied)
                                 .collect(Collectors.toList());
    }

}
