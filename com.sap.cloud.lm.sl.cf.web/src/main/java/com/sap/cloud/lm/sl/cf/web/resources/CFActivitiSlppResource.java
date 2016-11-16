package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.util.AuthorizationUtil;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationUtil;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.activiti.ProcessLog;
import com.sap.cloud.lm.sl.slp.activiti.SlpObjectFactory;
import com.sap.cloud.lm.sl.slp.model.ServiceMetadata;
import com.sap.cloud.lm.sl.slp.resources.ActivitiSlppResource;
import com.sap.cloud.lm.sl.slp.resources.Configuration;
import com.sap.lmsl.slp.Logs;

@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CFActivitiSlppResource extends ActivitiSlppResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(CFActivitiSlppResource.class);

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
    protected void auditLogActionPerformed(String action, boolean success) {
        AuditLoggingProvider.getFacade().logActionStarted(action, success);
    }

    @Override
    protected void auditLogAboutToPerformAction(String action) {
        AuditLoggingProvider.getFacade().logAboutToStart(action);
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

    // The CTS requires a <Logs> element (note the 's') with one <Log> element inside, instead of a
    // single <Log> element as specified by the SLPP.
    @GET
    @Produces(MediaType.APPLICATION_XML)
    @Path("/" + Constants.CTS_LOGS_ENDPOINT + "{ctsLogId}")
    public Logs getLogs(@PathParam("ctsLogId") String ctsLogId) {
        ProcessLog processLog = new ProcessLog();
        processLog.setId(ctsLogId);
        return SlpObjectFactory.createLogs(processLog);
    }

}
