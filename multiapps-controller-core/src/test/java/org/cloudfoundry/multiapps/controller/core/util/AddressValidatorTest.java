package org.cloudfoundry.multiapps.controller.core.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class AddressValidatorTest {

    private static final String DENY_LIST = "127.0.0.0/8,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,169.254.0.0/16,100.100.100.200/32,::1/128,fe80::/10,fc00::/7";

    static Stream<Arguments> validateTargetRejectsDeniedAddress() {
        return Stream.of(Arguments.of("https://loopback.test/file", "127.0.0.1"),
                         Arguments.of("https://loopback.test/file", "127.5.6.7"),
                         Arguments.of("https://rfc1918-10.test/file", "10.1.2.3"),
                         Arguments.of("https://rfc1918-172.test/file", "172.16.5.5"),
                         Arguments.of("https://rfc1918-192.test/file", "192.168.0.1"),
                         Arguments.of("https://linklocal.test/file", "169.254.1.1"),
                         Arguments.of("https://metadata.test/file", "169.254.169.254"),
                         Arguments.of("https://alicloud-metadata.test/file", "100.100.100.200"),
                         Arguments.of("https://ipv6-loopback.test/file", "::1"),
                         Arguments.of("https://ipv6-linklocal.test/file", "fe80::1"),
                         Arguments.of("https://ipv6-ula.test/file", "fc00::1"));
    }

    @MethodSource
    @ParameterizedTest
    void validateTargetRejectsDeniedAddress(String url, String resolvedIp) {
        AddressValidator validator = validatorResolvingTo(resolvedIp);
        Assertions.assertThrows(SLException.class, () -> validator.validateTarget(url, "job-1"));
    }

    static Stream<Arguments> validateTargetAllowsPublicAddress() {
        return Stream.of(Arguments.of("https://public.test/file", "8.8.8.8"),
                         Arguments.of("https://public.test/file", "93.184.216.34"),
                         Arguments.of("https://alicloud-near.test/file", "100.100.100.199"),
                         Arguments.of("https://ipv6-public.test/file", "2001:4860:4860::8888"));
    }

    @MethodSource
    @ParameterizedTest
    void validateTargetAllowsPublicAddress(String url, String resolvedIp) {
        AddressValidator validator = validatorResolvingTo(resolvedIp);
        Assertions.assertDoesNotThrow(() -> validator.validateTarget(url, "job-1"));
    }

    @Test
    void validateTargetRejectsWhenAnyResolvedAddressIsDenied() {
        AddressValidator validator = validatorResolvingTo("8.8.8.8", "127.0.0.1");
        Assertions.assertThrows(SLException.class, () -> validator.validateTarget("https://mixed.test/file", "job-1"));
    }

    @Test
    void validateTargetRejectsUrlWithoutHost() {
        AddressValidator validator = validatorResolvingTo("8.8.8.8");
        Assertions.assertThrows(SLException.class, () -> validator.validateTarget("file:///etc/passwd", "job-1"));
    }

    private static AddressValidator validatorResolvingTo(String... ips) {
        ApplicationConfiguration configuration = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(configuration.isDeployFromUrlAddressValidationEnabled())
               .thenReturn(true);
        Mockito.when(configuration.getDeployFromUrlAddressDenyList())
               .thenReturn(DENY_LIST);
        return new AddressValidator(configuration) {
            @Override
            protected InetAddress[] resolveAddresses(String host) {
                try {
                    InetAddress[] addresses = new InetAddress[ips.length];
                    for (int i = 0; i < ips.length; i++) {
                        addresses[i] = InetAddress.getByName(ips[i]);
                    }
                    return addresses;
                } catch (UnknownHostException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }
}
