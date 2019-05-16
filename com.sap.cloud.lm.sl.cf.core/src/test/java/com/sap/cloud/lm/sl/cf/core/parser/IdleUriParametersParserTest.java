package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class IdleUriParametersParserTest {

    private final Tester tester = Tester.forClass(getClass());

    public static Stream<Arguments> testResolve() {
        return Stream.of(
        // @formatter:off
            // TODO: add actual tests
            Arguments.of("parameters1.json", new Expectation(Expectation.Type.JSON, "parsed-routes.json")));
        // @formatter:on
    }

    @ParameterizedTest
    @MethodSource
    public void testResolve(String modulePropertiesLocation, Expectation expectation) {
        List<?> moduleProperties = TestUtil.getList(modulePropertiesLocation, getClass());
        // TODO: a better way to do this?
        for (Object props : moduleProperties) {
            if (props instanceof Map) {
                Map propsMap = (Map<String, Object>) props;
                for (Object key : propsMap.keySet()) {
                    if (propsMap.get(key) instanceof Double) {
                        propsMap.put(key, new Integer(((Double) propsMap.get(key)).intValue()));
                    }
                }
            }
        }

        IdleUriParametersParser idleParser = new IdleUriParametersParser(true, "not-used", "not-used", 1, "not-used", true, "protocol");

        tester.test(() -> idleParser.parse((List<Map<String, Object>>) moduleProperties), expectation);
    }

}
