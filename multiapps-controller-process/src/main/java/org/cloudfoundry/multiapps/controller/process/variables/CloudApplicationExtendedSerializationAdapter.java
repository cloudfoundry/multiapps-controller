package org.cloudfoundry.multiapps.controller.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.Lifecycle;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended.AttributeUpdateStrategy;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.RestartParameters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CloudApplicationExtendedSerializationAdapter implements Serializer<CloudApplicationExtended> {

    @Override
    public Object serialize(CloudApplicationExtended value) {
        return JsonUtil.toJson(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CloudApplicationExtended deserialize(Object serializedValue) {
        Map<String, Object> appMap = JsonUtil.fromJson((String) serializedValue, new TypeReference<>() {
        });
        if (appMap.containsKey("lifecycle")) {
            return JsonUtil.fromJson((String) serializedValue, CloudApplicationExtended.class);
        }
        var name = (String) appMap.get("name");
        var state = (String) appMap.get("state");
        var staging = (Map<String, Object>) appMap.getOrDefault("staging", Collections.emptyMap());
        var dockerInfo = staging.get("dockerInfo");
        Lifecycle lifecycle;
        if (dockerInfo != null) {
            lifecycle = ImmutableLifecycle.builder()
                                          .type(LifecycleType.DOCKER)
                                          .data(Map.of())
                                          .build();
        } else {
            var buildpacks = (List<String>) staging.getOrDefault("buildpacks", Collections.emptyList());
            var stack = (String) staging.getOrDefault("stackName", "");
            lifecycle = ImmutableLifecycle.builder()
                                          .type(LifecycleType.BUILDPACK)
                                          .data(Map.of("buildpacks", buildpacks, "stack", stack))
                                          .build();
        }
        var routes = (List<Map<String, Object>>) appMap.getOrDefault("routes", Collections.emptyList());
        var idleRoutes = (List<Map<String, Object>>) appMap.getOrDefault("idleRoutes", Collections.emptyList());
        var moduleName = (String) appMap.get("moduleName");
        var memory = (Integer) appMap.getOrDefault("memory", 0);
        var diskQuota = (Integer) appMap.getOrDefault("diskQuota", 0);
        var instances = (Integer) appMap.getOrDefault("instances", 1);
        var services = (List<String>) appMap.getOrDefault("services", Collections.emptyList());
        var env = (Map<String, String>) appMap.getOrDefault("env", Collections.emptyMap());
        var bindingParameters = (Map<String, Map<String, Object>>) appMap.getOrDefault("bindingParameters", Collections.emptyMap());
        return ImmutableCloudApplicationExtended.builder()
                                                .name(name)
                                                .metadata(deserializeObject(appMap.get("metadata"), CloudMetadata.class))
                                                .v3Metadata(deserializeObject(appMap.get("v3Metadata"), Metadata.class))
                                                .moduleName(moduleName)
                                                .lifecycle(lifecycle)
                                                .state(state == null ? CloudApplication.State.STOPPED : CloudApplication.State.valueOf(state))
                                                .space(deserializeObject(appMap.get("space"), CloudSpace.class))
                                                .memory(memory)
                                                .diskQuota(diskQuota)
                                                .instances(instances)
                                                .staging(deserializeObject(staging, Staging.class))
                                                .services(services)
                                                .env(env)
                                                .bindingParameters(bindingParameters)
                                                .tasks(deserializeObject(appMap.getOrDefault("tasks", Collections.emptyList()), new TypeReference<>() {
                                                }))
                                                .serviceKeysToInject(deserializeObject(appMap.getOrDefault("serviceKeysToInject", Collections.emptyList()), new TypeReference<>() {
                                                }))
                                                .restartParameters(deserializeObject(appMap.get("restartParameters"), RestartParameters.class))
                                                .dockerInfo(deserializeObject(appMap.get("dockerInfo"), DockerInfo.class))
                                                .attributesUpdateStrategy(deserializeObject(appMap.get("attributesUpdateStrategy"), AttributeUpdateStrategy.class))
                                                .routes(routes.stream()
                                                              .map(this::resolveRoute)
                                                              .collect(Collectors.toList()))
                                                .idleRoutes(idleRoutes.stream()
                                                                      .map(this::resolveRoute)
                                                                      .collect(Collectors.toList()))
                                                .build();
    }

    private CloudRoute resolveRoute(Map<String, Object> routeMap) {
        UUID guid = null;
        var guidValue = (String) routeMap.get("guid");
        if (guidValue != null) {
            guid = UUID.fromString(guidValue);
        }

        UUID domainGuid = null;
        var domainGuidValue = (String) routeMap.get("domainGuid");
        if (domainGuidValue != null) {
            domainGuid = UUID.fromString(domainGuidValue);
        }

        var domainValue = routeMap.get("domain");
        String domain;
        if (domainValue instanceof Map) {
            domain = (String) ((Map<String, Object>) domainValue).get("name");
        } else {
            domain = (String) domainValue;
        }
        var host = (String) routeMap.get("host");
        var path = (String) routeMap.get("path");
        var port = (Integer) routeMap.get("port");

        String url = null;
        if (host != null) {
            url = host + ".";
        }
        url += domain;
        if (port != null) {
            url += ":" + port;
        }
        if (path != null) {
            url += path;
        }

        return ImmutableCloudRoute.builder()
                                  .metadata(ImmutableCloudMetadata.of(guid))
                                  .path(path)
                                  .domain(ImmutableCloudDomain.builder()
                                                              .metadata(ImmutableCloudMetadata.of(domainGuid))
                                                              .name(domain)
                                                              .build())
                                  .port(port)
                                  .host(host)
                                  .url(url)
                                  .build();
    }

    private <T> T deserializeObject(Object value, Class<T> clazz) {
        String serializedValue = JsonUtil.toJson(value);
        return JsonUtil.fromJson(serializedValue, clazz);
    }

    private <T> T deserializeObject(Object value, TypeReference<T> reference) {
        String serializedValue = JsonUtil.toJson(value);
        return JsonUtil.fromJson(serializedValue, reference);
    }
}
