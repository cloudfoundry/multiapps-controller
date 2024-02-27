package com.sap.cloud.lm.sl.cf.web.util;

import java.text.MessageFormat;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

@Named
@Profile("cf")
public class TokenGeneratorFactory {

    protected static final String BASIC_TOKEN_TYPE = "basic";
    protected static final String BEARER_TOKEN_TYPE = "bearer";

    protected final AccessTokenDao accessTokenDao;
    protected final ApplicationConfiguration applicationConfiguration;
    protected final TokenParserChain tokenParserChain;
    protected final TokenReuser tokenReuser;

    @Inject
    public TokenGeneratorFactory(AccessTokenDao accessTokenDao, ApplicationConfiguration applicationConfiguration,
                                 TokenParserChain tokenParserChain, TokenReuser tokenReuser) {
        this.accessTokenDao = accessTokenDao;
        this.applicationConfiguration = applicationConfiguration;
        this.tokenParserChain = tokenParserChain;
        this.tokenReuser = tokenReuser;
    }

    public TokenGenerator createGenerator(String tokenType) {
        if (BEARER_TOKEN_TYPE.equalsIgnoreCase(tokenType)) {
            return new OauthTokenGenerator(accessTokenDao, tokenParserChain, tokenReuser);
        }
        throw new InternalAuthenticationServiceException(MessageFormat.format(Messages.UNSUPPORTED_TOKEN_TYPE, tokenType));
    }

}
