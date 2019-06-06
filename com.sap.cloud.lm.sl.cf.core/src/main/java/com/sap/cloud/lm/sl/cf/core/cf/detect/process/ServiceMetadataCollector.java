package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataCollector;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ServiceMetadataMapper;
import com.sap.cloud.lm.sl.cf.core.cf.detect.metadata.criteria.MtaMetadataCriteria;
import com.sap.cloud.lm.sl.cf.core.model.ServiceMtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class ServiceMetadataCollector implements MtaMetadataCollector<ServiceMetadataEntity> {

    @Autowired
    private ServiceMetadataMapper mapper;
    
    @Override
    public List<ServiceMetadataEntity> collect(MtaMetadataCriteria criteria, CloudControllerClient client) {
        List<ServiceMetadataEntity> resultEntities = new ArrayList<ServiceMetadataEntity>();

        List<CloudService> allServices = client.getServicesByMetadata(criteria.get());
        for (CloudService service : allServices) {
            ServiceMtaMetadata serviceMetadata = mapper.mapMetadata(service);

            if (serviceMetadata == null) {
                continue;
            }
            System.out.println("Collector collected this service metadata: " + JsonUtil.toJson(serviceMetadata, true));
            resultEntities.add(new ServiceMetadataEntity(serviceMetadata, service, serviceMetadata.getMtaMetadata()));
        }
        return resultEntities;
    }


}
