package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

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

    @ParameterizedTest
    @MethodSource
    public void testExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) throws Exception {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractor(mtar, fileName, maxFileUploadSize);
        appPath = extractor.extract();
        assertTrue(Files.exists(appPath));
    }

    @ParameterizedTest
    @MethodSource
    public void testFailingExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractor(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class, () -> appPath = extractor.extract());
        assertEquals(expectedException, exception.getCause()
            .getMessage());
    }

    @ParameterizedTest
    @MethodSource
    public void testBadAbsolutePathExtract(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveExtractor extractor = getApplicationArchiveExtractorForAbsolutePath(mtar, fileName, maxFileUploadSize);
        Exception exception = Assertions.assertThrows(Exception.class, () -> appPath = extractor.extract());
        assertEquals(expectedException, exception.getCause()
            .getMessage());
    }

    private ApplicationArchiveExtractor getApplicationArchiveExtractor(String mtar, String fileName, long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveExtractor(mtarInputStream, fileName, maxFileUploadSize, null) {
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
        return new ApplicationArchiveExtractor(mtarInputStream, fileName, maxFileUploadSize, null) {
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
}
