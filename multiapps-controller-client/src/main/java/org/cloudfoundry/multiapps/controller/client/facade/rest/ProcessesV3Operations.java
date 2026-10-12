package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudProcess;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.DropletInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AppFeature;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Droplet;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3GuidReference;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3InstancesInfoMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Process;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ProcessMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class ProcessesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3AppFeature>> APP_FEATURE_PAGE = new ParameterizedTypeReference<>() {
    };

    private static final ParameterizedTypeReference<V3ListResponse<V3GuidReference>> APP_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public ProcessesV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public CloudProcess getApplicationProcess(UUID applicationGuid) {
        V3Process applicationProcess = client.get(
            V3Uri.path(CloudControllerV3Endpoints.APP_WEB_PROCESS, applicationGuid)
                 .build(),
            V3Process.class);

        return applicationProcess == null ? null : V3ProcessMapper.toCloudProcess(applicationProcess);
    }

    public InstancesInfo getApplicationInstances(CloudApplication application) {
        if (application.getState()
                       .equals(CloudApplication.State.STARTED)) {
            return getApplicationInstances(application.getGuid());
        }

        return ImmutableInstancesInfo.builder()
                                     .instances(Collections.emptyList())
                                     .build();
    }

    public InstancesInfo getApplicationInstances(UUID applicationGuid) {
        V3Process.V3ProcessStats stats = client.get(V3Uri.path(CloudControllerV3Endpoints.APP_WEB_PROCESS_STATS, applicationGuid)
                                                         .build(),
                                                    V3Process.V3ProcessStats.class);

        if (stats == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.STATISTICS_FOR_APPLICATION_INSTANCES_WITH_GUID_0_NOT_FOUND,
                                                                   applicationGuid));
        }

        return V3InstancesInfoMapper.toInstancesInfo(stats);
    }

    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        List<V3AppFeature> applicationFeatures = client.list(V3Uri.path(CloudControllerV3Endpoints.APP_FEATURES, applicationGuid)
                                                                  .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                                                                  .build(),
                                                             APP_FEATURE_PAGE);

        Map<String, Boolean> result = new HashMap<>();
        for (V3AppFeature feature : applicationFeatures) {
            result.put(feature.name(), feature.enabled());
        }

        return result;
    }

    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        V3Droplet currentDroplet = client.get(V3Uri.path(CloudControllerV3Endpoints.APP_DROPLET_CURRENT, applicationGuid)
                                                   .build(),
                                              V3Droplet.class);

        if (currentDroplet == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.APPLICATION_WITH_GUID_0_DOES_NOT_HAVE_A_DROPLET,
                                                                   applicationGuid));
        }

        return OperationsUtil.parseDropletInfo(currentDroplet);
    }

    public void updateApplicationStaging(String applicationName, Staging staging) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        Map<String, Object> applicationLifecycle = OperationsUtil.buildApplicationLifecycle(staging);

        client.patch(CloudControllerV3Endpoints.APP_BY_GUID, Map.of(V3Fields.LIFECYCLE, applicationLifecycle), applicationGuid);
        updateApplicationProcess(applicationGuid, staging);
    }

    private void updateApplicationProcess(UUID applicationGuid, Staging staging) {
        staging.getAppFeatures()
               .forEach((featureName, enabled) -> updateAppFeature(applicationGuid, featureName, enabled));

        V3Process process = client.get(
            V3Uri.path(CloudControllerV3Endpoints.APP_WEB_PROCESS, applicationGuid)
                 .build(),
            V3Process.class);

        if (process == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.APPLICATION_PROCESS_FOR_APPLICATION_WITH_GUID_0_NOT_FOUND,
                                                                   applicationGuid));
        }

        Map<String, Object> updateProcessBody = OperationsUtil.buildUpdatedProcessBody(staging);

        client.patch(CloudControllerV3Endpoints.PROCESS_BY_GUID, updateProcessBody, process.guid());
    }

    private void updateAppFeature(UUID applicationGuid, String featureName, boolean enabled) {
        client.patch(CloudControllerV3Endpoints.APP_FEATURE, Map.of(V3Fields.ENABLED, enabled), applicationGuid, featureName);
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        String applicationsQuery = OperationsQueryUtil.buildApplicationsQuery(applicationName, targetSpace);

        List<V3GuidReference> apps = client.list(applicationsQuery, APP_PAGE);

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}
