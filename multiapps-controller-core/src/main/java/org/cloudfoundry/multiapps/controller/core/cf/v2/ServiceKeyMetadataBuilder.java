package org.cloudfoundry.multiapps.controller.core.cf.v2;

import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Metadata.Builder;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataLabels;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class ServiceKeyMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, String namespace, String spaceGuid) {

        Builder builder = MtaMetadataBuilder.init(deploymentDescriptor, namespace)
                                            .label(MtaMetadataLabels.SPACE_GUID, spaceGuid);

        return builder.build();
    }

    private ServiceKeyMetadataBuilder() {
    }

}
