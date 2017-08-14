package com.sap.cloud.lm.sl.cf.client.lib.domain;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;

public class CloudServiceBrokerExtended extends CloudServiceBroker {

    private String spaceGuid;

    public CloudServiceBrokerExtended(String name, String url, String username, String password, String spaceGuid) {
        this(null, name, url, username, password, spaceGuid);
    }

    public CloudServiceBrokerExtended(Meta meta, String name, String url, String username, String password, String spaceGuid) {
        super(meta, name, url, username, password);
        this.spaceGuid = spaceGuid;
    }

    public String getSpaceGuid() {
        return spaceGuid;
    }

    public void setSpaceGuid(String spaceGuid) {
        this.spaceGuid = spaceGuid;
    }

}
