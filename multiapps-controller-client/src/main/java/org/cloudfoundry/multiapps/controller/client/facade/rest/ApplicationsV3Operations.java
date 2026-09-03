package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ApplicationMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class ApplicationsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Application>> APPLICATION_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public ApplicationsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public UUID createApplication(ApplicationToCreateDto dto) {
        if (target == null || target.getGuid() == null) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST,
                                              Messages.TARGET_SPACE_REQUIRED_TO_CREATE_AN_APPLICATION);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("name", dto.getName());
        body.put("lifecycle", buildLifecycle(dto.getStaging()));
        body.put("relationships", Map.of("space", Map.of("data", Map.of("guid", target.getGuid()
                                                                                      .toString()))));
        if (dto.getEnv() != null) {
            body.put("environment_variables", dto.getEnv());
        }
        if (dto.getMetadata() != null) {
            body.put("metadata", Map.of("labels", dto.getMetadata()
                                                     .getLabels(),
                                        "annotations", dto.getMetadata()
                                                          .getAnnotations()));
        }
        V3Application created = cc.getRestClient()
                                  .post()
                                  .uri(CloudControllerV3Endpoints.APPS)
                                  .body(body)
                                  .retrieve()
                                  .body(V3Application.class);
        UUID appGuid = UUID.fromString(created.guid());
        Map<String, Object> scale = new HashMap<>();
        if (dto.getMemoryInMb() != null) {
            scale.put("memory_in_mb", dto.getMemoryInMb());
        }
        if (dto.getDiskQuotaInMb() != null) {
            scale.put("disk_in_mb", dto.getDiskQuotaInMb());
        }
        if (!scale.isEmpty()) {
            scaleWebProcess(appGuid, scale);
        }
        return appGuid;
    }

    private Map<String, Object> buildLifecycle(Staging staging) {
        if (staging == null) {
            return Map.of("type", "buildpack", "data", Map.of());
        }
        if (staging.getDockerInfo() != null) {
            return Map.of("type", "docker", "data", Map.of());
        }
        String type = staging.getLifecycleType() != null ? staging.getLifecycleType()
                                                                  .name()
                                                                  .toLowerCase()
            : "buildpack";
        Map<String, Object> data = new HashMap<>();
        if (staging.getBuildpacks() != null) {
            data.put("buildpacks", staging.getBuildpacks());
        }
        if (staging.getStackName() != null) {
            data.put("stack", staging.getStackName());
        }
        return Map.of("type", type, "data", data);
    }

    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getApplicationGuid(applicationName);
        var response = cc.getRestClient()
                         .delete()
                         .uri(CloudControllerV3Endpoints.APP_BY_GUID, applicationGuid)
                         .retrieve()
                         .toBodilessEntity();
        cc.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    public CloudApplication getApplication(String applicationName, boolean required) {
        V3Application app = findApplicationByName(applicationName);
        if (app == null) {
            if (required) {
                throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                                  MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
            }
            return null;
        }
        return V3ApplicationMapper.toCloudApplication(app, target);
    }

    public UUID getApplicationGuid(String applicationName) {
        return getApplication(applicationName).getGuid();
    }

    public String getApplicationName(UUID applicationGuid) {
        V3Application app = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid, V3Application.class);
        return app == null ? null : app.name();
    }

    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        V3Application.V3EnvironmentVariables env = cc.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid
                                                              + "/environment_variables",
                                                          V3Application.V3EnvironmentVariables.class);
        return env == null || env.var() == null ? Map.of() : env.var();
    }

    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return getApplicationEnvironment(getApplicationGuid(applicationName));
    }

    public List<CloudApplication> getApplications() {
        return listApplications(applicationsQuery(null)).stream()
                                                        .map(app -> V3ApplicationMapper.toCloudApplication(app, target))
                                                        .toList();
    }

    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        String query = applicationsQuery(null);
        if (labelSelector != null) {
            query = query + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;
        }
        return listApplications(query).stream()
                                      .map(app -> V3ApplicationMapper.toCloudApplication(app, target))
                                      .toList();
    }

    public void startApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .post()
          .uri(CloudControllerV3Endpoints.APP_START, guid)
          .retrieve()
          .toBodilessEntity();
    }

    public void stopApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .post()
          .uri(CloudControllerV3Endpoints.APP_STOP, guid)
          .retrieve()
          .toBodilessEntity();
    }

    public void rename(String applicationName, String newName) {
        UUID guid = getApplicationGuid(applicationName);
        try {
            cc.getRestClient()
              .patch()
              .uri(CloudControllerV3Endpoints.APP_BY_GUID, guid)
              .body(Map.of("name", newName))
              .retrieve()
              .toBodilessEntity();
        } catch (CloudOperationException e) {
            //the Cloud Controller can return 503 but the rename might have happened already
            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && newName.equals(getApplicationName(guid))) {
                return;
            }
            throw e;
        }
    }

    public void updateApplicationInstances(String applicationName, int instances) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("instances", instances));
    }

    public void updateApplicationMemory(String applicationName, int memory) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("memory_in_mb", memory));
    }

    public void updateApplicationDiskQuota(String applicationName, int disk) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of("disk_in_mb", disk));
    }

    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID guid = getApplicationGuid(applicationName);
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.APP_ENV_VARS, guid)
          .body(Map.of("var", env))
          .retrieve()
          .toBodilessEntity();
    }

    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.APP_CURRENT_DROPLET, applicationGuid)
          .body(Map.of("data", Map.of("guid", dropletGuid.toString())))
          .retrieve()
          .toBodilessEntity();
    }

    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        cc.getRestClient()
          .patch()
          .uri(CloudControllerV3Endpoints.APP_BY_GUID, guid)
          .body(Map.of("metadata", Map.of("labels", metadata.getLabels(), "annotations", metadata.getAnnotations())))
          .retrieve()
          .toBodilessEntity();
    }

    private void scaleWebProcess(UUID applicationGuid, Map<String, Object> scaleBody) {
        cc.getRestClient()
          .post()
          .uri(CloudControllerV3Endpoints.APP_WEB_PROCESS_SCALE, applicationGuid)
          .body(scaleBody)
          .retrieve()
          .toBodilessEntity();
    }

    private V3Application findApplicationByName(String applicationName) {
        List<V3Application> apps = listApplications(applicationsQuery(applicationName));
        return apps.isEmpty() ? null : apps.get(0);
    }

    private List<V3Application> listApplications(String query) {
        return cc.list(query, APPLICATION_PAGE);
    }

    private String applicationsQuery(String name) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }
        if (name != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
                 .append(name);
        }
        return query.toString();
    }

}
