package com.sap.cloud.lm.sl.cf.core.parser;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class IdleUriParametersParser extends UriParametersParser {

    public IdleUriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String routePath, boolean includeProtocol, String protocol) {
        super(portBasedRouting, defaultHost, defaultDomain, defaultPort, SupportedParameters.IDLE_HOST, SupportedParameters.IDLE_DOMAIN,
            SupportedParameters.IDLE_PORT, routePath, includeProtocol, protocol);
    }
}
