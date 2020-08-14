package org.cloudfoundry.multiapps.controller.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

class SecurityUtilTest {

    private static final String USER_ID = "cf";
    private static final String USER_NAME = "CF_USER";
    private static final String TOKEN = "dUTjdafgtw3wRUMkt4XDu2IidcEHNPoh";

    @Test
    void testGetTokenUserInfo() {
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(TOKEN);
        Map<String, Object> additionalInformation = asMap(USER_ID, USER_NAME);
        token.setAdditionalInformation(additionalInformation);
        UserInfo userInfo = SecurityUtil.getTokenUserInfo(token);
        assertEquals(USER_ID, userInfo.getId());
        assertEquals(USER_NAME, userInfo.getName());
        assertEquals(TOKEN, userInfo.getToken()
                                    .getValue());
    }

    private Map<String, Object> asMap(String userId, String username) {
        Map<String, Object> additionalInformationMap = new HashMap<>();
        additionalInformationMap.put("user_name", username);
        additionalInformationMap.put("user_id", userId);
        return additionalInformationMap;
    }

}
