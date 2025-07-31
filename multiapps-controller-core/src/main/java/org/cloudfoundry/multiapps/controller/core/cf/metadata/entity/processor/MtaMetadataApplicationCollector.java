package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

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
