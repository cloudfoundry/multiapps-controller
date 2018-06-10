package com.sap.cloud.lm.sl.cf.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.client.message.Messages;

@RunWith(Parameterized.class)
public class SaveFileStreamToDirectoryTest {

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            
            // (0) Doesn't have any directory entries
            
            { "archive-with-no-directory-entries-01.mtar", "web/", 3, 1024 * 1024 * 1024l, null},
            
            // (1) Doesn't have only root directory entry
            
            { "archive-with-no-directory-entries-02.mtar", "web/", 3, 1024 * 1024 * 1024l, null},
            
            // (2) Size should be too big
            
            { "archive-with-no-directory-entries-01.mtar", "web/", 3, 200, MessageFormat.format(Messages.ERROR_SIZE_OF_APPLICATION_EXCEEDS_MAX_SIZE_LIMIT, 200)}
            // @formatter:on
        });
    }

    private final String mtar;
    private final String fileName;
    private final int rootLevelEntryCount;
    private final long maxFileUploadSize;
    private TemporaryFolder tempDir = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public SaveFileStreamToDirectoryTest(String mtar, String fileName, int rootLevelEntryCount, long maxFileUploadSize,
        String expectedExceptionMessage) {
        this.mtar = mtar;
        this.fileName = fileName;
        this.rootLevelEntryCount = rootLevelEntryCount;
        this.maxFileUploadSize = maxFileUploadSize;
        if (expectedExceptionMessage != null) {
            thrown.expect(Exception.class);
            thrown.expectMessage(expectedExceptionMessage);
        }
    }

    @Before
    public void setUp() throws IOException {
        tempDir.create();
    }

    @Test
    public void testSaveStreamToDirectory() throws Exception {
        InputStream stream = StreamUtil.class.getResourceAsStream(mtar);
        try (InputStreamProducer streamProducer = new InputStreamProducer(stream, fileName, 250 * 1024 * 1024)) {
            InputStream streamToSave = streamProducer.getNextInputStream();
            String streamEntryName = streamProducer.getStreamEntryName();
            long filesSize = 0;
            while (streamToSave != null) {
                if (!streamEntryName.endsWith("/")) {
                    StreamUtil streamUtil = new StreamUtil(streamToSave);
                    filesSize = streamUtil.saveStreamToDirectory(streamEntryName, fileName, getDirPath(), filesSize, maxFileUploadSize);
                }
                streamToSave = streamProducer.getNextInputStream();
                streamEntryName = streamProducer.getStreamEntryName();
            }
            assertTrue(Files.exists(getDirPath()));
            assertEquals(rootLevelEntryCount, getDir().list().length);
            Path path1 = getDirPath().resolve("node_modules")
                .resolve("sap-hdi-deploy")
                .resolve("package.json");
            assertEquals(10762, Files.size(path1));
            Path path2 = getDirPath().resolve("package.json");
            assertEquals(209, Files.size(path2));
            Path path3 = getDirPath().resolve("src")
                .resolve("job_log.hdbtable");
            assertEquals(459, Files.size(path3));
        }
    }

    @After
    public void tearDown() throws IOException {
        assertNotNull(getDir());
        FileUtils.deleteDirectory(getDir());
        assertTrue(!Files.exists(getDirPath()));
    }

    public File getDir() {
        return tempDir.getRoot();
    }

    public Path getDirPath() {
        return tempDir.getRoot()
            .toPath();
    }
}
