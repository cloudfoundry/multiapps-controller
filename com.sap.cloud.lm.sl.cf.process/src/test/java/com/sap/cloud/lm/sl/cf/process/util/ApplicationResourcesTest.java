package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.domain.CloudResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ApplicationResourcesTest {

    @Mock
    private CloudControllerClient client;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetEmptyKnownResources() {
        ApplicationResources applicationResources = new ApplicationResources();
        when(client.getKnownRemoteResources(any())).thenReturn(applicationResources.getCloudResources());
        CloudResources cloudResources = applicationResources.getKnownRemoteResources(client);
        assertTrue(cloudResources.asList()
            .isEmpty());
    }

    @Test
    public void testGetKnownResources() {
        List<CloudResource> cloudResourceColletion = new ArrayList<>();
        cloudResourceColletion.add(new CloudResource("app1", 10, "5AAAD"));
        cloudResourceColletion.add(new CloudResource("app2", 45, "8ACDQWEF"));
        ApplicationResources applicationResources = new ApplicationResources();
        applicationResources.createCloudResources(cloudResourceColletion);

        cloudResourceColletion.remove(0);
        when(client.getKnownRemoteResources(any())).thenReturn(new CloudResources(cloudResourceColletion));
        CloudResources knownRemoteResources = applicationResources.getKnownRemoteResources(client);
        assertEquals(cloudResourceColletion.size(), knownRemoteResources.asList()
            .size());
    }

}
