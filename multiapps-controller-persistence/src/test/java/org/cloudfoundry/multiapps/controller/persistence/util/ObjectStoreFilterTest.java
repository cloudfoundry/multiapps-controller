package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectStoreFilterTest {

    private static final String SPACE_ID = "spaceId";
    private static final String BLOBNAME = "blobName";
    private static final String SPACE_ID_2 = "spaceId2";
    private static final String NAMESPACE = "namespace";
    private static final String NAMESPACE_2 = "namespace2";

    @Test
    void testFilterBySpaceIdsWhenMetadataIsEmpty() {
        assertFalse(ObjectStoreFilter.filterBySpaceIds(Map.of(), List.of()));
    }

    @Test
    void testFilterBySpaceIdsWhenTheMetadataContainsTheSpaceId() {
        assertTrue(ObjectStoreFilter.filterBySpaceIds(buildMetadata(), List.of(SPACE_ID)));
    }

    @Test
    void testFilterBySpaceIdsWhenTheMetadataDoesNotContainTheSpaceId() {
        assertFalse(ObjectStoreFilter.filterBySpaceIds(buildMetadata(), List.of(SPACE_ID_2)));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenMetadataIsEmpty() {
        assertFalse(ObjectStoreFilter.filterBySpaceAndNamespace(Map.of(), SPACE_ID, NAMESPACE));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenTheSpacesAreDifferent() {
        assertFalse(ObjectStoreFilter.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID_2, NAMESPACE));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenTheNamespacesAreDifferent() {
        assertFalse(ObjectStoreFilter.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID, NAMESPACE_2));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenAllMatch() {
        assertTrue(ObjectStoreFilter.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID, NAMESPACE));
    }

    @Test
    void testFilterByModificationTimeWhenMetadataIsEmpty() {
        assertTrue(ObjectStoreFilter.filterByModificationTime(Map.of(), BLOBNAME, LocalDateTime.now()));
    }

    @Test
    void testFilterByModificationTimeWhenModificationTimeIsBeforeTheMetadataTime() {
        assertFalse(ObjectStoreFilter.filterByModificationTime(buildMetadata(), BLOBNAME, LocalDateTime.now()
                                                                                                       .minusMinutes(20)));
    }

    @Test
    void testFilterByModificationTimeWhenModificationTimeIsAfterTheMetadataTime() {
        assertTrue(ObjectStoreFilter.filterByModificationTime(buildMetadata(), BLOBNAME, LocalDateTime.now()
                                                                                                      .plusMinutes(20)));
    }

    private Map<String, String> buildMetadata() {
        return Map.of(Constants.FILE_ENTRY_SPACE.toLowerCase(), SPACE_ID, Constants.FILE_ENTRY_NAMESPACE.toLowerCase(), NAMESPACE,
                      Constants.FILE_ENTRY_MODIFIED.toLowerCase(), String.valueOf(Instant.now()
                                                                                         .toEpochMilli()));
    }
}
