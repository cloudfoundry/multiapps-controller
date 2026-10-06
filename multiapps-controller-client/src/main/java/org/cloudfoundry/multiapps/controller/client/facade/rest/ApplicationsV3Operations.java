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
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ApplicationMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApplicationsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Application>> APP_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final Duration DELETE_JOB_TIMEOUT = Duration.ofMinutes(5);

    private final CloudControllerV3Client client;
    private final CloudSpace targetSpace;

    public ApplicationsV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public UUID createApplication(ApplicationToCreateDto dto) {
        validateTargetSpace();

        String targetSpaceGuid = targetSpace.getGuid()
                                            .toString();
        Map<String, Object> body = OperationsUtil.buildApplicationBody(dto, targetSpaceGuid);

        UUID appGuid = sendCreateApplicationRequest(body);

        scaleApplicationIfNeeded(appGuid, dto);

        return appGuid;
    }

    private void validateTargetSpace() {
        if (targetSpace == null || targetSpace.getGuid() == null) {
            throw new CloudOperationException(HttpStatus.BAD_REQUEST, Messages.BAD_REQUEST,
                                              Messages.TARGET_SPACE_REQUIRED_TO_CREATE_AN_APPLICATION);
        }
    }

    private UUID sendCreateApplicationRequest(Map<String, Object> body) {
        V3Application created = client.postForObject(CloudControllerV3Endpoints.APPS, body, V3Application.class);
        return UUID.fromString(created.guid());
    }

    private void scaleApplicationIfNeeded(UUID appGuid, ApplicationToCreateDto dto) {
        Map<String, Object> scale = new HashMap<>();
        if (dto.getMemoryInMb() != null) {
            scale.put(V3Fields.MEMORY_IN_MB, dto.getMemoryInMb());
        }

        if (dto.getDiskQuotaInMb() != null) {
            scale.put(V3Fields.DISK_IN_MB, dto.getDiskQuotaInMb());
        }

        if (!scale.isEmpty()) {
            scaleWebProcess(appGuid, scale);
        }
    }

    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getApplicationGuid(applicationName);

        ResponseEntity<Void> response = client.delete(CloudControllerV3Endpoints.APP_BY_GUID, applicationGuid);

        client.followAsyncJob(response, DELETE_JOB_TIMEOUT);
    }

    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    public CloudApplication getApplication(String applicationName, boolean required) {
        V3Application app = findApplicationByName(applicationName);

        if (app != null) {
            return V3ApplicationMapper.toCloudApplication(app, targetSpace);
        }

        if (required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return null;
    }

    public UUID getApplicationGuid(String applicationName) {
        return getApplication(applicationName).getGuid();
    }

    public String getApplicationName(UUID applicationGuid) {
        V3Application app = client.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid, V3Application.class);

        return app == null ? null : app.name();
    }

    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        V3Application.V3EnvironmentVariables environmentVariablesJson = client.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid
                                                                                       + "/environment_variables",
                                                                                   V3Application.V3EnvironmentVariables.class);

        return environmentVariablesJson == null || environmentVariablesJson.environmentVariables() == null
            ? Map.of()
            : environmentVariablesJson.environmentVariables();
    }

    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return getApplicationEnvironment(getApplicationGuid(applicationName));
    }

    public List<CloudApplication> getApplications() {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(null, targetSpace);

        return listApplications(applicationsQuery).stream()
                                                  .map(app -> V3ApplicationMapper.toCloudApplication(app, targetSpace))
                                                  .toList();
    }

    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        String query = OperationsQueryUtil.buildApplicationsQuery(null, targetSpace);

        if (labelSelector != null) {
            query = query + CloudControllerV3Endpoints.AMPERSAND_LABEL_SELECTOR + labelSelector;
        }

        return listApplications(query).stream()
                                      .map(app -> V3ApplicationMapper.toCloudApplication(app, targetSpace))
                                      .toList();
    }

    public void startApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);

        client.post(CloudControllerV3Endpoints.APP_START, null, guid);
    }

    public void stopApplication(String applicationName) {
        UUID guid = getApplicationGuid(applicationName);

        client.post(CloudControllerV3Endpoints.APP_STOP, null, guid);
    }

    public void rename(String applicationName, String newName) {
        UUID guid = getApplicationGuid(applicationName);

        try {
            client.patch(CloudControllerV3Endpoints.APP_BY_GUID, Map.of(V3Fields.NAME, newName), guid);
        } catch (CloudOperationException e) {
            //the Cloud Controller can return 503 but the rename might have happened already
            if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && newName.equals(getApplicationName(guid))) {
                return;
            }
            throw e;
        }
    }

    public void updateApplicationInstances(String applicationName, int instances) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of(V3Fields.INSTANCES, instances));
    }

    public void updateApplicationMemory(String applicationName, int memory) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of(V3Fields.MEMORY_IN_MB, memory));
    }

    public void updateApplicationDiskQuota(String applicationName, int disk) {
        scaleWebProcess(getApplicationGuid(applicationName), Map.of(V3Fields.DISK_IN_MB, disk));
    }

    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID guid = getApplicationGuid(applicationName);

        client.patch(CloudControllerV3Endpoints.APP_ENV_VARS, Map.of(V3Fields.VAR, env), guid);
    }

    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        client.patch(CloudControllerV3Endpoints.APP_CURRENT_DROPLET, Map.of(V3Fields.DATA, Map.of(V3Fields.GUID, dropletGuid.toString())),
                     applicationGuid);
    }

    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        client.patch(CloudControllerV3Endpoints.APP_BY_GUID, Map.of(V3Fields.METADATA, OperationsUtil.toMetadataMap(metadata)), guid);
    }

    private void scaleWebProcess(UUID applicationGuid, Map<String, Object> scaleBody) {
        client.post(CloudControllerV3Endpoints.APP_WEB_PROCESS_SCALE, scaleBody, applicationGuid);
    }

    private V3Application findApplicationByName(String applicationName) {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(applicationName, targetSpace);
        List<V3Application> apps = listApplications(applicationsQuery);

        return apps.isEmpty() ? null : apps.getFirst();
    }

    private List<V3Application> listApplications(String query) {
        return client.list(query, APP_PAGE);
    }

}
