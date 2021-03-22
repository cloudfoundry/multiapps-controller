package org.cloudfoundry.multiapps.controller.web.util;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloudfoundry.client.facade.util.RestUtil;

@Named
public class TokenParsingStrategyFactory {

    private final ApplicationConfiguration applicationConfiguration;
    private final TokenParserChain tokenParserChain;
    private final RestUtil restUtil = createRestUtil();

    public TokenParsingStrategyFactory(ApplicationConfiguration applicationConfiguration, TokenParserChain tokenParserChain) {
        this.applicationConfiguration = applicationConfiguration;
        this.tokenParserChain = tokenParserChain;
    }

    public static final String BASIC_TOKEN_TYPE = "basic";
    public static final String BEARER_TOKEN_TYPE = "bearer";

    public TokenParsingStrategy createStrategy(String tokenType) {
        if (BASIC_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            return new BasicTokenParsingStrategy(applicationConfiguration, restUtil);
        } else if (BEARER_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            return new OauthTokenParsingStrategy(tokenParserChain);
        }
        throw new InternalAuthenticationServiceException(MessageFormat.format("Unsupported token type: \"{0}\".", tokenType));
    }

    protected RestUtil createRestUtil() {
        return new RestUtil();
    }
}
