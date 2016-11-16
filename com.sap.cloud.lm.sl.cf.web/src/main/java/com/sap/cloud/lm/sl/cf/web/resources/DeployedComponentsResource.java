package com.sap.cloud.lm.sl.cf.web.resources;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.detect.DeployedComponentsDetector;
import com.sap.cloud.lm.sl.cf.core.model.DeployedComponents;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.NotFoundException;
import com.sap.cloud.lm.sl.common.SLException;

@Component
@Path("{org}/{space}/components")
@Produces(MediaType.APPLICATION_XML)
public class DeployedComponentsResource {

    private static final String ACTION = "Get deployed components";

    @PathParam("org")
    private String organization;

    @PathParam("space")
    private String space;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    private HttpServletRequest request;

    @GET
    public DeployedComponents getDeployedComponents() throws SLException {
        AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, SecurityContextUtil.getUserInfo(), organization, space,
            ACTION);
        List<CloudApplication> applications = getCloudFoundryClient().getApplications();
        return new DeployedComponentsDetector().detectAllDeployedComponents(applications);
    }

    @GET
    @Path("{mtaId}")
    public DeployedMta getDeployedMTA(@PathParam("mtaId") final String mtaId) throws SLException {
        DeployedMta mta = getDeployedComponents().findDeployedMta(mtaId);
        if (mta == null) {
            throw new NotFoundException(Messages.MTA_NOT_FOUND, mtaId);
        }
        return mta;
    }

    private CloudFoundryOperations getCloudFoundryClient() throws SLException {
        return clientProvider.getCloudFoundryClient(SecurityContextUtil.getUserInfo().getToken(), organization, space, null);
    }

}
