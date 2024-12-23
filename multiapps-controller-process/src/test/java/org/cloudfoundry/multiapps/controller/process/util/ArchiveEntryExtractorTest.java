package org.cloudfoundry.multiapps.controller.process.util;

import org.apache.commons.io.input.BoundedInputStream;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class ArchiveEntryExtractorTest {

    private static final ArchiveEntryWithStreamPositions DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                        .name(
                                                                                                                                            "META-INF/mtad.yaml")
                                                                                                                                        .startPosition(271)
                                                                                                                                        .endPosition(315)
                                                                                                                                        .compressionMethod(
                                                                                                                                            ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                                                                                                        .isDirectory(false)
                                                                                                                                        .build();

    private static final ArchiveEntryWithStreamPositions STORED_DEPLOYMENT_DESCRIPTOR_ENTRY = ImmutableArchiveEntryWithStreamPositions.builder()
                                                                                                                                      .name(
                                                                                                                                          "META-INF/mtad.yaml")
                                                                                                                                      .startPosition(271)
                                                                                                                                      .endPosition(320)
                                                                                                                                      .compressionMethod(
                                                                                                                                          ArchiveEntryWithStreamPositions.CompressionMethod.STORED)
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

    static Stream<Arguments> readFullDeploymentDescriptorFile() {
        return Stream.of(Arguments.of("stored-mta.mtar", STORED_DEPLOYMENT_DESCRIPTOR_ENTRY),
                         Arguments.of("deflated-mta.mtar", DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY));
    }

    @ParameterizedTest
    @MethodSource
    void readFullDeploymentDescriptorFile(String mtarFileName, ArchiveEntryWithStreamPositions deploymentDescriptorEntry)
        throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream(mtarFileName), deploymentDescriptorEntry.getStartPosition(),
                           deploymentDescriptorEntry.getEndPosition());
        byte[] bytesRead = archiveEntryExtractor.extractEntryBytes(buildFileEntryProperty(Integer.MAX_VALUE), deploymentDescriptorEntry);
        assertDeploymentDescriptorAreEqual(new String(bytesRead));
    }

    private FileEntryProperties buildFileEntryProperty(long maxFileSizeInBytes) {
        return ImmutableFileEntryProperties.builder()
                                           .guid(UUID.randomUUID()
                                                     .toString())
                                           .name("archive-entry")
                                           .maxFileSizeInBytes(maxFileSizeInBytes)
                                           .spaceGuid(UUID.randomUUID()
                                                          .toString())
                                           .build();
    }

    private void assertDeploymentDescriptorAreEqual(String actualDeploymentDescriptor) throws IOException {
        String expectedDeploymentDescriptor = new String(getClass().getResourceAsStream("expected-mtad.yaml")
                                                                   .readAllBytes()).replaceAll("\\r\\n", "\n");
        assertEquals(expectedDeploymentDescriptor, actualDeploymentDescriptor);
    }

    static Stream<Arguments> processFileEntryBytes() {
        return Stream.of(Arguments.of("stored-mta.mtar", STORED_DEPLOYMENT_DESCRIPTOR_ENTRY),
                         Arguments.of("deflated-mta.mtar", DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY));
    }

    @ParameterizedTest
    @MethodSource
    void processFileEntryBytes(String mtarFileName, ArchiveEntryWithStreamPositions deploymentDescriptorEntry) throws FileStorageException, IOException {
        prepareFileService(getClass().getResourceAsStream(mtarFileName), deploymentDescriptorEntry.getStartPosition(),
                           deploymentDescriptorEntry.getEndPosition());
        StringBuilder actualDeploymentDescriptorContent = new StringBuilder();
        archiveEntryExtractor.processFileEntryBytes(buildFileEntryProperty(Integer.MAX_VALUE), deploymentDescriptorEntry,
                                                    (byteBuffer, bytesRead) -> actualDeploymentDescriptorContent.append(
                                                        new String(Arrays.copyOfRange(byteBuffer, 0, bytesRead))));
        assertDeploymentDescriptorAreEqual(actualDeploymentDescriptorContent.toString());
    }

    static Stream<Arguments> processFileEntryContentWithExceedingSizeEntry() {
        return Stream.of(Arguments.of("stored-mta.mtar", STORED_DEPLOYMENT_DESCRIPTOR_ENTRY),
                         Arguments.of("deflated-mta.mtar", DEFLATED_DEPLOYMENT_DESCRIPTOR_ENTRY));
    }

    @ParameterizedTest
    @MethodSource
    void processFileEntryContentWithExceedingSizeEntry(String mtarFileName, ArchiveEntryWithStreamPositions deploymentDescriptorEntry)
        throws FileStorageException {
        prepareFileService(getClass().getResourceAsStream(mtarFileName), deploymentDescriptorEntry.getStartPosition(),
                           deploymentDescriptorEntry.getEndPosition());
        Exception exception = assertThrows(ContentException.class,
                                           () -> archiveEntryExtractor.processFileEntryBytes(buildFileEntryProperty(2), deploymentDescriptorEntry,
                                                                                             (byteBuffer, bytesRead) -> {
                                                                                             }));
        assertEquals("The size \"49\" of mta file \"archive-entry\" exceeds the configured max size limit \"2\"", exception.getMessage());
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
