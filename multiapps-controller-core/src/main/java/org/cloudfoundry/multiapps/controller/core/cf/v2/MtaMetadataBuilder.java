package org.cloudfoundry.multiapps.controller.core.cf.v2;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableMetadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableMetadata.Builder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class MtaMetadataBuilder {

    public static Builder init(DeploymentDescriptor deploymentDescriptor, String namespace) {
        String hashedMtaId = MtaMetadataUtil.getHashedLabel(deploymentDescriptor.getId());
        Builder builder = ImmutableMetadata.builder()
                                           .putLabel(MtaMetadataLabels.MTA_ID, hashedMtaId)
                                           .putAnnotation(MtaMetadataAnnotations.MTA_ID, deploymentDescriptor.getId())
                                           .putAnnotation(MtaMetadataAnnotations.MTA_VERSION, deploymentDescriptor.getVersion());

        if (StringUtils.isNotEmpty(namespace)) {
            String hashedMtaNamespace = MtaMetadataUtil.getHashedLabel(namespace);
            builder.putLabel(MtaMetadataLabels.MTA_NAMESPACE, hashedMtaNamespace)
                   .putAnnotation(MtaMetadataAnnotations.MTA_NAMESPACE, namespace);
        }
        return builder;
    }

    private MtaMetadataBuilder() {
    }

}
