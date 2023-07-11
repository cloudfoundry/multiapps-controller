package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.ArrayList;
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
 * Needed for backwards compatibility due to changes in how this parameter was serialized between versions - from Resource to a
 * CloudServiceInstanceExtended. This class should be removed in a following release.
 *
 */
public class BatchToProcessSerializationAdapter implements Serializer<List<CloudServiceInstanceExtended>> {

    public static TypeReference<List<CloudServiceInstanceExtended>> BATCH_TO_PROCESS_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };
    public static TypeReference<CloudServiceInstanceExtended> BATCH_TO_PROCESS_ELEMENT_TYPE_REFERENCE = new TypeReference<>() {
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
    public Object serialize(List<CloudServiceInstanceExtended> value) {
        return value.stream()
                    .map(JsonUtil::toJson)
                    .collect(Collectors.toList());
    }

    @Override
    public List<CloudServiceInstanceExtended> deserialize(Object serializedValue, VariableContainer container) {
        List<CloudServiceInstanceExtended> instances = new ArrayList<>();
        if (serializedValue instanceof String) {
            try {
                deserialize(serializedValue).forEach(instances::add);
            } catch (ParsingException e) {
                deserializeAsResource(serializedValue, container).forEach(instances::add);
            }
        } else if (serializedValue instanceof List<?>) {
            List<?> listOfSerializedValue = (List<?>) serializedValue;
            if (!listOfSerializedValue.isEmpty() && listOfSerializedValue.get(0) instanceof String) {
                List<String> listIfSeralizedInstances = (List<String>) serializedValue;
                listIfSeralizedInstances.stream()
                                        .map(s -> JsonUtil.fromJsonBinary((s).getBytes(), BATCH_TO_PROCESS_ELEMENT_TYPE_REFERENCE))
                                        .collect(Collectors.toList())
                                        .forEach(instances::add);
            }

        }
        return instances;
    }

    @Override
    public List<CloudServiceInstanceExtended> deserialize(Object serializedValue) {
        return JsonUtil.fromJsonBinary(((String) serializedValue).getBytes(), BATCH_TO_PROCESS_LIST_TYPE_REFERENCE);
    }

    public List<CloudServiceInstanceExtended> deserializeAsResource(Object serializedValue, VariableContainer container) {
        List<Resource> serviceInstances = JsonUtil.fromJsonBinary(((String) serializedValue).getBytes(), RESOURCE_LIST_TYPE_REFERENCE);
        ServicesCloudModelBuilder servicesCloudModelBuilder = getServicesCloudModelBuilder(container);
        return servicesCloudModelBuilder.build(serviceInstances);
    }
}
