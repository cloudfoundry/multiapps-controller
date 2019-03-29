package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collection;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.domain.CloudResources;

public class ApplicationResources {
    private CloudResources cloudResources;

    public void createCloudResources(Collection<CloudResource> cloudResourceCollection) {
        this.cloudResources = new CloudResources(cloudResourceCollection);
    }

    public CloudResources getCloudResources() {
        if (cloudResources == null) {
            return new CloudResources();
        }
        return cloudResources;
    }

    public CloudResources getKnownRemoteResources(CloudControllerClient client) {
        CloudResources knownRemoteResources = client.getKnownRemoteResources(getCloudResources());
        return knownRemoteResources != null ? knownRemoteResources : new CloudResources();
    }

}
