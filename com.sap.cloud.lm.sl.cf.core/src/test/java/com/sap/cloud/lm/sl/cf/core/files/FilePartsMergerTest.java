package com.sap.cloud.lm.sl.cf.core.files;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.common.SLException;

public class FilePartsMergerTest {

    private static final String TEST_FILE_NAME = "test";

    @Test
    public void mergeFileTestLength() throws IOException {
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
    public void mergeFileCleanAndCheckIfFileExists() throws IOException {
        try (FilePartsMerger filePartsMerger = new FilePartsMerger("file")) {
            InputStream testInputStream = getClass().getResourceAsStream(TEST_FILE_NAME);
            filePartsMerger.merge(testInputStream);
            filePartsMerger.cleanUp();
            Assertions.assertFalse(filePartsMerger.getMergedFilePath()
                                                  .toFile()
                                                  .exists());
        }
    }

    @Test
    public void testWithInvalidFile() {
        Exception exception = Assertions.assertThrows(SLException.class, () -> new FilePartsMerger("/some/invalid/file"));
        Assertions.assertTrue(exception.getCause()
                                       .toString()
                                       .contains("java.nio.file.NoSuchFileException"));
    }

}
