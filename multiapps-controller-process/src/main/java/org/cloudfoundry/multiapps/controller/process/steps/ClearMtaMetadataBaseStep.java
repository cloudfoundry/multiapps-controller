package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.UUID;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;

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

    protected void deleteMtaMetadataFromServiceKey(CloudControllerClient client, CloudServiceKey serviceKeyToDetachFromMta) {
        Metadata serviceKeyMetadata = serviceKeyToDetachFromMta.getV3Metadata();
        if (serviceKeyMetadata == null || isEmpty(serviceKeyMetadata)) {
            return;
        }
        getStepLogger().info(MessageFormat.format(Messages.DETACHING_SERVICE_KEY_0_FROM_MTA, serviceKeyToDetachFromMta.getName()));
        UUID serviceKeyGuid = serviceKeyToDetachFromMta.getGuid();
        try {
            client.updateServiceBindingMetadata(serviceKeyGuid, MtaMetadataUtil.getMetadataWithoutMtaFields(serviceKeyMetadata));
        } catch (CloudOperationException e) {
            getStepLogger().errorWithoutProgressMessage(e, String.format(Messages.DETACHING_SERVICE_KEY_0_FAILED,
                                                                         serviceKeyToDetachFromMta.getName()));
        }
    }

    private boolean isEmpty(Metadata metadata) {
        return MapUtils.isEmpty(metadata.getAnnotations()) && MapUtils.isEmpty(metadata.getLabels());
    }
}
