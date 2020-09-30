package org.cloudfoundry.multiapps.controller.core.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.common.util.DigestHelper;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationBitsChangesDetectorTest {

    private static final String FILE_NAME = "src/test/resources/org/cloudfoundry/multiapps/controller/core/helpers/node-hello-world-0.1.0-SNAPSHOT.mtar";
    private Map<String, String> appEnv;
    private File applicationFile;
    private ApplicationFileDigestDetector detector;

    @BeforeEach
    public void setUp() {
        appEnv = new HashMap<>(Map.of(Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(new TreeMap<>())));
        detector = new ApplicationFileDigestDetector(appEnv);
        applicationFile = Paths.get(FILE_NAME)
                               .toFile();
    }

    @Test
    void testDetectCurrentAppFileDigestWithNoInfoInEnv() {
        detector = new ApplicationFileDigestDetector(appEnv);

        assertNull(detector.detectCurrentAppFileDigest());
    }

    @Test
    void testDetectWithFileDigestInTheEnv() throws NoSuchAlgorithmException, IOException {
        String testFileDigest = getTestFileDigest();
        Map<String, Object> deployAttributes = Map.of(Constants.ATTR_APP_CONTENT_DIGEST, testFileDigest);
        appEnv.put(Constants.ENV_DEPLOY_ATTRIBUTES, JsonUtil.toJson(deployAttributes));
        detector = new ApplicationFileDigestDetector(appEnv);

        assertEquals(testFileDigest, detector.detectCurrentAppFileDigest());
    }

    private String getTestFileDigest() throws IOException, NoSuchAlgorithmException {
        return DigestHelper.computeFileChecksum(Paths.get(applicationFile.toURI()), "MD5");
    }
}
