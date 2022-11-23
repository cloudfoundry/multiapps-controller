package org.cloudfoundry.multiapps.controller.process.steps;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;

import java.text.MessageFormat;
import java.util.UUID;

public abstract class ClearMtaMetadataBaseStep extends SyncFlowableStep {

    protected void deleteMtaMetadataFromService(CloudControllerClient client, CloudServiceInstance serviceToDetachFromMta) {
        Metadata serviceMetadata = serviceToDetachFromMta.getV3Metadata();
        if (serviceMetadata == null) {
            return;
        }
        getStepLogger().info(MessageFormat.format(Messages.DETACHING_SERVICE_0_FROM_MTA, serviceToDetachFromMta.getName()));
        UUID serviceGuid = serviceToDetachFromMta.getGuid();
        client.updateServiceInstanceMetadata(serviceGuid, MtaMetadataUtil.getMetadataWithoutMtaFields(serviceMetadata));
    }
}
