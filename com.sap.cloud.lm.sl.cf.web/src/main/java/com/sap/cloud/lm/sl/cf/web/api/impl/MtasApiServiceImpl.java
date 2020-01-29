package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.http.ResponseEntity;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
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

    @Override
    public ResponseEntity<List<Mta>> getMtas(String spaceGuid) {
        DeployedComponents deployedComponents = detectDeployedComponents(spaceGuid);
        return ResponseEntity.ok()
                             .body(getMtas(deployedComponents));
    }

    @Override
    public ResponseEntity<Mta> getMta(String spaceGuid, String mtaId) {
        DeployedMta mta = detectDeployedComponents(spaceGuid).findDeployedMta(mtaId);
        if (mta == null) {
            throw new NotFoundException(Messages.MTA_NOT_FOUND, mtaId);
        }
        return ResponseEntity.ok()
                             .body(getMta(mta));
    }

    private DeployedComponents detectDeployedComponents(String spaceGuid) {
        List<CloudApplication> applications = getCloudFoundryClient(spaceGuid).getApplications();
        return new DeployedComponentsDetector().detectAllDeployedComponents(applications);
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
    }

    private List<Mta> getMtas(DeployedComponents components) {
        return components.getMtas()
                         .stream()
                         .map(this::getMta)
                         .collect(Collectors.toList());
    }

    private Mta getMta(DeployedMta mta) {
        return ImmutableMta.builder()
                           .metadata(getMetadata(mta.getMetadata()))
                           .modules(getModules(mta.getApplications()))
                           .services(mta.getServices())
                           .build();
    }

    private List<Module> getModules(List<DeployedMtaApplication> deployedApplications) {
        return deployedApplications.stream()
                      .map(this::getModule)
                      .collect(Collectors.toList());
    }

    private Module getModule(DeployedMtaApplication deployedApplication) {
        return ImmutableModule.builder()
                              .appName(deployedApplication.getAppName())
                              .moduleName(deployedApplication.getModuleName())
                              .providedDendencyNames(deployedApplication.getProvidedDependencyNames())
                              .uris(deployedApplication.getUris())
                              .services(deployedApplication.getServices())
                              .build();
    }

    private Metadata getMetadata(DeployedMtaMetadata metadata) {
        return ImmutableMetadata.builder()
                                .id(metadata.getId())
                                .version(metadata.getVersion()
                                                 .toString())
                                .build();
    }

}
