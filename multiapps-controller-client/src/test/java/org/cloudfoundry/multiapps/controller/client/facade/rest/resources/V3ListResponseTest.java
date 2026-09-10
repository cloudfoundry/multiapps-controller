package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse.Link;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse.Pagination;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ListResponseTest {

    @Test
    void testResourcesWithNullReturnsEmptyList() {
        V3ListResponse<String> response = new V3ListResponse<>(null, null);

        Assertions.assertTrue(response.resources()
                                      .isEmpty());
    }

    @Test
    void testResourcesReturnsSuppliedList() {
        V3ListResponse<String> response = new V3ListResponse<>(null, List.of("a", "b"));

        Assertions.assertEquals(List.of("a", "b"), response.resources());
    }

    @Test
    void testNextPageHrefWithNullPaginationReturnsNull() {
        V3ListResponse<String> response = new V3ListResponse<>(null, List.of());

        Assertions.assertNull(response.nextPageHref());
    }

    @Test
    void testNextPageHrefWithNullNextLinkReturnsNull() {
        Pagination pagination = new Pagination(0, 1, null, null);
        V3ListResponse<String> response = new V3ListResponse<>(pagination, List.of());

        Assertions.assertNull(response.nextPageHref());
    }

    @Test
    void testNextPageHrefReturnsHref() {
        Pagination pagination = new Pagination(100, 2, new Link("/v3/apps?page=2"), null);
        V3ListResponse<String> response = new V3ListResponse<>(pagination, List.of());

        Assertions.assertEquals("/v3/apps?page=2", response.nextPageHref());
    }

}
