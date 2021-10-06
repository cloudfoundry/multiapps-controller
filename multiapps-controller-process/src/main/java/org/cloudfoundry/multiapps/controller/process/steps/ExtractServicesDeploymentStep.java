package org.cloudfoundry.multiapps.controller.process.steps;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import javax.inject.Named;
import java.util.List;
import java.util.stream.Collectors;

@Named("extractServicesDeploymentStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExtractServicesDeploymentStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.EXTRACT_SERVICES_FROM_BATCH);
        // get singular batch of resources variable
        List<Resource> batchOfResources = context.getVariable(Variables.BATCH_TO_PROCESS);
        // extract all services from the batch of resources
        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(context);
        List<CloudServiceInstanceExtended> servicesCalculatedForDeployment = servicesCloudModelBuilder.build(batchOfResources);
        // Build a list of services for creation and save them in the context:
        List<CloudServiceInstanceExtended> servicesToCreate = servicesCalculatedForDeployment.stream()
                                                                                             .filter(CloudServiceInstanceExtended::isManaged)
                                                                                             .collect(Collectors.toList());
        getStepLogger().debug(Messages.SERVICES_TO_CREATE, SecureSerialization.toJson(servicesToCreate));
        context.setVariable(Variables.SERVICES_TO_CREATE, servicesToCreate);

        // Needed by CreateOrUpdateServicesStep, as it is used as an iteration variable:
        context.setVariable(Variables.SERVICES_TO_CREATE_COUNT, servicesToCreate.size());

        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_RESOURCES_FOR_PROCESSING;
    }

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(ProcessContext context) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory(context.getExecution());
        DeploymentDescriptor deploymentDescriptor = context.getVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        String namespace = context.getVariable(Variables.MTA_NAMESPACE);

        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }
}
