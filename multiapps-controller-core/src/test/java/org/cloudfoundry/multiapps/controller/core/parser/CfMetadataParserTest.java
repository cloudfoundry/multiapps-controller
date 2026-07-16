package org.cloudfoundry.multiapps.controller.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.api.Test;

class CfMetadataParserTest {

    private final CfMetadataParser parser = new CfMetadataParser();

    @Test
    void parseAbsentCfMetadataReturnsEmpty() {
        CfUserMetadata result = parser.parse(List.of(Map.of()));

        assertTrue(result.getLabels().isEmpty());
        assertTrue(result.getAnnotations().isEmpty());
    }

    @Test
    void parseLabelsAndAnnotations() {
        Map<String, Object> params = Map.of(SupportedParameters.CF_METADATA,
                                            Map.of("labels", Map.of("env", "prod"),
                                                   "annotations", Map.of("owner", "team-a")));

        CfUserMetadata result = parser.parse(List.of(params));

        assertEquals(Map.of("env", "prod"), result.getLabels());
        assertEquals(Map.of("owner", "team-a"), result.getAnnotations());
    }

    @Test
    void parseLabelsOnly() {
        Map<String, Object> params = Map.of(SupportedParameters.CF_METADATA,
                                            Map.of("labels", Map.of("cost-center", "1234")));

        CfUserMetadata result = parser.parse(List.of(params));

        assertEquals(Map.of("cost-center", "1234"), result.getLabels());
        assertTrue(result.getAnnotations().isEmpty());
    }

    @Test
    void cfMetadataNotAMapThrowsContentException() {
        Map<String, Object> params = Map.of(SupportedParameters.CF_METADATA, "not-a-map");

        assertThrows(ContentException.class, () -> parser.parse(List.of(params)));
    }

    @Test
    void labelsValueNotAMapThrowsContentException() {
        Map<String, Object> params = Map.of(SupportedParameters.CF_METADATA,
                                            Map.of("labels", "not-a-map"));

        assertThrows(ContentException.class, () -> parser.parse(List.of(params)));
    }

}
