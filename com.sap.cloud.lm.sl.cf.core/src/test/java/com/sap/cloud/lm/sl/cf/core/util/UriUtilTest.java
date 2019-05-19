package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.Pair;

@RunWith(Enclosed.class)
public class UriUtilTest {

    @RunWith(Parameterized.class)
    public static class HostAndDomainTest {

        private String uri;
        private String expectedHost;
        private String expectedDomain;
        private String expectedPath;

        @Parameters
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

        public HostAndDomainTest(String uri, String expectedHost, String expectedDomain, String expectedPath) {
            this.uri = uri;
            this.expectedHost = expectedHost;
            this.expectedDomain = expectedDomain;
            this.expectedPath = expectedPath;
        }

        @Test
        public void testGetHostAndDomain() {
            Pair<String, String> hostDomain = UriUtil.getHostAndDomain(uri);
            String path = UriUtil.getPath(uri);
            validateHostDomain(hostDomain);
            validatePath(path);
        }

        @Test
        public void testGetHostAndDomainWithApplicationURI() {
            ApplicationURI applicationURI = new ApplicationURI(uri);
            // the old function returns the port in the host position ...
            validateHost(applicationURI.getHost());
            validateDomain(applicationURI.getDomain());
            validatePath(applicationURI.getPath());
        }

        private void validateHostDomain(Pair<String, String> hostDomain) {
            validateHost(hostDomain._1);
            validateDomain(hostDomain._2);
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

    @RunWith(Parameterized.class)
    public static class TestGetUriWithoutScheme {
        private String uri;
        private String expectedUri;

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Test with no-port in the uri
                {
                    "https://valid-host.valid-domain", "valid-host.valid-domain"
                },
                // (01) Test without scheme
                {                    
                    "10.244.0.34.xip.io:4443", "10.244.0.34.xip.io:4443"
                }
// @formatter:on
            });
        }

        public TestGetUriWithoutScheme(String uri, String expectedUri) {
            this.uri = uri;
            this.expectedUri = expectedUri;
        }

        @Test
        public void testGetUriWithoutScheme() {
            String actualUri = UriUtil.getUriWithoutScheme(uri);
            assertEquals(expectedUri, actualUri);
        }

        @Test
        public void testGetUriWithoutSchemeWithApplicationURI() {
            ApplicationURI actualUri = new ApplicationURI(uri);

            actualUri.setScheme(null);
            assertEquals(expectedUri, actualUri.toString());
        }
    }

    @RunWith(Parameterized.class)
    public static class RouteMatchesUriTest {

        private CloudRoute routeInput;
        private String uri;
        private boolean expectedResult;

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0) Test with host-based uri and with port
                {
                    "https://valid-host.valid-domain:4000", false
                },
                // (1) Test with host-based uri and no port
                {
                    "https://valid-host.valid-domain", true
                },
                // (2) Test with port-based uri
                {
                    "https://valid-domain:4000", false
                }
// @formatter:on
            });
        }

        public RouteMatchesUriTest(String uri, boolean expectedResult) throws ParsingException, IOException {
            this.routeInput = new CloudRoute(null, "valid-host", new CloudDomain(null, "valid-domain", null), 0);
            this.uri = uri;
            this.expectedResult = expectedResult;
        }

        @Test
        public void testRouteMatchesUri() {
            boolean actualResult = UriUtil.routeMatchesUri(routeInput, uri);
            assertEquals(expectedResult, actualResult);
        }

        @Test
        public void testRouteMatchesUriWithApplicationURI() {
            ApplicationURI uri = new ApplicationURI(this.uri);

            boolean routeHostMatches = false;
            boolean routeDomainMatches = false;
            ApplicationURI uriCopy = new ApplicationURI(uri.toString());
            routeHostMatches = routeInput.getHost()
                .equals(uriCopy.getHost());
            routeDomainMatches = routeInput.getDomain()
                .getName()
                .equals(uriCopy.getDomain());

            assertEquals(expectedResult, routeHostMatches && routeDomainMatches);
        }
    }

}
