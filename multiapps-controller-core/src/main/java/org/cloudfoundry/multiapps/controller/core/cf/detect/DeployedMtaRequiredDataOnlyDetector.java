package org.cloudfoundry.multiapps.controller.core.cf.detect;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataEntityCollector;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;

@Named("deployedMtaRequiredDataOnlyDetector")
public class DeployedMtaRequiredDataOnlyDetector extends DeployedMtaDetector {

    @Override
    protected <T extends CloudEntity> List<T> collect(MtaMetadataEntityCollector<T> collector, MtaMetadataCriteria criteria, CloudControllerClient client) {
        return collector.collectRequiredDataOnly(client, criteria);
    }
}
