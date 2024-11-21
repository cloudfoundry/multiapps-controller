package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.input.BoundedInputStream;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentProcessor;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentToProcess;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApplicationZipBuilderTest {

    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024L; // 1gb

    @Mock
    private FileService fileService;

    private Path appPath = null;

    static Stream<Arguments> testCreateNewZip() {
        return Stream.of(Arguments.of(SAMPLE_MTAR, "db"), Arguments.of(SAMPLE_MTAR, "web"), Arguments.of(SAMPLE_MTAR, "web/web-server.zip"),
                         Arguments.of(SAMPLE_FLAT_MTAR, "db"), Arguments.of(SAMPLE_FLAT_MTAR, "web"));
    }

    static Stream<Arguments> testCreateZipOnlyWithMissingResources() {
        return Stream.of(Arguments.of(SAMPLE_MTAR, "db/", Stream.of("readme.txt")
                                                                .collect(Collectors.toSet())),
                         Arguments.of(SAMPLE_FLAT_MTAR, "web",
                                      Stream.of("xs-app.json", "readme.txt", "local-destinations.json", "resources/index.html")
                                            .collect(Collectors.toSet())),
                         Arguments.of(SAMPLE_MTAR, "db/pricing-db.zip", Stream.of("pricing-db.zip")
                                                                              .collect(Collectors.toSet())));
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (appPath != null) {
            Files.deleteIfExists(appPath);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testCreateNewZip(String mtar, String fileName) throws Exception {
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName);
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(fileService,
                                                                     new ApplicationArchiveIterator(),
                                                                     new ArchiveEntryExtractor(fileService));
        appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext);
        assertTrue(Files.exists(appPath));
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertFalse(zipEntriesName.isEmpty());
        }
    }

    private ApplicationArchiveContext getApplicationArchiveContext(String mtar, String fileName) throws FileStorageException {
        mockProcessingOfFileContent(mtar);
        mockConsumptionOfFileContent(mtar);
        mockConsumptionOfFileContentWithOffset(mtar);
        ArchiveEntryStreamWithStreamPositionsDeterminer archiveEntryStreamWithStreamPositionsDeterminer = new ArchiveEntryStreamWithStreamPositionsDeterminer(fileService);
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = archiveEntryStreamWithStreamPositionsDeterminer.determineArchiveEntries("123",
                                                                                                                                                          "123");
        return new ApplicationArchiveContext(fileName, MAX_UPLOAD_FILE_SIZE, archiveEntriesWithStreamPositions, "123", "123");
    }

    private void mockProcessingOfFileContent(String mtar) throws FileStorageException {
        doAnswer(answer -> {
            try (InputStream inputStream = getClass().getResourceAsStream(mtar)) {
                FileContentProcessor<?> fileContentProcessor = answer.getArgument(2);
                return fileContentProcessor.process(inputStream);
            } catch (IOException e) {
                throw new SLException(e, e.getMessage());
            }
        }).when(fileService)
          .processFileContent(any(), any(), any());
    }

    private void mockConsumptionOfFileContent(String mtar) throws FileStorageException {
        doAnswer(answer -> {
            try (InputStream inputStream = getClass().getResourceAsStream(mtar)) {
                FileContentConsumer fileContentConsumer = answer.getArgument(2);
                fileContentConsumer.consume(inputStream);
                return null;
            } catch (IOException e) {
                throw new SLException(e, e.getMessage());
            }
        }).when(fileService)
          .consumeFileContent(any(), any(), any());
    }

    private void mockConsumptionOfFileContentWithOffset(String mtar) throws FileStorageException {
        doAnswer(answer -> {
            try (InputStream inputStream = getClass().getResourceAsStream(mtar)) {
                FileContentToProcess fileContentToProcess = answer.getArgument(0);
                inputStream.skip(fileContentToProcess.getStartOffset());
                BoundedInputStream boundedInputStream = new BoundedInputStream.Builder().setInputStream(inputStream)
                                                                                        .setCount(fileContentToProcess.getStartOffset())
                                                                                        .setMaxCount(fileContentToProcess.getEndOffset())
                                                                                        .get();
                FileContentConsumer fileContentConsumer = answer.getArgument(1);
                fileContentConsumer.consume(boundedInputStream);
                return null;
            } catch (IOException e) {
                throw new SLException(e, e.getMessage());
            }
        }).when(fileService)
          .consumeFileContentWithOffset(any(), any());
    }

    @ParameterizedTest
    @MethodSource
    void testCreateZipOnlyWithMissingResources(String mtar, String fileName, Set<String> alreadyUploadedFiles)
        throws IOException, FileStorageException {
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName);
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(fileService,
                                                                     new ApplicationArchiveIterator(),
                                                                     new ArchiveEntryExtractor(fileService));
        appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext);
        assertTrue(Files.exists(appPath));
        Set<String> relativizedFilePaths = relativizeUploadedFilesPaths(fileName, alreadyUploadedFiles);
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertTrue(Collections.disjoint(relativizedFilePaths, zipEntriesName),
                       MessageFormat.format("Expected resources:{0} but was:{1}", relativizedFilePaths, zipEntriesName));
        }
    }

    private Set<String> relativizeUploadedFilesPaths(String fileName, Set<String> alreadyUploadedFiles) {
        return alreadyUploadedFiles.stream()
                                   .map(filePath -> FileUtils.getRelativePath(fileName, filePath))
                                   .collect(Collectors.toSet());
    }

    private Set<String> getZipEntriesName(InputStream inputStream) throws IOException {
        Set<String> zipEntriesName = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null;) {
                if (!zipEntry.isDirectory()) {
                    zipEntriesName.add(zipEntry.getName());
                }
            }
        }
        return zipEntriesName;
    }

    @Test
    void testFailToCreateZip() throws FileStorageException {
        String fileName = "db/";
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(fileService,
                                                                     new ApplicationArchiveIterator(),
                                                                     new ArchiveEntryExtractor(fileService)) {
            @Override
            protected void copy(InputStream input, OutputStream output, ApplicationArchiveContext applicationArchiveContext)
                throws IOException {
                throw new IOException();
            }
        };
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(SAMPLE_MTAR, fileName);
        Assertions.assertThrows(SLException.class, () -> appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext));
    }

}
