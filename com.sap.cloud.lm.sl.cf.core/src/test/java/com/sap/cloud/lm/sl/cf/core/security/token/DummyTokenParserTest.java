package com.sap.cloud.lm.sl.cf.core.security.token;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;

public class DummyTokenParserTest {

    @Test
    public void testParseDummyToken() {
        DummyTokenParser parser = new DummyTokenParser(new TokenFactory());
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
    public void testParseDummyTokenWithNoDummyTokenProvided() {
        DummyTokenParser parser = new DummyTokenParser(new TokenFactory());
        OAuth2AccessToken parsedToken = parser.parse("NOT_DUMMY_TOKEN_AT_ALL");
        Assert.assertNull(parsedToken);
    }

}
