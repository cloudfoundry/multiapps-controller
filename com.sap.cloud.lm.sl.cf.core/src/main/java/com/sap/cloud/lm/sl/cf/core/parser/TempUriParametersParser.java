package com.sap.cloud.lm.sl.cf.core.parser;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class TempUriParametersParser extends UriParametersParser {

    public TempUriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String routePath) {
        super(portBasedRouting, defaultHost, defaultDomain, defaultPort, SupportedParameters.TEMP_HOST, SupportedParameters.TEMP_DOMAIN,
            SupportedParameters.TEMP_PORT, routePath);
    }

    public TempUriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
        String hostParameterName, String domainParameterName, String portParameterName, String routePath) {
        super(portBasedRouting, defaultHost, defaultDomain, defaultPort, hostParameterName, domainParameterName, portParameterName,
            routePath);
    }

}
