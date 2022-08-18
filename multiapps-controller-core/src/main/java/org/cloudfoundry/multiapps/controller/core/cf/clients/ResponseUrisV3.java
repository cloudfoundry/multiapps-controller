package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class ResponseUrisV3 {
    private final Map<String, Object> includedUris;

    public ResponseUrisV3(Map<String, Object> includedUris) {
        this.includedUris = includedUris;
    }

    public String getUriString(String uriKey) {
        UriComponents uriComponents = getUriComponents(uriKey);
        return uriComponents == null ? null : uriComponents.toUriString();
    }

    public UriComponents getUriComponents(String uriKey) {
        if (includedUris == null || includedUris.get(uriKey) == null) {
            return null;
        }
        Map<String, Object> uriMap = (Map<String, Object>) includedUris.get(uriKey);
        String url = (String) uriMap.get("href");
        return url == null ? null : retrieveUriFromUrl(url);
    }

    private UriComponents retrieveUriFromUrl(String url) {
        String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);
        return UriComponentsBuilder.fromHttpUrl(decodedUrl)
                                   .host(null)
                                   .scheme(null)
                                   .build();
    }
}
