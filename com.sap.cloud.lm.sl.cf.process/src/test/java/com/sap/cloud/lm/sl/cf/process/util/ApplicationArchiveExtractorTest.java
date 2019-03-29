package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.domain.CloudResources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;

public class ApplicationArchiveExtractorTest {

    private static final String ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT = "The size of the application exceeds max size limit \"{0}\"";
    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024l; // 1gb

    private Path appPath = null;

    public static Stream<Arguments> testExtract() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip", null, MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/", null, MAX_UPLOAD_FILE_SIZE));
        // @formatter:on
    }

    public static Stream<Arguments> testFailingExtract() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_FLAT_MTAR, "xxx/", MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, "xxx/"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/", MessageFormat.format(FileUtils.PATH_SHOULD_BE_NORMALIZED, "web/../asd"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR, "db/", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200l),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200));
        // @formatter:on
    }

    public static Stream<Arguments> testBadAbsolutePathExtract() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH, "/web/", MessageFormat.format(FileUtils.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/"), MAX_UPLOAD_FILE_SIZE));
        // @formatter:on
    }

    public static Stream<Arguments> testGetApplicationMetadata() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "db/"),
            Arguments.of(SAMPLE_MTAR, "web/"),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip"),
            Arguments.of(SAMPLE_FLAT_MTAR, "db/"),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/"));
        // @formatter:on
    }

    public static Stream<Arguments> testExtractNotCachedResources() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "db/", Stream.of("db/readme.txt").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/", Stream.of("web/xs-app.json", "web/readme.txt", "web/local-destinations.json", "web/resources/index.html").collect(Collectors.toSet())),
            Arguments.of(SAMPLE_MTAR, "db/pricing-db.zip", Stream.of("db/pricing-db.zip").collect(Collectors.toSet())));
        // @formatter:on
    }

    @AfterEach
    public void tearDown() throws IOException {
        if (appPath != null) {
            Files.deleteIfExists(appPath);
        }
    }

    @ParameterizedTest
    @MethodSource
    public void testExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) throws Exception {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractor(mtar, fileName, maxFileUploadSize);
        appPath = extractor.extractApplicationInNewArchive();
        assertTrue(Files.exists(appPath));
    }

    @ParameterizedTest
    @MethodSource
    public void testFailingExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractor(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class, () -> appPath = extractor.extractApplicationInNewArchive());
        assertEquals(expectedException, exception.getCause()
            .getMessage());
    }

    @ParameterizedTest
    @MethodSource
    public void testBadAbsolutePathExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractorForAbsolutePath(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class, () -> appPath = extractor.extractApplicationInNewArchive());
        assertEquals(expectedException, exception.getCause()
            .getMessage());
    }

    @ParameterizedTest
    @MethodSource
    public void testGetApplicationMetadata(String mtar, String fileName) throws IOException {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractor(mtar, fileName, MAX_UPLOAD_FILE_SIZE);
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        Set<String> zipEntriesName = getZipEntriesName(mtarInputStream, fileName);
        Collection<CloudResource> cloudResourceCollection = extractor.getApplicationMetaData();
        assertEquals(zipEntriesName.size(), cloudResourceCollection.size());
        assertTrue(zipEntriesName.containsAll(new CloudResources(cloudResourceCollection).getFilenames()));
    }

    @ParameterizedTest
    @MethodSource
    public void testExtractNotCachedResources(String mtar, String fileName, Set<String> knownFileNames) throws IOException {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractorForKnownMetadata(mtar, fileName, MAX_UPLOAD_FILE_SIZE,
            knownFileNames);
        appPath = extractor.extractApplicationInNewArchive();
        try (InputStream fileInputStream = Files.newInputStream(appPath)) {
            Set<String> zipEntries = getZipEntriesName(fileInputStream, "");
            assertTrue(Collections.disjoint(zipEntries, getRelativePathsOfKnownFileNames(fileName, knownFileNames)));
        }
    }

    private Set<String> getRelativePathsOfKnownFileNames(String fileName, Set<String> knownFileNames) {
        Set<String> relativePathsOfKnownFileNames = new HashSet<>();
        knownFileNames.stream()
            .forEach(knownFileName -> {
                relativePathsOfKnownFileNames.add(Paths.get(fileName)
                    .relativize(Paths.get(knownFileName))
                    .toString());
            });
        return relativePathsOfKnownFileNames;
    }

    private Set<String> getZipEntriesName(InputStream inputStream, String fileName) throws IOException {
        Set<String> zipEntriesName = new HashSet<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null;) {
                if (zipEntry.getName()
                    .startsWith(fileName) && !FileUtils.isDirectory(zipEntry.getName())) {
                    zipEntriesName.add(zipEntry.getName());
                }
            }
        }
        return zipEntriesName;
    }

    private ApplicationArchiveExtractor getApplicationArchiveExtractor(String mtar, String fileName, long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveExtractor(mtarInputStream, fileName, maxFileUploadSize, Collections.emptySet(), null) {
            @Override
            protected void cleanUp(Path appPath) {
                if (appPath == null || !Files.exists(appPath)) {
                    return;
                }
                org.apache.commons.io.FileUtils.deleteQuietly(appPath.toFile());
            }
        };
    }

    private ApplicationArchiveExtractor getApplicationArchiveExtractorForAbsolutePath(String mtar, String fileName,
        long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveExtractor(mtarInputStream, fileName, maxFileUploadSize, Collections.emptySet(), null) {
            @Override
            protected void cleanUp(Path appPath) {
                if (appPath == null || !Files.exists(appPath)) {
                    return;
                }
                org.apache.commons.io.FileUtils.deleteQuietly(appPath.toFile());
            }

            @Override
            protected void validateEntry(ZipEntry entry) {
                String path = entry.getName();
                if (!path.equals(FilenameUtils.normalize(path, true))) {
                    throw new IllegalArgumentException(MessageFormat.format(FileUtils.PATH_SHOULD_BE_NORMALIZED, path));
                }
                if (FilenameUtils.getPrefixLength(path) != 0 || Paths.get(path)
                    .isAbsolute()) {
                    throw new IllegalArgumentException(MessageFormat.format(FileUtils.PATH_SHOULD_NOT_BE_ABSOLUTE, path));
                }
            }
        };
    }

    private ApplicationArchiveExtractor getApplicationArchiveExtractorForKnownMetadata(String mtar, String fileName, long maxFileUploadSize,
        Set<String> knownFileNames) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveExtractor(mtarInputStream, fileName, maxFileUploadSize, knownFileNames, null) {
            @Override
            protected void cleanUp(Path appPath) {
                if (appPath == null || !Files.exists(appPath)) {
                    return;
                }
                org.apache.commons.io.FileUtils.deleteQuietly(appPath.toFile());
            }
        };
    }
}
