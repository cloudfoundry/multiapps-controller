package org.cloudfoundry.multiapps.controller.core.cf.metadata.util;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.springframework.util.DigestUtils;

import com.sap.cloudfoundry.client.facade.domain.CloudEntity;

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

    public static Metadata getMetadataWithoutMtaFields(Metadata metadata) {
        return Metadata.builder()
                       .from(metadata)
                       .label(MtaMetadataLabels.MTA_ID, null)
                       .label(MtaMetadataLabels.MTA_NAMESPACE, null)
                       .annotation(MtaMetadataAnnotations.MTA_ID, null)
                       .annotation(MtaMetadataAnnotations.MTA_VERSION, null)
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, null)
                       .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, null)
                       .build();
    }

    private MtaMetadataUtil() {
    }

}
