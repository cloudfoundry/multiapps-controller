package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.mta.model.Module;

@RunWith(Parameterized.class)
public class ParametersValidatorHelperTest {

    @SuppressWarnings("serial")
    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // [0]
            {new TreeMap<String, Object>() {{
                    put("port", 1234);
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
                    put("routes", Arrays.asList(new TreeMap<String, String>() {{
                                                        put("route", "only_one%route.$$$in.need$$of$$$correction^^^");
                                                }}));
                    put("host", "a-proper-host");
                    put("ports", Arrays.asList(1, 2));
                    put("domains", Arrays.asList("one.correct.domain", "and#one%with@special^^characters"));
                }},
                new TreeMap<String, Object>() {{
                    put("routes", Arrays.asList(new TreeMap<String, String>() {{
                                                        put("route", "only-one-route.in.need--of---correction");
                                                }}));
                    put("host", "a-proper-host");
                    put("ports", Arrays.asList(1, 2));
                    put("domains", Arrays.asList("one.correct.domain", "and-one-with-special--characters"));
                }}
            },
// @formatter:on
        });
    }

    private List<ParameterValidator> validators = Arrays.asList(new PortValidator(), new HostValidator(), new DomainValidator(),
        new RoutesValidator());
    private ParametersValidatorHelper validatorHelper = new ParametersValidatorHelper(validators, false);
    private Module container = Module.createV2();

    private Map<String, Object> initialParameters;
    private Map<String, Object> correctParameters;

    public ParametersValidatorHelperTest(Map<String, Object> initialParameters, Map<String, Object> correctParameters) {
        this.initialParameters = initialParameters;
        this.correctParameters = correctParameters != null ? correctParameters : initialParameters;
    }

    @Test
    public void testValidate() {
        Map<String, Object> afterCorrection = validatorHelper.validate("", container, container.getClass(), initialParameters);
        assertEquals(correctParameters, afterCorrection);
    }
}
