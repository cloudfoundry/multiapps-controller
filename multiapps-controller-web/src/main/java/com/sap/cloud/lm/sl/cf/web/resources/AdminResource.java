package com.sap.cloud.lm.sl.cf.web.resources;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.security.AuthorizationChecker;
import com.sap.cloud.lm.sl.cf.web.util.SecurityContextUtil;

@Path("/admin")
@Component
public class AdminResource {

    @Inject
    private ApplicationShutdownResource appShutdownResource;

    @Inject
    private AuthorizationChecker authorizationChecker;

    @Inject
    private ApplicationConfiguration appConfiguration;

    @Context
    private HttpServletRequest request;

    @Path("/shutdown")
    public ApplicationShutdownResource getApplicationShutdownResource() {
        ensureUserIsAuthorized(ApplicationShutdownResource.ACTION);
        return appShutdownResource;
    }

    private void ensureUserIsAuthorized(String resourceAction) {
        authorizationChecker.ensureUserIsAuthorized(request, SecurityContextUtil.getUserInfo(), appConfiguration.getSpaceId(),
                                                    resourceAction);
    }

}
