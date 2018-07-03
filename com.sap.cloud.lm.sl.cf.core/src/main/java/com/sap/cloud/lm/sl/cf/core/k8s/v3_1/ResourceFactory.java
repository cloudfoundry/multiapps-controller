package com.sap.cloud.lm.sl.cf.core.k8s.v3_1;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.Module;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

import io.fabric8.kubernetes.api.model.HasMetadata;

public interface ResourceFactory {

    default List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Module module, Map<String, String> labels) {
        return Collections.emptyList();
    }

    default List<HasMetadata> createFrom(DeploymentDescriptor descriptor, Resource resource, Map<String, String> labels) {
        return Collections.emptyList();
    }

    List<String> getSupportedResourceTypes();

}
