package com.sap.cloud.lm.sl.cf.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.client.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(value = Parameterized.class)
public class StreamUtilTest {

    private static final String SAMPLE_MTAR = "com.sap.mta.sample-1.2.1-beta.mtar";
    private static final String SAMPLE_FLAT_MTAR = "com.sap.mta.sample-1.2.1-beta-flat.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH = "archive-entry-with-absolute-path.mtar";
    private static final String SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH = "archive-entry-with-not-normalized-path.mtar";
    private static final long MAX_UPLOAD_FILE_SIZE = 1024 * 1024 * 1024l;

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            { SAMPLE_MTAR, "web/web-server.zip", "web/web-server.zip", null, MAX_UPLOAD_FILE_SIZE},
            { SAMPLE_FLAT_MTAR, "web/", "web/", null, MAX_UPLOAD_FILE_SIZE},
            { SAMPLE_FLAT_MTAR, "web/", "xxx/web/", null, MAX_UPLOAD_FILE_SIZE},
            { SAMPLE_MTAR_WITH_JAR_ENTRY_ABSOLUTE_PATH, "/web/", "/web/", MessageFormat.format(StreamUtil.PATH_SHOULD_NOT_BE_ABSOLUTE, "/web/asd"), MAX_UPLOAD_FILE_SIZE},
            { SAMPLE_MTAR_WITH_JAR_ENTRY_NOT_NORMALIZED_PATH, "web/", "web/", MessageFormat.format(StreamUtil.PATH_SHOULD_BE_NORMALIZED, "web/../asd"), MAX_UPLOAD_FILE_SIZE},
            { SAMPLE_MTAR, "db/", "db/", MessageFormat.format(Messages.ERROR_SIZE_OF_UNCOMPRESSED_FILE_EXCEEDS_MAX_SIZE_LIMIT, 201,
                "db/pricing-db.zip", 200), 200l}
            // @formatter:on
        });
    }

    private final String mtar;
    private final String entryName;
    private final String fileName;
    private final String exceptionMessage;
    private final long maxFileUploadSize;

    public StreamUtilTest(String mtar, String entryName, String fileName, String exceptionMessage, long maxFileUploadSize) {
        this.mtar = mtar;
        this.entryName = entryName;
        this.fileName = fileName;
        this.exceptionMessage = exceptionMessage;
        this.maxFileUploadSize = maxFileUploadSize;
    }

    @Test
    public void testSaveStream() throws Exception {
        InputStream ras = StreamUtilTest.class.getResourceAsStream(mtar);
        File file = null;
        try (InputStream is = getInputStream(ras, entryName)) {
            StreamUtil streamUtil = new StreamUtil(is);
            if (StreamUtil.isArchiveEntryDirectory(fileName)) {
                file = streamUtil.saveZipStreamToDirectory(fileName, maxFileUploadSize);
            } else {
                file = streamUtil.saveStreamToFile(fileName);
            }
            assertTrue(file.exists());
        } catch (Exception e) {
            if (exceptionMessage != null) {
                assertEquals(exceptionMessage, e.getMessage());
            }
        } finally {
            ras.close();
            if (file != null) {
                StreamUtil.deleteFile(file);
                assertTrue(!file.exists());
            }
        }
    }

    private static InputStream getInputStream(InputStream is, String entryName) throws SLException {
        try {
            ZipInputStream zis = new ZipInputStream(is);
            for (ZipEntry e; (e = zis.getNextEntry()) != null;) {
                if (e.getName()
                    .equals(entryName)) {
                    return zis;
                }
            }
            throw new SLException("Cannot find archive entry \"{0}\"", entryName);
        } catch (IOException e) {
            throw new SLException(e, "Error while retrieving archive entry \"{0}\"", entryName);
        }
    }
}
