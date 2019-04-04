package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.AppMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.AppMetadataMapper;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

@Component
@Profile("cf")
public class AppMetadataCollector implements MtaMetadataCollector<AppMetadataEntity> {

    @Autowired
    private AppMetadataMapper mapper;
    
    @Override
    public List<AppMetadataEntity> collect(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<AppMetadataEntity> resultEntities = new ArrayList<AppMetadataEntity>();

        List<CloudApplication> allApps = client.getApplicationsByMetadata(criteria.get());
        for (CloudApplication app : allApps) {
            ApplicationMtaMetadata appMetadata = mapMetadata(app);

            if (appMetadata == null) {
                continue;
            }
            resultEntities.add(new AppMetadataEntity(appMetadata, app, appMetadata.getMtaMetadata()));
        }
        return resultEntities;
    }

    private ApplicationMtaMetadata mapMetadata(CloudApplication app) {
        if (app.getMetadata() == null) {
            return null;
        }

        DeployedMtaMetadata mtaMetadata = new DeployedMtaMetadata();
        mtaMetadata.setId(mapper.getMtaId(app.getMetadata()));
        mtaMetadata.setVersion(mapper.getMtaVersion(app.getMetadata()));
        String moduleName = mapper.getModuleName(app.getMetadata());
        List<String> providedDependencyNames = mapper.getProvidedDependencyNames(app.getMetadata());
        List<DeployedMtaResource> deployedMtaResources = mapper.getResource(app.getMetadata());

        return new ApplicationMtaMetadata(mtaMetadata, deployedMtaResources, moduleName, providedDependencyNames);
    }
}
