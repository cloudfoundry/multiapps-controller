package com.sap.cloud.lm.sl.cf.core.security.token;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;

public class JwtTokenParserTest {

    private JwtTokenParser parser = new JwtTokenParser(new TokenFactory());

    @Test
    public void testParseNotCorrectJwtTokenWithOnlyTwoFiledsPresented() {
        String notCorrectJwtToken = "test.test";
        OAuth2AccessToken token = parser.parse(notCorrectJwtToken);
        Assert.assertNull(token);
    }

    @Test
    public void testParseJwtTokenWithNoExpirationTime() {
        String testNotCorrectToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImtleS0xIiwidHlwIjoiSldUIn0.ew0KICAgICJpc3MiOiAiT25saW5lIEpXVCBCdWlsZGVyIiwNCiAgICAiaWF0IjogMTUyNTMzNjQ3OSwNCiAgICAiZXhwIjogbnVsbCwNCiAgICAiYXVkIjogInd3dy5leGFtcGxlLmNvbSIsDQogICAgInN1YiI6ICJqcm9ja2V0QGV4YW1wbGUuY29tIiwNCiAgICAidXNlcl9uYW1lIjogInRlc3QiLA0KICAgICJlbWFpbCI6ICJ0ZXN0X2VtYWlsIiwNCiAgICAiY2xpZW50X2lkIjogInRlc3QtY2xpZW50LWlkIg0KfQ==.mWp4NUYyfHwuncjmgmnVEzMkttUSN0H4HV5WQJ1zgQSOtYS51_OmiXPRQfjy4tzIWkc71uwLjAVPeGTgh6Fh9P5L8didVTxdYD9cEHz0iXOkKZZlVTgf91lRFKa2u4aNMsWtPG2AtsxlD1cSjAkSa3Q2aVZ83gWwRmQKo4jXxuy08AXdEW9ooalR767V-C3F_MfkBi0mxW4bJrmurM8UiLouBoX71sXjzCy52aFumvwl5BEyRvKnN_5NTsJZmBHC28UysHgENzhKA651azbc3oDpbEGWe3fkafNbdQelsb6p9Tgbbcy2iPYvhrwOc9kfARAcbVMt-npmq0Aww02hvQ";
        OAuth2AccessToken token = parser.parse(testNotCorrectToken);
        Assert.assertNull(token);
    }

    @Test
    public void testParseJwtTokenWithCorrectedValues() {
        String correctToken = "eeyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkY2NlNmE0MDc5ZmI0MzhhYTRiMTEwMDU5Y2I1MDU5OSIsInN1YiI6ImYwNjlhODA5LTExMDctNDRjNC1iNzQyLWI0NTNmYjYzYjFkNyIsInNjb3BlIjpbImNsb3VkX2NvbnRyb2xsZXIucmVhZCIsInBhc3N3b3JkLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiJmMDY5YTgwOS0xMTA3LTQ0YzQtYjc0Mi1iNDUzZmI2M2IxZDciLCJvcmlnaW4iOiJsZGFwIiwidXNlcl9uYW1lIjoidGVzdF90ZXN0IiwiZW1haWwiOiJ0ZXN0X3Rlc3QiLCJyZXZfc2lnIjoiMzY1MjU5YzQiLCJpYXQiOjE1MjUzMzA4NTYsImV4cCI6MTUyNTM0MDczMiwiaXNzIjoiaHR0cHM6Ly91YWEuY2Yuc2FwLmhhbmEub25kZW1hbmQuY29tL29hdXRoL3Rva2VuIiwiemlkIjoidWFhIiwiYXVkIjpbImNsb3VkX2NvbnRyb2xsZXIiLCJwYXNzd29yZCIsImNmIiwidWFhIiwib3BlbmlkIl19.OvQpaeituiCEUK-MoSzO43datlGWX95bywpTICxxN-8";
        OAuth2AccessToken token = parser.parse(correctToken);
        Assert.assertNotNull(token);
        Assert.assertTrue(!token.getAdditionalInformation()
            .isEmpty());
        Assert.assertEquals("cf", token.getAdditionalInformation()
            .get("client_id"));
        Assert.assertNotNull(token.getAdditionalInformation()
            .get("exp"));
        Assert.assertNotNull(token.getAdditionalInformation()
            .get("scope"));
        Assert.assertEquals("test_test", token.getAdditionalInformation()
            .get("user_name"));
    }
}
