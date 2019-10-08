package com.sap.cloud.lm.sl.cf.process.steps;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceInstanceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.v3.ServicesCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.resolvers.v3.ServiceDependencyResolver;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named("computeNextServicesToProcess")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ComputeNextServicesToProcessStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        DeploymentDescriptor descriptor = context.getVariable(Variables.DEPLOYMENT_DESCRIPTOR);
        ServiceDependencyResolver dependencyResolver = new ServiceDependencyResolver(descriptor);

        Map<Resource, Set<Resource>> serviceDependencies = context.getVariable(Variables.SERVICE_DEPENDENCIES);

        serviceDependencies = dependencyResolver.getServicesToProcess(serviceDependencies);
        Set<Resource> servicesToProcessInParallel = dependencyResolver.getServicesWithoutDependencies(serviceDependencies);

        ServicesCloudModelBuilder servicesBuilder = new ServicesCloudModelBuilder(descriptor);
        List<CloudServiceInstanceExtended> servicesToProcess = servicesBuilder.build(new ArrayList<>(servicesToProcessInParallel));

        context.setVariable(Variables.SERVICE_DEPENDENCIES, serviceDependencies);
        context.setVariable(Variables.SERVICES_TO_PROCESS_IN_PARALLEL, servicesToProcess);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_COMPUTING_NEXT_SERVICES_FOR_PARALLEL_ITERATION;
    }

}
