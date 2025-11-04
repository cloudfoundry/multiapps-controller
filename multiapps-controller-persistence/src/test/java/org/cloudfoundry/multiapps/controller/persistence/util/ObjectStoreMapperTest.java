package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectStoreMapperTest {

    private static final String SPACE_ID = "spaceId";
    private static final String NAMESPACE = "namespace";

    @Test
    void testCreateFileEntryMetadata() {
        LocalDateTime modifiedTime = LocalDateTime.now();
        String modifiedTimeInstantString = Long.toString(modifiedTime
                                                             .atZone(
                                                                 ZoneId.systemDefault())
                                                             .toInstant()
                                                             .toEpochMilli());
        Map<String, String> metadata = ObjectStoreMapper.createFileEntryMetadata(buildCreateFileEntry(modifiedTime));

        assertEquals(SPACE_ID, metadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase()));
        assertEquals(modifiedTimeInstantString, metadata.get(Constants.FILE_ENTRY_MODIFIED.toLowerCase()));
        assertEquals(NAMESPACE, metadata.get(Constants.FILE_ENTRY_NAMESPACE.toLowerCase()));
    }

    @Test
    void testCreateFileEntry() {
        String id = UUID.randomUUID()
                        .toString();
        FileEntry fileEntry = ObjectStoreMapper.createFileEntry(SPACE_ID, id);

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
}
