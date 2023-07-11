package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.resolvers.v3.DynamicParametersResolver;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerialization;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.helpers.VisitableObject;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

@Named("extractBatchedServicesWithResolvedDynamicParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExtractBatchedServicesWithResolvedDynamicParametersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.EXTACT_SERVICES_AND_RESOLVE_DYNAMIC_PARAMETERS_FROM_BATCH);

        Set<DynamicResolvableParameter> dynamicResolvableParameters = context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS);
        List<CloudServiceInstanceExtended> servicesCalculatedForDeployment = context.getVariableBackwardsCompatible(Variables.BATCH_TO_PROCESS);
        Set<DynamicResolvableParameter> dynamicParametersWithResolvedExistingInstances = resolveDynamicPramatersWithExistingInstances(context.getControllerClient(),
                                                                                                                                      dynamicResolvableParameters,
                                                                                                                                      servicesCalculatedForDeployment);

        List<CloudServiceInstanceExtended> resolvedServiceInstances = servicesCalculatedForDeployment.stream()
                                                                                                     .map(service -> resolveDynamicParametersOfServiceInstance(service,
                                                                                                                                                               dynamicParametersWithResolvedExistingInstances))
                                                                                                     .collect(Collectors.toList());

        setServicesToCreate(context, resolvedServiceInstances);
        context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS, dynamicParametersWithResolvedExistingInstances);
        return StepPhase.DONE;
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return Messages.ERROR_PREPARING_RESOURCES_FOR_PROCESSING_AND_RESOLVE_DYNAMIC_PARAMETERS;
    }

    private Set<DynamicResolvableParameter>
            resolveDynamicPramatersWithExistingInstances(CloudControllerClient client,
                                                         Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                                         List<CloudServiceInstanceExtended> servicesCalculatedForDeployment) {
        Map<String, String> existingServiceGuids = getExistingServiceGuidsIfNeeded(client, dynamicResolvableParameters,
                                                                                   servicesCalculatedForDeployment);
        Set<DynamicResolvableParameter> resolvedDynamicParameters = new HashSet<>(dynamicResolvableParameters);
        for (var dynamicParameter : dynamicResolvableParameters) {
            if (existingServiceGuids.containsKey(dynamicParameter.getRelationshipEntityName())) {
                resolvedDynamicParameters.remove(dynamicParameter);
                resolvedDynamicParameters.add(ImmutableDynamicResolvableParameter.copyOf(dynamicParameter)
                                                                                 .withValue(existingServiceGuids.get(dynamicParameter.getRelationshipEntityName())));
            }
        }
        return resolvedDynamicParameters;
    }

    private Map<String, String> getExistingServiceGuidsIfNeeded(CloudControllerClient client,
                                                                Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                                                List<CloudServiceInstanceExtended> servicesCalculatedForDeployment) {
        Map<String, String> existingServiceGuids = new HashMap<>();
        for (var serviceCalculatedForDeployment : servicesCalculatedForDeployment) {
            if (!serviceCalculatedForDeployment.isManaged()
                && isServiceInstanceGuidRequired(dynamicResolvableParameters, serviceCalculatedForDeployment)) {
                existingServiceGuids.put(serviceCalculatedForDeployment.getResourceName(),
                                         client.getRequiredServiceInstanceGuid(serviceCalculatedForDeployment.getName())
                                               .toString());
            }

        }

        return existingServiceGuids;
    }

    private boolean isServiceInstanceGuidRequired(Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                                  CloudServiceInstanceExtended serviceCalculatedForDeployment) {
        return dynamicResolvableParameters.stream()
                                          .anyMatch(dynamicParameter -> dynamicParameter.getRelationshipEntityName()
                                                                                        .equals(serviceCalculatedForDeployment.getResourceName()));
    }

    private CloudServiceInstanceExtended
            resolveDynamicParametersOfServiceInstance(CloudServiceInstanceExtended service,
                                                      Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        DynamicParametersResolver resolver = new DynamicParametersResolver(service.getResourceName(),
                                                                           new DynamicResolvableParametersHelper(dynamicResolvableParameters));
        Map<String, Object> resolvedServiceParameters = MiscUtil.cast(new VisitableObject(service.getCredentials()).accept(resolver));
        return ImmutableCloudServiceInstanceExtended.copyOf(service)
                                                    .withCredentials(resolvedServiceParameters);

    }

    private void setServicesToCreate(ProcessContext context, List<CloudServiceInstanceExtended> servicesCalculatedForDeployment) {

        List<CloudServiceInstanceExtended> servicesToCreate = servicesCalculatedForDeployment.stream()
                                                                                             .filter(CloudServiceInstanceExtended::isManaged)
                                                                                             .collect(Collectors.toList());
        getStepLogger().debug(Messages.SERVICES_TO_CREATE, SecureSerialization.toJson(servicesToCreate));
        context.setVariable(Variables.SERVICES_TO_CREATE, servicesToCreate);
        context.setVariable(Variables.SERVICES_TO_CREATE_COUNT, servicesToCreate.size());
    }

}
