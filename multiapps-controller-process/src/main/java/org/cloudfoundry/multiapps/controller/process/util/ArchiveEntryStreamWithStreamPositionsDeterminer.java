package org.cloudfoundry.multiapps.controller.process.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.FileMimeTypeValidator;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;

@Named
public class ArchiveEntryStreamWithStreamPositionsDeterminer {

    public static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;
    private final FileMimeTypeValidator fileMimeTypeValidator;

    @Inject
    public ArchiveEntryStreamWithStreamPositionsDeterminer(FileService fileService, FileMimeTypeValidator fileMimeTypeValidator) {
        this.fileService = fileService;
        this.fileMimeTypeValidator = fileMimeTypeValidator;
    }

    public List<ArchiveEntryWithStreamPositions> determineArchiveEntries(String spaceGuid, String appArchiveId,
                                                                         Consumer<String> stepLogger) {
        try {
            return fileService.processFileContent(spaceGuid, appArchiveId, archiveStream -> {
                List<ArchiveEntryWithStreamPositions> archiveEntriesWithPositions = new ArrayList<>();
                fileMimeTypeValidator.validateArchiveType(spaceGuid, appArchiveId, stepLogger);
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
                                                                                                .compressionMethod(
                                                                                                    ArchiveEntryWithStreamPositions.CompressionMethod.parseValue(
                                                                                                        entry.getMethod()))
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
