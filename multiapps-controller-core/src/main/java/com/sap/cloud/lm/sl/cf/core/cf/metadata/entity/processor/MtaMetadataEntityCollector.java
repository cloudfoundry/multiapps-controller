package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEntity;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.criteria.MtaMetadataCriteria;

public interface MtaMetadataEntityCollector<T extends CloudEntity> {

    List<T> collect(CloudControllerClient client, MtaMetadataCriteria criteria);

}
