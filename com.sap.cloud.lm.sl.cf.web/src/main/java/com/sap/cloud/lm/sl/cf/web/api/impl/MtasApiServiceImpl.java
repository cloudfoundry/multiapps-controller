package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.cf.web.api.MtasApiService;
import com.sap.cloud.lm.sl.cf.web.api.model.Metadata;
import com.sap.cloud.lm.sl.cf.web.api.model.Module;
import com.sap.cloud.lm.sl.cf.web.api.model.Mta;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@RequestScoped
@Component
public class MtasApiServiceImpl implements MtasApiService {

    private static final String ACTION = "Get deployed components";

    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private AuthorizationChecker authorizationChecker;

    @Autowired
    private DeployedComponentsDetector deployedComponentsDetector;

    public Response getMta(String mtaId, SecurityContext securityContext, String spaceGuid, HttpServletRequest request) {
        authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, ACTION);
        Optional<DeployedMta> optionalDeployedMta = deployedComponentsDetector.getDeployedMta(mtaId, getCloudFoundryClient(spaceGuid));
        DeployedMta deployedMta = optionalDeployedMta.orElseThrow(() -> new NotFoundException(Messages.MTA_NOT_FOUND, mtaId));
        return Response.ok()
                       .entity(getMta(deployedMta))
                       .build();
    }

    public Response getMtas(SecurityContext securityContext, String spaceGuid, HttpServletRequest request) {
        authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, ACTION);
        Optional<List<DeployedMta>> deployedMtas = deployedComponentsDetector.getAllDeployedMta(getCloudFoundryClient(spaceGuid));
        List<Mta> mtas = getMtas(deployedMtas.orElseGet(() -> Collections.emptyList()));
        return Response.ok()
                       .entity(mtas)
                       .build();
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
    }

    private List<Mta> getMtas(List<DeployedMta> deployedMtas) {
        List<Mta> mtas = new ArrayList<>();
        for (DeployedMta mta : deployedMtas) {
            mtas.add(getMta(mta));
        }

        return mtas;
    }

    private Mta getMta(DeployedMta mta) {
        Mta result = new Mta();
        result.setMetadata(getMetadata(mta.getMetadata()));
        result.setModules(getModules(mta.getModules()));
        result.setServices(mta.getServices()
                              .stream()
                              .map(s -> s.getServiceName())
                              .collect(Collectors.toSet()));
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
        result.setServices(module.getServices()
                                 .stream()
                                 .map(s -> s.getServiceName())
                                 .collect(Collectors.toList()));
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
