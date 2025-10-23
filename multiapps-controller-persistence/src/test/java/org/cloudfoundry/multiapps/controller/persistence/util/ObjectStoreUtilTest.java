package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectStoreUtilTest {

    private static final String SPACE_ID = "spaceId";
    private static final String SPACE_ID_2 = "spaceId2";
    private static final String NAMESPACE = "namespace";
    private static final String BLOBNAME = "blobName";
    private static final String NAMESPACE_2 = "namespace2";

    @Test
    void testFilterBySpaceIdsWhenMetadataIsEmpty() {
        assertFalse(ObjectStoreUtil.filterBySpaceIds(Map.of(), List.of()));
    }

    @Test
    void testFilterBySpaceIdsWhenTheMetadataContainsTheSpaceId() {
        assertTrue(ObjectStoreUtil.filterBySpaceIds(buildMetadata(), List.of(SPACE_ID)));
    }

    @Test
    void testFilterBySpaceIdsWhenTheMetadataDoesNotContainTheSpaceId() {
        assertFalse(ObjectStoreUtil.filterBySpaceIds(buildMetadata(), List.of(SPACE_ID_2)));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenMetadataIsEmpty() {
        assertFalse(ObjectStoreUtil.filterBySpaceAndNamespace(Map.of(), SPACE_ID, NAMESPACE));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenTheSpacesAreDifferent() {
        assertFalse(ObjectStoreUtil.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID_2, NAMESPACE));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenTheNamespacesAreDifferent() {
        assertFalse(ObjectStoreUtil.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID, NAMESPACE_2));
    }

    @Test
    void testFilterBySpaceAndNamespaceWhenAllMatch() {
        assertTrue(ObjectStoreUtil.filterBySpaceAndNamespace(buildMetadata(), SPACE_ID, NAMESPACE));
    }

    @Test
    void testFilterByModificationTimeWhenMetadataIsEmpty() {
        assertTrue(ObjectStoreUtil.filterByModificationTime(Map.of(), BLOBNAME, LocalDateTime.now()));
    }

    @Test
    void testFilterByModificationTimeWhenModificationTimeIsBeforeTheMetadataTime() {
        assertFalse(ObjectStoreUtil.filterByModificationTime(buildMetadata(), BLOBNAME, LocalDateTime.now()
                                                                                                     .minusMinutes(20)));
    }

    @Test
    void testFilterByModificationTimeWhenModificationTimeIsAfterTheMetadataTime() {
        assertTrue(ObjectStoreUtil.filterByModificationTime(buildMetadata(), BLOBNAME, LocalDateTime.now()
                                                                                                    .plusMinutes(20)));
    }

    @Test
    void testCreateFileEntryMetadata() {
        LocalDateTime modifiedTime = LocalDateTime.now();
        String modifiedTimeInstantString = Long.toString(modifiedTime
                                                             .atZone(
                                                                 ZoneId.systemDefault())
                                                             .toInstant()
                                                             .toEpochMilli());
        Map<String, String> metadata = ObjectStoreUtil.createFileEntryMetadata(buildCreateFileEntry(modifiedTime));

        assertEquals(SPACE_ID, metadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase()));
        assertEquals(modifiedTimeInstantString, metadata.get(Constants.FILE_ENTRY_MODIFIED.toLowerCase()));
        assertEquals(NAMESPACE, metadata.get(Constants.FILE_ENTRY_NAMESPACE.toLowerCase()));
    }

    @Test
    void testCreateFileEntry() {
        String id = UUID.randomUUID()
                        .toString();
        FileEntry fileEntry = ObjectStoreUtil.createFileEntry(SPACE_ID, id);

        assertEquals(SPACE_ID, fileEntry.getSpace());
        assertEquals(id, fileEntry.getId());
    }

    private FileEntry buildCreateFileEntry(LocalDateTime modifiedTime) {
        return ImmutableFileEntry.builder()
                                 .space(SPACE_ID)
                                 .modified(modifiedTime)
                                 .namespace(NAMESPACE)
                                 .build();
    }

    private Map<String, String> buildMetadata() {
        return Map.of(Constants.FILE_ENTRY_SPACE.toLowerCase(), SPACE_ID, Constants.FILE_ENTRY_NAMESPACE.toLowerCase(), NAMESPACE,
                      Constants.FILE_ENTRY_MODIFIED.toLowerCase(), String.valueOf(Instant.now()
                                                                                         .toEpochMilli()));
    }
}
