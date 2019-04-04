package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v3.Metadata;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ServiceMetadataMapper extends MetadataMapper {

    public List<String> getBoundApps(Metadata metadata) {
        String boundAppsJson = metadata.getAnnotations().getOrDefault("bound_apps", null);
        return JsonUtil.convertJsonToList(boundAppsJson, new TypeToken<List<String>>() {
        }.getType());
    }

    public Map<String, String> getAppsCredentials(Metadata metadata) {
        String appsCredentialsJson = metadata.getAnnotations().getOrDefault("apps_credentials", null);
        return JsonUtil.fromJson(appsCredentialsJson, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    public DeployedMtaResource getResource(Metadata metadata) {
        String resourceJson = metadata.getAnnotations().getOrDefault("resource", null);
        return JsonUtil.fromJson(resourceJson, new TypeToken<DeployedMtaResource>() {
        }.getType());
    }

}
