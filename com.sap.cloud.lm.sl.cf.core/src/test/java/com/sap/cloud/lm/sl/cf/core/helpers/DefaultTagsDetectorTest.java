package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Arrays;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DefaultTagsDetectorTest {

    private static final String CONTROLLER_ENDPOINT = "https://api.cf.sap.com";
    private static final String V2_SERVICE_ENDPOINT = "https://api.cf.sap.com/v2/services";
    private static final String TOKEN = "DUMMY";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The controller returns a list of service offerings, all of which have some default tags:
            {
                "v2-services-response-00.json", "R:expected-default-tags-00.json",
            },
            // (1) The controller returns a list of service offerings, some of which do not have default tags (tags element is empty):
            {
                "v2-services-response-01.json", "R:expected-default-tags-01.json",
            },
            // (2) The controller returns a list of service offerings, some of which do not have default tags (tags element is  null):
            {
                "v2-services-response-02.json", "R:expected-default-tags-02.json",
            },
// @formatter:on
        });
    }

    private RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private CloudFoundryOperations client;
    @InjectMocks
    private DefaultTagsDetector defaultTagsDetector = getDefaultTagsDetector();

    private String responseLocation;
    private String expectedResultLocation;

    private DefaultTagsDetector.Resources response;

    public DefaultTagsDetectorTest(String responseLocation, String expectedResultLocation) {
        this.responseLocation = responseLocation;
        this.expectedResultLocation = expectedResultLocation;
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        prepareTestParams();
        prepareRestClient();
        prepareControllerClient();
    }

    private void prepareControllerClient() throws Exception {
        when(client.login()).thenReturn(new DefaultOAuth2AccessToken(TOKEN));
        when(client.getCloudControllerUrl()).thenReturn(new URL(CONTROLLER_ENDPOINT));
    }

    private void prepareTestParams() throws Exception {
        this.response = JsonUtil.fromJson(TestUtil.getResourceAsString(responseLocation, getClass()), DefaultTagsDetector.Resources.class);
    }

    private void prepareRestClient() throws Exception {
        when(restTemplateFactory.getRestTemplate(eq(client))).thenReturn(restTemplate);
        when(restTemplate.getForObject(V2_SERVICE_ENDPOINT, DefaultTagsDetector.Resources.class)).thenReturn(response);
    }

    @Test
    public void testComputeDefaultTags() {
        TestUtil.test(() -> defaultTagsDetector.computeDefaultTags(client), expectedResultLocation, getClass());
    }

    private DefaultTagsDetector getDefaultTagsDetector() {
        return new DefaultTagsDetector();
    }

}
