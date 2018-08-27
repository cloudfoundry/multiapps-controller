package com.sap.cloud.lm.sl.cf.web.api.impl;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.springframework.stereotype.Component;

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
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.NotFoundException;

@RequestScoped
@Component
public class MtasApiServiceImpl implements MtasApiService {

    private static final String ACTION = "Get deployed components";

    @Inject
    private CloudControllerClientProvider clientProvider;
    @Inject
    private AuthorizationChecker authorizationChecker;

    public Response getMta(String mtaId, SecurityContext securityContext, String spaceGuid, HttpServletRequest request) {
        authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, ACTION);
        DeployedMta mta = detectDeployedComponents(spaceGuid).findDeployedMta(mtaId);
        if (mta == null) {
            throw new NotFoundException(Messages.MTA_NOT_FOUND, mtaId);
        }
        return Response.ok()
            .entity(getMta(mta))
            .build();
    }

    public Response getMtas(SecurityContext securityContext, String spaceGuid, HttpServletRequest request) {
        authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), spaceGuid, ACTION);
        DeployedComponents deployedComponents = detectDeployedComponents(spaceGuid);
        return Response.ok()
            .entity(getMtas(deployedComponents))
            .build();

    }

    private DeployedComponents detectDeployedComponents(String spaceGuid) {
        List<CloudApplication> applications = getCloudFoundryClient(spaceGuid).getApplications("0");
        return new DeployedComponentsDetector().detectAllDeployedComponents(applications);
    }

    private CloudControllerClient getCloudFoundryClient(String spaceGuid) {
        UserInfo userInfo = SecurityContextUtil.getUserInfo();
        return clientProvider.getControllerClient(userInfo.getName(), spaceGuid);
    }

    private List<Mta> getMtas(DeployedComponents components) {
        List<Mta> mtas = new ArrayList<>();
        List<DeployedMta> deployedMtas = components.getMtas();
        for (DeployedMta mta : deployedMtas) {
            mtas.add(getMta(mta));
        }

        return mtas;
    }

    private Mta getMta(DeployedMta mta) {
        Mta result = new Mta();
        result.setMetadata(getMetadata(mta.getMetadata()));
        result.setModules(getModules(mta.getModules()));
        result.setServices(mta.getServices());
        return result;
    }

    private List<Module> getModules(List<DeployedMtaModule> modules) {
        List<Module> result = new ArrayList<>();
        for (DeployedMtaModule module : modules) {
            result.add(getModule(module));
        }
        return result;
    }

    private Module getModule(DeployedMtaModule module) {
        Module result = new Module();
        result.setAppName(module.getAppName());
        result.setDeployAttributes(module.getDeployAttributes());
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
