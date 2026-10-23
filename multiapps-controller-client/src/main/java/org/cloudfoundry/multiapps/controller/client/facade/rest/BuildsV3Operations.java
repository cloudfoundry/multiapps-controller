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
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Fields;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class BuildsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Build>> BUILD_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    public BuildsV3Operations(CloudControllerV3Client client) {
        this.client = client;
    }

    public CloudBuild createBuild(UUID packageGuid) {
        V3Build build = client.postForObject(CloudControllerV3Endpoints.BUILDS,
                                             Map.of(V3Fields.PACKAGE, Map.of(V3Fields.GUID, packageGuid.toString())), V3Build.class);

        return V3BuildMapper.toCloudBuild(build);
    }

    public CloudBuild getBuild(UUID buildGuid) {
        V3Build build = client.get(V3Uri.path(CloudControllerV3Endpoints.BUILD_BY_GUID, buildGuid)
                                        .build(),
                                   V3Build.class);

        if (build == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              MessageFormat.format(Messages.BUILD_WITH_GUID_0_NOT_FOUND, buildGuid));
        }

        return V3BuildMapper.toCloudBuild(build);
    }

    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.APP_BUILDS, applicationGuid)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .build();

        return client.list(uri, BUILD_PAGE)
                     .stream()
                     .map(V3BuildMapper::toCloudBuild)
                     .toList();
    }

    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.BUILDS)
                          .query(V3QueryParameters.PACKAGE_GUIDS, packageGuid)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .build();

        return client.list(uri, BUILD_PAGE)
                     .stream()
                     .map(V3BuildMapper::toCloudBuild)
                     .toList();
    }

}
