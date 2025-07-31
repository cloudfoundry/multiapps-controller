package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

@Named
public class MtaMetadataServiceCollector implements MtaMetadataEntityCollector<CloudServiceInstance> {

    @Override
    public List<CloudServiceInstance> collect(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getServiceInstancesByMetadataLabelSelector(criteria.get());
    }

    @Override
    public List<CloudServiceInstance> collectRequiredDataOnly(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(criteria.get());
    }
}
