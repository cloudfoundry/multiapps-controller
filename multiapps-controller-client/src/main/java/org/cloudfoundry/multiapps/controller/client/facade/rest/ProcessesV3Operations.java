package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.Messages;
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

public class ProcessesV3Operations {

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ProcessesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        V3Process applicationProcess = cc.get(
            CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/processes/" + Constants.WEB_PROCESS_TYPE,
            V3Process.class);

        return applicationProcess == null ? null : V3ProcessMapper.toCloudProcess(applicationProcess);
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
        V3Process.V3ProcessStats stats = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/processes/"
                                                    + Constants.WEB_PROCESS_TYPE + "/stats",
                                                V3Process.V3ProcessStats.class);

        return V3InstancesInfoMapper.toInstancesInfo(stats);
    }

    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        V3Process.V3SshEnabled sshEnabled = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/ssh_enabled",
                                                   V3Process.V3SshEnabled.class);

        if (sshEnabled == null || sshEnabled.enabled() == null) {
            return false;
        }

        return sshEnabled.enabled();
    }

    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        List<V3Process.V3AppFeature> applicationFeatures = cc.list(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/features"
                                                                       + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                                       + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE,
                                                                   new ParameterizedTypeReference<V3ListResponse<V3Process.V3AppFeature>>() {
                                                                   });

        Map<String, Boolean> result = new HashMap<>();
        for (V3Process.V3AppFeature feature : applicationFeatures) {
            result.put(feature.name(), feature.enabled());
        }

        return result;
    }

    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        V3Droplet currentDroplet = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/droplets/current", V3Droplet.class);

        if (currentDroplet == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_WITH_GUID_0_DOES_NOT_HAVE_A_DROPLET,
                                                                   applicationGuid));
        }

        return parseDropletInfo(currentDroplet);
    }

    private DropletInfo parseDropletInfo(V3Droplet droplet) {
        String packageUrl = droplet.links()
                                   .get(Constants.PACKAGE_LINK)
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
          .uri(CloudControllerV3Endpoints.APP_BY_GUID, applicationGuid)
          .body(Map.of("lifecycle", buildApplicationLifecycle(staging)))
          .retrieve()
          .toBodilessEntity();
        updateApplicationProcess(applicationGuid, staging);
    }

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

    private void updateApplicationProcess(UUID applicationGuid, Staging staging) {
        staging.getAppFeatures()
               .forEach((featureName, enabled) -> updateAppFeature(applicationGuid, featureName, enabled));

        V3Process process = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/processes/" + Constants.WEB_PROCESS_TYPE,
                                   V3Process.class);

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
          .uri(CloudControllerV3Endpoints.PROCESS_BY_GUID, process.guid())
          .body(updateProcessBody)
          .retrieve()
          .toBodilessEntity();
    }

    private void updateAppFeature(UUID applicationGuid, String featureName, boolean enabled) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.APP_FEATURE, applicationGuid, featureName)
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

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(applicationName);

        List<V3Application> apps = cc.list(query.toString(),
                                           new ParameterizedTypeReference<V3ListResponse<V3Application>>() {
                                           });

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}
