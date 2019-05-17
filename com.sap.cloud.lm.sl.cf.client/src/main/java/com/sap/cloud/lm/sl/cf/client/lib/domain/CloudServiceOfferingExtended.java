package com.sap.cloud.lm.sl.cf.client.lib.domain;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;

public class CloudServiceOfferingExtended extends CloudServiceOffering {

    private List<String> tags = new ArrayList<>();

    // Required by Jackson.
    protected CloudServiceOfferingExtended() {
    }

    public CloudServiceOfferingExtended(Meta meta, String name) {
        super(meta, name);
    }

    public CloudServiceOfferingExtended(Meta meta, String name, String provider, String version) {
        super(meta, name, provider, version);
    }

    public CloudServiceOfferingExtended(Meta meta, String name, String provider, String version, String description, boolean active,
        boolean bindable, String url, String infoUrl, String uniqueId, String extra, String docUrl, List<String> tags) {
        super(meta, name, provider, version, description, active, bindable, url, infoUrl, uniqueId, extra, docUrl);
        this.tags = tags;
    }

    public List<String> getTags() {
        return tags;
    }

}
