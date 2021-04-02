package org.cloudfoundry.multiapps.controller.web.api.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.ConflictException;
import org.cloudfoundry.multiapps.common.NotFoundException;
import org.cloudfoundry.multiapps.controller.api.MtasApiService;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableModule;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableMta;
import org.cloudfoundry.multiapps.controller.api.model.Metadata;
import org.cloudfoundry.multiapps.controller.api.model.Module;
import org.cloudfoundry.multiapps.controller.api.model.Mta;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.detect.DeployedMtaDetector;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;

@Named
public class MtasApiServiceImpl implements MtasApiService {

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    @Qualifier("deployedMtaRequiredDataOnlyDetector")
    private DeployedMtaDetector deployedMtaDetector;

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid) {
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasWithoutNamespace(getCloudFoundryClient(spaceGuid));
        List<Mta> mtas = getMtas(deployedMtas);
        return ResponseEntity.ok()
                             .body(mtas);
    }

    @Override
    public ResponseEntity<Mta> getMta(String spaceGuid, String mtaId) {
        List<DeployedMta> mtas = deployedMtaDetector.detectDeployedMtasByName(mtaId, getCloudFoundryClient(spaceGuid));

        if (mtas.isEmpty()) {
            throw new NotFoundException(Messages.MTA_NOT_FOUND, mtaId);
        }

        if (mtas.size() != 1) {
            throw new ConflictException(Messages.MTA_SEARCH_NOT_UNIQUE_BY_NAME, mtaId);
        }

        return ResponseEntity.ok()
                             .body(getMta(mtas.get(0)));
    }

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid, String namespace, String name) {

        if (name == null && namespace == null) {
            return getAllMtas(spaceGuid);
        }

        if (namespace == null) {
            return getMtasByName(spaceGuid, name);
        }

        if (name == null) {
            return getMtasByNamespace(spaceGuid, namespace);
        }

        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMtaByNameAndNamespace(name, namespace,
                                                                                                            getCloudFoundryClient(spaceGuid));
        DeployedMta deployedMta = optionalDeployedMta.orElseThrow(() -> new NotFoundException(Messages.SPECIFIC_MTA_NOT_FOUND,
                                                                                              name,
                                                                                              namespace));

        return ResponseEntity.ok()
                             .body(Arrays.asList(getMta(deployedMta)));
    }

    protected ResponseEntity<List<Mta>> getAllMtas(String spaceGuid) {
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtas(getCloudFoundryClient(spaceGuid));

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas));
    }

    protected ResponseEntity<List<Mta>> getMtasByNamespace(String spaceGuid, String namespace) {
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasByNamespace(namespace, getCloudFoundryClient(spaceGuid));

        if (deployedMtas.isEmpty()) {
            throw new NotFoundException(Messages.MTAS_NOT_FOUND_BY_NAMESPACE, namespace);
        }

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas));
    }

    protected ResponseEntity<List<Mta>> getMtasByName(String spaceGuid, String name) {
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtasByName(name, getCloudFoundryClient(spaceGuid));

        if (deployedMtas.isEmpty()) {
            throw new NotFoundException(Messages.MTAS_NOT_FOUND_BY_NAME, name);
        }

        return ResponseEntity.ok()
                             .body(getMtas(deployedMtas));
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid, null);
    }

    private List<Mta> getMtas(List<DeployedMta> deployedMtas) {
        return deployedMtas.stream()
                           .map(this::getMta)
                           .collect(Collectors.toList());
    }

    private Mta getMta(DeployedMta mta) {
        return ImmutableMta.builder()
                           .metadata(getMetadata(mta.getMetadata()))
                           .modules(getModules(mta.getApplications()))
                           .services(mta.getServices()
                                        .stream()
                                        .map(DeployedMtaService::getName)
                                        .collect(Collectors.toSet()))
                           .build();
    }

    private List<Module> getModules(List<DeployedMtaApplication> deployedApplications) {
        return deployedApplications.stream()
                                   .map(this::getModule)
                                   .collect(Collectors.toList());
    }

    private Module getModule(DeployedMtaApplication deployedMtaApplication) {
        return ImmutableModule.builder()
                              .appName(deployedMtaApplication.getName())
                              .moduleName(deployedMtaApplication.getModuleName())
                              .providedDendencyNames(deployedMtaApplication.getProvidedDependencyNames())
                              .uris(parseRoutesToUris(deployedMtaApplication.getRoutes()))
                              .services(deployedMtaApplication.getBoundMtaServices())
                              .build();
    }

    private Metadata getMetadata(MtaMetadata metadata) {
        return ImmutableMetadata.builder()
                                .id(metadata.getId())
                                .version(getVersion(metadata.getVersion()))
                                .namespace(metadata.getNamespace())
                                .build();
    }

    private List<String> parseRoutesToUris(Set<CloudRouteSummary> routes) {
        return routes.stream()
                     .map(CloudRouteSummary::toUriString)
                     .collect(Collectors.toList());
    }

    private String getVersion(Version version) {
        return version != null ? version.toString() : null;
    }

}
