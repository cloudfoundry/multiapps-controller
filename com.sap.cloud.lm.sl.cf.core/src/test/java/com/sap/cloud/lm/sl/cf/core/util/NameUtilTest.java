package com.sap.cloud.lm.sl.cf.core.util;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.repeat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil.NameRequirements;
import com.sap.cloud.lm.sl.common.ContentException;

public class NameUtilTest {

    private static final String LONG_ENV_NAME = repeat("f", NameRequirements.ENVIRONMENT_NAME_MAX_LENGTH + 1);
    private static final List<String> VALID_ENV_NAMES = Arrays.asList("_foo", "Foo", "foo_bar", "Foo_Bar", "x_1");
    private static final List<String> INVALID_ENV_NAMES = Arrays.asList("1foo", "   ", "foo bar", "foo-bar", "x&y", LONG_ENV_NAME);

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testIsValidXsAppNameWhenNamesAreValid() {
        testIsValidName(Arrays.asList("Fo0", "foo.bar", "foo_bar", "foo-bar", "//foo\\"), NameRequirements.XS_APP_NAME_PATTERN, true);
    }

    @Test
    public void testIsValidXsAppNameWhenNamesAreInvalid() {
        String longName = repeat("f", NameRequirements.XS_APP_NAME_MAX_LENGTH + 1);

        testIsValidName(Arrays.asList("sap_system", "sap_system_1", "foo&&bar", "foo bar", longName), NameRequirements.XS_APP_NAME_PATTERN,
            false);
    }

    @Test
    public void testIsValidEnvNameWhenNamesAreValid() {
        testIsValidName(VALID_ENV_NAMES, NameRequirements.ENVIRONMENT_NAME_PATTERN, true);
    }

    @Test
    public void testIsValidEnvNameWhenNamesAreInvalid() {
        testIsValidName(INVALID_ENV_NAMES, NameRequirements.ENVIRONMENT_NAME_PATTERN, false);
    }

    @Test
    public void testIsValidContainerNameWhenNamesAreValid() {
        testIsValidName(Arrays.asList("FOO", "FOO_BAR", "1_FOO", "FOO_1_BAR"), NameRequirements.CONTAINER_NAME_PATTERN, true);
    }

    @Test
    public void testIsValidContainerNameWhenNamesAreInvalid() {
        String longName = repeat("F", NameRequirements.CONTAINER_NAME_MAX_LENGTH + 1);

        testIsValidName(Arrays.asList("foo", "FOO BAR", "1-FOO", "FOO_&_BAR", "_FOO", " ", longName),
            NameRequirements.CONTAINER_NAME_PATTERN, false);
    }

    private void testIsValidName(List<String> names, String namePattern, boolean expectedResult) {
        for (String name : names) {
            assertEquals(name, expectedResult, NameUtil.isValidName(name, namePattern));
        }
    }

    @Test
    public void testEnsureValidEnvNameAreValidNotAllowInvalidEnvNames() {
        for (String validEnvName : VALID_ENV_NAMES) {
            NameUtil.ensureValidEnvName(validEnvName, false);
        }
    }

    @Test
    public void testEnsureValidEnvNameAreInvalidNotAllowInvalidEnvNames() {
        for (String invalidEnvName : INVALID_ENV_NAMES) {
            expectedEx.expect(ContentException.class);
            expectedEx.expectMessage(MessageFormat.format(Messages.INVALID_ENVIRONMENT_VARIABLE_NAME, invalidEnvName));
            NameUtil.ensureValidEnvName(invalidEnvName, false);
        }
    }

    @Test
    public void testEnsureValidEnvNameAreInvalidAllowInvalidEnvNames() {
        for (String invalidEnvName : INVALID_ENV_NAMES) {
            NameUtil.ensureValidEnvName(invalidEnvName, true);
        }
    }

    @Test
    public void testGetNameWithProperLength() throws Exception {
        assertEquals("foo", NameUtil.getNameWithProperLength("foo", 3));
        assertEquals("foo", NameUtil.getNameWithProperLength("foo", 5));

        assertEquals("com.sap.clou3726daf0", NameUtil.getNameWithProperLength("com.sap.cloud.lm.sl.xs2.core.test", 20));
    }

    @Test
    public void testCreateValidContainerName() throws Exception {
        String containerName = NameUtil.createValidContainerName("initial", "initial",
            "com.sap.cloud.lm.sl.xs2.a.very.very.long.service.name.with.illegal.container.name.characters");
        assertEquals("INITIAL_INITIAL_COM_SAP_CLOUD_LM_SL_XS2_A_VERY_VERY_LONG3AC0B612", containerName);
        assertTrue(NameUtil.isValidName(containerName, NameRequirements.CONTAINER_NAME_PATTERN));
    }

    @Test
    public void testCreateValidXsAppName() throws Exception {
        String xsAppName1 = NameUtil.createValidXsAppName("sap_system_com.sap.cloud.lm.sl.xs2.deploy-service-database");
        assertEquals("_com.sap.cloud.lm.sl.xs2.deploy-service-database", xsAppName1);
        assertTrue(NameUtil.isValidName(xsAppName1, NameRequirements.XS_APP_NAME_PATTERN));

        String xsAppName2 = NameUtil.createValidXsAppName("sap_system");
        assertEquals("_", xsAppName2);
        assertTrue(NameUtil.isValidName(xsAppName2, NameRequirements.XS_APP_NAME_PATTERN));

        String xsAppName3 = NameUtil.createValidXsAppName("com.sap.cloud.lm.sl.xs@.deploy-service-database");
        assertEquals("com.sap.cloud.lm.sl.xs_.deploy-service-database", xsAppName3);
        assertTrue(NameUtil.isValidName(xsAppName3, NameRequirements.XS_APP_NAME_PATTERN));
    }

}
