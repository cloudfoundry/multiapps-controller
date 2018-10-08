package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.security.serialization.SecureSerializationFacade;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component("computeNextAppsStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextAppsStep extends SyncFlowableStep {

    protected SecureSerializationFacade secureSerializer = new SecureSerializationFacade();

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws Exception {
        getStepLogger().debug(Messages.COMPUTING_NEXT_APPS_FOR_PARALLEL_ITERATION);
        List<CloudApplicationExtended> allApplicationsToDeploy = StepsUtil.getAppsToDeploy(execution.getContext());
        List<CloudApplicationExtended> completedApplications = StepsUtil.getIteratedAppsInParallel(execution.getContext());
        List<String> completedModuleNames = completedApplications.stream()
            .map(CloudApplicationExtended::getModuleName)
            .collect(Collectors.toList());

        // Set next iteration data
        List<CloudApplicationExtended> applicationsForNextIteration = computeApplicationsForNextIteration(allApplicationsToDeploy,
            completedModuleNames);
        StepsUtil.setAppsToIterateInParallel(execution.getContext(), applicationsForNextIteration);

        // Mark next iteration data as computed
        StepsUtil.setIteratedAppsInParallel(execution.getContext(), ListUtils.union(completedApplications, applicationsForNextIteration));

        getStepLogger().debug(Messages.COMPUTED_NEXT_APPS_FOR_PARALLEL_ITERATION, secureSerializer.toJson(applicationsForNextIteration));
        return StepPhase.DONE;
    }

    private List<CloudApplicationExtended> computeApplicationsForNextIteration(List<CloudApplicationExtended> allApplicationsToDeploy,
        List<String> completedModules) {
        allApplicationsToDeploy.removeIf(app -> completedModules.contains(app.getModuleName()));
        return allApplicationsToDeploy.stream()
            .filter(app -> applicationHasAllDependenciesSatisfied(completedModules, app))
            .collect(Collectors.toList());
    }

    private boolean applicationHasAllDependenciesSatisfied(List<String> completedModules, CloudApplicationExtended app) {
        return app.getDeployedAfter()
            .isEmpty() || completedModules.containsAll(app.getDeployedAfter());
    }

}
