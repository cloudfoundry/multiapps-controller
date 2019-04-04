package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloud.lm.sl.mta.model.Version;

public class MetadataMapper {

    public String getMtaId(Metadata metadata) {
        return metadata.getLabels().getOrDefault("mta_id", null);
    }

    public Version getMtaVersion(Metadata metadata) {
        String version = metadata.getAnnotations().getOrDefault("mta_version", null);
        return version == null ? null : Version.parseVersion(version);
    }
}
