package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;;

public class UriParametersParserTest {

    private final Tester tester = Tester.forClass(getClass());

    private static final String DEFAULT_HOST = "test-host";
    private static final String DEFAULT_DOMAIN = "default-domain.com";

    public static Stream<Arguments> testUriParameterParsing() {
        // @formatter:off
        return Stream.of(
            // with no uri parameters
            Arguments.of(null, null, null, null, 
                new Expectation(Expectation.Type.STRING, Arrays.asList(DEFAULT_HOST + "." + DEFAULT_DOMAIN).toString())),
            // with only host parameter
            Arguments.of("some-host", null, null, null, 
                new Expectation(Expectation.Type.STRING, Arrays.asList("some-host." + DEFAULT_DOMAIN).toString())),
            // with host and domain parameters
            Arguments.of("some-host", "some-domain.com", null, null,
                new Expectation(Expectation.Type.STRING, Arrays.asList("some-host.some-domain.com").toString())),
            // with plural hosts and domains parameters
            Arguments.of(null, null, Arrays.asList("host1", "host2"), Arrays.asList("domain1.com", "domain2.com"),
                new Expectation(Expectation.Type.STRING, 
                    Arrays.asList("host1.domain1.com", "host2.domain1.com", "host1.domain2.com", "host2.domain2.com").toString())),
            // with both singular and plural parameters, testing that only the plural parameters are taken
            Arguments.of("host1", "domain1.com", Arrays.asList("host2"), Arrays.asList("domain2.com", "domain3.com"),
                new Expectation(Expectation.Type.STRING, Arrays.asList("host2.domain2.com", "host2.domain3.com").toString()))
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testUriParameterParsing(String host, String domain, List<String> hosts, List<String> domains, Expectation expectation) {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(SupportedParameters.HOST, host);
        parameterMap.put(SupportedParameters.HOSTS, hosts);
        parameterMap.put(SupportedParameters.DOMAIN, domain);
        parameterMap.put(SupportedParameters.DOMAINS, domains);

        tester.test(() -> new UriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(Arrays.asList(parameterMap)), expectation);
    }
}
