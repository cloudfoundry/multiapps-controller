package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.util.FileUtils;

public class ApplicationArchiveReaderTest {

    private static final String ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT = "The size of the application exceeds max size limit \"{0}\"";
    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024l; // 1gb

    public static Stream<Arguments> testReadResources() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip", MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_FLAT_MTAR, "web/", MAX_UPLOAD_FILE_SIZE));
        // @formatter:on
    }

    public static Stream<Arguments> testFailingReadResources() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_FLAT_MTAR, "xxx/", MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, "xxx/"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/", MessageFormat.format(FileUtils.PATH_SHOULD_BE_NORMALIZED, "web/../asd"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR, "db/", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200l),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200));
        // @formatter:on
    }

    public static Stream<Arguments> testBadAbsolutePathRead() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH, "/web/", MessageFormat.format(FileUtils.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/"), MAX_UPLOAD_FILE_SIZE));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testReadResources(String mtar, String fileName, long maxFileUploadSize) throws Exception {
        ApplicationArchiveReader reader = getApplicationArchiveReader(mtar, fileName, maxFileUploadSize);
        ApplicationResources applicationResources = new ApplicationResources();
        reader.initializeApplicationResources(applicationResources);
        assertFalse(applicationResources.getCloudResourceList()
            .isEmpty());
        assertNotNull(applicationResources.getApplicationDigest());
        try (InputStream mtarStream = getClass().getResourceAsStream(mtar)) {
            Set<String> zipEntriesName = getZipEntriesName(mtarStream, fileName);
            Set<String> applicationResourcesNames = applicationResources.toCloudResources()
                .getFileNames();
            assertTrue(zipEntriesName.containsAll(applicationResourcesNames));
        }
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

    @ParameterizedTest
    @MethodSource
    public void testFailingReadResources(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveReader reader = getApplicationArchiveReader(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class,
            () -> reader.initializeApplicationResources(new ApplicationResources()));
        assertEquals(expectedException, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource
    public void testBadAbsolutePathRead(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveReader reader = getApplicationArchiveReaderForAbsolutePath(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class,
            () -> reader.initializeApplicationResources(new ApplicationResources()));
        assertEquals(expectedException, exception.getMessage());
    }

    private ApplicationArchiveReader getApplicationArchiveReader(String mtar, String fileName, long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveReader(mtarInputStream, fileName, maxFileUploadSize);
    }

    private ApplicationArchiveReader getApplicationArchiveReaderForAbsolutePath(String mtar, String fileName, long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveReader(mtarInputStream, fileName, maxFileUploadSize) {
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

}
