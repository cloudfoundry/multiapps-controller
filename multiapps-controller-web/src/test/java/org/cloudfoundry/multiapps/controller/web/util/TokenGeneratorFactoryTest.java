package org.cloudfoundry.multiapps.controller.web.util;

import static org.cloudfoundry.multiapps.controller.web.util.TokenGeneratorFactory.BASIC_TOKEN_TYPE;
import static org.cloudfoundry.multiapps.controller.web.util.TokenGeneratorFactory.BEARER_TOKEN_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.cloudfoundry.multiapps.controller.core.security.token.parsers.TokenParserChain;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

class TokenGeneratorFactoryTest {

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private TokenParserChain tokenParserChain;
    @InjectMocks
    private TokenGeneratorFactory tokenGeneratorFactory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testCreateBasicTokenStrategy() {
        TokenGenerator tokenGenerator = tokenGeneratorFactory.createGenerator(BASIC_TOKEN_TYPE);
        assertEquals(BasicTokenGenerator.class, tokenGenerator.getClass());
    }

    @Test
    void testCreateOauthTokenStrategy() {
        TokenGenerator tokenGenerator = tokenGeneratorFactory.createGenerator(BEARER_TOKEN_TYPE);
        assertEquals(OauthTokenGenerator.class, tokenGenerator.getClass());
    }

    @Test
    void testThrowingExceptionIfInvalidTypeIsPassed() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> tokenGeneratorFactory.createGenerator("invalid"));
        assertEquals("Unsupported token type: \"invalid\".", exception.getMessage());
    }
}
