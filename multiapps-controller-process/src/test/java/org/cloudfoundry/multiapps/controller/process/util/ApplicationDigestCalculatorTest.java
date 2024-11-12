package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.io.InputStream;
import java.util.List;
import java.util.function.ObjIntConsumer;

import org.apache.commons.io.input.BoundedInputStream;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.persistence.services.FileContentConsumer;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.mta.util.EntryToInflate;
import org.cloudfoundry.multiapps.mta.util.InflatorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApplicationDigestCalculatorTest {

    private static final String SPACE_GUID = "123";
    private static final String APP_ARCHIVE_ID = "132";
    private static final String DB_DIRECTORY_MODULE_DIGEST = "71017C6429E2E1FA4ED2AD97ABF321A0";
    private static final String WEB_SERVER_MODULE_DIGEST = "4C64A36CDC073B5D07947005F630DACC";

    @Mock
    private FileService fileService;
    @Mock
    private ArchiveEntryExtractor archiveEntryExtractor;

    private ApplicationDigestCalculator applicationDigestCalculator;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        doAnswer(answer -> {
            FileContentConsumer fileContentConsumer = answer.getArgument(2);
            fileContentConsumer.consume(getClass().getResourceAsStream("com.sap.mta.sample-1.2.1-beta.mtar"));
            return null;
        }).when(fileService)
          .consumeFileContent(any(), any(), any());
        doAnswer(answer -> {
            FileEntryProperties fileEntryProperties = answer.getArgument(0);
            ArchiveEntryWithStreamPositions archiveEntryWithStreamPositions = answer.getArgument(1);
            ObjIntConsumer<byte[]> consumer = answer.getArgument(2);
            InputStream resourceAsStream = getClass().getResourceAsStream("com.sap.mta.sample-1.2.1-beta.mtar");
            resourceAsStream.skip(archiveEntryWithStreamPositions.getStartPosition());
            BoundedInputStream boundedInputStream = new BoundedInputStream.Builder().setInputStream(resourceAsStream)
                                                                                    .setCount(archiveEntryWithStreamPositions.getStartPosition())
                                                                                    .setMaxCount(archiveEntryWithStreamPositions.getEndPosition())
                                                                                    .get();
            InflatorUtil.inflate(new EntryToInflate("web/web-server.zip", fileEntryProperties.getMaxFileSizeInBytes(), boundedInputStream),
                                 consumer);
            return null;
        }).when(archiveEntryExtractor)
          .processFileEntryContent(any(), any(), any());
        applicationDigestCalculator = new ApplicationDigestCalculator(fileService, new ApplicationArchiveIterator(), archiveEntryExtractor);
    }

    @Test
    void testDigestCalculationWhenModuleIsDirectory() {
        ArchiveEntryWithStreamPositions directoryModuleEntry = buildDbModule();
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = List.of(directoryModuleEntry);
        ApplicationArchiveContext applicationArchiveContext = new ApplicationArchiveContext("db/",
                                                                                            Integer.MAX_VALUE,
                                                                                            archiveEntriesWithStreamPositions,
                                                                                            SPACE_GUID,
                                                                                            APP_ARCHIVE_ID);
        String appDigest = applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext);
        assertEquals(DB_DIRECTORY_MODULE_DIGEST, appDigest);
    }

    private ArchiveEntryWithStreamPositions buildDbModule() {
        return ImmutableArchiveEntryWithStreamPositions.builder()
                                                       .name("db/")
                                                       .startPosition(0)
                                                       .endPosition(0)
                                                       .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                       .isDirectory(true)
                                                       .build();
    }

    @Test
    void testDigestCalculationWhenModuleIsDirectoryAndExceedsMaxSizeLimit() {
        ArchiveEntryWithStreamPositions directoryModuleEntry = buildDbModule();
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = List.of(directoryModuleEntry);
        ApplicationArchiveContext applicationArchiveContext = new ApplicationArchiveContext("db/",
                                                                                            1,
                                                                                            archiveEntriesWithStreamPositions,
                                                                                            SPACE_GUID,
                                                                                            APP_ARCHIVE_ID);
        Exception exception = assertThrows(Exception.class,
                                           () -> applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext));
        assertEquals("The size \"201\" of mta file \"db/\" exceeds the configured max size limit \"1\"", exception.getMessage());
    }

    @Test
    void testDigestCalculationWhenModuleIsZip() {
        ArchiveEntryWithStreamPositions zipModuleEntry = buildWebServerModule();
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = List.of(zipModuleEntry);
        ApplicationArchiveContext applicationArchiveContext = new ApplicationArchiveContext("web/web-server.zip",
                                                                                            Integer.MAX_VALUE,
                                                                                            archiveEntriesWithStreamPositions,
                                                                                            SPACE_GUID,
                                                                                            APP_ARCHIVE_ID);
        String appDigest = applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext);
        assertEquals(WEB_SERVER_MODULE_DIGEST, appDigest);
    }

    private ArchiveEntryWithStreamPositions buildWebServerModule() {
        return ImmutableArchiveEntryWithStreamPositions.builder()
                                                       .name("web/web-server.zip")
                                                       .startPosition(531)
                                                       .endPosition(678)
                                                       .compressionMethod(ArchiveEntryWithStreamPositions.CompressionMethod.DEFLATED)
                                                       .isDirectory(false)
                                                       .build();
    }

    @Test
    void testDigestCalculationWhenModuleIsZipAndExceedsMaxSizeLimit() {
        ArchiveEntryWithStreamPositions zipModuleEntry = buildWebServerModule();
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = List.of(zipModuleEntry);
        ApplicationArchiveContext applicationArchiveContext = new ApplicationArchiveContext("web/web-server.zip",
                                                                                            1,
                                                                                            archiveEntriesWithStreamPositions,
                                                                                            SPACE_GUID,
                                                                                            APP_ARCHIVE_ID);
        Exception exception = assertThrows(Exception.class,
                                           () -> applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext));
        assertEquals("The size \"203\" of mta file \"web/web-server.zip\" exceeds the configured max size limit \"1\"",
                     exception.getMessage());
    }

    @Test
    void testDigestCalculationWhenExceptionIsThrown() {
        ArchiveEntryWithStreamPositions zipModuleEntry = buildWebServerModule();
        List<ArchiveEntryWithStreamPositions> archiveEntriesWithStreamPositions = List.of(zipModuleEntry);
        ApplicationArchiveContext applicationArchiveContext = new ApplicationArchiveContext("web/web-server.zip",
                                                                                            1,
                                                                                            archiveEntriesWithStreamPositions,
                                                                                            SPACE_GUID,
                                                                                            APP_ARCHIVE_ID);
        doThrow(new SLException("Cannot calculate digest")).when(archiveEntryExtractor)
                                                           .processFileEntryContent(any(), any(), any());
        Exception exception = assertThrows(Exception.class,
                                           () -> applicationDigestCalculator.calculateApplicationDigest(applicationArchiveContext));
        assertEquals("Cannot calculate digest", exception.getMessage());
    }

}
