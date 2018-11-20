package com.sap.cloud.lm.sl.cf.core.parser;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.junit.Before;
import org.junit.Test;

public class DockerInfoParserTest {

    private DockerInfoParser dockerInfoParser;

    @Before
    public void setUp() {
        dockerInfoParser = new DockerInfoParser();
    }

    @Test
    public void testWithValidImageWithoutUserCredentials() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        Map<String, Object> moduleParameters = new HashMap<>();
        Map<String, String> dockerParameters = new HashMap<>();
        String sampleImage = "cloudfoundry/test-app";
        dockerParameters.put("image", sampleImage);
        moduleParameters.put("docker", dockerParameters);
        parameters.add(moduleParameters);

        DockerInfo actualDockerInfo = dockerInfoParser.parse(parameters);
        DockerInfo expectedDockerInfo = new DockerInfo(sampleImage);

        assertEquals(expectedDockerInfo.getImage(), actualDockerInfo.getImage());
        assertEquals(expectedDockerInfo.getDockerCredentials(), actualDockerInfo.getDockerCredentials());
    }

    @Test
    public void testWithValidImageAndCredentials() {
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
        DockerInfo expectedDockerInfo = new DockerInfo(sampleImage);
        expectedDockerInfo.setDockerCredentials(new DockerCredentials(username, password));

        assertEquals(expectedDockerInfo.getImage(), actualDockerInfo.getImage());

        assertEquals(expectedDockerInfo.getDockerCredentials()
            .getUsername(),
            actualDockerInfo.getDockerCredentials()
                .getUsername());

        assertEquals(expectedDockerInfo.getDockerCredentials()
            .getPassword(),
            actualDockerInfo.getDockerCredentials()
                .getPassword());
    }

    @Test
    public void testWithoutArguments() {
        List<Map<String, Object>> parameters = new ArrayList<>();
        Map<String, Object> moduleParameters = new HashMap<>();
        Map<String, String> dockerParameters = new HashMap<>();
        moduleParameters.put("docker", dockerParameters);
        parameters.add(moduleParameters);

        DockerInfo actualDockerInfo = dockerInfoParser.parse(parameters);

        assertNull(actualDockerInfo);
    }
    
    @Test
    public void testWithoutDocker() {
        List<Map<String, Object>> parameters = Collections.emptyList();
        
        DockerInfo dockerInfo = dockerInfoParser.parse(parameters);
        
        assertNull(dockerInfo);
    }
}
