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
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.api.MtasApiService;
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
        List<CloudApplication> applications = getCloudFoundryClient(spaceGuid).getApplications(false);
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
        Mta result = new Mta();
        result.setMetadata(getMetadata(mta.getMetadata()));
        result.setModules(getModules(mta.getModules()));
        result.setServices(mta.getServices());
        return result;
    }

    private List<Module> getModules(List<DeployedMtaModule> modules) {
        return modules.stream()
                      .map(this::getModule)
                      .collect(Collectors.toList());
    }

    private Module getModule(DeployedMtaModule module) {
        Module result = new Module();
        result.setAppName(module.getAppName());
        result.setModuleName(module.getModuleName());
        result.setProvidedDendencyNames(module.getProvidedDependencyNames());
        result.setUris(module.getUris());
        result.setServices(module.getServices());
        return result;
    }

    private Metadata getMetadata(DeployedMtaMetadata metadata) {
        Metadata result = new Metadata();
        result.setId(metadata.getId());
        result.setVersion(metadata.getVersion()
                                  .toString());
        return result;
    }

}
