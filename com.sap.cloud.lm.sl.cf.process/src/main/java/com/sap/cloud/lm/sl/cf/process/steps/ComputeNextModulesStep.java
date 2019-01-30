package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("computeNextModulesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextModulesStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        getStepLogger().debug(Messages.COMPUTING_NEXT_MODULES_FOR_PARALLEL_ITERATION);
        List<ModuleToDeploy> allModulesToDeploy = StepsUtil.getModulesToDeploy(execution.getContext());
        List<ModuleToDeploy> completedApplications = StepsUtil.getIteratedModulesInParallel(execution.getContext());
        List<String> completedModuleNames = completedApplications.stream()
            .map(ModuleToDeploy::getName)
            .collect(Collectors.toList());

        CloudControllerClient client = execution.getControllerClient();
        // Set next iteration data
        List<ModuleToDeploy> modulesForNextIteration = computeApplicationsForNextIteration(client, allModulesToDeploy,
            completedModuleNames);
        StepsUtil.setModulesToIterateInParallel(execution.getContext(), modulesForNextIteration);

        // Mark next iteration data as computed
        StepsUtil.setIteratedModulesInParallel(execution.getContext(), ListUtils.union(completedApplications, modulesForNextIteration));

        getStepLogger().debug(Messages.COMPUTED_NEXT_MODULES_FOR_PARALLEL_ITERATION, secureSerializer.toJson(modulesForNextIteration));
        return StepPhase.DONE;
    }

    private List<ModuleToDeploy> computeApplicationsForNextIteration(CloudControllerClient client, List<ModuleToDeploy> allModulesToDeploy,
        List<String> completedModules) {
        allModulesToDeploy.removeIf(module -> completedModules.contains(module.getName()));
        return allModulesToDeploy.stream()
            .filter(module -> applicationHasAllDependenciesSatisfied(client, completedModules, module))
            .collect(Collectors.toList());
    }

    private boolean applicationHasAllDependenciesSatisfied(CloudControllerClient client, List<String> completedModules,
        ModuleToDeploy app) {
        return app.getDeployedAfter()
            .isEmpty() || completedModules.containsAll(app.getDeployedAfter())
            || areAllDependenciesAlreadyDeployed(client, app.getDeployedAfter());
    }

    private boolean areAllDependenciesAlreadyDeployed(CloudControllerClient client, Set<String> deployedAfter) {
        List<CloudApplication> nonDeployedApplications = deployedAfter.stream()
            .map(deployAfterDependency -> client.getApplication(deployAfterDependency, false))
            .filter(Objects::isNull)
            .collect(Collectors.toList());
        return nonDeployedApplications.isEmpty();
    }

}
