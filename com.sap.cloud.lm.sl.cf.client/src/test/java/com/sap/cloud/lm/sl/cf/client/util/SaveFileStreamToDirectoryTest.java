package com.sap.cloud.lm.sl.cf.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SaveFileStreamToDirectoryTest {

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            
            // (0) Doesn't have any directory entries
            
            { "archive-with-no-directory-entries-01.mtar", "web/", 3},
            
            // (1) Doesn't have only root directory entry
            
            { "archive-with-no-directory-entries-02.mtar", "web/", 3}
            // @formatter:on
        });
    }

    private final String mtar;
    private final String fileName;
    private final int rootLevelEntryCount;
    private Path directoryPath;

    public SaveFileStreamToDirectoryTest(String mtar, String fileName, int rootLevelEntryCount) {
        this.mtar = mtar;
        this.fileName = fileName;
        this.rootLevelEntryCount = rootLevelEntryCount;
    }

    @Before
    public void setUp() throws IOException {
        directoryPath = StreamUtil.getTempDirectory(fileName);
    }

    @Test
    public void testSaveStreamToDirectory() throws Exception {
        InputStream stream = StreamUtil.class.getResourceAsStream(mtar);
        try (InputStreamProducer streamProducer = new InputStreamProducer(stream, fileName, 250 * 1024 * 1024 )) {
            InputStream streamToSave = streamProducer.getNextInputStream();
            String streamEntryName = streamProducer.getStreamEntryName();
            directoryPath = StreamUtil.getTempDirectory(fileName);
            while (streamToSave != null) {
                if (!streamEntryName.endsWith("/")) {
                    StreamUtil streamUtil = new StreamUtil(streamToSave);
                    streamUtil.saveStreamToDirectory(streamEntryName, fileName, directoryPath);
                }
                streamToSave = streamProducer.getNextInputStream();
                streamEntryName = streamProducer.getStreamEntryName();
            }
            assertTrue(Files.exists(directoryPath));
            assertEquals(rootLevelEntryCount, directoryPath.toFile().list().length);
            Path path1 = directoryPath.resolve("node_modules").resolve("sap-hdi-deploy").resolve("package.json");
            assertEquals(10762, Files.size(path1));
            Path path2 = directoryPath.resolve("package.json");
            assertEquals(209, Files.size(path2));
            Path path3 = directoryPath.resolve("src").resolve("job_log.hdbtable");
            assertEquals(459, Files.size(path3));
        }
    }

    @After
    public void tearDown() throws IOException {
        assertNotNull(directoryPath);
        StreamUtil.deleteFile(directoryPath.toFile());
        assertTrue(!Files.exists(directoryPath));
    }
}
