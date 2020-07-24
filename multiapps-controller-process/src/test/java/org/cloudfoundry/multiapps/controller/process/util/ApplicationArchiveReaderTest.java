package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ApplicationArchiveReaderTest {

    private static final String ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT = "The size of the application exceeds max size limit \"{0}\"";
    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024L; // 1gb

    public static Stream<Arguments> testFailingCalculateDigest() {
        // @formatter:off
        return Stream.of(
            Arguments.of(SAMPLE_FLAT_MTAR, "xxx/", MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.CANNOT_FIND_ARCHIVE_ENTRY, "xxx/"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/", MessageFormat.format(FileUtils.PATH_SHOULD_BE_NORMALIZED, "web/../asd"), MAX_UPLOAD_FILE_SIZE),
            Arguments.of(SAMPLE_MTAR, "db/", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200L),
            Arguments.of(SAMPLE_MTAR, "web/web-server.zip", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testFailingCalculateDigest(String mtar, String fileName, String expectedException, long maxFileUploadSize) {
        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName, maxFileUploadSize);
        ApplicationArchiveReader reader = getApplicationArchiveReader();
        Exception exception = Assertions.assertThrows(Exception.class, () -> reader.calculateApplicationDigest(applicationArchiveContext));
        assertEquals(expectedException, exception.getMessage());
    }

    @Test
    public void testBadAbsolutePathRead() {
        String mtar = SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH;
        String fileName = "/web/";
        String expectedException = MessageFormat.format(FileUtils.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/");
        long maxFileUploadSize = MAX_UPLOAD_FILE_SIZE;

        ApplicationArchiveContext applicationArchiveContext = getApplicationArchiveContext(mtar, fileName, maxFileUploadSize);
        ApplicationArchiveReader reader = getApplicationArchiveReaderForAbsolutePath();
        Exception exception = Assertions.assertThrows(Exception.class, () -> reader.calculateApplicationDigest(applicationArchiveContext));
        assertEquals(expectedException, exception.getMessage());
    }

    private ApplicationArchiveContext getApplicationArchiveContext(String mtar, String fileName, long maxFileUploadSize) {
        InputStream mtarInputStream = getClass().getResourceAsStream(mtar);
        return new ApplicationArchiveContext(mtarInputStream, fileName, maxFileUploadSize);
    }

    private ApplicationArchiveReader getApplicationArchiveReader() {
        return new ApplicationArchiveReader();
    }

    private ApplicationArchiveReader getApplicationArchiveReaderForAbsolutePath() {
        return new ApplicationArchiveReader() {
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
