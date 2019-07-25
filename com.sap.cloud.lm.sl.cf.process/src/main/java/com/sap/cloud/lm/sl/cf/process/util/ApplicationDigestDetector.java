package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.helpers.ApplicationFileDigestDetector;

public class ApplicationDigestDetector {
    private CloudApplication app;
    private CloudControllerClient client;

    public ApplicationDigestDetector(CloudApplication app, CloudControllerClient client) {
        this.app = app;
        this.client = client;
    }

    public String getExistingApplicationDigest() {
        CloudApplication existingApp = client.getApplication(app.getName());
        ApplicationFileDigestDetector applicationFileDigestDetector = new ApplicationFileDigestDetector(existingApp.getEnv());
        return applicationFileDigestDetector.detectCurrentAppFileDigest();
    }

    public boolean hasApplicationContentDigestChanged(String newFileDigest, String currentFileDigest) {
        return !newFileDigest.equals(currentFileDigest);
    }
}
