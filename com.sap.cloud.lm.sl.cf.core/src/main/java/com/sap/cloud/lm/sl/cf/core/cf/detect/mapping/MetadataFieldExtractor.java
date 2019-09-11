package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloud.lm.sl.mta.model.Version;

public class MetadataFieldExtractor {

    public static final String MTA_VERSION = "mta_version";
    public static final String MTA_ID = "mta_id";

    public String getMtaId(Metadata metadata) {
        if(metadata.getLabels() == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_LABELS);
        }
        return metadata.getLabels().get(MTA_ID);
    }

    public Version getMtaVersion(Metadata metadata) {
        if(metadata.getLabels() == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_LABELS);
        }
        String version = metadata.getLabels().get(MTA_VERSION);
        return version == null ? null : Version.parseVersion(version);
    }
}
