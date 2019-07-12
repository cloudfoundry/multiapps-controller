package com.sap.cloud.lm.sl.cf.core.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

public class ApplicationURITest {

    private static Stream<Arguments> testParameters() {
        return Stream.of(
            Arguments.of("https://valid-host.valid-domain", "valid-host","valid-domain", null),
            Arguments.of("https://valid-domain", "", "valid-domain", null),
            Arguments.of("valid-domain", "", "valid-domain", null),
            Arguments.of("https://valid-domain/really/long/path", "", "valid-domain", "/really/long/path"),
            Arguments.of("https://valid-host.valid-domain/really/long/path", "valid-host", "valid-domain", "/really/long/path"),
            Arguments.of("deploy-service.cfapps.industrycloud-staging.siemens.com", "deploy-service", "cfapps.industrycloud-staging.siemens.com", null)
        );
    }

    @ParameterizedTest
    @MethodSource("testParameters")
    public void testGetHostDomainPath(String uri, String expectedHost, String expectedDomain, String expectedPath) {
        ApplicationURI applicationURI = new ApplicationURI(uri);
        Assertions.assertEquals(expectedDomain, applicationURI.getDomain());
        Assertions.assertEquals(expectedDomain, ApplicationURI.getDomainFromURI(uri));
        Assertions.assertEquals(expectedHost, applicationURI.getHost());
        Assertions.assertEquals(expectedPath, applicationURI.getPath());
    }
}
