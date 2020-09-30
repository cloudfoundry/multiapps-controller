package org.cloudfoundry.multiapps.controller.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.junit.jupiter.api.Test;

class DockerInfoParserTest {

    private final DockerInfoParser dockerInfoParser = new DockerInfoParser();

    @Test
    void testWithValidImageWithoutUserCredentials() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        Map<String, Object> moduleParameters = new HashMap<>();
        Map<String, String> dockerParameters = new HashMap<>();
        String sampleImage = "cloudfoundry/test-app";
        dockerParameters.put("image", sampleImage);
        moduleParameters.put("docker", dockerParameters);
        parameters.add(moduleParameters);
        DockerInfo actualDockerInfo = dockerInfoParser.parse(parameters);
        DockerInfo expectedDockerInfo = ImmutableDockerInfo.builder()
                                                           .image(sampleImage)
                                                           .build();
        assertEquals(expectedDockerInfo, actualDockerInfo);
    }

    @Test
    void testWithValidImageAndCredentials() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        Map<String, Object> moduleParameters = new HashMap<>();
        Map<String, String> dockerParameters = new HashMap<>();
        String sampleImage = "cloudfoundry/test-app";
        String username = "someUsername";
        String password = "somePassword";
        dockerParameters.put("image", sampleImage);
        dockerParameters.put("username", username);
        dockerParameters.put("password", password);
        moduleParameters.put("docker", dockerParameters);
        parameters.add(moduleParameters);
        DockerInfo actualDockerInfo = dockerInfoParser.parse(parameters);
        DockerInfo expectedDockerInfo = creteDockerInfo(sampleImage, username, password);
        assertEquals(expectedDockerInfo, actualDockerInfo);
    }

    private ImmutableDockerInfo creteDockerInfo(String sampleImage, String username, String password) {
        return ImmutableDockerInfo.builder()
                                  .image(sampleImage)
                                  .credentials(ImmutableDockerCredentials.builder()
                                                                         .username(username)
                                                                         .password(password)
                                                                         .build())
                                  .build();
    }

    @Test
    void testWithoutArguments() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        Map<String, Object> moduleParameters = new HashMap<>();
        Map<String, String> dockerParameters = new HashMap<>();
        moduleParameters.put("docker", dockerParameters);
        parameters.add(moduleParameters);
        DockerInfo actualDockerInfo = dockerInfoParser.parse(parameters);
        assertNull(actualDockerInfo);
    }

    @Test
    void testWithoutDocker() {
        List<Map<String, Object>> parameters = Collections.emptyList();
        DockerInfo dockerInfo = dockerInfoParser.parse(parameters);
        assertNull(dockerInfo);
    }
}
