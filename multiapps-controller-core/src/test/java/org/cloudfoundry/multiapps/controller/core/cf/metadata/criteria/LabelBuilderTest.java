package org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LabelBuilderTest {

    private static final String LABEL = "mta_id";
    private static final String VALUE = "abc";
    private static final String LEGACY_VALUE = "def";

    @Test
    void testHasValueWithLegacyFallbackProducesSetMembership() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallback(VALUE, LEGACY_VALUE)
                                                 .build()
                                                 .get();
        assertEquals("mta_id in (abc,def)", query);
    }

    @Test
    void testHasValueWithLegacyFallbackDegradesToEqualityWhenLegacyIsNull() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallback(VALUE, null)
                                                 .build()
                                                 .get();
        assertEquals("mta_id=abc", query);
    }

    @Test
    void testHasValueWithLegacyFallbackDegradesToEqualityWhenLegacyIsEmpty() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallback(VALUE, "")
                                                 .build()
                                                 .get();
        assertEquals("mta_id=abc", query);
    }

    @Test
    void testHasValueWithLegacyFallbackOrIsntPresentUsesSetMembership() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallbackOrIsntPresent(VALUE, LEGACY_VALUE)
                                                 .build()
                                                 .get();
        assertEquals("mta_id in (abc,def)", query);
    }

    @Test
    void testHasValueWithLegacyFallbackOrIsntPresentDegradesToEqualityWhenLegacyIsNull() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallbackOrIsntPresent(VALUE, null)
                                                 .build()
                                                 .get();
        assertEquals("mta_id=abc", query);
    }

    @Test
    void testHasValueWithLegacyFallbackOrIsntPresentBecomesAbsenceCheckWhenValueIsEmpty() {
        String query = MtaMetadataCriteriaBuilder.builder()
                                                 .label(LABEL)
                                                 .hasValueWithLegacyFallbackOrIsntPresent("", LEGACY_VALUE)
                                                 .build()
                                                 .get();
        assertEquals("!mta_id", query);
    }
}
