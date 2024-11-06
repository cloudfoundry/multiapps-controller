package org.cloudfoundry.multiapps.controller.process.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ArchiveEntryStreamWithStreamPositionsDeterminer {

    public static final int BUFFER_SIZE = 8 * 1024;

    private final FileService fileService;

    @Inject
    public ArchiveEntryStreamWithStreamPositionsDeterminer(FileService fileService) {
        this.fileService = fileService;
    }

    public List<ArchiveEntryWithStreamPositions> determineArchiveEntries(ProcessContext context, String appArchiveId) {
        try {
            return fileService.processFileContent(context.getVariable(Variables.SPACE_GUID), appArchiveId, archiveStream -> {
                List<ArchiveEntryWithStreamPositions> archiveEntriesWithPositions = new ArrayList<>();
                try (
                    ZipArchiveInputStream zipStream = new ZipArchiveInputStream(archiveStream, StandardCharsets.UTF_8.name(), true, true)) {
                    ZipArchiveEntry entry = zipStream.getNextEntry();
                    while (entry != null) {
                        validateEntry(entry);
                        long startOffset = entry.getDataOffset();
                        long endOffset = startOffset;
                        byte[] buffer = new byte[BUFFER_SIZE];
                        while (zipStream.read(buffer, 0, buffer.length) != -1) {
                            // read the entry, to calculate the compressed size
                        }
                        endOffset += zipStream.getCompressedCount();
                        archiveEntriesWithPositions.add(ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                .name(entry.getName())
                                                                                                .startPosition(startOffset)
                                                                                                .endPosition(endOffset)
                                                                                                .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.parseValue(entry.getMethod()))
                                                                                                .isDirectory(entry.isDirectory())
                                                                                                .build());
                        entry = zipStream.getNextEntry();
                    }
                }
                return archiveEntriesWithPositions;
            });
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    protected void validateEntry(ZipEntry entry) {
        FileUtils.validatePath(entry.getName());
    }

}
