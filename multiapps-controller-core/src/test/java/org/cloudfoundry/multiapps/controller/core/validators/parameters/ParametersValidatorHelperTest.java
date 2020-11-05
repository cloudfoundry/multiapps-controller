package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParametersValidatorHelperTest {

    private final List<ParameterValidator> validators = List.of(new HostValidator(null, false), new DomainValidator(),
                                                                new RoutesValidator(null, false),
                                                                new ApplicationNameValidator("namespace", true));
    private final ParametersValidatorHelper validatorHelper = new ParametersValidatorHelper(validators, false);
    private final Module container = Module.createV2();

    @SuppressWarnings("serial")
    public static Stream<Arguments> testValidate() {
        return Stream.of(
// @formatter:off
            // [0]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("domain", "correct-domain.com");
                         }},
                    null),
            // [1]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("hosts", List.of("correct-host-1", "correct-host-2"));
                             put("domain", "correct-domain.com");
                         }},
                    null),
            // [2]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("routes", List.of(new TreeMap<String, String>() {{
                                 put("route", "already-correct-route.com");
                             }}, new TreeMap<String, String>() {{
                                 put("route", "route_for_correction.com");
                             }}));
                         }},
                    new TreeMap<String, Object>() {{
                        put("routes", List.of(new TreeMap<String, String>() {{
                            put("route", "already-correct-route.com");
                        }}, new TreeMap<String, String>() {{
                            put("route", "route-for-correction.com");
                        }}));
                    }}),
            // [3]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("routes", List.of(new TreeMap<String, String>() {{
                                 put("route", "only_one%route.$$$in.need$$of$$$correction^^^");
                             }}));
                             put("host", "a-proper-host");
                             put("domains", List.of("one.correct.domain", "and#one%with@special^^characters"));
                         }},
                    new TreeMap<String, Object>() {{
                        put("routes", List.of(new TreeMap<String, String>() {{
                            put("route", "only-one-route.in.need--of---correction");
                        }}));
                        put("host", "a-proper-host");
                        put("domains", List.of("one.correct.domain", "and-one-with-special--characters"));
                    }}),
            // [4]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("app-name", "app1");
                             put("apply-namespace", Boolean.TRUE);
                         }},
                    new TreeMap<String, Object>() {{
                        put("app-name", "namespace-app1");
                        put("apply-namespace", Boolean.TRUE);
                    }}),
            // [5]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("app-name", "app2");
                             put("apply-namespace", Boolean.FALSE);
                         }},
                    new TreeMap<String, Object>() {{
                        put("app-name", "app2");
                        put("apply-namespace", Boolean.FALSE);
                    }}),
            // [6]
            Arguments.of(new TreeMap<String, Object>() {{
                             put("app-name", "app3");
                         }},
                    new TreeMap<String, Object>() {{
                        put("app-name", "namespace-app3");
                    }})
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testValidate(Map<String, Object> initialParameters, Map<String, Object> correctParameters) {
        Map<String, Object> afterCorrection = validatorHelper.validate("", container.getClass(), initialParameters);
        assertEquals(correctParameters != null ? correctParameters : initialParameters, afterCorrection);
    }
}
