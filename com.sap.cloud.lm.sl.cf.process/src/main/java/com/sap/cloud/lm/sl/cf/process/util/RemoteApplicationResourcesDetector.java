package com.sap.cloud.lm.sl.cf.process.util;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudResources;

public class RemoteApplicationResourcesDetector {
    private CloudControllerClient client;
    private CloudResources cloudResources;

    public RemoteApplicationResourcesDetector(CloudControllerClient client, CloudResources cloudResources) {
        this.client = client;
        this.cloudResources = cloudResources;
    }

    public CloudResources getCloudResources() {
        return client.getKnownRemoteResources(cloudResources);
    }

}
