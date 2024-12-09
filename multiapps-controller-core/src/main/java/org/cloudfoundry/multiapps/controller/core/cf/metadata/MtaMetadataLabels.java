package org.cloudfoundry.multiapps.controller.core.cf.metadata;

public class MtaMetadataLabels {

    public static final String MTA_ID = "mta_id";
    public static final String MTA_NAMESPACE = "mta_namespace";
    public static final String SPACE_GUID = "space_guid";
    public static final String MTA_DESCRIPTOR_CHECKSUM = "mta_descriptor_checksum";
    public static final String AUTOSCALER_LABEL = "app-autoscaler.cloudfoundry.org/disable-autoscaling";

    private MtaMetadataLabels() {
    }
}
