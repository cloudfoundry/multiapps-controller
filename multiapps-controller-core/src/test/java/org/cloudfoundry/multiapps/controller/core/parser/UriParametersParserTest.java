package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UriParametersParserTest {

    private static final String DEFAULT_HOST = "test-host";
    private static final String DEFAULT_DOMAIN = "default-domain.com";

    private final Tester tester = Tester.forClass(getClass());

    static Stream<Arguments> testUriParameterParsing() {
        return Stream.of(
                         // with no uri parameters
                         Arguments.of(null, null, null, null, null,
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList(DEFAULT_HOST + "." + DEFAULT_DOMAIN)
                                                                 .toString())),
                         // with only host parameter
                         Arguments.of("some-host", null, null, null, null,
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList("some-host." + DEFAULT_DOMAIN)
                                                                 .toString())),
                         // with host and domain parameters
                         Arguments.of("some-host", "some-domain.com", null, null, null,
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList("some-host.some-domain.com")
                                                                 .toString())),
                         // with plural hosts and domains parameters
                         Arguments.of(null, null, Arrays.asList("host1", "host2"), Arrays.asList("domain1.com", "domain2.com"), null,
                                      new Expectation(Expectation.Type.STRING,
                                                      Arrays.asList("host1.domain1.com", "host2.domain1.com", "host1.domain2.com",
                                                                    "host2.domain2.com")
                                                            .toString())),
                         // with both singular and plural parameters, testing that only the plural parameters are taken
                         Arguments.of("host1", "domain1.com", Collections.singletonList("host2"),
                                      Arrays.asList("domain2.com", "domain3.com"), null,
                                      new Expectation(Expectation.Type.STRING,
                                                      Arrays.asList("host2.domain2.com", "host2.domain3.com")
                                                            .toString())),
                         // with only routes parameters
                         Arguments.of(null, null, null, null, Collections.singletonList("my.custom.route"),
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList("my.custom.route")
                                                                 .toString())),
                         // with host and routes parameters - host is ignored
                         Arguments.of("some-host", null, null, null, Collections.singletonList("my.custom.route"),
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList("my.custom.route")
                                                                 .toString())),
                         // with domain and routes parameters - host is ignored
                         Arguments.of(null, "some-domain.com", null, null, Collections.singletonList("my.custom.route"),
                                      new Expectation(Expectation.Type.STRING,
                                                      Collections.singletonList("my.custom.route")
                                                                 .toString())),
                         // with routes parameters containing starting with http schema - it is removed
                         Arguments.of(null, null, null, null, Arrays.asList("https://my.custom.route", "http://*.my.custom.route"),
                                      new Expectation(Expectation.Type.STRING,
                                                      Arrays.asList("my.custom.route", "*.my.custom.route")
                                                            .toString())));
    }

    @ParameterizedTest
    @MethodSource
    void testUriParameterParsing(String host, String domain, List<String> hosts, List<String> domains, List<String> routes,
                                 Expectation expectation) {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.put(SupportedParameters.HOST, host);
        parameterMap.put(SupportedParameters.HOSTS, hosts);
        parameterMap.put(SupportedParameters.DOMAIN, domain);
        parameterMap.put(SupportedParameters.DOMAINS, domains);
        parameterMap.put(SupportedParameters.ROUTES, constructRoutesParameter(routes));

        tester.test(() -> new UriParametersParser(DEFAULT_HOST, DEFAULT_DOMAIN, null).parse(Collections.singletonList(parameterMap)),
                    expectation);
    }

    private List<Map<String, String>> constructRoutesParameter(List<String> routes) {
        if (routes == null) {
            return null;
        }
        return routes.stream()
                     .map(route -> MapUtil.asMap(SupportedParameters.ROUTE, route))
                     .collect(Collectors.toList());
    }
}
