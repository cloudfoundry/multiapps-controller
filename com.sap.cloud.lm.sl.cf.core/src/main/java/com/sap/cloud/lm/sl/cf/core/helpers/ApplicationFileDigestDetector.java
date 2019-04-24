package com.sap.cloud.lm.sl.cf.core.helpers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.DigestHelper;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationFileDigestDetector {

    private CloudApplication app;

    public ApplicationFileDigestDetector(CloudApplication app) {
        this.app = app;
    }

    public String detectCurrentAppFileDigest() {
        Map<String, Object> applicationDeployAttributes = getApplicationDeployAttributes();
        Object currentFileDigest = applicationDeployAttributes.get(Constants.ATTR_APP_CONTENT_DIGEST);
        return currentFileDigest == null ? null : (String) currentFileDigest;
    }

    private Map<String, Object> getApplicationDeployAttributes() {
        Map<String, String> applicationEnv = app.getEnv();
        String applicationDeployAttributes = applicationEnv.get(Constants.ENV_DEPLOY_ATTRIBUTES);
        return JsonUtil.convertJsonToMap(applicationDeployAttributes);
    }

    public String detectNewAppFileDigest(File applicationFile) {
        try {
            if (applicationFile.isDirectory()) {
                return DigestHelper.computeDirectoryCheckSum(Paths.get(applicationFile.toURI()), "MD5");
            }

            return DigestHelper.computeFileChecksum(Paths.get(applicationFile.toURI()), "MD5");
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new SLException(MessageFormat.format(Messages.ERROR_COMPUTING_CHECKSUM_OF_FILE, applicationFile.getName(), app.getName()),
                e);
        }
    }

}
