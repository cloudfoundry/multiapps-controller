package org.cloudfoundry.multiapps.controller.web.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.cloudfoundry.multiapps.controller.web.resources.ConfigurationEntriesResource;

@Named
public class PurgeApiAuthorizationFilter extends SpaceNameBasedAuthorizationFilter {

    @Inject
    public PurgeApiAuthorizationFilter(AuthorizationChecker authorizationChecker) {
        super(authorizationChecker);
    }

    @Override
    public String getUriRegex() {
        return "/rest/configuration-entries/purge";
    }

    @Override
    protected CloudTarget extractTarget(HttpServletRequest request) {
        String organizationName = request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_ORGANIZATION);
        String spaceName = request.getParameter(ConfigurationEntriesResource.REQUEST_PARAM_SPACE);
        if (StringUtils.isAnyEmpty(organizationName, spaceName)) {
            throw new SLException(Messages.ORG_AND_SPACE_MUST_BE_SPECIFIED);
        }
        return new CloudTarget(organizationName, spaceName);
    }

}
