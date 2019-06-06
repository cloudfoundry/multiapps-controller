package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.AppMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.AppMetadataMapper;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class AppMetadataCollector implements MtaMetadataCollector<AppMetadataEntity> {

    @Autowired
    private AppMetadataMapper mapper;
    
    @Override
    public List<AppMetadataEntity> collect(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<AppMetadataEntity> resultEntities = new ArrayList<AppMetadataEntity>();

        List<CloudApplication> allApps = client.getApplicationsByMetadata(criteria.get());
        for (CloudApplication app : allApps) {
            ApplicationMtaMetadata appMetadata = mapper.mapMetadata(app);
            System.out.println("Collector collected this app metadata: " + JsonUtil.toJson(appMetadata, true));
            if (appMetadata == null) {
                continue;
            }
            resultEntities.add(new AppMetadataEntity(appMetadata, app, appMetadata.getMtaMetadata()));
        }
        return resultEntities;
    }

}
