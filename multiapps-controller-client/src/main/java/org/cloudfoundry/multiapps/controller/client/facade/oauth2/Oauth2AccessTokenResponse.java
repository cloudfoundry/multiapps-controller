package org.cloudfoundry.multiapps.controller.client.facade.oauth2;

import org.immutables.value.Value;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableOauth2AccessTokenResponse.class)
@JsonDeserialize(as = ImmutableOauth2AccessTokenResponse.class)
public interface Oauth2AccessTokenResponse {

    @JsonProperty("access_token")
    String getAccessToken();

    @JsonProperty("token_type")
    String getTokenType();

    @JsonProperty("id_token")
    String getIdToken();

    @JsonProperty("refresh_token")
    String getRefreshToken();

    @JsonProperty("expires_in")
    long getExpiresIn();

    String getScope();

    String getJti();
}
