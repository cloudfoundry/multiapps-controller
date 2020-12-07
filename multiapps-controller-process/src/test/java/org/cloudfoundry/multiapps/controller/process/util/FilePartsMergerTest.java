package org.cloudfoundry.multiapps.controller.process.util;

import java.io.IOException;
import java.io.InputStream;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FilePartsMergerTest {

    private static final String TEST_FILE_NAME = "test";

    @Test
    void mergeFileTestLength() throws IOException {
        try (FilePartsMerger filePartsMerger = new FilePartsMerger("file")) {
            InputStream testInputStream = getClass().getResourceAsStream(TEST_FILE_NAME);
            filePartsMerger.merge(testInputStream);
            Assertions.assertTrue(filePartsMerger.getMergedFilePath()
                                                 .toFile()
                                                 .exists());
            Assertions.assertEquals(getClass().getResourceAsStream(TEST_FILE_NAME)
                                              .available(),
                                    filePartsMerger.getMergedFilePath()
                                                   .toFile()
                                                   .length());
            filePartsMerger.cleanUp();
        }
    }

    @Test
    void mergeFileCleanAndCheckIfFileExists() throws IOException {
        try (FilePartsMerger filePartsMerger = new FilePartsMerger("file")) {
            InputStream testInputStream = getClass().getResourceAsStream(TEST_FILE_NAME);
            filePartsMerger.merge(testInputStream);
            filePartsMerger.cleanUp();
            Assertions.assertFalse(filePartsMerger.getMergedFilePath()
                                                  .toFile()
                                                  .getAbsoluteFile()
                                                  .exists());
        }
    }

    @Test
    void testWithInvalidFile() {
        Exception exception = Assertions.assertThrows(SLException.class, () -> new FilePartsMerger("/some/invalid/file"));
        Assertions.assertTrue(exception.getCause()
                                       .toString()
                                       .contains("java.nio.file.NoSuchFileException"));
    }

}
