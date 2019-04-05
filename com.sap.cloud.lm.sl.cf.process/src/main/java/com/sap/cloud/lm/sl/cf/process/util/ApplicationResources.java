package com.sap.cloud.lm.sl.cf.process.util;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.domain.CloudResources;

public class ApplicationResources {
    private List<CloudResource> cloudResourceList;
    private String applicationDigest;

    public ApplicationResources() {
        this.cloudResourceList = new ArrayList<>();
    }

    public void addCloudResource(CloudResource cloudResource) {
        cloudResourceList.add(cloudResource);
    }

    public CloudResources toCloudResources() {
        return new CloudResources(cloudResourceList);
    }

    protected List<CloudResource> getCloudResourceList() {
        return cloudResourceList;
    }

    public String getApplicationDigest() {
        return applicationDigest;
    }

    public void setApplicationDigest(String applicationDigest) {
        this.applicationDigest = applicationDigest;
    }

}
