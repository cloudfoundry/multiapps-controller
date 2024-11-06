package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.mta.Messages;

public class ArchiveEntryExtractorUtil {

    private ArchiveEntryExtractorUtil() {

    }

    public static ArchiveEntryWithStreamPositions findEntry(String entryName,
                                                            List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        return archiveEntriesWithStreamPositions.stream()
                                                .filter(e -> e.getName()
                                                              .startsWith(entryName))
                                                .findFirst()
                                                .orElseThrow(() -> new SLException(Messages.CANNOT_FIND_ARCHIVE_ENTRY, entryName));
    }

    public static boolean hasDirectory(String entryName, List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        return archiveEntriesWithStreamPositions.stream()
                                                .filter(entry -> entry.getName()
                                                                      .startsWith(entryName))
                                                .anyMatch(ArchiveEntryWithStreamPositions::isDirectory);
    }

}
