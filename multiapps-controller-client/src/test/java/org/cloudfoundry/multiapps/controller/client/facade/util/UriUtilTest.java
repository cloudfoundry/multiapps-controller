package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UriUtilTest {

    @Test
    void testEncodeCharsWithOneChar() {
        String encodedString = UriUtil.encodeChars("space,name", List.of(","));
        assertEquals("space%2Cname", encodedString);
    }

    @Test
    void testEncodeCharsWithMultipleChars() {
        String encodedString = UriUtil.encodeChars("org space,name", List.of(",", " "));
        assertEquals("org%20space%2Cname", encodedString);
    }

    @Test
    void testEncodeCharsWhenCharNotPresentReturnsInputUnchanged() {
        String encodedString = UriUtil.encodeChars("plain-value", List.of(","));
        assertEquals("plain-value", encodedString);
    }

    @Test
    void testEncodeCharsWithEmptyListReturnsInputUnchanged() {
        String encodedString = UriUtil.encodeChars("space,name", List.of());
        assertEquals("space,name", encodedString);
    }

    @Test
    void testEncodeCharsEncodesEveryOccurrence() {
        String encodedString = UriUtil.encodeChars("a,b,c", List.of(","));
        assertEquals("a%2Cb%2Cc", encodedString);
    }

    @Test
    void testEncodeCharsAppliesEncodingsInOrder() {
        String encodedString = UriUtil.encodeChars("a, b", List.of(",", " "));
        assertEquals("a%2C%20b", encodedString);
    }

    @Test
    void testExtractBaseUrlWithExplicitPortKeepsPort() throws Exception {
        String baseUrl = UriUtil.extractBaseUrl(new URI("https://api.example.com:8443/v3/apps").toURL())
                                .toString();
        assertEquals("https://api.example.com:8443", baseUrl);
    }

    @Test
    void testExtractBaseUrlWithoutPortOmitsPort() throws Exception {
        String baseUrl = UriUtil.extractBaseUrl(new URI("https://api.example.com/v3/apps?per_page=50").toURL())
                                .toString();
        assertEquals("https://api.example.com", baseUrl);
    }

    @Test
    void testExtractBaseUrlStripsPathAndQuery() throws Exception {
        String baseUrl = UriUtil.extractBaseUrl(new URI("http://localhost:9000/some/deep/path?x=1#frag").toURL())
                                .toString();
        assertEquals("http://localhost:9000", baseUrl);
    }

    @Test
    void testExtractBaseUrlPreservesHttpScheme() throws Exception {
        String baseUrl = UriUtil.extractBaseUrl(new URI("http://cf.example.org/").toURL())
                                .toString();
        assertEquals("http://cf.example.org", baseUrl);
    }

    @Test
    void testResolveV3ApiUrlFromRootDocumentLinks() throws Exception {
        Map<String, Object> links = Map.of("cloud_controller_v3", Map.of("href", "https://api.example.com/v3"));
        String apiUrl = UriUtil.resolveV3ApiUrl(controllerUrl(), links)
                               .toString();

        assertEquals("https://api.example.com/v3", apiUrl);
    }

    @Test
    void testResolveV3ApiUrlFallsBackWhenLinkMissing() throws Exception {
        String apiUrl = UriUtil.resolveV3ApiUrl(controllerUrl(), Map.of())
                               .toString();
        assertEquals("https://api.example.com/v3", apiUrl);
    }

    @Test
    void testResolveV3ApiUrlFallsBackWhenLinkHasNoHref() throws Exception {
        Map<String, Object> links = Map.of("cloud_controller_v3", Map.of());
        String apiUrl = UriUtil.resolveV3ApiUrl(controllerUrl(), links)
                               .toString();

        assertEquals("https://api.example.com/v3", apiUrl);
    }

    @Test
    void testResolveV3ApiUrlFallsBackWhenHrefIsNull() throws Exception {
        Map<String, Object> href = new HashMap<>();
        href.put("href", null);
        Map<String, Object> links = Map.of("cloud_controller_v3", href);
        String apiUrl = UriUtil.resolveV3ApiUrl(controllerUrl(), links)
                               .toString();

        assertEquals("https://api.example.com/v3", apiUrl);
    }

    @Test
    void testResolveLogCacheUrlFromRootDocumentLinks() throws Exception {
        Map<String, Object> links = Map.of("log_cache", Map.of("href", "https://log-cache.example.com"));
        String logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl(), links)
                                    .toString();

        assertEquals("https://log-cache.example.com", logCacheUrl);
    }

    @Test
    void testResolveLogCacheUrlFallsBackReplacingApiHostPrefix() throws Exception {
        String logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl(), Map.of())
                                    .toString();
        assertEquals("https://log-cache.example.com", logCacheUrl);
    }

    @Test
    void testResolveLogCacheUrlFallsBackPrependingWhenHostHasNoApiPrefix() throws Exception {
        URL controllerUrl = new URI("https://cf.example.com").toURL();
        String logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl, Map.of())
                                    .toString();

        assertEquals("https://log-cache.cf.example.com", logCacheUrl);
    }

    @Test
    void testResolveLogCacheUrlFallsBackWhenLinkHasNoHref() throws Exception {
        Map<String, Object> links = Map.of("log_cache", Map.of());
        String logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl(), links)
                                    .toString();

        assertEquals("https://log-cache.example.com", logCacheUrl);
    }

    @Test
    void testResolveV3ApiUrlFallsBackWhenLinksMapIsNull() throws Exception {
        String apiUrl = UriUtil.resolveV3ApiUrl(controllerUrl(), null)
                               .toString();
        assertEquals("https://api.example.com/v3", apiUrl);
    }

    @Test
    void testResolveLogCacheUrlFallsBackWhenLinksMapIsNull() throws Exception {
        String logCacheUrl = UriUtil.resolveLogCacheUrl(controllerUrl(), null)
                                    .toString();
        assertEquals("https://log-cache.example.com", logCacheUrl);
    }

    private static URL controllerUrl() throws Exception {
        return new URI("https://api.example.com").toURL();
    }
}
