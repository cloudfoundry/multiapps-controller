package com.sap.cloud.lm.sl.cf.core.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Enclosed.class)
public class UriUtilTest {

    @RunWith(Parameterized.class)
    public static class HostAndDomainTest {

        private String uri;
        private String expectedHost;
        private String expectedDomain;
        private String expectedPath;

        public HostAndDomainTest(String uri, String expectedHost, String expectedDomain, String expectedPath) {
            this.uri = uri;
            this.expectedHost = expectedHost;
            this.expectedDomain = expectedDomain;
            this.expectedPath = expectedPath;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Test with host-based uri
                {
                    "https://valid-host.valid-domain", "valid-host","valid-domain", null
                },
                // (01) Test with port-based uri
                {
                    "https://valid-domain:4000", "4000", "valid-domain", null
                },
                // (02) Test with host-based uri without host
                {
                    "https://valid-domain", "", "valid-domain", null
                },
                // (03) Test with host-based uri without host and scheme
                {
                    "valid-domain", "", "valid-domain", null
                },
                // (04) Test with port-based uri with path
                {
                    "https://valid-domain:3000/really/long/path", "3000", "valid-domain", "/really/long/path"
                },
                 // (05) Test with host-based uri with path and no host
                {
                    "https://valid-domain/really/lomg/path", "", "valid-domain", "/really/lomg/path"
                },
                // (06) Test with host-based uri with path
                {
                    "https://valid-host.valid-domain/really/long/path", "valid-host", "valid-domain", "/really/long/path"
                },
                // (07) Test with port-based uri without scheme
                {
                    "valid-domain:3000", "3000", "valid-domain", null
                },
                // (08) Test with host-based routing with port
                {
                    "https://valid-host.valid-domain:3000", "3000", "valid-host.valid-domain", null
                },
                // (09) Test host-based routing with port and path
                {
                    "https://valid-host.valid-domain:3000/too/long/path", "3000", "valid-host.valid-domain", "/too/long/path"
                },
                // (10) Test siemens landscape uri
                {
                    "deploy-service.cfapps.industrycloud-staging.siemens.com", "deploy-service", "cfapps.industrycloud-staging.siemens.com", null
                },

// @formatter:on
            });
        }

        @Test
        public void testGetHostAndDomain() {
            Pair<String, String> hostDomain = UriUtil.getHostAndDomain(uri);
            String path = UriUtil.getPath(uri);
            validateHostDomain(hostDomain);
            validatePath(path);
        }

        private void validatePath(String path) {
            assertEquals(expectedPath, path);
        }

        private void validateHostDomain(Pair<String, String> hostDomain) {
            assertEquals(expectedHost, hostDomain._1);
            assertEquals(expectedDomain, hostDomain._2);
        }

    }

    @RunWith(Parameterized.class)
    public static class TestRemovePort {
        private String uri;
        private String expectedUri;

        public TestRemovePort(String uri, String expectedUri) {
            this.uri = uri;
            this.expectedUri = expectedUri;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Test with no-port in the uri
                {
                    "https://valid-host.valid-domain", "https://valid-host.valid-domain"
                },
                // (01) Test with no-port in the uri and no host
                {
                    "https://valid-domain", "https://valid-domain"
                },
                // (02) Test with no-port in the uri and no schema
                {
                    "valid-domain", "valid-domain"
                },
                // (03) Test with host-based uri and with port
                {
                    "https://valid-host.valid-domain:3000", "https://valid-host.valid-domain"
                },
                // (04) Test with host-based uri and with port and with path
                {
                    "https://valid-host.valid-domain:3000/too/long/path", "https://valid-host.valid-domain/too/long/path"
                },
                // (05) Test with host-based uri and with path
                {
                    "https://valid-host.valid-domain/too/long/path", "https://valid-host.valid-domain/too/long/path"
                },
                // (06) Test with port-based uri and with path
                {
                    "https://valid-domain/too/long/path", "https://valid-domain/too/long/path"
                },
                // (07) Test with port-based uri with port and with path
                {
                    "https://valid-domain:3000/too/long/path", "https://valid-domain/too/long/path"
                },
                // (08) Test with port-based uri with non-valid port and with path
                {
                    "https://valid-domain:not-valid/too/long/path", "https://valid-domain:not-valid/too/long/path"
                },
                // (09) Test with port in the uri and no schema
                {
                    "valid-domain:3000", "valid-domain"
                },
                // (010) Test with port in the uri and schema
                {
                    "schema:valid-domain", "schema:valid-domain"
                },
                // (011) Test with port in the uri and schema
                {
                    "schema:valid-domain:", "schema:valid-domain"
                },
// @formatter:on
            });
        }

        @Test
        public void testRemovePort() {
            String uriWithoutPort = UriUtil.removePort(uri);
            assertEquals(expectedUri, uriWithoutPort);
        }
    }

    @RunWith(Parameterized.class)
    public static class TestGetUriWithoutScheme {
        private String uri;
        private String expectedUri;

        public TestGetUriWithoutScheme(String uri, String expectedUri) {
            this.uri = uri;
            this.expectedUri = expectedUri;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Test with no-port in the uri
                {
                    "https://valid-host.valid-domain", "valid-host.valid-domain"
                },
                // (01) Test with port in the uri
                {
                    "tcp://10.244.0.34.xip.io:4443", "10.244.0.34.xip.io:4443"
                }
// @formatter:on
            });
        }

        @Test
        public void testGetUriWithoutScheme() {
            String actualUri = UriUtil.getUriWithoutScheme(uri);
            assertEquals(expectedUri, actualUri);
        }
    }

    @RunWith(Parameterized.class)
    public static class routeMatchesUriTest {

        private CloudRoute routeInput;
        private String uri;
        private boolean isPortBasedRouting;
        private boolean expectedResult;

        public routeMatchesUriTest(String routeInput, String uri, boolean isPortBasedRouting, boolean expectedResult)
            throws ParsingException, IOException {
            this.routeInput = JsonUtil.fromJson(TestUtil.getResourceAsString(routeInput, UriUtil.class), CloudRoute.class);
            this.uri = uri;
            this.isPortBasedRouting = isPortBasedRouting;
            this.expectedResult = expectedResult;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (00) Test with host-based uri and with port
                {
                    "uri-util-input-00.json", "https://valid-host.valid-domain:4000" , false, true
                },
                // (01) Test with port-based uri
                {
                    "uri-util-input-01.json", "https://valid-domain:4000" , true, true
                },
                // (02) Test with host-based uri and with port
                {
                    "uri-util-input-02.json", "https://valid-host.valid-domain:4000" , true, false
                },
                // (03) Test with host-based uri and no port
                {
                    "uri-util-input-03.json", "https://valid-host.valid-domain" , false, true
                },
                // (04) Test with port-based uri
                {
                    "uri-util-input-04.json", "https://valid-domain:4000" , true, false
                }
// @formatter:on
            });
        }

        @Test
        public void testRouteMatchesUri() {
            boolean actualResult = UriUtil.routeMatchesUri(routeInput, uri, isPortBasedRouting);
            assertEquals(expectedResult, actualResult);
        }
    }

}
