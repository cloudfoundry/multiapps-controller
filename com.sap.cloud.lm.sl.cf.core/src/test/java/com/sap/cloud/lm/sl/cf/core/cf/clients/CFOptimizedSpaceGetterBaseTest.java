package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

public abstract class CFOptimizedSpaceGetterBaseTest {

    protected static final String CONTROLLER_URL = "https://api.cf.sap.com";
    protected static final String DUMMY = "DUMMY";

    @Mock
    protected RestTemplate restTemplate;
    @Mock
    protected RestTemplateFactory restTemplateFactory;
    @Mock
    protected CloudControllerClient client;

    protected CFOptimizedSpaceGetter spaceGetter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.spaceGetter = new CFOptimizedSpaceGetter(restTemplateFactory);
        when(restTemplateFactory.getRestTemplate(client)).thenReturn(restTemplate);
        when(client.login()).thenReturn(new OAuth2AccessTokenWithAdditionalInfo(Mockito.mock(OAuth2AccessToken.class),
                                                                                Collections.emptyMap()));
        when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
    }

}
