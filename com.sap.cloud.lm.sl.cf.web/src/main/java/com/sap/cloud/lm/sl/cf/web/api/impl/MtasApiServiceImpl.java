package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedMtaDetector;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.api.MtasApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableMetadata;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableModule;
import com.sap.cloud.lm.sl.cf.web.api.model.ImmutableMta;
import com.sap.cloud.lm.sl.cf.web.api.model.Metadata;
import com.sap.cloud.lm.sl.cf.web.api.model.Module;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
public class MtasApiServiceImpl implements MtasApiService {

    @Inject
    private CloudControllerClientProvider clientProvider;

    @Inject
    private DeployedMtaDetector deployedMtaDetector;

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid) {
        List<DeployedMta> deployedMtas = deployedMtaDetector.detectDeployedMtas(getCloudFoundryClient(spaceGuid));
        List<Mta> mtas = getMtas(deployedMtas);
        return ResponseEntity.ok()
                             .body(mtas);
    }

    @Override
    public ResponseEntity<Mta> getMta(String spaceGuid, String mtaId) {
        Optional<DeployedMta> optionalDeployedMta = deployedMtaDetector.detectDeployedMta(mtaId, getCloudFoundryClient(spaceGuid));
        DeployedMta deployedMta = optionalDeployedMta.orElseThrow(() -> new NotFoundException(Messages.MTA_NOT_FOUND, mtaId));
        return ResponseEntity.ok()
                             .body(getMta(deployedMta));
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
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
                              .uris(deployedMtaApplication.getUris())
                              .services(deployedMtaApplication.getBoundMtaServices())
                              .build();
    }

    private Metadata getMetadata(MtaMetadata metadata) {
        return ImmutableMetadata.builder()
                                .id(metadata.getId())
                                .version(metadata.getVersion()
                                                 .toString())
                                .build();
    }

}
