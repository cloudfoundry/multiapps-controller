package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ApplicationMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ApplicationMetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class AppMetadataCollector implements MtaMetadataCollector<ApplicationMetadataEntity> {

    @Autowired
    private ApplicationMetadataFieldExtractor fieldExtractor;
    
    @Override
    public List<ApplicationMetadataEntity> collect(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<ApplicationMetadataEntity> resultEntities = new ArrayList<ApplicationMetadataEntity>();

        List<CloudApplication> allApps = client.getApplicationsByMetadata(criteria.get());
        for (CloudApplication app : allApps) {
            ApplicationMtaMetadata appMetadata = fieldExtractor.extractMetadata(app);
            resultEntities.add(new ApplicationMetadataEntity(appMetadata.getMtaMetadata(), appMetadata, app));
        }
        return resultEntities;
    }
}
