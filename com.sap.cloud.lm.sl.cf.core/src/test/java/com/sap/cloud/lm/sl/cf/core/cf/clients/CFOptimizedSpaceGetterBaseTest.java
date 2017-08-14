package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.mockito.Mockito.when;

import java.net.URL;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

public abstract class CFOptimizedSpaceGetterBaseTest {

    protected static final String CONTROLLER_URL = "https://api.cf.sap.com";
    protected static final String DUMMY = "DUMMY";

    @Mock
    protected RestTemplate restTemplate;
    @Mock
    protected RestTemplateFactory restTemplateFactory;
    @Mock
    protected CloudFoundryOperations client;

    protected CFOptimizedSpaceGetter spaceGetter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.spaceGetter = new CFOptimizedSpaceGetter(restTemplateFactory);
        when(restTemplateFactory.getRestTemplate(client)).thenReturn(restTemplate);
        when(client.login()).thenReturn(new DefaultOAuth2AccessToken(DUMMY));
        when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
    }

}
