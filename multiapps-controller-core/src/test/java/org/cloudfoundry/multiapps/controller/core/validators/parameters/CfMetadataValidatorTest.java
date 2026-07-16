package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCfUserMetadata;
import org.junit.jupiter.api.Test;

class CfMetadataValidatorTest {

    private final CfMetadataValidator validator = new CfMetadataValidator();

    @Test
    void validLabelAndAnnotationPassValidation() {
        var metadata = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("env", "prod"))
                                              .annotations(Map.of("owner", "team-a"))
                                              .build();

        assertDoesNotThrow(() -> validator.validate(metadata));
    }

    @Test
    void labelKeyWithSpaceThrowsInvalidKeyException() {
        var metadata = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("bad key", "value"))
                                              .build();

        assertThrows(ContentException.class, () -> validator.validate(metadata));
    }

    @Test
    void reservedMtaLabelKeyThrowsReservedKeyException() {
        var metadata = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("mta_id", "some-mta"))
                                              .build();

        ContentException ex = assertThrows(ContentException.class, () -> validator.validate(metadata));
        assertContains(ex.getMessage(), "reserved");
    }

    @Test
    void labelKeyStartingWithMtaUnderscoreThrowsReservedKeyException() {
        var metadata = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("mta_custom", "value"))
                                              .build();

        ContentException ex = assertThrows(ContentException.class, () -> validator.validate(metadata));
        assertContains(ex.getMessage(), "reserved");
    }

    @Test
    void labelValueExceeding63CharsThrowsTooLongException() {
        String longValue = "x".repeat(64);
        var metadata = ImmutableCfUserMetadata.builder()
                                              .labels(Map.of("valid-key", longValue))
                                              .build();

        ContentException ex = assertThrows(ContentException.class, () -> validator.validate(metadata));
        assertContains(ex.getMessage(), "63");
    }

    @Test
    void annotationValueExceeding5000CharsThrowsTooLongException() {
        String longValue = "x".repeat(5001);
        var metadata = ImmutableCfUserMetadata.builder()
                                              .annotations(Map.of("valid-key", longValue))
                                              .build();

        ContentException ex = assertThrows(ContentException.class, () -> validator.validate(metadata));
        assertContains(ex.getMessage(), "5000");
    }

    @Test
    void reservedAnnotationKeyThrowsReservedKeyException() {
        var metadata = ImmutableCfUserMetadata.builder()
                                              .annotations(Map.of("mta_resource", "value"))
                                              .build();

        ContentException ex = assertThrows(ContentException.class, () -> validator.validate(metadata));
        assertContains(ex.getMessage(), "reserved");
    }

    private void assertContains(String message, String expected) {
        if (!message.contains(expected)) {
            throw new AssertionError("Expected message to contain '" + expected + "' but was: " + message);
        }
    }

}
