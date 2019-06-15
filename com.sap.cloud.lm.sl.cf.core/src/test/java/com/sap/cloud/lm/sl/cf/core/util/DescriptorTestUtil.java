package com.sap.cloud.lm.sl.cf.core.util;

import static java.text.MessageFormat.format;
import static org.junit.Assert.fail;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ExtensionDescriptor;
import com.sap.cloud.lm.sl.mta.model.Platform;

public class DescriptorTestUtil {

    private static final String COULD_NOT_LOAD_PLATFORM = "Could not load test platform: {0}";
    private static final String COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR = "Could not load test deployment descriptor: {0}";
    private static final DescriptorParserFacade DESCRIPTOR_PARSER_FACADE = new DescriptorParserFacade();

    public static Platform loadPlatform(String filePath, Class<?> testClass) {
        try {
            return new ConfigurationParser().parsePlatformJson(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_PLATFORM, e.getMessage()));
            return null;
        }
    }

    public static DeploymentDescriptor loadDeploymentDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseDeploymentDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static ExtensionDescriptor loadExtensionDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseExtensionDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            fail(format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

}
