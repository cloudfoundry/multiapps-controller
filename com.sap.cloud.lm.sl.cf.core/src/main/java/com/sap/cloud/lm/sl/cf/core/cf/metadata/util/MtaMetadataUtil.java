package com.sap.cloud.lm.sl.cf.core.cf.metadata.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.util.DigestUtils;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;

public class MtaMetadataUtil {

    public static final List<String> ENV_MTA_METADATA_FIELDS = Collections.unmodifiableList(Arrays.asList(Constants.ENV_MTA_METADATA,
                                                                                                          Constants.ENV_MTA_MODULE_METADATA,
                                                                                                          Constants.ENV_MTA_SERVICES,
                                                                                                          Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES));

    public static final List<String> MTA_METADATA_LABELS = Collections.unmodifiableList(Arrays.asList(MtaMetadataLabels.MTA_ID));
    public static final List<String> MTA_METADATA_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(MtaMetadataAnnotations.MTA_ID,
                                                                                                           MtaMetadataAnnotations.MTA_VERSION));
    public static final List<String> MTA_METADATA_APPLICATION_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(MtaMetadataAnnotations.MTA_MODULE,
                                                                                                                       MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES,
                                                                                                                       MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES));
    public static final List<String> MTA_METADATA_SERVICE_ANNOTATIONS = Collections.unmodifiableList(Arrays.asList(MtaMetadataAnnotations.MTA_RESOURCE));

    public static boolean hasEnvMtaMetadata(CloudApplication application) {
        return application.getEnv()
                          .keySet()
                          .stream()
                          .anyMatch(ENV_MTA_METADATA_FIELDS::contains);
    }

    public static boolean hasMtaMetadata(CloudEntity entity) {
        Metadata metadata = entity.getV3Metadata();
        if (metadata == null || metadata.getLabels() == null) {
            return false;
        }
        Set<String> metadataLabels = metadata.getLabels()
                                             .keySet();
        Set<String> metadataAnnotations = metadata.getAnnotations()
                                                  .keySet();
        return metadataLabels.containsAll(MTA_METADATA_LABELS) && metadataAnnotations.containsAll(MTA_METADATA_ANNOTATIONS);
    }

    public static String getHashedMtaId(String mtaId) {
        return DigestUtils.md5DigestAsHex(mtaId.getBytes());
    }

    private MtaMetadataUtil() {
    }

}
