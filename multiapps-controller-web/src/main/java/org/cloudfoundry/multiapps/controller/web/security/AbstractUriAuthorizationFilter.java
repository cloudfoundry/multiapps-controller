package org.cloudfoundry.multiapps.controller.web.security;

import org.cloudfoundry.multiapps.controller.web.util.SecurityContextUtil;

public abstract class AbstractUriAuthorizationFilter implements UriAuthorizationFilter {

    protected String extractUserGuid() {
        return SecurityContextUtil.getUserGuid();
    }

}
