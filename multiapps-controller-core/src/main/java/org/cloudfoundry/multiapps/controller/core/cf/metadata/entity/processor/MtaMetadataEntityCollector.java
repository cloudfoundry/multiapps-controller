package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEntity;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

public interface MtaMetadataEntityCollector<T extends CloudEntity> {

    List<T> collect(CloudControllerClient client, MtaMetadataCriteria criteria);

    List<T> collectRequiredDataOnly(CloudControllerClient client, MtaMetadataCriteria criteria);

}
