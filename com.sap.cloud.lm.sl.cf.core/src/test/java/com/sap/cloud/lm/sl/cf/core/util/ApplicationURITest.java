package com.sap.cloud.lm.sl.cf.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ApplicationURITest {

    private String uri;
    private String expectedHost;
    private String expectedDomain;
    private String expectedPath;

    @Parameterized.Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00)
            {
                "https://valid-host.valid-domain", "valid-host","valid-domain", null,
            },
            // (01) Test without host:
            {
                "https://valid-domain", "", "valid-domain", null,
            },
            // (02) Test without host and scheme:
            {
                "valid-domain", "", "valid-domain", null,
            },
            // (03) Test with path and no host:
            {
                "https://valid-domain/really/long/path", "", "valid-domain", "/really/long/path",
            },
            // (04) Test with path:
            {
                "https://valid-host.valid-domain/really/long/path", "valid-host", "valid-domain", "/really/long/path",
            },
            // (05) Test Siemens landscape URI:
            {
                "deploy-service.cfapps.industrycloud-staging.siemens.com", "deploy-service", "cfapps.industrycloud-staging.siemens.com", null,
            },
// @formatter:on
        });
    }

    public ApplicationURITest(String uri, String expectedHost, String expectedDomain, String expectedPath) {
        this.uri = uri;
        this.expectedHost = expectedHost;
        this.expectedDomain = expectedDomain;
        this.expectedPath = expectedPath;
    }

    @Test
    public void testGetHostDomainPath() {
        ApplicationURI applicationURI = new ApplicationURI(uri);
        validateHost(applicationURI.getHost());
        validateDomain(applicationURI.getDomain());
        validatePath(applicationURI.getPath());
    }

    private void validatePath(String path) {
        assertEquals(expectedPath, path);
    }

    private void validateHost(String host) {
        assertEquals(expectedHost, host);
    }

    private void validateDomain(String domain) {
        assertEquals(expectedDomain, domain);
    }
}
