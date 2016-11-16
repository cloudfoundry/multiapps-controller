package com.sap.cloud.lm.sl.cf.web.resources;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.util.AuthorizationUtil;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.resources.ActivitiSlmpResource;
import com.sap.cloud.lm.sl.slp.resources.Configuration;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Path("{org}/{space}/slp")
public class CFActivitiSlmpResource extends ActivitiSlmpResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFActivitiSlmpResource.class);

    @PathParam("org")
    private String organization;

    @PathParam("space")
    private String space;

    @Inject
    private CloudFoundryClientProvider clientProvider;

    @Context
    private HttpServletRequest request;

    @Context
    private SecurityContext securityContext;

    @Context
    private ResourceContext resourceContext;

    @Override
    protected boolean isServiceAccessibleForCurrentUser(ServiceMetadata serviceMetadata) {
        // TODO
        return true;
    }

    @Override
    protected String getAuthenticatedUser() {
        String user = null;
        if (securityContext.getUserPrincipal() != null) {
            user = securityContext.getUserPrincipal().getName();
        } else {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        LOGGER.debug("Authenticated user is: " + user);
        return user;
    }

    @Override
    protected void ensureUserIsAuthorized(ServiceMetadata serviceMetadata, String action) {
        AuthorizationChecker.ensureUserIsAuthorized(request, clientProvider, SecurityContextUtil.getUserInfo(), organization, space,
            action);
    }

    @Override
    protected void auditLogAboutToPerformAction(String action, String serviceId) {
        Map<String, Object> params = new HashMap<>();
        params.put("Service ID", serviceId);
        AuditLoggingProvider.getFacade().logAboutToStart(action, params);
    }

    @Override
    protected void auditLogActionPerformed(String action, String serviceId, boolean success) {
        AuditLoggingProvider.getFacade().logActionStarted(action, success);
    }

    @Override
    public CFActivitiSlppResource getProcess(String serviceId, String processId) {
        return resourceContext.getResource(CFActivitiSlppResource.class);
    }

    @Override
    protected String getSpace() throws SLException {
        return AuthorizationUtil.getSpaceId(clientProvider, SecurityContextUtil.getUserInfo(), organization, space, null);
    }

    @Override
    protected String getSpaceForProcess(String processId) throws SLException {
        return AuthorizationUtil.getProcessSpaceId(processId, clientProvider, SecurityContextUtil.getUserInfo(), organization, space);
    }

    @Override
    protected Configuration getConfiguration() {
        return ConfigurationUtil.getSlpConfiguration();
    }

    @Override
    protected Map<String, Object> getAdditionalVariablesForProcessStart() {
        Map<String, Object> vars = new HashMap<>();
        vars.put(Constants.VAR_ORG, organization);
        vars.put(Constants.VAR_SPACE, space);
        return vars;
    }

}
