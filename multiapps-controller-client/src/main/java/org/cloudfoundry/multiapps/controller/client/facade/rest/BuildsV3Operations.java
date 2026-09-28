package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3BuildMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class BuildsV3Operations {

    private final CloudControllerV3Client client;

    public BuildsV3Operations(CloudControllerV3Client client) {
        this.client = client;
    }

    public CloudBuild createBuild(UUID packageGuid) {
        V3Build build = client.getRestClient()
                              .post()
                              .uri(CloudControllerV3Endpoints.BUILDS)
                              .body(Map.of("package", Map.of("guid", packageGuid.toString())))
                              .retrieve()
                              .body(V3Build.class);

        return V3BuildMapper.toCloudBuild(build);
    }

    public CloudBuild getBuild(UUID buildGuid) {
        V3Build build = client.get(CloudControllerV3Endpoints.BUILDS + "/" + buildGuid, V3Build.class);

        if (build == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.BUILD_WITH_GUID_0_NOT_FOUND, buildGuid));
        }

        return V3BuildMapper.toCloudBuild(build);
    }

    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        String uri = CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/builds" + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
                     })
                     .stream()
                     .map(V3BuildMapper::toCloudBuild)
                     .toList();
    }

    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        String uri = CloudControllerV3Endpoints.BUILDS + CloudControllerV3Endpoints.QUERY_PACKAGE_GUIDS + packageGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;

        return client.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
                     })
                     .stream()
                     .map(V3BuildMapper::toCloudBuild)
                     .toList();
    }

}
