package com.sap.cloud.lm.sl.cf.core.cf.detect;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ApplicationMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MtaMetadataEntityAggregator {

    @Autowired
    private MtaMetadataExtractorFactory<MetadataEntity> metadataExtractorFactory;

    public DeployedMta aggregate(List<MetadataEntity> entities) {
        DeployedMta deployedMta = new DeployedMta();
        entities.forEach(e -> metadataExtractorFactory.get(e).extract(e, deployedMta));
        return deployedMta;
    }
}
