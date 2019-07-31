package com.sap.cloud.lm.sl.cf.core.parser;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class IdleUriParametersParser extends UriParametersParser {

    public IdleUriParametersParser(String defaultHost, String defaultDomain, String routePath) {
        super(defaultHost, defaultDomain, SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_DOMAIN, true, routePath);
    }

    public IdleUriParametersParser(String defaultHost, String defaultDomain, String hostParameterName, String domainParameterName,
                                   String routePath) {
        super(defaultHost, defaultDomain, hostParameterName, domainParameterName, true, routePath);
    }

}
