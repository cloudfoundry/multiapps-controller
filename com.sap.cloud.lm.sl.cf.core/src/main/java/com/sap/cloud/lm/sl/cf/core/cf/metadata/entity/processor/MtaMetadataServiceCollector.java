package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria.MtaMetadataCriteria;

@Named
public class MtaMetadataServiceCollector implements MtaMetadataEntityCollector<CloudServiceInstance> {

    @Override
    public List<CloudServiceInstance> collect(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getServiceInstancesByMetadataLabelSelector(criteria.get());
    }
}
