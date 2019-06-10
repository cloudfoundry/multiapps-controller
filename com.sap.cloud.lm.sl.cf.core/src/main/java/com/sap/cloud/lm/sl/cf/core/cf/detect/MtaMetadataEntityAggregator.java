package com.sap.cloud.lm.sl.cf.core.cf.detect;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MtaMetadataEntityAggregator {

    @Autowired
    private MtaMetadataExtractorFactory<MetadataEntity> metadataExtractorFactory;

    public DeployedMta aggregate(List<MetadataEntity> entities) {
        DeployedMta deployedMta = new DeployedMta();
        System.out.println("Extracting list of entities: " + JsonUtil.toJson(entities, true));
        entities.forEach(e -> metadataExtractorFactory.get(e).extract(e, deployedMta));
        return deployedMta;
    }
}
