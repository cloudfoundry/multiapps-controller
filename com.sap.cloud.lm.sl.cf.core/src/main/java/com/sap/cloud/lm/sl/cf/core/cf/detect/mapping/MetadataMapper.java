package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloud.lm.sl.mta.model.Version;

public class MetadataMapper {

    public static final String MTA_VERSION = "mta_version";
    public static final String MTA_ID = "mta_id";

    public String getMtaId(Metadata metadata) {
        return metadata.getLabels().get(MTA_ID);
    }

    public Version getMtaVersion(Metadata metadata) {
        String version = metadata.getLabels().get(MTA_VERSION);
        return version == null ? null : Version.parseVersion(version);
    }
}
