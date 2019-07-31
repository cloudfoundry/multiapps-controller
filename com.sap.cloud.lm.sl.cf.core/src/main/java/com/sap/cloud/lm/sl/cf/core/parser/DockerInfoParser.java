package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;

import com.sap.cloud.lm.sl.mta.util.PropertiesUtil;

public class DockerInfoParser implements ParametersParser<DockerInfo> {

    private static final String DOCKER = "docker";

    @Override
    public DockerInfo parse(List<Map<String, Object>> parametersList) {
        Map<String, String> dockerParams = getDockerParams(parametersList);

        return getDockerInfo(dockerParams);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getDockerParams(List<Map<String, Object>> parametersList) {
        return (Map<String, String>) PropertiesUtil.getPropertyValue(parametersList, DOCKER, null);
    }

    private DockerInfo getDockerInfo(Map<String, String> docker) {
        if (docker == null) {
            return null;
        }

        String image = docker.get("image");
        if (image == null) {
            return null;
        }
        DockerInfo dockerInfo = new DockerInfo(image);

        String username = docker.get("username");
        String password = docker.get("password");
        if (username == null || password == null) {
            return dockerInfo;
        }

        DockerCredentials dockerCredentials = new DockerCredentials(username, password);
        dockerInfo.setDockerCredentials(dockerCredentials);

        return dockerInfo;
    }

}
