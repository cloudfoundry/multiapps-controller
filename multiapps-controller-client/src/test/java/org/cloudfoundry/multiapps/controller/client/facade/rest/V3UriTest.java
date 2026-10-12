package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3UriTest {

    @Test
    void testPathWithoutPlaceholdersOrQueryReturnsPathAsIs() {
        Assertions.assertEquals("/v3/domains", V3Uri.path("/v3/domains")
                                                    .build());
    }

    @Test
    void testPathFillsSinglePlaceholder() {
        Assertions.assertEquals("/v3/jobs/job-guid", V3Uri.path("/v3/jobs/{guid}", "job-guid")
                                                          .build());
    }

    @Test
    void testPathFillsMultiplePlaceholdersInOrder() {
        Assertions.assertEquals("/v3/apps/app-guid/features/ssh", V3Uri.path("/v3/apps/{guid}/features/{name}", "app-guid", "ssh")
                                                                       .build());
    }

    @Test
    void testPathFillsPlaceholdersInsideAndQueryAfter() {
        Assertions.assertEquals("/v3/service_instances/si-guid/parameters",
                                V3Uri.path("/v3/service_instances/{guid}/parameters", "si-guid")
                                     .build());
    }

    @Test
    void testPerPageAddedAsFirstQueryParameter() {
        Assertions.assertEquals("/v3/domains?per_page=5000", V3Uri.path("/v3/domains")
                                                                  .perPage(5000)
                                                                  .build());
    }

    @Test
    void testMultipleQueryParametersUseAmpersandSeparators() {
        String uri = V3Uri.path("/v3/service_instances")
                          .perPage(5000)
                          .query("space_guids", "space-guid")
                          .query("type", "key")
                          .build();

        Assertions.assertEquals("/v3/service_instances?per_page=5000&space_guids=space-guid&type=key", uri);
    }

    @Test
    void testNullQueryValueIsSkipped() {
        String uri = V3Uri.path("/v3/apps")
                          .perPage(5000)
                          .query("names", (Object) null)
                          .build();

        Assertions.assertEquals("/v3/apps?per_page=5000", uri);
    }

    @Test
    void testCollectionQueryValueIsCommaJoined() {
        String uri = V3Uri.path("/v3/service_instances")
                          .perPage(5000)
                          .query("names", List.of("a", "b", "c"))
                          .build();

        Assertions.assertEquals("/v3/service_instances?per_page=5000&names=a,b,c", uri);
    }

    @Test
    void testCollectionQueryEncodesCommaWithinAnElement() {
        String uri = V3Uri.path("/v3/service_instances")
                          .perPage(5000)
                          .query("names", List.of("a,b", "c"))
                          .build();

        Assertions.assertEquals("/v3/service_instances?per_page=5000&names=a%2Cb,c", uri);
    }

    @Test
    void testQueryEncodedEscapesCommaInSingleValue() {
        String uri = V3Uri.path("/v3/apps")
                          .perPage(5000)
                          .queryEncoded("names", "my,app")
                          .build();

        Assertions.assertEquals("/v3/apps?per_page=5000&names=my%2Capp", uri);
    }

    @Test
    void testQueryEncodedLeavesCommaFreeValueUnchanged() {
        String uri = V3Uri.path("/v3/apps")
                          .queryEncoded("names", "my-app")
                          .build();

        Assertions.assertEquals("/v3/apps?names=my-app", uri);
    }

    @Test
    void testQueryEncodedSkipsNullValue() {
        String uri = V3Uri.path("/v3/apps")
                          .perPage(5000)
                          .queryEncoded("names", null)
                          .build();

        Assertions.assertEquals("/v3/apps?per_page=5000", uri);
    }

    @Test
    void testNullCollectionQueryValueIsSkipped() {
        String uri = V3Uri.path("/v3/service_instances")
                          .perPage(5000)
                          .query("names", null)
                          .build();

        Assertions.assertEquals("/v3/service_instances?per_page=5000", uri);
    }

    @Test
    void testEmptyCollectionQueryValueIsSkipped() {
        String uri = V3Uri.path("/v3/service_instances")
                          .perPage(5000)
                          .query("names", List.of())
                          .build();

        Assertions.assertEquals("/v3/service_instances?per_page=5000", uri);
    }

    @Test
    void testPlaceholderValueContainingDollarAndBackslashIsNotInterpretedAsRegex() {
        String uri = V3Uri.path("/v3/apps/{guid}", "weird$name\\value")
                          .build();

        Assertions.assertEquals("/v3/apps/weird$name\\value", uri);
    }

    @Test
    void testQueryValueContainingSpecialCharsIsKeptRaw() {
        String uri = V3Uri.path("/v3/apps")
                          .query("label_selector", "a=b,c in (d)")
                          .build();

        Assertions.assertEquals("/v3/apps?label_selector=a=b,c in (d)", uri);
    }

}
