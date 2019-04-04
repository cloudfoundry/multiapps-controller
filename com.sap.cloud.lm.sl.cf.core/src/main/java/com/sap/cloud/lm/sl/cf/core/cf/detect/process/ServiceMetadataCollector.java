package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.AppMetadataMapper;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ServiceMetadataMapper;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.model.ServiceMtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

@Component
@Profile("cf")
public class ServiceMetadataCollector implements MtaMetadataCollector<ServiceMetadataEntity> {

    @Autowired
    private ServiceMetadataMapper mapper;
    
    @Override
    public List<ServiceMetadataEntity> collect(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<ServiceMetadataEntity> resultEntities = new ArrayList<ServiceMetadataEntity>();

        List<CloudService> allServices = client.getServicesByMetadata(criteria.get());
        for (CloudService service : allServices) {
            ServiceMtaMetadata serviceMetadata = mapMetadata(service);

            if (serviceMetadata == null) {
                continue;
            }
            resultEntities.add(new ServiceMetadataEntity(serviceMetadata, service, serviceMetadata.getMtaMetadata()));
        }
        return resultEntities;
    }

    private ServiceMtaMetadata mapMetadata(CloudService service) {
        if (service.getMetadata() == null) {
            return null;
        }

        DeployedMtaMetadata mtaMetadata = new DeployedMtaMetadata();
        mtaMetadata.setId(mapper.getMtaId(service.getMetadata()));
        mtaMetadata.setVersion(mapper.getMtaVersion(service.getMetadata()));
        
        List<String> boundApps = mapper.getBoundApps(service.getMetadata());
        Map<String,String> appsCredentials = mapper.getAppsCredentials(service.getMetadata());
        DeployedMtaResource resource = mapper.getResource(service.getMetadata());
        
        return ServiceMtaMetadata.builder()
                                 .withAppsCredentials(appsCredentials)
                                 .withBoundApps(boundApps)
                                 .withDeployedMtaResource(resource)
                                 .withMtaMetadata(mtaMetadata)
                                 .build();
    }
}
