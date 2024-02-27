package com.sap.cloud.lm.sl.cf.core.parser;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class IdleUriParametersParser extends UriParametersParser {

    public IdleUriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
                                   String routePath, boolean includeProtocol, String protocol) {
        super(portBasedRouting,
              defaultHost,
              defaultDomain,
              defaultPort,
              SupportedParameters.IDLE_HOST,
              SupportedParameters.IDLE_DOMAIN,
              SupportedParameters.IDLE_PORT,
              null,
              null,
              routePath,
              includeProtocol,
              protocol);
    }

    public IdleUriParametersParser(boolean portBasedRouting, String defaultHost, String defaultDomain, Integer defaultPort,
                                   String hostParameterName, String domainParameterName, String portParameterName, String routePath,
                                   boolean includeProtocol, String protocol) {
        super(portBasedRouting,
              defaultHost,
              defaultDomain,
              defaultPort,
              hostParameterName,
              domainParameterName,
              portParameterName,
              null,
              null,
              routePath,
              includeProtocol,
              protocol);
    }

}
