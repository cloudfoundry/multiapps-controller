package org.cloudfoundry.multiapps.controller.persistence.util;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public class ObjectStoreFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreFilter.class);

    public static boolean filterBySpaceIds(Map<String, String> metadata, List<String> spaceIds) {
        if (CollectionUtils.isEmpty(metadata)) {
            return false;
        }
        String spaceParameter = metadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase());

        return spaceIds.contains(spaceParameter);
    }

    public static boolean filterBySpaceAndNamespace(Map<String, String> metadata, String space, String namespace) {
        if (CollectionUtils.isEmpty(metadata)) {
            return false;
        }

        String spaceParameter = metadata.get(Constants.FILE_ENTRY_SPACE.toLowerCase());
        String namespaceParameter = metadata.get(Constants.FILE_ENTRY_NAMESPACE.toLowerCase());
        return space.equals(spaceParameter) && namespace.equals(namespaceParameter);
    }

    public static boolean filterByModificationTime(Map<String, String> metadata, String blobName, LocalDateTime modificationTime) {
        // Clean up any blobStore entries that don't have any metadata as we can't check their creation date
        if (CollectionUtils.isEmpty(metadata)) {
            LOGGER.warn(MessageFormat.format(Messages.USER_METADATA_OF_BLOB_0_EMPTY_AND_WILL_BE_DELETED, blobName));
            return true;
        }
        String longString = metadata.get(Constants.FILE_ENTRY_MODIFIED.toLowerCase());
        try {
            long dateLong = Long.parseLong(longString);
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(dateLong), ZoneId.systemDefault());
            return date.isBefore(modificationTime);
        } catch (NumberFormatException e) {
            // Clean up any blobStore entries that have invalid timestamp
            LOGGER.warn(MessageFormat.format(Messages.DATE_METADATA_OF_BLOB_0_IS_NOT_IN_PROPER_FORMAT_AND_WILL_BE_DELETED,
                                             blobName),
                        e);
            return true;
        }
    }
}
