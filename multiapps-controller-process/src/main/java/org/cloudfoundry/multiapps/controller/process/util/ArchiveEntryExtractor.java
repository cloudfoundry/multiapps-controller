package org.cloudfoundry.multiapps.controller.process.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.ObjIntConsumer;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentToProcess;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ImmutableFileContentToProcess;
import org.cloudfoundry.multiapps.controller.process.stream.DefaultLimitedInputStream;
import org.cloudfoundry.multiapps.mta.util.EntryToInflate;
import org.cloudfoundry.multiapps.mta.util.InflatorUtil;

import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
public class ArchiveEntryExtractor {

    private static final int BUFFER_SIZE = 4 * 1024; // 4KB

    private final FileService fileService;

    @Inject
    public ArchiveEntryExtractor(FileService fileService) {
        this.fileService = fileService;
    }

    public byte[] readFullEntry(FileEntryProperties fileEntryProperties, ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions) {
        try {
            return fileService.processFileContentWithOffset(toFileContentToProcess(fileEntryProperties, archiveEntryWithStreamPositions),
                                                            fileEntryStream -> {
                                                                if (archiveEntryWithStreamPositions.getCompressionMethod() == ArchiveEntryWithStreamPositions.CompressionMethod.STORED) {
                                                                    return new DefaultLimitedInputStream(fileEntryStream,
                                                                                                         fileEntryProperties.getName(),
                                                                                                         fileEntryProperties.getMaxFileSizeInBytes()).readAllBytes();
                                                                }
                                                                return inflateFileContent(fileEntryProperties, fileEntryStream);
                                                            });
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

    private FileContentToProcess toFileContentToProcess(FileEntryProperties fileEntryProperties,
                                                        ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions) {
        return ImmutableFileContentToProcess.builder()
                                            .guid(fileEntryProperties.getGuid())
                                            .spaceGuid(fileEntryProperties.getSpaceGuid())
                                            .startOffset(archiveEntryWithStreamPositions.getStartPosition())
                                            .endOffset(archiveEntryWithStreamPositions.getEndPosition())
                                            .build();
    }

    private byte[] inflateFileContent(FileEntryProperties fileEntryProperties, InputStream fileEntryStream) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            InflatorUtil.inflate(new EntryToInflate(fileEntryProperties.getName(),
                                                    fileEntryProperties.getMaxFileSizeInBytes(),
                                                    fileEntryStream),
                                 (bytesBuffer, bytesRead) -> byteArrayOutputStream.write(bytesBuffer, 0, bytesRead));
            return byteArrayOutputStream.toByteArray();
        }
    }

    public void processFileEntryContent(FileEntryProperties fileEntryProperties,
                                        ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions,
                                        ObjIntConsumer<byte[]> decompressedBytesConsumer) {
        try {
            fileService.consumeFileContentWithOffset(toFileContentToProcess(fileEntryProperties, archiveEntryWithStreamPositions),
                                                     fileEntryStream -> {
                                                         if (archiveEntryWithStreamPositions.getCompressionMethod() == ArchiveEntryWithStreamPositions.CompressionMethod.STORED) {
                                                             byte[] buffer = new byte[BUFFER_SIZE];
                                                             int bytesRead;
                                                             try (
                                                                 InputStream inputStream = new DefaultLimitedInputStream(fileEntryStream,
                                                                                                                         fileEntryProperties.getName(),
                                                                                                                         fileEntryProperties.getMaxFileSizeInBytes())) {
                                                                 while ((bytesRead = inputStream.read(buffer)) != -1) {
                                                                     decompressedBytesConsumer.accept(buffer, bytesRead);
                                                                 }
                                                             }
                                                         } else {
                                                             InflatorUtil.inflate(new EntryToInflate(fileEntryProperties.getName(),
                                                                                                     fileEntryProperties.getMaxFileSizeInBytes(),
                                                                                                     fileEntryStream),
                                                                                  decompressedBytesConsumer);
                                                         }
                                                     });
        } catch (FileStorageException e) {
            throw new SLException(e, e.getMessage());
        }
    }

}
