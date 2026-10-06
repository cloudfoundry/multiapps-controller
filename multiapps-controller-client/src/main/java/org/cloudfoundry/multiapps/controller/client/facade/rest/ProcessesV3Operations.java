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
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableInstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.InstancesInfo;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
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

    private static final ParameterizedTypeReference<V3ListResponse<V3Process.V3AppFeature>> APP_FEATURE_PAGE = new ParameterizedTypeReference<>() {
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
            CloudControllerV3Endpoints.APPS + "/" + applicationGuid + CloudControllerV3Endpoints.PROCESSES + Constants.WEB_PROCESS_TYPE,
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
        V3Process.V3ProcessStats stats = client.get(
            CloudControllerV3Endpoints.APPS + "/" + applicationGuid + CloudControllerV3Endpoints.PROCESSES + Constants.WEB_PROCESS_TYPE
                + "/stats",
            V3Process.V3ProcessStats.class);

        if (stats == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.STATISTICS_FOR_APPLICATION_INSTANCES_WITH_GUID_0_NOT_FOUND,
                                                                   applicationGuid));
        }

        return V3InstancesInfoMapper.toInstancesInfo(stats);
    }

    public boolean getApplicationSshEnabled(UUID applicationGuid) {
        V3Process.V3SshEnabled sshEnabled = client.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/ssh_enabled",
                                                       V3Process.V3SshEnabled.class);

        if (sshEnabled == null || sshEnabled.enabled() == null) {
            return false;
        }

        return sshEnabled.enabled();
    }

    public Map<String, Boolean> getApplicationFeatures(UUID applicationGuid) {
        List<V3Process.V3AppFeature> applicationFeatures = client.list(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/features"
                                                                           + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                                           + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE,
                                                                       APP_FEATURE_PAGE);

        Map<String, Boolean> result = new HashMap<>();
        for (V3Process.V3AppFeature feature : applicationFeatures) {
            result.put(feature.name(), feature.enabled());
        }

        return result;
    }

    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        V3Droplet currentDroplet = client.get(CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/droplets/current",
                                              V3Droplet.class);

        if (currentDroplet == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
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
            CloudControllerV3Endpoints.APPS + "/" + applicationGuid + CloudControllerV3Endpoints.PROCESSES + Constants.WEB_PROCESS_TYPE,
            V3Process.class);

        if (process == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}
