package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import java.util.List;

import org.cloudfoundry.client.v3.Metadata;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class AppMetadataMapper extends MetadataMapper {

    public String getModuleName(Metadata metadata) {
        return metadata.getAnnotations().getOrDefault("module_name", null);
    }

    public List<String> getProvidedDependencyNames(Metadata metadata) {
        String providedDependencyNamesJson = metadata.getAnnotations().getOrDefault("provided_dependency_names", null);
        return JsonUtil.convertJsonToList(providedDependencyNamesJson, new TypeToken<List<String>>() {
        }.getType());
    }

    public List<DeployedMtaResource> getResource(Metadata metadata) {
        String resourcesJson = metadata.getAnnotations().getOrDefault("resources", null);
        return JsonUtil.fromJson(resourcesJson, new TypeToken<List<DeployedMtaResource>>() {
        }.getType());
    }

}
