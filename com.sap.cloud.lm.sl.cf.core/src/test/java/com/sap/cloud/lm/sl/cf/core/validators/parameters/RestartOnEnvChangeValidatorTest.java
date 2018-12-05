package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.Pair;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

class RestartOnEnvChangeValidatorTest {
    private RestartOnEnvChangeValidator validator = new RestartOnEnvChangeValidator();

    @Test
    void testGetParameterName() {
        assertEquals("restart-on-env-change", validator.getParameterName());
    }

    @Test
    void testGetContainerType() {
        assertTrue(validator.getContainerType()
            .isAssignableFrom(Module.class));
    }

    @ParameterizedTest
    @MethodSource
    public void testValidate(Map<String, Boolean> restartParameters, boolean isValid) {
        assertEquals(isValid, validator.isValid(restartParameters));
    }

    public static Stream<Arguments> testValidate() {
        return Stream.of(
        // @formatter:off
            // (1) All conditions are true
            Arguments.of(MapUtil.of(new Pair("vcap-application",true), new Pair("vcap-services",true), new Pair("user-provided",true)), true),
            // (2) All conditions are false
            Arguments.of(MapUtil.of(new Pair("vcap-application",false), new Pair("vcap-services",false), new Pair("user-provided",false)), true),
            // (3) Test with error value for vcap-application
            Arguments.of(MapUtil.of(new Pair("vcap-application","foo.bar"), new Pair("vcap-services",false), new Pair("user-provided",false)), false),
            // (4) Test with error value for vcap-services
            Arguments.of(MapUtil.of(new Pair("vcap-application",false), new Pair("vcap-services","foo.bar"), new Pair("user-provided",true)), false),
            // (5) Test with error value for user-provided
            Arguments.of(MapUtil.of(new Pair("vcap-application",false), new Pair("vcap-services",true), new Pair("user-provided","bar.xyz")), false),
            // (6) Test with not map object
            Arguments.of(null, false),
            // (7) 
            Arguments.of(MapUtil.of(new Pair("vcap-application",false), new Pair("vcap-services",true), new Pair("user-provided",false)), true),
            // (8)
            Arguments.of(MapUtil.of(new Pair("vcap-application",true), new Pair("vcap-services",true), new Pair("user-provided",false)), true),
            // (9) Not all parameters set
            Arguments.of(MapUtil.of(new Pair("vcap-services",false), new Pair("user-provided",true)), true),
            // (10)
            Arguments.of(MapUtil.of(new Pair("user-provided",true)), true),
            // (11)
            Arguments.of(MapUtil.of(new Pair("vcap-application",false)), true)
        // @formatter:on
        );
    }


}
