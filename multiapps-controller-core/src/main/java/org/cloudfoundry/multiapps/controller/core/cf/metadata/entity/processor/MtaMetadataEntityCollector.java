package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

public interface MtaMetadataEntityCollector<T extends CloudEntity> {

    List<T> collect(CloudControllerClient client, MtaMetadataCriteria criteria);

}
