package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.sap.cloud.lm.sl.common.util.TestUtil;

public class CFOptimizedSpaceGetterTest extends CFOptimizedSpaceGetterBaseTest {

    private static final String GET_SPACE_ENDPOINT = "https://api.cf.sap.com/v2/spaces/{id}?inline-relations-depth=1";

    @Test
    public void testGetSpace() throws Exception {
        String response = TestUtil.getResourceAsString("get-space-response.json", getClass());
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("id", "1");
        when(restTemplate.getForObject(GET_SPACE_ENDPOINT, String.class, urlVariables)).thenReturn(response);
        TestUtil.test(() -> spaceGetter.getSpace(client, "1"), "R:expected-cloud-space-00.json", getClass());
    }

    @Test
    public void testGetSpaceWithHttpErrorCode404() {
        testGetSpaceWithHttpErrorCode(HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGetSpaceWithHttpErrorCode403() {
        testGetSpaceWithHttpErrorCode(HttpStatus.FORBIDDEN);
    }

    private void testGetSpaceWithHttpErrorCode(HttpStatus status) {
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("id", "1");
        when(restTemplate.getForObject(GET_SPACE_ENDPOINT, String.class, urlVariables)).thenThrow(new HttpClientErrorException(status));
        try {
            spaceGetter.getSpace(client, "1");
        } catch (CloudOperationException e) {
            assertEquals(e.getStatusCode(), status);
            return;
        }
        fail();
    }

}
