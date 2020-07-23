package com.sap.cloud.lm.sl.cf.core.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.core.Constants;

public class ApplicationBitsChangesDetectorTest {

    private static final String FILE_NAME = "src/test/resources/com/sap/cloud/lm/sl/cf/core/helpers/node-hello-world-0.1.0-SNAPSHOT.mtar";
    private Map<String, String> appEnv;
    private File applicationFile;
    private ApplicationFileDigestDetector detector;

    @Before
    public void setUp() {
        appEnv = MapUtil.asMap(Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(new TreeMap<>()));
        detector = new ApplicationFileDigestDetector(appEnv);
        applicationFile = Paths.get(FILE_NAME)
                               .toFile();
    }

    @Test
    public void testDetectCurrentAppFileDigestWithNoInfoInEnv() {
        detector = new ApplicationFileDigestDetector(appEnv);

        assertNull(detector.detectCurrentAppFileDigest());
    }

    @Test
    public void testDetectWithFileDigestInTheEnv() throws NoSuchAlgorithmException, IOException {
        String testFileDigest = getTestFileDigest();
        Map<String, Object> deployAttributes = MapUtil.asMap(Constants.ATTR_APP_CONTENT_DIGEST, testFileDigest);
        appEnv.put(Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(deployAttributes));
        detector = new ApplicationFileDigestDetector(appEnv);

        assertEquals(testFileDigest, detector.detectCurrentAppFileDigest());
    }

    private String getTestFileDigest() throws IOException, NoSuchAlgorithmException {
        return DigestHelper.computeFileChecksum(Paths.get(applicationFile.toURI()), "MD5");
    }
}
