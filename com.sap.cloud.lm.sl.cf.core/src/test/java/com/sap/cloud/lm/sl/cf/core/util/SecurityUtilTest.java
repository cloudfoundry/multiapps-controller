package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

public class SecurityUtilTest {

    @ParameterizedTest
    @MethodSource
    void testGetTokenUserInfo(String userId, String username, String tokenString, String exchangedTokenString, UserInfo expectedUserInfo) {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(tokenString);
        Map<String, Object> additionalInformation = setAdditionalInformation(userId, username, tokenString, exchangedTokenString);
        token.setAdditionalInformation(additionalInformation);
        UserInfo userInfo = SecurityUtil.getTokenUserInfo(token);
        assertEquals(expectedUserInfo.getId(), userInfo.getId());
        assertEquals(expectedUserInfo.getName(), userInfo.getName());
        assertEquals(expectedUserInfo.getToken()
            .getValue(),
            userInfo.getToken()
                .getValue());
    }

    public static Stream<Arguments> testGetTokenUserInfo() {
        return Stream.of(
        // @formatter:off
            // (1) UserInfo with user token
            Arguments.of("cf","CF_USER", "dUTjdafgtw3wRUMkt4XDu2IidcEHNPoh", null, new UserInfo("cf", "CF_USER", new DefaultOAuth2AccessToken("dUTjdafgtw3wRUMkt4XDu2IidcEHNPoh"))),
            // (2) UserInfo with exchanged token
            Arguments.of("cf","CF_USER", "dUTjdafgtw3wRUMkt4XDu2IidcEHNPoh", "Xk2s5nIQPqjzoZ9KPwL2uPUhuiuUsbm2", 
                new UserInfo("cf", "CF_USER", new DefaultOAuth2AccessToken("Xk2s5nIQPqjzoZ9KPwL2uPUhuiuUsbm2")))
        // @formatter:on
        );
    }

    private Map<String, Object> setAdditionalInformation(String userid, String username, String tokenString, String exchangedTokenString) {
        Map<String, Object> additionalInformationMap = new HashMap<>();
        additionalInformationMap.put("user_name", username);
        additionalInformationMap.put("user_id", userid);
        additionalInformationMap.put("exchangedToken", exchangedTokenString);
        return additionalInformationMap;
    }

}
