package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

@Named
public class MtaMetadataApplicationCollector implements MtaMetadataEntityCollector<CloudApplication> {

    @Override
    public List<CloudApplication> collect(CloudControllerClient client, MtaMetadataCriteria criteria) {
        return client.getApplicationsByMetadataLabelSelector(criteria.get());
    }
}
