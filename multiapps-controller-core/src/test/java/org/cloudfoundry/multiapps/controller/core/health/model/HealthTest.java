package org.cloudfoundry.multiapps.controller.core.health.model;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.core.type.TypeReference;

@RunWith(Parameterized.class)
public class HealthTest {

    private final Tester tester = Tester.forClass(getClass());

    @Parameters
    public static Iterable<Object[]> getParameter() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                "successful-operations.json", new Expectation(Expectation.Type.JSON, "good-health.json"), 
            },
            // (1)
            {
                "failed-operations.json", new Expectation(Expectation.Type.JSON, "poor-health.json"), 
            },
            // @formatter:on
        });
    }

    private final String operationsJsonLocation;
    private final Expectation expectation;

    private List<Operation> operations;

    public HealthTest(String operationsJsonLocation, Expectation expectation) {
        this.operationsJsonLocation = operationsJsonLocation;
        this.expectation = expectation;
    }

    @Before
    public void loadOperations() {
        String operationsJson = TestUtil.getResourceAsString(operationsJsonLocation, getClass());
        this.operations = JsonUtil.fromJson(operationsJson, new TypeReference<List<Operation>>() {
        });
    }

    @Test
    public void testFromOperations() {
        tester.test(() -> Health.fromOperations(operations), expectation);
    }

}
