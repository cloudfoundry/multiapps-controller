package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class DockerInfoParser implements ParametersParser<DockerInfo> {

    private static final String DOCKER = "docker";

    @Override
    public DockerInfo parse(List<Map<String, Object>> parametersList) {
        Map<String, String> dockerParams = getDockerParams(parametersList);

        return getDockerInfo(dockerParams);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getDockerParams(List<Map<String, Object>> parametersList) {
        return (Map<String, String>) PropertiesUtil.getPropertyValue(parametersList, DOCKER, Collections.emptyMap());
    }

    private DockerInfo getDockerInfo(Map<String, String> docker) {
        if (docker == null) {
            return null;
        }

        String image = docker.get("image");
        if (image == null) {
            return null;
        }
        return ImmutableDockerInfo.builder()
                                  .image(image)
                                  .credentials(getDockerCredentials(docker))
                                  .build();
    }

    private DockerCredentials getDockerCredentials(Map<String, String> docker) {
        String username = docker.get("username");
        String password = docker.get("password");
        if (username == null || password == null) {
            return null;
        }
        return ImmutableDockerCredentials.builder()
                                         .username(username)
                                         .password(password)
                                         .build();
    }

}
