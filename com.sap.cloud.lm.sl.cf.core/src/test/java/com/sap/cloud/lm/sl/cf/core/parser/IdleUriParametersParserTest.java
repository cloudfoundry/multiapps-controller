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
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class IdleUriParametersParserTest {

    private final Tester tester = Tester.forClass(getClass());

    private static final String DEFAULT_HOST = "default-host";
    private static final String DEFAULT_DOMAIN = "default-domain";

    public static Stream<Arguments> testIdleUriParametersParsing() {
        // @formatter:off
        return Stream.of(
                // with default host and default domain
                Arguments.of(null, null, null, null,
                        new Expectation(Expectation.Type.STRING, Arrays.asList(DEFAULT_HOST + "." + DEFAULT_DOMAIN).toString())),
                // one idle host with default domain
                Arguments.of("hello-host", null, null, null,
                        new Expectation(Expectation.Type.STRING, Arrays.asList("hello-host" + "." + DEFAULT_DOMAIN).toString())),
                // two idle hosts with default domain
                Arguments.of(null, Arrays.asList("hello-host", "hello-host-another"), null, null,
                        new Expectation(Expectation.Type.STRING,
                                Arrays.asList("hello-host" + "." + DEFAULT_DOMAIN, "hello-host-another" + "." + DEFAULT_DOMAIN).toString())),
                // one idle domain with default host
                Arguments.of(null, null, "hello-domain", null,
                        new Expectation(Expectation.Type.STRING, Arrays.asList(DEFAULT_HOST + "." + "hello-domain").toString())),
                // two idle domains with default host
                Arguments.of(null, null, null, Arrays.asList("hello-domain", "hello-another-domain"),
                        new Expectation(Expectation.Type.STRING,
                                Arrays.asList(DEFAULT_HOST + "." + "hello-domain", DEFAULT_HOST + "." + "hello-another-domain").toString())),
                // one idle host with one idle domain
                Arguments.of("hello-host", null, "hello-domain", null,
                        new Expectation(Expectation.Type.STRING, Arrays.asList("hello-host.hello-domain").toString())),
                // two idle hosts with one idle domain
                Arguments.of(null, Arrays.asList("hello-host", "hello-host-another"), "hello-domain", null,
                        new Expectation(Expectation.Type.STRING, Arrays.asList("hello-host.hello-domain", "hello-host-another.hello-domain").toString())),
                // one idle host with two idle domains
                Arguments.of("hello-host", null, null, Arrays.asList("hello-domain", "hello-another-domain"),
                        new Expectation(Expectation.Type.STRING, Arrays.asList("hello-host.hello-domain", "hello-host.hello-another-domain").toString())),
                // two idle hosts with two idle domains
                Arguments.of(null, Arrays.asList("hello-host", "hello-host-another"), null, Arrays.asList("hello-domain", "hello-another-domain"),
                        new Expectation(Expectation.Type.STRING,
                                Arrays.asList("hello-host.hello-domain", "hello-host-another.hello-domain", 
                                        "hello-host.hello-another-domain", "hello-host-another.hello-another-domain").toString()))
        );
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testIdleUriParametersParsing(String idleHost, List<String> idleHosts, String idleDomain, List<String> idleDomains,
                                             Expectation expectation) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(SupportedParameters.IDLE_HOST, idleHost);
        parameters.put(SupportedParameters.IDLE_HOSTS, idleHosts);
        parameters.put(SupportedParameters.IDLE_DOMAIN, idleDomain);
        parameters.put(SupportedParameters.IDLE_DOMAINS, idleDomains);

        IdleUriParametersParser idleParser = new IdleUriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null);
        tester.test(() -> idleParser.parse(Arrays.asList(parameters)), expectation);
    }

}
