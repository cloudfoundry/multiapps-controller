package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;

public class ApplicationDigestDetector {
    private final CloudApplication app;

    public ApplicationDigestDetector(CloudApplication app) {
        this.app = app;
    }

    public String getExistingApplicationDigest() {
        ApplicationFileDigestDetector applicationFileDigestDetector = new ApplicationFileDigestDetector(app.getEnv());
        return applicationFileDigestDetector.detectCurrentAppFileDigest();
    }

    public boolean hasApplicationContentDigestChanged(String newFileDigest, String currentFileDigest) {
        return !newFileDigest.equals(currentFileDigest);
    }
}
