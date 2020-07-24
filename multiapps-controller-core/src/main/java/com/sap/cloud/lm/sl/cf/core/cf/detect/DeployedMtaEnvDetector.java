package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.mta.model.Version;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;

/**
 * Remains solely for backwards compatibility with the 'environment' approach of detecting MTAs. Once past the deprecation period this will
 * be deleted, allowing the 'metadata' {@link com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedMtaDetector} to become the standard approach for
 * detecting MTAs.
 */
@Deprecated
@Named
public class DeployedMtaEnvDetector {

    private EnvMtaMetadataParser envMtaMetadataParser;

    @Inject
    public DeployedMtaEnvDetector(EnvMtaMetadataParser envMtaMetadataParser) {
        this.envMtaMetadataParser = envMtaMetadataParser;
    }

    public List<DeployedMta> detectDeployedMtas(CloudControllerClient client) {
        Map<String, List<CloudApplication>> applicationsByMtaId = getApplicationsWithEnvMetadata(client).stream()
                                                                                                        .collect(Collectors.groupingBy(this::getQualifiedMtaId));
        return applicationsByMtaId.entrySet()
                                  .stream()
                                  .map(entry -> toDeployedMta(entry.getValue()))
                                  .collect(Collectors.toList());
    }

    private List<CloudApplication> getApplicationsWithEnvMetadata(CloudControllerClient client) {
        return client.getApplications()
                     .stream()
                     .filter(MtaMetadataUtil::hasEnvMtaMetadata)
                     .collect(Collectors.toList());
    }

    private String getQualifiedMtaId(CloudApplication application) {
        MtaMetadata metadata = envMtaMetadataParser.parseMtaMetadata(application);

        if (StringUtils.isEmpty(metadata.getNamespace())) {
            return metadata.getId();
        }

        return metadata.getNamespace() + Constants.NAMESPACE_SEPARATOR + metadata.getId();
    }

    private DeployedMta toDeployedMta(List<CloudApplication> applications) {
        MtaMetadata mtaMetadata = getMtaMetadata(applications);
        List<DeployedMtaApplication> apps = new ArrayList<>();
        List<DeployedMtaService> services = new ArrayList<>();
        for (CloudApplication application : applications) {
            DeployedMtaApplication deployedMtaApplication = envMtaMetadataParser.parseDeployedMtaApplication(application);
            apps.add(deployedMtaApplication);
            services.addAll(getServices(deployedMtaApplication, services));
        }
        return ImmutableDeployedMta.builder()
                                   .metadata(mtaMetadata)
                                   .applications(apps)
                                   .services(services)
                                   .build();
    }

    private MtaMetadata getMtaMetadata(List<CloudApplication> applications) {
        String mtaId = null;
        String mtaNamespace = null;
        Version mtaVersion = null;

        for (CloudApplication application : applications) {
            MtaMetadata metadata = envMtaMetadataParser.parseMtaMetadata(application);
            if (mtaId == null) {
                mtaId = metadata.getId();
                mtaNamespace = metadata.getNamespace();
            }

            Version currentVersion = metadata.getVersion();
            if (mtaVersion != null && !mtaVersion.equals(currentVersion)) {
                mtaVersion = null;
                break;
            }
            mtaVersion = currentVersion;
        }

        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .namespace(mtaNamespace)
                                   .version(mtaVersion)
                                   .build();
    }

    private List<DeployedMtaService> getServices(DeployedMtaApplication deployedMtaApplication, List<DeployedMtaService> existingServices) {
        return deployedMtaApplication.getBoundMtaServices()
                                     .stream()
                                     .filter(boundService -> !containsService(existingServices, boundService))
                                     .map(this::toDeployedMtaService)
                                     .collect(Collectors.toList());
    }

    private boolean containsService(List<DeployedMtaService> services, String serviceName) {
        return services.stream()
                       .anyMatch(service -> service.getName()
                                                   .equals(serviceName));
    }

    private DeployedMtaService toDeployedMtaService(String serviceName) {
        return ImmutableDeployedMtaService.builder()
                                          .name(serviceName)
                                          .build();
    }

    public List<DeployedMta> detectDeployedMtaWithoutNamespace(String mtaId, CloudControllerClient client) {
        return detectDeployedMtas(client).stream()
                                         .filter(mta -> mtaIdMatchesAndNoNamespace(mta, mtaId))
                                         .collect(Collectors.toList());
    }

    private boolean mtaIdMatchesAndNoNamespace(DeployedMta mta, String mtaId) {
        MtaMetadata metadataFromEnv = mta.getMetadata();
        return metadataFromEnv.getId()
                              .equalsIgnoreCase(mtaId)
            && StringUtils.isEmpty(metadataFromEnv.getNamespace());
    }

    public List<DeployedMta> detectDeployedMtasWithoutNamespace(CloudControllerClient client) {
        return detectDeployedMtas(client).stream()
                                         .filter(mta -> StringUtils.isEmpty(mta.getMetadata()
                                                                               .getNamespace()))
                                         .collect(Collectors.toList());
    }
}
