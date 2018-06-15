package com.sap.cloud.lm.sl.cf.process.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(Enclosed.class)
public class ApplicationArchiveExtractorTest {

    @RunWith(Parameterized.class)
    public static class ExtractTest {

        private static final String ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT = "The size of the application exceeds max size limit \"{0}\"";
        private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
        private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
        private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
        private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";
        private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024l; // 1gb

        @Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                { SAMPLE_MTAR, "web/web-server.zip", null, MAX_UPLOAD_FILE_SIZE},
                { SAMPLE_FLAT_MTAR, "web/", null, MAX_UPLOAD_FILE_SIZE},
                { SAMPLE_FLAT_MTAR, "web/", MessageFormat.format(com.sap.cloud.lm.sl.mta.message.Messages.CANNOT_FIND_ARCHIVE_ENTRY, "xxx/web/"), MAX_UPLOAD_FILE_SIZE},
                { SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH, "/web/", MessageFormat.format(ApplicationArchiveExtractor.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/asd"), MAX_UPLOAD_FILE_SIZE},
                { SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/", MessageFormat.format(ApplicationArchiveExtractor.PATH_SHOULD_BE_NORMALIZED, "web/../asd"), MAX_UPLOAD_FILE_SIZE},
                { SAMPLE_MTAR, "db/", MessageFormat.format(ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 200), 200l}
                // @formatter:on
            });
        }

        private final String mtar;
        private final String fileName;
        private final String exceptionMessage;
        private final long maxFileUploadSize;

        public ExtractTest(String mtar, String fileName, String exceptionMessage, long maxFileUploadSize) {
            this.mtar = mtar;
            this.fileName = fileName;
            this.exceptionMessage = exceptionMessage;
            this.maxFileUploadSize = maxFileUploadSize;
        }

        @Test
        public void testExtract() throws Exception {
            InputStream ras = getClass().getResourceAsStream(mtar);
            ApplicationArchiveExtractor streamProducerHandler = new ApplicationArchiveExtractor(ras, fileName);
            try {
                Path filePath = streamProducerHandler.extract(new ExtractStatusCallback() {
                    private Path filePath;
                    private int totalSize = 0;

                    @Override
                    public void onFileCreated(Path filePath) {
                        this.filePath = filePath;
                    }

                    @Override
                    public void onBytesToWrite(int bytes) throws SLException {
                        if (totalSize + bytes > maxFileUploadSize) {
                            throw new ContentException(
                                MessageFormat.format(Messages.ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxFileUploadSize));
                        }
                        totalSize += bytes;
                    }

                    @Override
                    public void onError(Exception e) {
                        assertEquals(exceptionMessage, e.getMessage());
                        if (filePath != null && Files.exists(filePath)) {
                            try {
                                if (Files.isDirectory(filePath)) {
                                    FileUtils.deleteDirectory(filePath.toFile());
                                } else {
                                    Files.delete(filePath);
                                }
                            } catch (IOException ex) {
                            }
                            assertTrue(!Files.exists(filePath));
                        }
                    }
                });
                if (filePath != null) {
                    assertTrue(Files.exists(filePath));
                }
            } catch (Exception e) {
                assertEquals(exceptionMessage, e.getMessage());
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class SaveFileStreamToDirectoryTest {

        @Parameters
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][] {
                // @formatter:off
                
                // (0) Doesn't have any directory entries
                
                { "archive-with-no-directory-entries-01.mtar", "web/", 3, (int)ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE, null},
                
                // (1) Doesn't have only root directory entry
                
                { "archive-with-no-directory-entries-02.mtar", "web/", 3, (int)ApplicationConfiguration.DEFAULT_MAX_RESOURCE_FILE_SIZE, null},
                
                // (2) Fail because too big
                
                { "archive-with-no-directory-entries-01.mtar", "web/", 3, 1, MessageFormat.format(Messages.ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, 1)}
                
                // @formatter:on
            });
        }

        private final String mtar;
        private final String fileName;
        private final int rootLevelEntryCount;
        private final int maxSize;
        private final String expectedExceptionMessage;
        private Path directoryPath;

        public SaveFileStreamToDirectoryTest(String mtar, String fileName, int rootLevelEntryCount, int maxSize,
            String expectedExceptionMessage) {
            this.mtar = mtar;
            this.fileName = fileName;
            this.rootLevelEntryCount = rootLevelEntryCount;
            this.maxSize = maxSize;
            this.expectedExceptionMessage = expectedExceptionMessage;
        }

        @Test
        public void testSaveStreamToDirectory() throws Exception {
            InputStream stream = getClass().getResourceAsStream(mtar);
            try (InputStreamProducer streamProducer = new InputStreamProducer(stream, fileName)) {
                ApplicationArchiveExtractor streamProducerHandler = new ApplicationArchiveExtractor(stream, fileName);
                streamProducer.getNextInputStream();
                try {
                    streamProducerHandler.saveToDirectoryUnordered(streamProducer, new ExtractStatusCallback() {
                        private int totalSize = 0;

                        @Override
                        public void onFileCreated(Path filePath) {
                            directoryPath = filePath;
                        }

                        @Override
                        public void onBytesToWrite(int bytes) throws SLException {
                            if (totalSize + bytes > maxSize) {
                                throw new ContentException(
                                    MessageFormat.format(Messages.ERROR_SIZE_OF_APP_EXCEEDS_MAX_SIZE_LIMIT, maxSize));
                            }
                            totalSize += bytes;
                        }

                        // Not called by the tested method, so we have to catch()
                        @Override
                        public void onError(Exception e) {
                        }
                    });
                } catch (Exception e) {
                    assertEquals(expectedExceptionMessage, e.getMessage());
                    return;
                }
                assertTrue(Files.exists(directoryPath));
                assertEquals(rootLevelEntryCount, directoryPath.toFile()
                    .list().length);
                Path path1 = directoryPath.resolve("node_modules")
                    .resolve("sap-hdi-deploy")
                    .resolve("package.json");
                assertEquals(10762, Files.size(path1));
                Path path2 = directoryPath.resolve("package.json");
                assertEquals(209, Files.size(path2));
                Path path3 = directoryPath.resolve("src")
                    .resolve("job_log.hdbtable");
                assertEquals(459, Files.size(path3));
            }
        }

        @After
        public void tearDown() throws IOException {
            assertNotNull(directoryPath);
            deleteFile(directoryPath);
            assertTrue(!Files.exists(directoryPath));
        }

        private void deleteFile(Path filePath) throws IOException {
            if (filePath != null && Files.exists(filePath)) {
                if (Files.isDirectory(filePath)) {
                    FileUtils.deleteDirectory(filePath.toFile());
                } else {
                    Files.delete(filePath);
                }
            }
        }
    }
}
