package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.mta.model.Module;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ParametersValidatorHelperTest {

    @SuppressWarnings("serial")
    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // [0]
            {new TreeMap<String, Object>() {{
                    put("domain", "correct-domain.com");
                }},
                null
            },
            // [1]
            {new TreeMap<String, Object>() {{
                    put("hosts", Arrays.asList("correct-host-1", "correct-host-2"));
                    put("domain", "correct-domain.com");
                }},
                null
            },
            // [2]
            {new TreeMap<String, Object>() {{
                    put("routes", Arrays.asList(new TreeMap<String, String>() {{
                                                        put("route", "already-correct-route.com");
                                                }}, new TreeMap<String, String>() {{
                                                        put("route", "route_for_correction.com");
                                                }}));
                }},
                new TreeMap<String, Object>() {{
                    put("routes", Arrays.asList(new TreeMap<String, String>() {{
                                                        put("route", "already-correct-route.com");
                                                }}, new TreeMap<String, String>() {{
                                                        put("route", "route-for-correction.com");
                                                }}));
                    }}
            },
            // [3]
            {new TreeMap<String, Object>() {{
                    put("routes", Collections.singletonList(new TreeMap<String, String>() {{
                        put("route", "only_one%route.$$$in.need$$of$$$correction^^^");
                    }}));
                    put("host", "a-proper-host");
                    put("domains", Arrays.asList("one.correct.domain", "and#one%with@special^^characters"));
                }},
                new TreeMap<String, Object>() {{
                    put("routes", Collections.singletonList(new TreeMap<String, String>() {{
                        put("route", "only-one-route.in.need--of---correction");
                    }}));
                    put("host", "a-proper-host");
                    put("domains", Arrays.asList("one.correct.domain", "and-one-with-special--characters"));
                }}
            },
            // [4]
            {new TreeMap<String, Object>() {{
                    put("app-name", "app1");
                    put("apply-namespace", Boolean.TRUE);
                }},
                new TreeMap<String, Object>() {{
                    put("app-name", "namespace-app1");
                    put("apply-namespace", Boolean.TRUE);
                }}
            },
            // [5]
            {new TreeMap<String, Object>() {{
                    put("app-name", "app2");
                    put("apply-namespace", Boolean.FALSE);
                }},
                new TreeMap<String, Object>() {{
                    put("app-name", "app2");
                    put("apply-namespace", Boolean.FALSE);
                }}
            },
            // [4]
            {new TreeMap<String, Object>() {{
                    put("app-name", "app3");
                }},
                new TreeMap<String, Object>() {{
                    put("app-name", "namespace-app3");
                }}
            },
// @formatter:on
        });
    }

    private final List<ParameterValidator> validators = Arrays.asList(new HostValidator(), new DomainValidator(), new RoutesValidator(),
                                                                      new ApplicationNameValidator("namespace", true));
    private final ParametersValidatorHelper validatorHelper = new ParametersValidatorHelper(validators, false);
    private final Module container = Module.createV2();

    private final Map<String, Object> initialParameters;
    private final Map<String, Object> correctParameters;

    public ParametersValidatorHelperTest(Map<String, Object> initialParameters, Map<String, Object> correctParameters) {
        this.initialParameters = initialParameters;
        this.correctParameters = correctParameters != null ? correctParameters : initialParameters;
    }

    @Test
    public void testValidate() {
        Map<String, Object> afterCorrection = validatorHelper.validate("", container.getClass(), initialParameters);
        assertEquals(correctParameters, afterCorrection);
    }
}
