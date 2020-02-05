package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria.MtaMetadataCriteria;

@Named
public class MtaMetadataServiceCollector implements MtaMetadataEntityCollector<CloudService> {

    @Override
    public List<CloudService> collect(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getServicesByMetadataLabelSelector(criteria.get());
    }
}
