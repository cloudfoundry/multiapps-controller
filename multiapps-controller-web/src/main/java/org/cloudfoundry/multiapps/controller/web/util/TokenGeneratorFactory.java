package org.cloudfoundry.multiapps.controller.web.util;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

@Named
public class TokenGeneratorFactory {

    static final String BASIC_TOKEN_TYPE = "basic";
    static final String BEARER_TOKEN_TYPE = "bearer";

    private final AccessTokenService accessTokenService;
    private final ApplicationConfiguration applicationConfiguration;
    private final TokenParserChain tokenParserChain;
    private final TokenReuser tokenReuser;

    public TokenGeneratorFactory(AccessTokenService accessTokenService, ApplicationConfiguration applicationConfiguration,
                                 TokenParserChain tokenParserChain, TokenReuser tokenReuser) {
        this.accessTokenService = accessTokenService;
        this.applicationConfiguration = applicationConfiguration;
        this.tokenParserChain = tokenParserChain;
        this.tokenReuser = tokenReuser;
    }

    public TokenGenerator createGenerator(String tokenType) {
        if (BASIC_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            return new BasicTokenGenerator(accessTokenService, applicationConfiguration, tokenReuser, tokenParserChain);
        } else if (BEARER_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            return new OauthTokenGenerator(accessTokenService, tokenParserChain, tokenReuser);
        }
        throw new InternalAuthenticationServiceException(MessageFormat.format(Messages.UNSUPPORTED_TOKEN_TYPE, tokenType));
    }

}
