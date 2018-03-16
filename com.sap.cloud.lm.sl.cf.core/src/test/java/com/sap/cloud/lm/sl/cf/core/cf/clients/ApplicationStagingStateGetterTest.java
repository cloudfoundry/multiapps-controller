package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.Assert.assertEquals;

import java.net.URL;
import java.util.Arrays;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStagingState;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ApplicationStagingStateGetterTest {

    private static final String CONTROLLER_URL = "https://api.cf.sap.com";
    private static final String APP_URL = "https://api.cf.sap.com/v2/apps/a72df2e8-b06c-44b2-a8fa-5cadb0239573";
    private static final String APP_GUID = "a72df2e8-b06c-44b2-a8fa-5cadb0239573";
    private static final String APP_NAME = "foo";
    private static final CloudApplication APP = new CloudApplication(new Meta(UUID.fromString(APP_GUID), null, null), APP_NAME);

    private String responseLocation;
    private ApplicationStagingState expected;
    private String response;

    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CloudFoundryOperations client;

    @InjectMocks
    private ApplicationStagingStateGetter applicationStagingStateGetter = new ApplicationStagingStateGetter();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            {
                "apps-response-with-package-state.json", ApplicationStagingState.STAGED,
            },
            {
                "apps-response-without-package-state.json", null
            },
// @formatter:on
        });
    }

    public ApplicationStagingStateGetterTest(String responseLocation, ApplicationStagingState expected) {
        this.responseLocation = responseLocation;
        this.expected = expected;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.response = TestUtil.getResourceAsString(responseLocation, getClass());
        Mockito.when(restTemplateFactory.getRestTemplate(client))
            .thenReturn(restTemplate);
        Mockito.when(restTemplate.getForObject(APP_URL, String.class))
            .thenReturn(response);
        Mockito.when(client.getCloudControllerUrl())
            .thenReturn(new URL(CONTROLLER_URL));
        Mockito.when(client.getApplication(APP_NAME))
            .thenReturn(APP);
    }

    @Test
    public void testGetApplicationStagingState() {
        assertEquals(expected, applicationStagingStateGetter.getApplicationStagingState(client, APP_NAME));
    }

}
