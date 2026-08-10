package org.cloudfoundry.multiapps.controller.client.facade.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CloudControllerV3ClientPageUriTest {

    @Test
    void testAddsPageWhenAbsentAndKeepsExistingQuery() {
        String uri = CloudControllerV3Client.pageUri("/v3/apps?per_page=5000&space_guids=abc", 3);

        Assertions.assertTrue(uri.contains("page=3"), uri);
        Assertions.assertTrue(uri.contains("per_page=5000"), uri);
        Assertions.assertTrue(uri.contains("space_guids=abc"), uri);
    }

    @Test
    void testReplacesExistingPageParam() {
        String uri = CloudControllerV3Client.pageUri("/v3/apps?per_page=5000&page=1", 4);

        Assertions.assertTrue(uri.contains("page=4"), uri);
        Assertions.assertFalse(uri.contains("page=1"), uri);
        // per_page must survive the replacement.
        Assertions.assertTrue(uri.contains("per_page=5000"), uri);
    }

    @Test
    void testPreservesMultipleFilters() {
        String first = "/v3/service_credential_bindings?per_page=5000&type=key&service_instance_guids=g1&names=n1";

        String uri = CloudControllerV3Client.pageUri(first, 2);

        Assertions.assertTrue(uri.contains("page=2"), uri);
        Assertions.assertTrue(uri.contains("type=key"), uri);
        Assertions.assertTrue(uri.contains("service_instance_guids=g1"), uri);
        Assertions.assertTrue(uri.contains("names=n1"), uri);
        Assertions.assertTrue(uri.contains("per_page=5000"), uri);
    }

}
