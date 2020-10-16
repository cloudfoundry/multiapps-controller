package org.cloudfoundry.multiapps.controller.core.cf.metadata.entity.processor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.mta.model.Version;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

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
        return entitiesByMtaId.values()
                              .stream()
                              .map(this::toDeployedMta)
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
