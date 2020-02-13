package com.sap.cloud.lm.sl.cf.core.security.token;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.core.security.token.parsers.DummyTokenParser;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class DummyTokenParserTest {

    @Mock
    private ApplicationConfiguration applicationConfiguration;
    private DummyTokenParser parser;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        this.parser = new DummyTokenParser(new TokenFactory(), applicationConfiguration);
    }

    @Test
    public void testParseDummyToken() {
        Mockito.when(applicationConfiguration.areDummyTokensEnabled())
               .thenReturn(true);

        OAuth2AccessToken parsedToken = parser.parse("DUMMY");

        Assert.assertNotNull(parsedToken);
        Assert.assertNotNull(parsedToken.getValue());
        Assert.assertEquals("DUMMY", parsedToken.getValue());
        Assert.assertEquals("dummy", parsedToken.getAdditionalInformation()
                                                .get("user_name"));
        Assert.assertEquals("cf", parsedToken.getAdditionalInformation()
                                             .get("client_id"));
    }

    @Test
    public void testParseDummyTokenWhenDummyTokensAreDisabled() {
        Mockito.when(applicationConfiguration.areDummyTokensEnabled())
               .thenReturn(false);
        OAuth2AccessToken parsedToken = parser.parse("DUMMY");
        Assert.assertNull(parsedToken);
    }

    @Test
    public void testParseDummyTokenWhenTokenIsNotDummy() {
        Mockito.when(applicationConfiguration.areDummyTokensEnabled())
               .thenReturn(true);
        OAuth2AccessToken parsedToken = parser.parse("NOT_DUMMY_TOKEN_AT_ALL");
        Assert.assertNull(parsedToken);
    }

}
