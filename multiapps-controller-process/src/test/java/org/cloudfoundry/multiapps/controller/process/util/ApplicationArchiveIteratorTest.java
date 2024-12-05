package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.cloudfoundry.multiapps.controller.core.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;

class ApplicationArchiveIteratorTest {

    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    static Stream<Arguments> testFailingCalculateDigest() {
        return Stream.of(Arguments.of(SAMPLE_FLAT_MTAR, "xxx/",
                                      MessageFormat.format(org.cloudfoundry.multiapps.mta.Messages.CANNOT_FIND_ARCHIVE_ENTRY, "xxx/")),
                         Arguments.of(SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/../asd",
                                      MessageFormat.format(FileUtils.PATH_SHOULD_BE_NORMALIZED, "web/../asd")));
    }

    @ParameterizedTest
    @MethodSource
    void testFailingCalculateDigest(String mtar, String fileName, String expectedException) {
        ApplicationArchiveIterator applicationArchiveIterator = new ApplicationArchiveIterator();
        try (InputStream inputStream = getClass().getResourceAsStream(mtar);
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(inputStream)) {
            Exception exception = assertThrows(Exception.class,
                                               () -> applicationArchiveIterator.getFirstZipEntry(fileName, zipArchiveInputStream));
            assertEquals(expectedException, exception.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testBadAbsolutePathRead() {
        String mtar = SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH;
        String fileName = "/web/";
        String expectedException = MessageFormat.format(FileUtils.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/");
        ApplicationArchiveIterator applicationArchiveIterator = getApplicationArchiveReaderForAbsolutePath();
        try (InputStream inputStream = getClass().getResourceAsStream(mtar);
            ZipArchiveInputStream zipArchiveInputStream = new ZipArchiveInputStream(inputStream)) {
            Exception exception = Assertions.assertThrows(Exception.class,
                                                          () -> applicationArchiveIterator.getFirstZipEntry(fileName,
                                                                                                            zipArchiveInputStream));
            assertEquals(expectedException, exception.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ApplicationArchiveIterator getApplicationArchiveReaderForAbsolutePath() {
        return new ApplicationArchiveIterator() {
            @Override
            protected void validateEntry(ZipArchiveEntry entry) {
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
