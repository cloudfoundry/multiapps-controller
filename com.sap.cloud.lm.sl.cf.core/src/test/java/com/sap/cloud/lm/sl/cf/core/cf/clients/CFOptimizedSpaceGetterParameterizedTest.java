package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class CFOptimizedSpaceGetterParameterizedTest extends CFOptimizedSpaceGetterBaseTest {

    private static final String FIND_SPACE_ENDPOINT = "https://api.cf.sap.com/v2/spaces?inline-relations-depth=1&q=name:{name}&q=organization_guid:{organization_guid}";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) The controller returns a single page result with a single resource on it:
            {
                "find-space-response-00.json", "R:expected-cloud-space-00.json",
            },
            // (1) The controller returns a multi-page result with a single resource on each:
            {
                "find-space-response-01.json", "E:The response of finding a space by org and space names should contain just one page",
            },
            // (2) The controller returns a single page result with zero resources in it:
            {
                "find-space-response-02.json", "null",
            },
            // (3) The controller returns a single page result without a 'resources' element:
            {
                "find-space-response-03.json", "E:The response of finding a space by org and space names should contain a 'resources' element",
            },
            // (4) The controller returns a single page result with multiple resources:
            {
                "find-space-response-04.json", "E:The response of finding a space by org and space names should not have more than one resource element",
            },
// @formatter:on
        });
    }

    private String responseLocation;
    private String expectedResultLocation;

    private String response;

    public CFOptimizedSpaceGetterParameterizedTest(String responseLocation, String expectedResultLocation) {
        this.responseLocation = responseLocation;
        this.expectedResultLocation = expectedResultLocation;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        loadTestInput();
        prepareRestClient();
        prepareControllerClient();
    }

    private void loadTestInput() throws Exception {
        this.response = TestUtil.getResourceAsString(responseLocation, getClass());
    }

    private void prepareRestClient() throws Exception {
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("name", DUMMY);
        urlVariables.put("organization_guid", "eed8e396-09f9-4fcc-8180-82de91dd626a");
        when(restTemplate.getForObject(FIND_SPACE_ENDPOINT, String.class, urlVariables)).thenReturn(response);
    }

    private void prepareControllerClient() throws Exception {
        CloudOrganization org = new CloudOrganization(new Meta(UUID.fromString("eed8e396-09f9-4fcc-8180-82de91dd626a"), null, null), DUMMY);
        when(client.getOrgByName(DUMMY, false)).thenReturn(org);
    }

    @Test
    public void testFindSpace() {
        TestUtil.test(() -> spaceGetter.findSpace(client, DUMMY, DUMMY), expectedResultLocation, getClass());
    }

}
