package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;

public interface MtaMetadataCollector<T extends MetadataEntity> {

    List<T> collect(MtaMetadataCriteria criteria, CloudControllerClient client);

}
