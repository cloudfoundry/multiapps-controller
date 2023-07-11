package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ServicesCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 
 * Needed for backwards compatibility due to changes in how this parameter was serialized between versions - from <List<Resource> to a
 * List<CloudServiceInstanceExtended>. This class should be removed in a following release.
 *
 */
public class BatchesToProcessSerializationAdapter implements Serializer<List<List<CloudServiceInstanceExtended>>> {

    public static TypeReference<List<List<CloudServiceInstanceExtended>>> BATCHES_TO_PROCESS_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };
    public static TypeReference<List<CloudServiceInstanceExtended>> BATCHES_TO_PROCESS_ELEMENT_TYPE_REFERENCE = new TypeReference<>() {
    };

    private static TypeReference<List<Resource>> RESOURCE_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };

    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(VariableContainer container) {
        CloudHandlerFactory handlerFactory = StepsUtil.getHandlerFactory((VariableScope) container);
        DeploymentDescriptor deploymentDescriptor = VariableHandling.get(container, Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR);
        String namespace = (String) container.getVariable(Variables.MTA_NAMESPACE.getName());
        return handlerFactory.getServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    public Object serialize(List<List<CloudServiceInstanceExtended>> value) {
        return value.stream()
                    .map(JsonUtil::toJson)
                    .collect(Collectors.toList());
    }

    @Override
    public List<List<CloudServiceInstanceExtended>> deserialize(Object serializedValue, VariableContainer container) {
        List<?> outerBatchesList = (List<?>) serializedValue;
        if (!outerBatchesList.isEmpty()) {
            return tryToDeserialize(outerBatchesList, container);
        }
        return Collections.emptyList();
    }

    @Override
    public List<List<CloudServiceInstanceExtended>> deserialize(Object serializedValue) {
        return ((List<String>) serializedValue).stream()
                                               .map(s -> JsonUtil.fromJsonBinary((s).getBytes(), BATCHES_TO_PROCESS_ELEMENT_TYPE_REFERENCE))
                                               .collect(Collectors.toList());
    }

    private List<List<CloudServiceInstanceExtended>> tryToDeserialize(Object serializedValue, VariableContainer container) {
        try {
            return deserialize(serializedValue);
        } catch (ParsingException e) {
            return deserializeAsResource(serializedValue, container);
        }
    }

    private List<List<CloudServiceInstanceExtended>> deserializeAsResource(Object serializedValue, VariableContainer container) {
        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(container);
        List<List<Resource>> serviceInstances = ((List<String>) serializedValue).stream()
                                                                                .map(s -> JsonUtil.fromJsonBinary((s).getBytes(),
                                                                                                                  RESOURCE_LIST_TYPE_REFERENCE))
                                                                                .collect(Collectors.toList());
        return serviceInstances.stream()
                               .map(servicesCloudModelBuilder::build)
                               .collect(Collectors.toList());
    }

}
