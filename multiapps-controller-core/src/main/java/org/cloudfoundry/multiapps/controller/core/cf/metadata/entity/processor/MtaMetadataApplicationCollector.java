package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;

@Named
public class MtaMetadataApplicationCollector implements MtaMetadataEntityCollector<CloudApplication> {

    @Override
    public List<CloudApplication> collect(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getApplicationsByMetadataLabelSelector(criteria.get());
    }

    @Override
    public List<CloudApplication> collectRequiredDataOnly(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return collect(client, criteria);
    }
}
