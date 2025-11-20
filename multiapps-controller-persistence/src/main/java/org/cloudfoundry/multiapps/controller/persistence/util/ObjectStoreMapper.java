package org.cloudfoundry.multiapps.controller.persistence.util;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;

public class ObjectStoreMapper {

    public static Map<String, String> createFileEntryMetadata(FileEntry fileEntry) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(Constants.FILE_ENTRY_SPACE.toLowerCase(), fileEntry.getSpace());
        metadata.put(Constants.FILE_ENTRY_MODIFIED.toLowerCase(),
                     Long.toString(fileEntry.getModified()
                                            .atZone(
                                                ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli()));
        if (fileEntry.getNamespace() != null) {
            metadata.put(Constants.FILE_ENTRY_NAMESPACE.toLowerCase(), fileEntry.getNamespace());
        }
        return metadata;
    }

    public static FileEntry createFileEntry(String space, String id) {
        return ImmutableFileEntry.builder()
                                 .space(space)
                                 .id(id)
                                 .build();
    }

}
