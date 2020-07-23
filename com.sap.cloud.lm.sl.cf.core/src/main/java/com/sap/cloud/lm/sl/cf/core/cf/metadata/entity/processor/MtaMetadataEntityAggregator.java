package com.sap.cloud.lm.sl.cf.core.cf.metadata.entity.processor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.mta.model.Version;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;

@Named
public class MtaMetadataEntityAggregator {

    private MtaMetadataParser mtaMetadataParser;

    @Inject
    public MtaMetadataEntityAggregator(MtaMetadataParser mtaMetadataParser) {
        this.mtaMetadataParser = mtaMetadataParser;
    }

    public List<DeployedMta> aggregate(List<CloudEntity> entities) {
        Map<String, List<CloudEntity>> entitiesByMtaId = entities.stream()
                                                                 .collect(Collectors.groupingBy(mtaMetadataParser::parseQualifiedMtaId));
        return entitiesByMtaId.entrySet()
                              .stream()
                              .map(entry -> toDeployedMta(entry.getValue()))
                              .collect(Collectors.toList());
    }

    private DeployedMta toDeployedMta(List<CloudEntity> entities) {
        MtaMetadata mtaMetadata = aggregateMetadata(entities);
        List<DeployedMtaApplication> applications = getApplications(entities);
        List<DeployedMtaService> services = getServices(entities);
        List<DeployedMtaService> userProvidedServices = getUserProvidedServices(applications, services);

        return ImmutableDeployedMta.builder()
                                   .metadata(mtaMetadata)
                                   .applications(applications)
                                   .services(ListUtils.union(services, userProvidedServices))
                                   .build();
    }

    public MtaMetadata aggregateMetadata(List<CloudEntity> entities) {
        String mtaId = null;
        String mtaNamespace = null;
        Version currentVersion = null;

        for (CloudEntity entity : entities) {
            MtaMetadata entityMetadata = mtaMetadataParser.parseMtaMetadata(entity);

            if (mtaId == null) {
                mtaId = entityMetadata.getId();
                mtaNamespace = entityMetadata.getNamespace();
            }

            Version version = entityMetadata.getVersion();
            if (currentVersion == null) {
                currentVersion = version;
            } else if (!currentVersion.equals(version)) {
                currentVersion = null;
                break;
            }
        }

        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .namespace(mtaNamespace)
                                   .version(currentVersion)
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
                       .filter(CloudServiceInstance.class::isInstance)
                       .map(CloudServiceInstance.class::cast)
                       .map(mtaMetadataParser::parseDeployedMtaService)
                       .collect(Collectors.toList());
    }

    private List<DeployedMtaService> getUserProvidedServices(List<DeployedMtaApplication> applications,
                                                             List<DeployedMtaService> existingServices) {
        Set<String> existingServiceNames = existingServices.stream()
                                                           .map(CloudServiceInstance::getName)
                                                           .collect(Collectors.toSet());
        return applications.stream()
                           .map(DeployedMtaApplication::getBoundMtaServices)
                           .flatMap(List::stream)
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
