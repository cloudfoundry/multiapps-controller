package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.common.SLException;

public class ArchiveEntryExtractorUtil {

    private ArchiveEntryExtractorUtil() {

    }

    public static ArchiveEntryWithStreamPositions findEntry(String entryName,
                                                            List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        return archiveEntriesWithStreamPositions.stream()
                                                .filter(e -> e.getName()
                                                              .startsWith(entryName))
                                                .findFirst()
                                                .orElseThrow(() -> new SLException("Entry with name: {0} not found", entryName));
    }

    public static boolean hasDirectory(String entryName, List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions) {
        return archiveEntriesWithStreamPositions.stream()
                                                .filter(entry -> entry.getName()
                                                                      .startsWith(entryName))
                                                .anyMatch(ArchiveEntryWithStreamPositions::isDirectory);
    }

}
