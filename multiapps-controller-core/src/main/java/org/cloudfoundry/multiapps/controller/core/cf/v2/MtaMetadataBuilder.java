package org.cloudfoundry.multiapps.controller.core.cf.v2;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Metadata.Builder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class MtaMetadataBuilder {

    public static Builder init(DeploymentDescriptor deploymentDescriptor, String namespace) {
        String hashedMtaId = MtaMetadataUtil.getHashedLabel(deploymentDescriptor.getId());
        Builder builder = Metadata.builder()
                                  .label(MtaMetadataLabels.MTA_ID, hashedMtaId)
                                  .annotation(MtaMetadataAnnotations.MTA_ID, deploymentDescriptor.getId())
                                  .annotation(MtaMetadataAnnotations.MTA_VERSION, deploymentDescriptor.getVersion());

        if (StringUtils.isNotEmpty(namespace)) {
            String hashedMtaNamespace = MtaMetadataUtil.getHashedLabel(namespace);
            builder.label(MtaMetadataLabels.MTA_NAMESPACE, hashedMtaNamespace)
                   .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, namespace);
        }

        return builder;
    }

    private MtaMetadataBuilder() {
    }

}
