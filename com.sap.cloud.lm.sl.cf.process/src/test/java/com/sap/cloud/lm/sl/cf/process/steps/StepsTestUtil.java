package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class StepsTestUtil {

    private static final String COULD_NOT_LOAD_PLATFORM_TYPES = "Could not load test platform types: {0}";
    private static final String COULD_NOT_LOAD_PLATFORMS = "Could not load test platforms: {0}";
    private static final String COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR = "Could not load test deployment descriptor: {0}";

    public static List<TargetPlatformType> loadPlatformTypes(ConfigurationParser parser, String location, Class<?> testClass) {
        try {
            return parser.parsePlatformTypesJson(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORM_TYPES, e.getMessage()));
            return null;
        }
    }

    public static DeploymentDescriptor loadDeploymentDescriptor(DescriptorParser parser, String location, Class<?> testClass) {
        try {
            return parser.parseDeploymentDescriptorYaml(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static List<TargetPlatform> loadPlatforms(ConfigurationParser parser, String location, Class<?> testClass) {
        try {
            return parser.parsePlatformsJson(TestUtil.getResourceAsString(location, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORMS, e.getMessage()));
            return null;
        }
    }

    public static void printingAssertEquals(String expected, String actual) {
        System.out.println(actual);
        assertEquals(expected, actual);
    }

}
