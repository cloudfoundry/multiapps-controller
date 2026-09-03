package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3BuildMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;

public class BuildsV3Operations {

    private final CloudControllerV3Client cc;

    public BuildsV3Operations(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public CloudBuild createBuild(UUID packageGuid) {
        V3Build build = cc.getRestClient()
                          .post()
                          .uri(CloudControllerV3Endpoints.BUILDS)
                          .body(Map.of("package", Map.of("guid", packageGuid.toString())))
                          .retrieve()
                          .body(V3Build.class);
        return V3BuildMapper.toCloudBuild(build);
    }

    public CloudBuild getBuild(UUID buildGuid) {
        V3Build build = cc.get(CloudControllerV3Endpoints.BUILDS + "/" + buildGuid, V3Build.class);
        return V3BuildMapper.toCloudBuild(build);
    }

    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        String uri = CloudControllerV3Endpoints.APPS + "/" + applicationGuid + "/builds" + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;
        return cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
                 })
                 .stream()
                 .map(V3BuildMapper::toCloudBuild)
                 .toList();
    }

    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        String uri = CloudControllerV3Endpoints.BUILDS + CloudControllerV3Endpoints.QUERY_PACKAGE_GUIDS + packageGuid
            + CloudControllerV3Endpoints.AMPERSAND_PER_PAGE + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE;
        return cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
                 })
                 .stream()
                 .map(V3BuildMapper::toCloudBuild)
                 .toList();
    }

}
