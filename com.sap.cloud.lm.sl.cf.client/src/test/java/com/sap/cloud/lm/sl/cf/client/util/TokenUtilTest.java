package com.sap.cloud.lm.sl.cf.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

@RunWith(Parameterized.class)
public class TokenUtilTest {

    private static final String userNameMock = "test-user";
    private static final String clientIdMock = "test-cliend-id";
    private static final String SAMPLE_TOKEN = "bearer eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI2MTkyM2FkYi1iYjViLTQ4NTktODIzNy0yM2YxNzg5ZTJmOTAiLCJzdWIiOiIxNTUxODQiLCJzY29wZSI6WyJjbG91ZF9jb250cm9sbGVyLnJlYWQiLCJjbG91ZF9jb250cm9sbGVyLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci5hZG1pbiIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIxNTUxODQiLCJ1c2VyX25hbWUiOiJYU01BU1RFUiIsImVtYWlsIjoiWFNNQVNURVJAdW5rbm93biIsImZhbWlseV9uYW1lIjoiWFNNQVNURVIiLCJpYXQiOjE0NDc3NDUzMjgsImV4cCI6MTQ0Nzc4ODUyOCwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3VhYS9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwiY2YiLCJ1YWEiXX0.P9XJTA4AV5aHS_ozw5WZXgIPc3M9Q_-1oKc1tLDEC5lkx1vNZjd5ozGaZs8UvgECJ_sTY_ZL2izDAKc3ew8hv9y6i6O3V-BxAs9pxkAIo2GPVmHzZQg8t6iG6c-iz1JnMan9nnbjFmMve5qjl9dgoCat-VaWfIW7TRagQ05dNO8DXJkQiiRioQ5kzoxQV4jUgxk5tczix-s8VQfqobW472A4t087DnaCYOOdz9MF8WLoffWRX8BkYJnBgVJ0kcWPZwMuB9BBPC4Les2NiZaKpLDahPrmp340izGg9pUhUsjPbllAph5odhMDb1Lc8_Q-yKiEt-DwZ72-VkCZE-MPjQ";

    @Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            { "", ""},
            { userNameMock, ""},
            { "", clientIdMock},
            { userNameMock, clientIdMock}
            // @formatter:on
        });
    }

    private String userName;
    private String clientId;

    public TokenUtilTest(String userName, String clientId) {
        this.userName = userName;
        this.clientId = clientId;
    }

    @Test
    public void testCreateToken() {
        OAuth2AccessToken token = null;
        if (userName.equals("") && clientId.equals("")) {
            token = TokenUtil.createToken(SAMPLE_TOKEN);
            assertToken(token, "XSMASTER", "cf", "155184");
        } else {
            token = TokenUtil.createDummyToken(userName, clientId);
            assertToken(token, userName, clientId, new UUID(0, 0).toString());
        }

        token = TokenUtil.createToken(getTestToken());
        assertTrue(token == null);
    }

    private void assertToken(OAuth2AccessToken token, String userName, String clientId, String uuid) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        assertEquals(clientId, tokenProperties.getClientId());
        assertEquals(userName, tokenProperties.getUserName());
        assertEquals(uuid, tokenProperties.getUserId());
    }

    private String getTestToken() {
        String testTokenJson = "{\"user_name\":\"test-user\"}";
        byte[] encodedData = Base64.getEncoder()
            .encode(testTokenJson.getBytes());
        return "bearer test." + new String(encodedData) + ".test";
    }
}
