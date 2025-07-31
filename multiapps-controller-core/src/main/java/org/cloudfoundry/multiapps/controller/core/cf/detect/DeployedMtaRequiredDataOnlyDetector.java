package org.cloudfoundry.multiapps.controller.core.cf.detect;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataEntityAggregator;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor.MtaMetadataEntityCollector;

@Named("deployedMtaRequiredDataOnlyDetector")
public class DeployedMtaRequiredDataOnlyDetector extends DeployedMtaDetector {

    @Inject
    public DeployedMtaRequiredDataOnlyDetector(List<MtaMetadataEntityCollector<?>> mtaMetadataEntityCollectors,
                                               MtaMetadataEntityAggregator mtaMetadataEntityAggregator) {
        super(mtaMetadataEntityCollectors, mtaMetadataEntityAggregator);
    }

    @Override
    protected <T extends CloudEntity> List<T> collect(MtaMetadataEntityCollector<T> collector, MtaMetadataCriteria criteria,
                                                      CloudControllerClient client) {
        return collector.collectRequiredDataOnly(client, criteria);
    }
}
