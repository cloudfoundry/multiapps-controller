package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudBuild;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Build;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3BuildMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;

/**
 * CF v3 <em>Builds</em> operations for the in-house {@link CloudControllerRestClientV3Impl}. Reproduces the HTTP shape of the OSS
 * {@code CloudControllerRestClientImpl} build methods (endpoints from {@code builds()} / {@code applicationsV3().listBuilds}), mapping the
 * v3 wire model to the {@link CloudBuild} domain type via {@link V3BuildMapper}.
 */
public class BuildsV3Operations {

    private static final int MAX_PAGE_SIZE = 5000;

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public BuildsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public CloudBuild createBuild(UUID packageGuid) {
        V3Build build = cc.getRestClient()
                          .post()
                          .uri("/v3/builds")
                          .body(Map.of("package", Map.of("guid", packageGuid.toString())))
                          .retrieve()
                          .body(V3Build.class);
        return V3BuildMapper.toCloudBuild(build);
    }

    public CloudBuild getBuild(UUID buildGuid) {
        V3Build build = cc.get("/v3/builds/" + buildGuid, V3Build.class);
        return V3BuildMapper.toCloudBuild(build);
    }

    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        String uri = "/v3/apps/" + applicationGuid + "/builds?per_page=" + MAX_PAGE_SIZE;
        return cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
        })
                 .stream()
                 .map(V3BuildMapper::toCloudBuild)
                 .toList();
    }

    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        String uri = "/v3/builds?package_guids=" + packageGuid + "&per_page=" + MAX_PAGE_SIZE;
        return cc.list(uri, new ParameterizedTypeReference<V3ListResponse<V3Build>>() {
        })
                 .stream()
                 .map(V3BuildMapper::toCloudBuild)
                 .toList();
    }

}
