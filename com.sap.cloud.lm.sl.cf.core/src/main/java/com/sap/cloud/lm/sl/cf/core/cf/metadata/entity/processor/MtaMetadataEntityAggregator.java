package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudService;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;
import com.sap.cloud.lm.sl.mta.model.Version;

@Named
public class MtaMetadataEntityAggregator {

    private MtaMetadataParser mtaMetadataParser;

    @Inject
    public MtaMetadataEntityAggregator(MtaMetadataParser mtaMetadataParser) {
        this.mtaMetadataParser = mtaMetadataParser;
    }

    public List<DeployedMta> aggregate(List<CloudEntity> entities) {
        Map<String, List<CloudEntity>> entitiesByMtaId = entities.stream()
                                                                 .collect(Collectors.groupingBy(this::getMtaId));
        return entitiesByMtaId.entrySet()
                              .stream()
                              .map(entry -> toDeployedMta(entry.getKey(), entry.getValue()))
                              .collect(Collectors.toList());
    }

    private String getMtaId(CloudEntity entity) {
        return mtaMetadataParser.parseMtaMetadata(entity)
                                .getId();
    }

    private DeployedMta toDeployedMta(String mtaId, List<CloudEntity> entities) {
        Version mtaVersion = getMtaVersion(entities);
        MtaMetadata mtaMetadata = getMtaMetadata(mtaId, mtaVersion);
        List<DeployedMtaApplication> applications = getApplications(entities);
        List<DeployedMtaService> services = getServices(entities);
        List<DeployedMtaService> userProvidedServices = getUserProvidedServices(applications, services);
        return ImmutableDeployedMta.builder()
                                   .metadata(mtaMetadata)
                                   .applications(applications)
                                   .services(ListUtils.union(services, userProvidedServices))
                                   .build();
    }

    private Version getMtaVersion(List<CloudEntity> entities) {
        Version currentVersion = null;
        for (CloudEntity entity : entities) {
            Version version = mtaMetadataParser.parseMtaMetadata(entity)
                                               .getVersion();
            if (currentVersion != null && !currentVersion.equals(version)) {
                currentVersion = null;
                break;
            }
            currentVersion = version;
        }
        return currentVersion;
    }

    private MtaMetadata getMtaMetadata(String mtaId, Version version) {
        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .version(version)
                                   .build();
    }

    private List<DeployedMtaApplication> getApplications(List<CloudEntity> entities) {
        return entities.stream()
                       .filter(CloudApplication.class::isInstance)
                       .map(CloudApplication.class::cast)
                       .map(mtaMetadataParser::parseDeployedMtaApplication)
                       .collect(Collectors.toList());
    }

    private List<DeployedMtaService> getServices(List<CloudEntity> entities) {
        return entities.stream()
                       .filter(CloudService.class::isInstance)
                       .map(CloudService.class::cast)
                       .map(mtaMetadataParser::parseDeployedMtaService)
                       .collect(Collectors.toList());
    }

    private List<DeployedMtaService> getUserProvidedServices(List<DeployedMtaApplication> applications,
                                                             List<DeployedMtaService> existingServices) {
        List<String> existingServiceNames = existingServices.stream()
                                                            .map(CloudService::getName)
                                                            .collect(Collectors.toList());
        return applications.stream()
                           .flatMap(application -> application.getBoundMtaServices()
                                                              .stream())
                           .filter(serviceName -> !existingServiceNames.contains(serviceName))
                           .map(this::buildUserProvidedService)
                           .collect(Collectors.toList());
    }

    private DeployedMtaService buildUserProvidedService(String serviceName) {
        return ImmutableDeployedMtaService.builder()
                                          .name(serviceName)
                                          .build();
    }

}
