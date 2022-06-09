package com.sap.cloud.lm.sl.cf.client.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

@RunWith(Enclosed.class)
public class TokenFactoryTest {

    private static void validateToken(OAuth2AccessTokenWithAdditionalInfo token, TokenProperties expectedTokenProperties) {
        TokenProperties tokenProperties = TokenProperties.fromToken(token);
        assertEquals(expectedTokenProperties.getClientId(), tokenProperties.getClientId());
        assertEquals(expectedTokenProperties.getUserName(), tokenProperties.getUserName());
        assertEquals(expectedTokenProperties.getUserId(), tokenProperties.getUserId());
    }

    private static void validateTokenAdditionalInfo(OAuth2AccessTokenWithAdditionalInfo token, Map<String, Object> tokenInfo) {
        Map<String, Object> tokenAdditionalInformation = token.getAdditionalInfo();
        assertEquals(tokenInfo, tokenAdditionalInformation);
    }

    @RunWith(Parameterized.class)
    public static class OauthTokenFactoryTest {

        private final String tokenString;
        private final TokenProperties expectedTokenProperties;
        private final Exception expectedExcetion;

        private TokenFactory tokenFactory = new TokenFactory();

        public OauthTokenFactoryTest(String tokenString, TokenProperties expectedTokenProperties, Exception expectedException) {
            this.tokenString = tokenString;
            this.expectedTokenProperties = expectedTokenProperties;
            this.expectedExcetion = expectedException;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0) Valid token:
                {
                    "eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI2MTkyM2FkYi1iYjViLTQ4NTktODIzNy0yM2YxNzg5ZTJmOTAiLCJzdWIiOiIxNTUxODQiLCJzY29wZSI6WyJjbG91ZF9jb250cm9sbGVyLnJlYWQiLCJjbG91ZF9jb250cm9sbGVyLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci5hZG1pbiIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIxNTUxODQiLCJ1c2VyX25hbWUiOiJYU01BU1RFUiIsImVtYWlsIjoiWFNNQVNURVJAdW5rbm93biIsImZhbWlseV9uYW1lIjoiWFNNQVNURVIiLCJpYXQiOjE0NDc3NDUzMjgsImV4cCI6MTQ0Nzc4ODUyOCwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3VhYS9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwiY2YiLCJ1YWEiXX0.P9XJTA4AV5aHS_ozw5WZXgIPc3M9Q_-1oKc1tLDEC5lkx1vNZjd5ozGaZs8UvgECJ_sTY_ZL2izDAKc3ew8hv9y6i6O3V-BxAs9pxkAIo2GPVmHzZQg8t6iG6c-iz1JnMan9nnbjFmMve5qjl9dgoCat-VaWfIW7TRagQ05dNO8DXJkQiiRioQ5kzoxQV4jUgxk5tczix-s8VQfqobW472A4t087DnaCYOOdz9MF8WLoffWRX8BkYJnBgVJ0kcWPZwMuB9BBPC4Les2NiZaKpLDahPrmp340izGg9pUhUsjPbllAph5odhMDb1Lc8_Q-yKiEt-DwZ72-VkCZE-MPjQ",
                    new TokenProperties("cf", "155184", "XSMASTER"), null,
                },
                // (1) Valid token, the body of which cannot be parsed with the standard base 64 decoder (a base 64 URL decoder must be used for this one):
                {
                    "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIzZmEyMGRjMzAwY2Y0ZDg2ODU4NWVjYzFhZGQyOWJlNyIsImV4dF9hdHRyIjp7ImVuaGFuY2VyIjoiWFNVQUEifSwic3ViIjoiMTQxMzIxMzI3Iiwic2NvcGUiOlsiY2xvdWRfY29udHJvbGxlci5yZWFkIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsInhzX3VzZXIucmVhZCIsImNsb3VkX2NvbnRyb2xsZXIuYWRtaW4iLCJ1YWEudXNlciJdLCJjbGllbnRfaWQiOiJjZiIsImNpZCI6ImNmIiwiYXpwIjoiY2YiLCJyZXZvY2FibGUiOnRydWUsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIxNDEzMjEzMjciLCJvcmlnaW4iOiJ1YWEiLCJ1c2VyX25hbWUiOiJYU0FfQURNSU4iLCJlbWFpbCI6IlhTQV9BRE1JTkB1bmtub3duIiwicmV2X3NpZyI6ImU2MDQyZGFhIiwiaWF0IjoxNTIxMzgwNjI1LCJleHAiOjE1MjE0MjM4MjUsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC91YWEvb2F1dGgvdG9rZW4iLCJ6aWQiOiJ1YWEiLCJoZGIubmFtZWR1c2VyLnNhbWwiOiI8P3htbCB2ZXJzaW9uPVwiMS4wXCIgZW5jb2Rpbmc9XCJVVEYtOFwiPz48c2FtbDI6QXNzZXJ0aW9uIHhtbG5zOnNhbWwyPVwidXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvblwiIElEPVwiX2Q5YjgxNWQ1LTNlMTYtNDU1Mi1iMjQwLTAyNmE2OGUwZTMzMlwiIElzc3VlSW5zdGFudD1cIjIwMTgtMDMtMTVUMTY6MTE6MjkuNzQxWlwiIFZlcnNpb249XCIyLjBcIj48c2FtbDI6SXNzdWVyPkNBRC1zYW1sPC9zYW1sMjpJc3N1ZXI-PGRzOlNpZ25hdHVyZSB4bWxuczpkcz1cImh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNcIj48ZHM6U2lnbmVkSW5mbz48ZHM6Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biNcIi8-PGRzOlNpZ25hdHVyZU1ldGhvZCBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTFcIi8-PGRzOlJlZmVyZW5jZSBVUkk9XCIjX2Q5YjgxNWQ1LTNlMTYtNDU1Mi1iMjQwLTAyNmE2OGUwZTMzMlwiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPVwiaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmVcIi8-PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biNcIi8-PC9kczpUcmFuc2Zvcm1zPjxkczpEaWdlc3RNZXRob2QgQWxnb3JpdGhtPVwiaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTFcIi8-PGRzOkRpZ2VzdFZhbHVlPlFKVG1obnJXMDY3Z3VDbWJmMTdXc1lYaCtKUT08L2RzOkRpZ2VzdFZhbHVlPjwvZHM6UmVmZXJlbmNlPjwvZHM6U2lnbmVkSW5mbz48ZHM6U2lnbmF0dXJlVmFsdWU-TzhxaDZZaWluUkhldUZkZk1qVkg0VVBlTzc5MWs2bmRrSU9wN2JQRkhnQVB0MWVpeGFGN2dTYnFMV2FmV2d5OHIrSUhlZTZmZXZjQkdMSzRMUGxPSjRmcTR3NFNsWVlzVDZBWGRKMyswZUxEQnR2QjRYN3B0MDRYMVR2UThUelVWSlovQmdLQ2huSEl4RlRocGJ6VXRINmpzQ2d1anoyaW1yenB0S083Z0JnRWplNUF6Lzg4OUJCZ29mZTRiTytJRk9hU3h2VnlKZWZJTDZSS1AzNVd6MFpOdzBqdEVNemtScEh3eFpKcVJtRjJ3Ym11YUk2ZDBNMXVVR09MVjhzUVkvdjZLbUw4U0JjZmZNZ2hDaUNFZDd6VHZqa2ZnVzBDU2xZVzFSV0ZvOURLaUFqeEt6NUtnMDliaCtZOENOaGViNFp4d09hNCtjd2d0eWVQUy9aWUZBPT08L2RzOlNpZ25hdHVyZVZhbHVlPjwvZHM6U2lnbmF0dXJlPjxzYW1sMjpTdWJqZWN0PjxzYW1sMjpOYW1lSUQgRm9ybWF0PVwidXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6dW5zcGVjaWZpZWRcIj5YU0FfQURNSU48L3NhbWwyOk5hbWVJRD48c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9XCJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6Y206YmVhcmVyXCI-PHNhbWwyOlN1YmplY3RDb25maXJtYXRpb25EYXRhIE5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMjA6MTY6MjkuNzQxWlwiLz48L3NhbWwyOlN1YmplY3RDb25maXJtYXRpb24-PC9zYW1sMjpTdWJqZWN0PjxzYW1sMjpDb25kaXRpb25zIE5vdEJlZm9yZT1cIjIwMTgtMDMtMTVUMTY6MTE6MjkuNzQxWlwiIE5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMjA6MTY6MjkuNzQxWlwiLz48c2FtbDI6QXV0aG5TdGF0ZW1lbnQgQXV0aG5JbnN0YW50PVwiMjAxOC0wMy0xNVQxNjoxNjoyOS43NDFaXCIgU2Vzc2lvbk5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMTY6MjE6MjkuNzQxWlwiPjxzYW1sMjpBdXRobkNvbnRleHQ-PHNhbWwyOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkPC9zYW1sMjpBdXRobkNvbnRleHRDbGFzc1JlZj48L3NhbWwyOkF1dGhuQ29udGV4dD48L3NhbWwyOkF1dGhuU3RhdGVtZW50Pjwvc2FtbDI6QXNzZXJ0aW9uPiIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwiY2YiLCJ1YWEiLCJ4c191c2VyIiwib3BlbmlkIl19.oCKdh2rOQ0CjlBV7D-aSsB2JQ20JMRyT4zmHc2iJDwAviCHvFx1_zdWHtH-XO67D78ElLriks3fKBnbcsLU_OlesF_1HbdE8IcQp0Wsmtc-ZvYQaGaEDkCC8iMx0ibGGEdWa264eejo2xbsNh0enDtL9SQZAfZkfDnhywpsrtJEQTgziQKFxR-AAzRQAX9H5esxI4KqWIMTveTp9rne-XH3bA70CCRH_3xiOvVaQ06tGjap6zkZTHrqFUXZvnTqK1fiJOzjeYLcyBYvd4C6gyiRyYOjyaMZlbj8SK3wZItQpCwPO40Yn7E5aOZdZpapEZ6qZ4rlptnT1WSx5-ZekYA",
                    new TokenProperties("cf", "141321327", "XSA_ADMIN"), null,
                },
                // (2) Token that contains only the user's name:
                {
                    "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyX25hbWUiOiJ0ZXN0LXVzZXIifQ==.P9XJTA4AV5aHS_ozw5WZXgIPc3M9Q_-1oKc1tLDEC5lkx1vNZjd5ozGaZs8UvgECJ_sTY_ZL2izDAKc3ew8hv9y6i6O3V-BxAs9pxkAIo2GPVmHzZQg8t6iG6c-iz1JnMan9nnbjFmMve5qjl9dgoCat-VaWfIW7TRagQ05dNO8DXJkQiiRioQ5kzoxQV4jUgxk5tczix-s8VQfqobW472A4t087DnaCYOOdz9MF8WLoffWRX8BkYJnBgVJ0kcWPZwMuB9BBPC4Les2NiZaKpLDahPrmp340izGg9pUhUsjPbllAph5odhMDb1Lc8_Q-yKiEt-DwZ72-VkCZE-MPjQ",
                    null, new IllegalStateException("One or more of the following elements are missing from the token: \"[scope, exp, iat]\"")
                },
                // (3) Token without a signature:
                {
                    "eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyX25hbWUiOiJ0ZXN0LXVzZXIifQ==",
                    null, new IllegalStateException("One or more of the following elements are missing from the token: \"[scope, exp, iat]\"")
                },
// @formatter:on
            });
        }

        @Test
        public void testCreateToken() {
            OAuth2AccessTokenWithAdditionalInfo token = null;
            try {
                token = tokenFactory.createToken(tokenString);
            } catch (Exception e) {
                if (expectedExcetion != null) {
                    assertEquals(expectedExcetion.getMessage(), e.getMessage());
                }
            }
            if (expectedTokenProperties != null) {
                validateToken(token, expectedTokenProperties);
            }

        }

    }

    @RunWith(Parameterized.class)
    public static class AdditionalInfoTokenFactoryTest {

        private final String additionalTokenString;
        private final String tokenString;
        private final Map<String, Object> tokenInfo;
        private TokenFactory tokenFactory = new TokenFactory();

        public AdditionalInfoTokenFactoryTest(String additionalTokenString, String tokenString, Map<String, Object> tokenInfo) {
            this.additionalTokenString = additionalTokenString;
            this.tokenString = tokenString;
            this.tokenInfo = tokenInfo;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0) Include exchangeToken:
                {
                    "a25723f22ac754f792c50f07623dzd75",
                    "aRh98oYD80teGrkjDFzg3ln55EV3O96y",
                    MapUtil.of(new Pair<>("scope", Arrays.asList("controller.read")), new Pair<>("exp", 999), new Pair<>("iat", 99))
                },
                // (1) Missing exchangedToken:
                {
                    null,
                    "aRh98oYD80teGrkjDFzg3ln55EV3O96y",
                    MapUtil.of(new Pair<>("scope", Arrays.asList("controller.read")), new Pair<>("exp", 999), new Pair<>("iat", 99))
                }
// @formatter:on
            });
        }

        @Test
        public void testAdditionalInfoToken() {
            tokenInfo.put("exchangedToken", additionalTokenString);
            OAuth2AccessTokenWithAdditionalInfo token = tokenFactory.createToken(tokenString, tokenInfo);
            validateTokenAdditionalInfo(token, tokenInfo);
        }

    }

}
