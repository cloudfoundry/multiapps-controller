package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.resolvers.v3.DynamicParametersResolver;
import org.cloudfoundry.multiapps.controller.core.security.serialization.DynamicSecureSerialization;
import org.cloudfoundry.multiapps.controller.core.security.serialization.SecureSerializationFactory;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.helpers.VisitableObject;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("extractBatchedServicesWithResolvedDynamicParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ExtractBatchedServicesWithResolvedDynamicParametersStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        getStepLogger().debug(Messages.EXTRACT_SERVICES_AND_RESOLVE_DYNAMIC_PARAMETERS_FROM_BATCH);
        Set<String> secretParameters = context.getVariable(Variables.SECURE_EXTENSION_DESCRIPTOR_PARAMETER_NAMES);
        DynamicSecureSerialization dynamicSecureSerialization = SecureSerializationFactory.ofAdditionalValues(secretParameters);

        Set<DynamicResolvableParameter> dynamicResolvableParameters = context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS);
        List<CloudServiceInstanceExtended> servicesCalculatedForDeployment = context.getVariableBackwardsCompatible(
            Variables.BATCH_TO_PROCESS);
        Set<DynamicResolvableParameter> dynamicParametersWithResolvedExistingInstances = resolveDynamicPramatersWithExistingInstances(
            context.getControllerClient(),
            dynamicResolvableParameters,
            servicesCalculatedForDeployment);

        List<CloudServiceInstanceExtended> resolvedServiceInstances = servicesCalculatedForDeployment.stream()
                                                                                                     .map(
                                                                                                         service -> resolveDynamicParametersOfServiceInstance(
                                                                                                             service,
                                                                                                             dynamicParametersWithResolvedExistingInstances))
                                                                                                     .collect(Collectors.toList());

        checkForDuplicatedServiceNameFields(resolvedServiceInstances);
        setServicesToCreate(context, resolvedServiceInstances, dynamicSecureSerialization);
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
                                                                                 .withValue(existingServiceGuids.get(
                                                                                     dynamicParameter.getRelationshipEntityName())));
            }
        }
        return resolvedDynamicParameters;
    }

    private Map<String, String> getExistingServiceGuidsIfNeeded(CloudControllerClient client,
                                                                Set<DynamicResolvableParameter> dynamicResolvableParameters,
                                                                List<CloudServiceInstanceExtended> servicesCalculatedForDeployment) {
        Map<String, String> existingServiceGuids = new HashMap<>();
        for (var serviceCalculatedForDeployment : servicesCalculatedForDeployment) {
            if (!serviceCalculatedForDeployment.isManaged() && isServiceInstanceGuidRequired(dynamicResolvableParameters,
                                                                                             serviceCalculatedForDeployment)) {
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
                                                                                        .equals(
                                                                                            serviceCalculatedForDeployment.getResourceName()));
    }

    private CloudServiceInstanceExtended
    resolveDynamicParametersOfServiceInstance(CloudServiceInstanceExtended service,
                                              Set<DynamicResolvableParameter> dynamicResolvableParameters) {
        DynamicParametersResolver resolver = new DynamicParametersResolver(service.getResourceName(),
                                                                           new DynamicResolvableParametersHelper(
                                                                               dynamicResolvableParameters));
        Map<String, Object> resolvedServiceParameters = MiscUtil.cast(new VisitableObject(service.getCredentials()).accept(resolver));
        return ImmutableCloudServiceInstanceExtended.copyOf(service)
                                                    .withCredentials(resolvedServiceParameters);

    }

    private void setServicesToCreate(ProcessContext context, List<CloudServiceInstanceExtended> servicesCalculatedForDeployment,
                                     DynamicSecureSerialization dynamicSecureSerialization) {

        List<CloudServiceInstanceExtended> servicesToCreate = servicesCalculatedForDeployment.stream()
                                                                                             .filter(
                                                                                                 CloudServiceInstanceExtended::isManaged)
                                                                                             .collect(Collectors.toList());
        getStepLogger().debug(Messages.SERVICES_TO_CREATE, dynamicSecureSerialization.toJson(servicesToCreate));
        context.setVariable(Variables.SERVICES_TO_CREATE, servicesToCreate);
        context.setVariable(Variables.SERVICES_TO_CREATE_COUNT, servicesToCreate.size());
    }

    private void checkForDuplicatedServiceNameFields(List<CloudServiceInstanceExtended> resolvedServiceInstances) {
        List<String> resolvedServiceInstancesNames = resolvedServiceInstances.stream()
                                                                             .map(CloudEntity::getName)
                                                                             .toList();

        List<String> duplicatedNames = getDuplicatedNames(resolvedServiceInstancesNames);
        if (!duplicatedNames.isEmpty()) {
            getStepLogger().warn(
                Messages.ONLY_FIRST_SERVICE_WILL_BE_CREATED, String.join(" ", duplicatedNames));
        }
    }

    private List<String> getDuplicatedNames(List<String> resolvedServiceInstancesNames) {
        List<String> duplicatedNames = new ArrayList<>();
        Map<String, Integer> frequencyOfNamesMap = new HashMap<>();
        for (String name : resolvedServiceInstancesNames) {
            frequencyOfNamesMap.merge(name, 1, Integer::sum);
        }

        for (String name : frequencyOfNamesMap.keySet()) {
            if (frequencyOfNamesMap.get(name) > 1) {
                duplicatedNames.add(name);
            }
        }
        return duplicatedNames;
    }

}
