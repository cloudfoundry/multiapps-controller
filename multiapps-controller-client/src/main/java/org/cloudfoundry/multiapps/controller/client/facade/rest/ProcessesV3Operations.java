package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableDropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Droplet;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3InstancesInfoMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Process;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ProcessMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

/**
 * CF v3 <em>process</em>-family operations for the cf-java-client replacement. Reproduces the HTTP shape and domain mapping of the
 * OSS {@code CloudControllerRestClientImpl} process/instance/feature/droplet methods:
 * <ul>
 * <li>{@code getApplicationProcess(appGuid)} &rarr; {@code GET /v3/apps/{guid}/processes/web};</li>
 * <li>{@code getApplicationInstances(app)} / {@code getApplicationInstances(appGuid)} &rarr;
 * {@code GET /v3/apps/{guid}/processes/web/stats} (empty for a non-STARTED app);</li>
 * <li>{@code getApplicationSshEnabled(appGuid)} &rarr; {@code GET /v3/apps/{guid}/ssh_enabled} (default {@code false});</li>
 * <li>{@code getApplicationFeatures(appGuid)} &rarr; {@code GET /v3/apps/{guid}/features} (paginated);</li>
 * <li>{@code getCurrentDropletForApplication(appGuid)} &rarr; {@code GET /v3/apps/{guid}/droplets/current} (404 &rarr;
 * {@link CloudOperationException});</li>
 * <li>{@code updateApplicationStaging(appName, staging)} &rarr; {@code PATCH /v3/apps/{guid}} (lifecycle) then update the web process
 * ({@code PATCH /v3/apps/{guid}/features/{name}} per feature and {@code PATCH /v3/processes/{guid}}).</li>
 * </ul>
 */
public class ProcessesV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;
    // The "package" link key in the current-droplet response; the package GUID is the last path segment of its href.
    private static final String PACKAGE_LINK = "package";
    private static final String WEB_PROCESS_TYPE = "web";

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ProcessesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        V3Process process = cc.get("/v3/apps/" + applicationGuid + "/processes/" + WEB_PROCESS_TYPE, V3Process.class);
        return process == null ? null : V3ProcessMapper.toCloudProcess(process);
    }

    public InstancesInfo getApplicationInstances(CloudApplication application) {
        if (application.getState()
                       .equals(CloudApplication.State.STARTED)) {
            return findApplicationInstances(application.getGuid());
        }
        return ImmutableInstancesInfo.builder()
                                     .instances(Collections.emptyList())
                                     .build();
    }

    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        return findApplicationInstances(applicationGuid);
    }

    private InstancesInfo findApplicationInstances(UUID applicationGuid) {
        V3Process.V3ProcessStats stats = cc.get("/v3/apps/" + applicationGuid + "/processes/" + WEB_PROCESS_TYPE + "/stats",
                                                 V3Process.V3ProcessStats.class);
        return V3InstancesInfoMapper.toInstancesInfo(stats);
    }

    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        V3Process.V3SshEnabled sshEnabled = cc.get("/v3/apps/" + applicationGuid + "/ssh_enabled", V3Process.V3SshEnabled.class);
        if (sshEnabled == null || sshEnabled.enabled() == null) {
            return false;
        }
        return sshEnabled.enabled();
    }

    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        List<V3Process.V3AppFeature> features = cc.list("/v3/apps/" + applicationGuid + "/features?per_page=" + DEFAULT_PAGE_SIZE,
                                                        new ParameterizedTypeReference<V3ListResponse<V3Process.V3AppFeature>>() {
                                                        });
        Map<String, Boolean> result = new HashMap<>();
        for (V3Process.V3AppFeature feature : features) {
            result.put(feature.name(), feature.enabled());
        }
        return result;
    }

    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        V3Droplet droplet = cc.get("/v3/apps/" + applicationGuid + "/droplets/current", V3Droplet.class);
        if (droplet == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not found",
                                              "Application with guid " + applicationGuid + " does not have a droplet");
        }
        return parseDropletInfo(droplet);
    }

    private DropletInfo parseDropletInfo(V3Droplet droplet) {
        String packageUrl = droplet.links()
                                   .get(PACKAGE_LINK)
                                   .href();
        if (packageUrl.endsWith("/")) {
            packageUrl = packageUrl.substring(0, packageUrl.lastIndexOf("/"));
        }
        String packageGuid = packageUrl.substring(packageUrl.lastIndexOf("/") + 1);
        return ImmutableDropletInfo.builder()
                                   .guid(UUID.fromString(droplet.guid()))
                                   .packageGuid(UUID.fromString(packageGuid))
                                   .build();
    }

    public void updateApplicationStaging(String applicationName, Staging staging) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}", applicationGuid)
          .body(Map.of("lifecycle", buildApplicationLifecycle(staging)))
          .retrieve()
          .toBodilessEntity();
        updateApplicationProcess(applicationGuid, staging);
    }

    // Mirrors the OSS impl's buildApplicationLifecycle: Docker if DockerInfo present, otherwise a buildpack (or CNB) lifecycle.
    private Map<String, Object> buildApplicationLifecycle(Staging staging) {
        if (staging.getDockerInfo() != null) {
            return Map.of("type", "docker", "data", Map.of());
        }
        String lifecycleType = staging.getLifecycleType() != null ? staging.getLifecycleType()
                                                                            .name()
                                                                            .toLowerCase()
                                                                  : "buildpack";
        if ("cnb".equals(lifecycleType) && (staging.getBuildpacks() == null || staging.getBuildpacks()
                                                                                      .isEmpty())) {
            throw new IllegalArgumentException("Buildpacks are required for the CNB lifecycle type");
        }
        Map<String, Object> data = new HashMap<>();
        if (staging.getStackName() != null) {
            data.put("stack", staging.getStackName());
        }
        if (staging.getBuildpacks() != null) {
            data.put("buildpacks", staging.getBuildpacks());
        }
        return Map.of("type", lifecycleType, "data", data);
    }

    // Mirrors the OSS impl's updateApplicationProcess: toggle app features, then PATCH the web process command + health checks.
    private void updateApplicationProcess(UUID applicationGuid, Staging staging) {
        staging.getAppFeatures()
               .forEach((featureName, enabled) -> updateAppFeature(applicationGuid, featureName, enabled));
        V3Process process = cc.get("/v3/apps/" + applicationGuid + "/processes/" + WEB_PROCESS_TYPE, V3Process.class);
        Map<String, Object> updateProcessBody = new HashMap<>();
        updateProcessBody.put("command", staging.getCommand());
        if (staging.getHealthCheckType() != null) {
            updateProcessBody.put("health_check", buildHealthCheck(staging));
        }
        if (staging.getReadinessHealthCheckType() != null) {
            updateProcessBody.put("readiness_health_check", buildReadinessHealthCheck(staging));
        }
        cc.getRestClient()
          .patch()
          .uri("/v3/processes/{guid}", process.guid())
          .body(updateProcessBody)
          .retrieve()
          .toBodilessEntity();
    }

    private void updateAppFeature(UUID applicationGuid, String featureName, boolean enabled) {
        cc.getRestClient()
          .patch()
          .uri("/v3/apps/{guid}/features/{name}", applicationGuid, featureName)
          .body(Map.of("enabled", enabled))
          .retrieve()
          .toBodilessEntity();
    }

    private Map<String, Object> buildHealthCheck(Staging staging) {
        Map<String, Object> data = new HashMap<>();
        putIfNotNull(data, "endpoint", staging.getHealthCheckHttpEndpoint());
        putIfNotNull(data, "timeout", staging.getHealthCheckTimeout());
        putIfNotNull(data, "invocation_timeout", staging.getInvocationTimeout());
        putIfNotNull(data, "interval", staging.getHealthCheckInterval());
        Map<String, Object> healthCheck = new HashMap<>();
        healthCheck.put("type", staging.getHealthCheckType());
        healthCheck.put("data", data);
        return healthCheck;
    }

    private Map<String, Object> buildReadinessHealthCheck(Staging staging) {
        Map<String, Object> data = new HashMap<>();
        putIfNotNull(data, "invocation_timeout", staging.getReadinessHealthCheckInvocationTimeout());
        putIfNotNull(data, "endpoint", staging.getReadinessHealthCheckHttpEndpoint());
        putIfNotNull(data, "interval", staging.getReadinessHealthCheckInterval());
        Map<String, Object> readinessHealthCheck = new HashMap<>();
        readinessHealthCheck.put("type", staging.getReadinessHealthCheckType());
        readinessHealthCheck.put("data", data);
        return readinessHealthCheck;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    // Mirrors the OSS impl's getRequiredApplicationGuid: look up the app in the target space and 404 if it is absent.
    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(),
                                           new ParameterizedTypeReference<V3ListResponse<V3Application>>() {
                                           });
        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

}
