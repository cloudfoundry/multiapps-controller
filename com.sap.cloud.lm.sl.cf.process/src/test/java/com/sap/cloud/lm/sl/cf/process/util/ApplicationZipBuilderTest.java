package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;
import com.sap.cloud.lm.sl.common.SLException;

public class ApplicationZipBuilderTest {
    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024l; // 1gb

    @Mock
    private StepLogger logger;

    private Path appPath = null;

    public static Stream<Arguments> testCreateNewZip() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "db"),
            Arguments.of(SAMPLE_MTAR, "web"),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip"),
            Arguments.of(SAMPLE_FLAT_MTAR, "db"),
            Arguments.of(SAMPLE_FLAT_MTAR, "web"));
        // @formatter:on
    }

    public static Stream<Arguments> testCreateZipOnlyWithMissingResources() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "db/", Stream.of("readme.txt").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_FLAT_MTAR, "web", Stream.of("xs-app.json", "readme.txt", "local-destinations.json", "resources/index.html").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_MTAR, "db/pricing-db.zip", Stream.of("pricing-db.zip").collect(Collectors.toSet())));
        // @formatter:on
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (appPath != null) {
            Files.deleteIfExists(appPath);
        }
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateNewZip(String mtar, String fileName) throws Exception {
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName);
        ApplicationArchiveReader reader = new ApplicationArchiveReader();
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader);
        appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext, logger);
        assertTrue(Files.exists(appPath));
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertFalse(zipEntriesName.isEmpty());
        }
    }

    private ApplicationArchiveContext getApplicationArchiveContext(String mtar, String fileName) {
        return new ApplicationArchiveContext(getClass().getResourceAsStream(mtar), fileName, MAX_UPLOAD_FILE_SIZE);
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateZipOnlyWithMissingResources(String mtar, String fileName, Set<String> alreadyUploadedFiles) throws IOException {
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName);
        ApplicationArchiveReader reader = new ApplicationArchiveReader();
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader);
        appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext, logger);
        assertTrue(Files.exists(appPath));
        Set<String> relativizedFilePaths = relativizeUploadedFilesPaths(zipBuilder, fileName, alreadyUploadedFiles);
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertTrue(Collections.disjoint(relativizedFilePaths, zipEntriesName),
                       MessageFormat.format("Expected resources:{0} but was:{1}", relativizedFilePaths, zipEntriesName));
        }
    }

    private Set<String> relativizeUploadedFilesPaths(ApplicationZipBuilder zipBuilder, String fileName, Set<String> alreadyUploadedFiles) {
        Set<String> relativizedFilePaths = new HashSet<>();
        alreadyUploadedFiles.stream()
                            .forEach(filePath -> relativizedFilePaths.add(FileUtils.getRelativePath(fileName, filePath)));
        return relativizedFilePaths;
    }

    private Set<String> getZipEntriesName(InputStream inputStream) throws IOException {
        Set<String> zipEntriesName = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null;) {
                if (!FileUtils.isDirectory(zipEntry.getName())) {
                    zipEntriesName.add(zipEntry.getName());
                }
            }
        }
        return zipEntriesName;
    }

    @Test
    public void testFailToCreateZip() {
        String fileName = "db/";
        ApplicationArchiveReader reader = new ApplicationArchiveReader();
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader) {
            @Override
            protected void copy(InputStream input, OutputStream output, ApplicationArchiveContext applicationArchiveContext)
                throws IOException {
                throw new IOException();
            }

        };
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(SAMPLE_MTAR, fileName);
        Assertions.assertThrows(SLException.class,
                                () -> appPath = zipBuilder.extractApplicationInNewArchive(applicationArchiveContext, logger));
    }

}
