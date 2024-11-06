package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;

import org.apache.commons.io.input.BoundedInputStream;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ArchiveEntryExtractorTest {

    private static final ArchiveEntryWithStreamPositions DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                        .name("META-INF/mtad.yaml")
                                                                                                                                        .startPosition(271)
                                                                                                                                        .endPosition(315)
                                                                                                                                        .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                                                                                                        .isDirectory(false)
                                                                                                                                        .build();

    private static final ArchiveEntryWithStreamPositions STORED_DEPLOYMENT_DESCRIPTOR_ENTRY = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                      .name("META-INF/mtad.yaml")
                                                                                                                                      .startPosition(271)
                                                                                                                                      .endPosition(320)
                                                                                                                                      .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.STORED)
                                                                                                                                      .isDirectory(false)
                                                                                                                                      .build();

    @Mock
    private FileService fileService;

    private ArchiveEntryExtractor archiveEntryExtractor;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        archiveEntryExtractor = new ArchiveEntryExtractor(fileService);
    }

    @Test
    void readFullStoredDeploymentDescriptorFile() throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream("stored-mta.mtar"), STORED_DEPLOYMENT_DESCRIPTOR_ENTRY.getStartPosition(),
                           STORED_DEPLOYMENT_DESCRIPTOR_ENTRY.getEndPosition());
        byte[] bytesRead = archiveEntryExtractor.readFullEntry(buildFileEntryProperty(), STORED_DEPLOYMENT_DESCRIPTOR_ENTRY);
        assertDeploymentDescriptorAreEqual(new String(bytesRead));
    }

    private FileEntryProperties buildFileEntryProperty() {
        return ImmutableFileEntryProperties.builder()
                                           .guid(UUID.randomUUID()
                                                     .toString())
                                           .maxFileSize(Integer.MAX_VALUE)
                                           .spaceGuid(UUID.randomUUID()
                                                          .toString())
                                           .build();
    }

    private void assertDeploymentDescriptorAreEqual(String actualDeploymentDescriptor) throws IOException {
        String expectedDeploymentDescriptor = new String(getClass().getResourceAsStream("expected-mtad.yaml")
                                                                   .readAllBytes());
        assertEquals(expectedDeploymentDescriptor, actualDeploymentDescriptor);
    }

    @Test
    void readFullDeflatedDeploymentDescriptorFile() throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream("deflated-mta.mtar"), DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY.getStartPosition(),
                           DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY.getEndPosition());
        byte[] bytesRead = archiveEntryExtractor.readFullEntry(buildFileEntryProperty(), DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY);
        assertDeploymentDescriptorAreEqual(new String(bytesRead));
    }

    @Test
    void processStoredFileEntryContent() throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream("stored-mta.mtar"), STORED_DEPLOYMENT_DESCRIPTOR_ENTRY.getStartPosition(),
                           STORED_DEPLOYMENT_DESCRIPTOR_ENTRY.getEndPosition());
        StringBuilder actualDeploymentDescriptorContent = new StringBuilder();
        archiveEntryExtractor.processFileEntryContent(buildFileEntryProperty(), STORED_DEPLOYMENT_DESCRIPTOR_ENTRY,
                                                      (byteBuffer,
                                                       bytesRead) -> actualDeploymentDescriptorContent.append(new String(Arrays.copyOfRange(byteBuffer,
                                                                                                                                            0,
                                                                                                                                            bytesRead))));
        assertDeploymentDescriptorAreEqual(actualDeploymentDescriptorContent.toString());
    }

    @Test
    void processDeflatedFileEntryContent() throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream("deflated-mta.mtar"), DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY.getStartPosition(),
                           STORED_DEPLOYMENT_DESCRIPTOR_ENTRY.getEndPosition());
        StringBuilder actualDeploymentDescriptorContent = new StringBuilder();
        archiveEntryExtractor.processFileEntryContent(buildFileEntryProperty(), DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY,
                                                      (byteBuffer,
                                                       bytesRead) -> actualDeploymentDescriptorContent.append(new String(Arrays.copyOfRange(byteBuffer,
                                                                                                                                            0,
                                                                                                                                            bytesRead))));
        assertDeploymentDescriptorAreEqual(actualDeploymentDescriptorContent.toString());
    }

    private void prepareFileService(InputStream fileEntryInputStream, long startPosition, long endPosition) throws FileStorageException {
        doAnswer(answer -> {
            FileContentProcessor<?> fileContentProcessor = answer.getArgument(1);
            fileEntryInputStream.skip(startPosition);
            BoundedInputStream boundedInputStream = new BoundedInputStream.Builder().setInputStream(fileEntryInputStream)
                                                                                    .setCount(startPosition)
                                                                                    .setMaxCount(endPosition)
                                                                                    .setPropagateClose(true)
                                                                                    .get();
            return fileContentProcessor.process(boundedInputStream);
        }).when(fileService)
          .processFileContentWithOffset(any(), any());
        doAnswer(answer -> {
            FileContentConsumer fileContentConsumer = answer.getArgument(1);
            fileEntryInputStream.skip(startPosition);
            BoundedInputStream boundedInputStream = new BoundedInputStream.Builder().setInputStream(fileEntryInputStream)
                                                                                    .setCount(startPosition)
                                                                                    .setMaxCount(endPosition)
                                                                                    .setPropagateClose(true)
                                                                                    .get();
            fileContentConsumer.consume(boundedInputStream);
            return null;
        }).when(fileService)
          .consumeFileContentWithOffset(any(), any());
    }

}
