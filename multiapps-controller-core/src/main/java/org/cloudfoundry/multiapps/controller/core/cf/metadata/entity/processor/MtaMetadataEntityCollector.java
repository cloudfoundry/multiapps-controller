package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.cf.metadata.criteria.MtaMetadataCriteria;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;

public interface MtaMetadataEntityCollector<T extends CloudEntity> {

    List<T> collect(CloudControllerClient client, MtaMetadataCriteria criteria);

}
