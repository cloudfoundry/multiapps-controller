package com.sap.cloud.lm.sl.cf.web.security;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResourceMetadata.RequestParameters;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

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
        String organizationName = request.getParameter(RequestParameters.ORG);
        String spaceName = request.getParameter(RequestParameters.SPACE);
        if (StringUtils.isAnyEmpty(organizationName, spaceName)) {
            throw new AuthorizationException(HttpStatus.BAD_REQUEST.value(), Messages.ORG_AND_SPACE_MUST_BE_SPECIFIED);
        }
        return new CloudTarget(organizationName, spaceName);
    }

}
