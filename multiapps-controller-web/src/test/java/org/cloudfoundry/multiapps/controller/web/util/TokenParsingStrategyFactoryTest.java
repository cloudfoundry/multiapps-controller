package org.cloudfoundry.multiapps.controller.web.util;

import static org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategyFactory.BASIC_TOKEN_TYPE;
import static org.cloudfoundry.multiapps.controller.web.util.TokenParsingStrategyFactory.BEARER_TOKEN_TYPE;
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

class TokenParsingStrategyFactoryTest {

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock
    private TokenParserChain tokenParserChain;
    @InjectMocks
    private TokenParsingStrategyFactory tokenParsingStrategyFactory;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testCreateBasicTokenStrategy() {
        TokenParsingStrategy tokenParsingStrategy = tokenParsingStrategyFactory.createStrategy(BASIC_TOKEN_TYPE);
        assertEquals(BasicTokenParsingStrategy.class, tokenParsingStrategy.getClass());
    }

    @Test
    void testCreateOauthTokenStrategy() {
        TokenParsingStrategy tokenParsingStrategy = tokenParsingStrategyFactory.createStrategy(BEARER_TOKEN_TYPE);
        assertEquals(OauthTokenParsingStrategy.class, tokenParsingStrategy.getClass());
    }

    @Test
    void testThrowingExceptionIfInvalidTypeIsPassed() {
        Exception exception = assertThrows(InternalAuthenticationServiceException.class,
                                           () -> tokenParsingStrategyFactory.createStrategy("invalid"));
        assertEquals("Unsupported token type: \"invalid\".", exception.getMessage());
    }
}
