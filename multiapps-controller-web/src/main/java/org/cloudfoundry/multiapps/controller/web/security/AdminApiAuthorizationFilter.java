package org.cloudfoundry.multiapps.controller.web.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

@Named
public class AdminApiAuthorizationFilter extends SpaceGuidBasedAuthorizationFilter {

    private final ApplicationConfiguration applicationConfiguration;

    @Inject
    public AdminApiAuthorizationFilter(ApplicationConfiguration applicationConfiguration, AuthorizationChecker authorizationChecker) {
        super(authorizationChecker);
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public String getUriRegex() {
        return "/rest/admin/.*";
    }

    @Override
    protected String extractSpaceGuid(HttpServletRequest request) {
        String spaceGuid = applicationConfiguration.getSpaceGuid();
        if (StringUtils.isEmpty(spaceGuid)) {
            throw new SLException("Could not retrieve the MTA deployer's space GUID.");
        }
        return spaceGuid;
    }

}
