package org.cloudfoundry.multiapps.controller.core.cf.metadata.util;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.springframework.util.DigestUtils;
import org.apache.commons.lang3.StringUtils;

public class MtaMetadataUtil {

    public static final List<String> MTA_METADATA_MANDATORY_LABELS = List.of(MtaMetadataLabels.MTA_ID);
    public static final List<String> MTA_METADATA_MANDATORY_ANNOTATIONS = List.of(MtaMetadataAnnotations.MTA_ID, MtaMetadataAnnotations.MTA_VERSION);
    public static final List<String> MTA_METADATA_APPLICATION_ANNOTATIONS = List.of(MtaMetadataAnnotations.MTA_MODULE, MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES,
                                                                                    MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES);
    public static final List<String> MTA_METADATA_SERVICE_ANNOTATIONS = List.of(MtaMetadataAnnotations.MTA_RESOURCE);

    public static boolean hasMtaMetadata(CloudEntity entity) {
        Metadata metadata = entity.getV3Metadata();
        if (metadata == null || metadata.getLabels() == null) {
            return false;
        }
        Set<String> metadataLabels = metadata.getLabels()
                                             .keySet();
        Set<String> metadataAnnotations = metadata.getAnnotations()
                                                  .keySet();
        return metadataLabels.containsAll(MTA_METADATA_MANDATORY_LABELS)
            && metadataAnnotations.containsAll(MTA_METADATA_MANDATORY_ANNOTATIONS);
    }

    public static String getHashedLabel(String mtaLabel) {
        if (StringUtils.isEmpty(mtaLabel)) {
            return mtaLabel;
        }

        return DigestUtils.md5DigestAsHex(mtaLabel.getBytes());
    }

    private MtaMetadataUtil() {
    }

}
