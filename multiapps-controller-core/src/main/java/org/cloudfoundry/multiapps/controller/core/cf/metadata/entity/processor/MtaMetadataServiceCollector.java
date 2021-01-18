package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

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
