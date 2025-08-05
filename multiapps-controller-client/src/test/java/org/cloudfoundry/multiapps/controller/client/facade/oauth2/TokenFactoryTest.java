package org.cloudfoundry.multiapps.controller.client.facade.oauth2;

import static org.cloudfoundry.multiapps.controller.client.facade.oauth2.TokenFactory.CLIENT_ID;
import static org.cloudfoundry.multiapps.controller.client.facade.oauth2.TokenFactory.USER_ID;
import static org.cloudfoundry.multiapps.controller.client.facade.oauth2.TokenFactory.USER_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TokenFactoryTest {

    private final TokenFactory tokenFactory = new TokenFactory();

    public static Stream<Arguments> testCreateToken() {
        return Stream.of(
// @formatter:off
                // (0) Valid token:
                Arguments.of("eyJhbGciOiJSUzI1NiJ9.eyJqdGkiOiI2MTkyM2FkYi1iYjViLTQ4NTktODIzNy0yM2YxNzg5ZTJmOTAiLCJzdWIiOiIxNTUxODQiLCJzY29wZSI6WyJjbG91ZF9jb250cm9sbGVyLnJlYWQiLCJjbG91ZF9jb250cm9sbGVyLndyaXRlIiwiY2xvdWRfY29udHJvbGxlci5hZG1pbiIsInVhYS51c2VyIl0sImNsaWVudF9pZCI6ImNmIiwiY2lkIjoiY2YiLCJhenAiOiJjZiIsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIxNTUxODQiLCJ1c2VyX25hbWUiOiJYU01BU1RFUiIsImVtYWlsIjoiWFNNQVNURVJAdW5rbm93biIsImZhbWlseV9uYW1lIjoiWFNNQVNURVIiLCJpYXQiOjE0NDc3NDUzMjgsImV4cCI6MTQ0Nzc4ODUyOCwiaXNzIjoiaHR0cDovL2xvY2FsaG9zdDo4MDgwL3VhYS9vYXV0aC90b2tlbiIsInppZCI6InVhYSIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwiY2YiLCJ1YWEiXX0.P9XJTA4AV5aHS_ozw5WZXgIPc3M9Q_-1oKc1tLDEC5lkx1vNZjd5ozGaZs8UvgECJ_sTY_ZL2izDAKc3ew8hv9y6i6O3V-BxAs9pxkAIo2GPVmHzZQg8t6iG6c-iz1JnMan9nnbjFmMve5qjl9dgoCat-VaWfIW7TRagQ05dNO8DXJkQiiRioQ5kzoxQV4jUgxk5tczix-s8VQfqobW472A4t087DnaCYOOdz9MF8WLoffWRX8BkYJnBgVJ0kcWPZwMuB9BBPC4Les2NiZaKpLDahPrmp340izGg9pUhUsjPbllAph5odhMDb1Lc8_Q-yKiEt-DwZ72-VkCZE-MPjQ",
                        Map.of(CLIENT_ID, "cf", USER_ID, "155184", USER_NAME, "XSMASTER")),
                // (1) Valid token, the body of which cannot be parsed with the standard base 64 decoder (a base 64 URL decoder must be used for this one):
                Arguments.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJqdGkiOiIzZmEyMGRjMzAwY2Y0ZDg2ODU4NWVjYzFhZGQyOWJlNyIsImV4dF9hdHRyIjp7ImVuaGFuY2VyIjoiWFNVQUEifSwic3ViIjoiMTQxMzIxMzI3Iiwic2NvcGUiOlsiY2xvdWRfY29udHJvbGxlci5yZWFkIiwiY2xvdWRfY29udHJvbGxlci53cml0ZSIsIm9wZW5pZCIsInhzX3VzZXIucmVhZCIsImNsb3VkX2NvbnRyb2xsZXIuYWRtaW4iLCJ1YWEudXNlciJdLCJjbGllbnRfaWQiOiJjZiIsImNpZCI6ImNmIiwiYXpwIjoiY2YiLCJyZXZvY2FibGUiOnRydWUsImdyYW50X3R5cGUiOiJwYXNzd29yZCIsInVzZXJfaWQiOiIxNDEzMjEzMjciLCJvcmlnaW4iOiJ1YWEiLCJ1c2VyX25hbWUiOiJYU0FfQURNSU4iLCJlbWFpbCI6IlhTQV9BRE1JTkB1bmtub3duIiwicmV2X3NpZyI6ImU2MDQyZGFhIiwiaWF0IjoxNTIxMzgwNjI1LCJleHAiOjE1MjE0MjM4MjUsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC91YWEvb2F1dGgvdG9rZW4iLCJ6aWQiOiJ1YWEiLCJoZGIubmFtZWR1c2VyLnNhbWwiOiI8P3htbCB2ZXJzaW9uPVwiMS4wXCIgZW5jb2Rpbmc9XCJVVEYtOFwiPz48c2FtbDI6QXNzZXJ0aW9uIHhtbG5zOnNhbWwyPVwidXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOmFzc2VydGlvblwiIElEPVwiX2Q5YjgxNWQ1LTNlMTYtNDU1Mi1iMjQwLTAyNmE2OGUwZTMzMlwiIElzc3VlSW5zdGFudD1cIjIwMTgtMDMtMTVUMTY6MTE6MjkuNzQxWlwiIFZlcnNpb249XCIyLjBcIj48c2FtbDI6SXNzdWVyPkNBRC1zYW1sPC9zYW1sMjpJc3N1ZXI-PGRzOlNpZ25hdHVyZSB4bWxuczpkcz1cImh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNcIj48ZHM6U2lnbmVkSW5mbz48ZHM6Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biNcIi8-PGRzOlNpZ25hdHVyZU1ldGhvZCBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTFcIi8-PGRzOlJlZmVyZW5jZSBVUkk9XCIjX2Q5YjgxNWQ1LTNlMTYtNDU1Mi1iMjQwLTAyNmE2OGUwZTMzMlwiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPVwiaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI2VudmVsb3BlZC1zaWduYXR1cmVcIi8-PGRzOlRyYW5zZm9ybSBBbGdvcml0aG09XCJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biNcIi8-PC9kczpUcmFuc2Zvcm1zPjxkczpEaWdlc3RNZXRob2QgQWxnb3JpdGhtPVwiaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3NoYTFcIi8-PGRzOkRpZ2VzdFZhbHVlPlFKVG1obnJXMDY3Z3VDbWJmMTdXc1lYaCtKUT08L2RzOkRpZ2VzdFZhbHVlPjwvZHM6UmVmZXJlbmNlPjwvZHM6U2lnbmVkSW5mbz48ZHM6U2lnbmF0dXJlVmFsdWU-TzhxaDZZaWluUkhldUZkZk1qVkg0VVBlTzc5MWs2bmRrSU9wN2JQRkhnQVB0MWVpeGFGN2dTYnFMV2FmV2d5OHIrSUhlZTZmZXZjQkdMSzRMUGxPSjRmcTR3NFNsWVlzVDZBWGRKMyswZUxEQnR2QjRYN3B0MDRYMVR2UThUelVWSlovQmdLQ2huSEl4RlRocGJ6VXRINmpzQ2d1anoyaW1yenB0S083Z0JnRWplNUF6Lzg4OUJCZ29mZTRiTytJRk9hU3h2VnlKZWZJTDZSS1AzNVd6MFpOdzBqdEVNemtScEh3eFpKcVJtRjJ3Ym11YUk2ZDBNMXVVR09MVjhzUVkvdjZLbUw4U0JjZmZNZ2hDaUNFZDd6VHZqa2ZnVzBDU2xZVzFSV0ZvOURLaUFqeEt6NUtnMDliaCtZOENOaGViNFp4d09hNCtjd2d0eWVQUy9aWUZBPT08L2RzOlNpZ25hdHVyZVZhbHVlPjwvZHM6U2lnbmF0dXJlPjxzYW1sMjpTdWJqZWN0PjxzYW1sMjpOYW1lSUQgRm9ybWF0PVwidXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6MS4xOm5hbWVpZC1mb3JtYXQ6dW5zcGVjaWZpZWRcIj5YU0FfQURNSU48L3NhbWwyOk5hbWVJRD48c2FtbDI6U3ViamVjdENvbmZpcm1hdGlvbiBNZXRob2Q9XCJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6Y206YmVhcmVyXCI-PHNhbWwyOlN1YmplY3RDb25maXJtYXRpb25EYXRhIE5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMjA6MTY6MjkuNzQxWlwiLz48L3NhbWwyOlN1YmplY3RDb25maXJtYXRpb24-PC9zYW1sMjpTdWJqZWN0PjxzYW1sMjpDb25kaXRpb25zIE5vdEJlZm9yZT1cIjIwMTgtMDMtMTVUMTY6MTE6MjkuNzQxWlwiIE5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMjA6MTY6MjkuNzQxWlwiLz48c2FtbDI6QXV0aG5TdGF0ZW1lbnQgQXV0aG5JbnN0YW50PVwiMjAxOC0wMy0xNVQxNjoxNjoyOS43NDFaXCIgU2Vzc2lvbk5vdE9uT3JBZnRlcj1cIjIwMTgtMDMtMTVUMTY6MjE6MjkuNzQxWlwiPjxzYW1sMjpBdXRobkNvbnRleHQ-PHNhbWwyOkF1dGhuQ29udGV4dENsYXNzUmVmPnVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphYzpjbGFzc2VzOlBhc3N3b3JkPC9zYW1sMjpBdXRobkNvbnRleHRDbGFzc1JlZj48L3NhbWwyOkF1dGhuQ29udGV4dD48L3NhbWwyOkF1dGhuU3RhdGVtZW50Pjwvc2FtbDI6QXNzZXJ0aW9uPiIsImF1ZCI6WyJjbG91ZF9jb250cm9sbGVyIiwiY2YiLCJ1YWEiLCJ4c191c2VyIiwib3BlbmlkIl19.oCKdh2rOQ0CjlBV7D-aSsB2JQ20JMRyT4zmHc2iJDwAviCHvFx1_zdWHtH-XO67D78ElLriks3fKBnbcsLU_OlesF_1HbdE8IcQp0Wsmtc-ZvYQaGaEDkCC8iMx0ibGGEdWa264eejo2xbsNh0enDtL9SQZAfZkfDnhywpsrtJEQTgziQKFxR-AAzRQAX9H5esxI4KqWIMTveTp9rne-XH3bA70CCRH_3xiOvVaQ06tGjap6zkZTHrqFUXZvnTqK1fiJOzjeYLcyBYvd4C6gyiRyYOjyaMZlbj8SK3wZItQpCwPO40Yn7E5aOZdZpapEZ6qZ4rlptnT1WSx5-ZekYA",
                        Map.of(CLIENT_ID, "cf", USER_ID, "141321327", USER_NAME, "XSA_ADMIN")),
                // (2) Token that contains only the user's name:
                Arguments.of("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyX25hbWUiOiJ0ZXN0LXVzZXIifQ==.P9XJTA4AV5aHS_ozw5WZXgIPc3M9Q_-1oKc1tLDEC5lkx1vNZjd5ozGaZs8UvgECJ_sTY_ZL2izDAKc3ew8hv9y6i6O3V-BxAs9pxkAIo2GPVmHzZQg8t6iG6c-iz1JnMan9nnbjFmMve5qjl9dgoCat-VaWfIW7TRagQ05dNO8DXJkQiiRioQ5kzoxQV4jUgxk5tczix-s8VQfqobW472A4t087DnaCYOOdz9MF8WLoffWRX8BkYJnBgVJ0kcWPZwMuB9BBPC4Les2NiZaKpLDahPrmp340izGg9pUhUsjPbllAph5odhMDb1Lc8_Q-yKiEt-DwZ72-VkCZE-MPjQ",
                        Collections.emptyMap()),
                // (3) Token without a signature:
                Arguments.of("eyJhbGciOiJSUzI1NiJ9.eyJ1c2VyX25hbWUiOiJ0ZXN0LXVzZXIifQ==",
                        Collections.emptyMap())
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCreateToken(String tokenString, Map<String, Object> expectedTokenProperties) {
        if (expectedTokenProperties.isEmpty()) {
            Exception exception = assertThrows(IllegalStateException.class, () -> tokenFactory.createToken(tokenString));
            assertEquals("One or more of the following elements are missing from the token: \"[scope, exp, iat]\"", exception.getMessage());
            return;
        }
        OAuth2AccessTokenWithAdditionalInfo token = tokenFactory.createToken(tokenString);
        validateToken(token, expectedTokenProperties);
    }

    public static Stream<Arguments> testAdditionalInfoToken() {
        return Stream.of(
// @formatter:off
                // (0) Include exchangeToken:
                Arguments.of("aRh98oYD80teGrkjDFzg3ln55EV3O96y",
                        Map.of("exchangedToken", "a25723f22ac754f792c50f07623dzd75", "scope", List.of("controller.read"), "exp", 999, "iat", 100)),
                // (1) Missing exchangedToken:
                Arguments.of("aRh98oYD80teGrkjDFzg3ln55EV3O96y",
                        Map.of("scope", List.of("controller.read"), "exp", 999, "iat", 100))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAdditionalInfoToken(String tokenString, Map<String, Object> tokenInfo) {
        OAuth2AccessTokenWithAdditionalInfo token = tokenFactory.createToken(tokenString, tokenInfo);
        validateTokenAdditionalInfo(token, tokenInfo);
    }

    private static void validateToken(OAuth2AccessTokenWithAdditionalInfo token, Map<String, Object> expectedAdditionalInfo) {
        Map<String, Object> additionalInfo = token.getAdditionalInfo();
        Assertions.assertEquals(additionalInfo.get(CLIENT_ID), expectedAdditionalInfo.get(CLIENT_ID));
        Assertions.assertEquals(additionalInfo.get(USER_NAME), expectedAdditionalInfo.get(USER_NAME));
        Assertions.assertEquals(additionalInfo.get(USER_ID), expectedAdditionalInfo.get(USER_ID));
    }

    private static void validateTokenAdditionalInfo(OAuth2AccessTokenWithAdditionalInfo token, Map<String, Object> tokenInfo) {
        Map<String, Object> tokenAdditionalInformation = token.getAdditionalInfo();
        assertEquals(tokenInfo, tokenAdditionalInformation);
    }

}
