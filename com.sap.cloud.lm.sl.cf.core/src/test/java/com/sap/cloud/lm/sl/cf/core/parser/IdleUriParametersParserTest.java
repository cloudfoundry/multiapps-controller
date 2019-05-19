package com.sap.cloud.lm.sl.cf.core.parser;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;

public class IdleUriParametersParserTest {

    private final Tester tester = Tester.forClass(getClass());

    @Test
    public void testResolve() {
        String parametersJson = TestUtil.getResourceAsString("parameters.json", getClass());
        List<Map<String, Object>> parameters = JsonUtil.fromJson(parametersJson, new TypeReference<List<Map<String, Object>>>() {
        });

        IdleUriParametersParser idleParser = new IdleUriParametersParser("not-used", "not-used", "not-used");

        tester.test(() -> idleParser.parse(parameters), new Expectation(Expectation.Type.JSON, "parsed-routes.json"));
    }

}
