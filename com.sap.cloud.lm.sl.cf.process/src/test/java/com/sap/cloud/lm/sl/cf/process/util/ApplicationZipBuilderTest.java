package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
            Arguments.of(SAMPLE_MTAR, "db/"),
            Arguments.of(SAMPLE_MTAR, "web/"),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip"),
            Arguments.of(SAMPLE_FLAT_MTAR, "db/"),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/"));
        // @formatter:on
    }

    public static Stream<Arguments> testCreateZipOnlyWithMissingResources() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "db/", Stream.of("db/readme.txt").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/", Stream.of("web/xs-app.json", "web/readme.txt", "web/local-destinations.json", "web/resources/index.html").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_MTAR, "db/pricing-db.zip", Stream.of("db/pricing-db.zip").collect(Collectors.toSet())));
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
        ApplicationArchiveReader reader = getApplicationArchiveReader(mtar, fileName);
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader, fileName, logger, Collections.emptySet());
        appPath = zipBuilder.extractApplicationInNewArchive();
        assertTrue(Files.exists(appPath));
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertFalse(zipEntriesName.isEmpty());
        }
    }

    private ApplicationArchiveReader getApplicationArchiveReader(String mtar, String fileName) {
        return new ApplicationArchiveReader(getClass().getResourceAsStream(mtar), fileName, MAX_UPLOAD_FILE_SIZE);
    }

    @ParameterizedTest
    @MethodSource
    public void testCreateZipOnlyWithMissingResources(String mtar, String fileName, Set<String> alreadyUploadedFiles) throws IOException {
        ApplicationArchiveReader reader = getApplicationArchiveReader(mtar, fileName);
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader, fileName, logger, alreadyUploadedFiles);
        appPath = zipBuilder.extractApplicationInNewArchive();
        assertTrue(Files.exists(appPath));
        Set<String> relativizedFilePaths = relativizeUploadedFilesPaths(zipBuilder, alreadyUploadedFiles);
        try (InputStream zipStream = Files.newInputStream(appPath)) {
            Set<String> zipEntriesName = getZipEntriesName(zipStream);
            assertTrue(Collections.disjoint(relativizedFilePaths, zipEntriesName));
        }
    }

    private Set<String> relativizeUploadedFilesPaths(ApplicationZipBuilder zipBuilder, Set<String> alreadyUploadedFiles) {
        Set<String> relativizedFilePaths = new HashSet<>();
        alreadyUploadedFiles.stream()
            .forEach(filePath -> {
                relativizedFilePaths.add(zipBuilder.getRelativePathOfZipEntry(filePath));
            });
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
        String mtar = SAMPLE_MTAR;
        String fileName = "db/";
        ApplicationArchiveReader reader = getApplicationArchiveReader(mtar, fileName);
        ApplicationZipBuilder zipBuilder = new ApplicationZipBuilder(reader, fileName, logger, Collections.emptySet()) {
            @Override
            protected void copy(InputStream input, OutputStream output) throws IOException {
                throw new IOException();
            }

        };
        Assertions.assertThrows(SLException.class, () -> appPath = zipBuilder.extractApplicationInNewArchive());
    }

}
