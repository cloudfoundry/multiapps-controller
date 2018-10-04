package com.sap.cloud.lm.sl.cf.core.validators.parameters.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.v3.VisibilityValidator;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.v3.ProvidedDependency;

@RunWith(Parameterized.class)
public class VisibilityValidatorTest {
    private VisibilityValidator validator = new VisibilityValidator();

    private boolean isValid;
    private String visibleTargets;

    public VisibilityValidatorTest(String visibleTargets, boolean isValid) {
        this.visibleTargets = visibleTargets;
        this.isValid = isValid;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
    // @formatter:off
            // (0)
            { "[{\"org\":\"org1\",\"space\":\"space1\"}]", true, },
            // (1)
            { "[{\"org\":\"org1\",\"space\":\"space1\"},{\"org\":\"org2\",\"space\":\"space2\"}]", true, },
            // (2) Test with only org without space:
            { "[{\"org\":\"org1\"}]", true, },
            // (3) Test with random object
            { "randomString", false, },
            // (4) Test with only space without org:
            { "[{\"space\":\"space1\"}]", false, },
            // (5) Not a List
            { "{\"org\":\"org1\",\"space\":\"space1\"}", false, },
            // (6) Test with org that is integer:
            { "[{\"org\": 3,\"space\":\"space1\"}]", false, },
            // (7) Test with space that is integer:
            { "[{\"org\":\"org1\",\"space\": 3}]", false, },
            // (8) Test with org and space that are integers:
            { "[{\"org\": 3,\"space\": 3}]", false, },
    // @formatter:on
        });
    }

    @Test
    public void testValidate() {
        Object visibleTargetsObject = JsonUtil.fromJson(visibleTargets, Object.class);
        assertEquals(isValid, validator.isValid(visibleTargetsObject));
    }

    @Test
    public void testGetParameterName() {
        assertEquals("visibility", validator.getParameterName());
    }

    @Test
    public void testGetContainerType() {
        assertTrue(validator.getContainerType()
            .isAssignableFrom(ProvidedDependency.class));
    }
}
