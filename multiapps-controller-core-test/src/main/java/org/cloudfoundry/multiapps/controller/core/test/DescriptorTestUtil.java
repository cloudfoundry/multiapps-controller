package org.cloudfoundry.multiapps.controller.core.test;

import java.text.MessageFormat;

import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.mta.handlers.ConfigurationParser;
import org.cloudfoundry.multiapps.mta.handlers.DescriptorParserFacade;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.Platform;
import org.junit.jupiter.api.Assertions;

public class DescriptorTestUtil {

    private static final String COULD_NOT_LOAD_PLATFORM = "Could not load test platform: {0}";
    private static final String COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR = "Could not load test deployment descriptor: {0}";
    private static final DescriptorParserFacade DESCRIPTOR_PARSER_FACADE = new DescriptorParserFacade();

    public static Platform loadPlatform(String filePath, Class<?> testClass) {
        try {
            return new ConfigurationParser().parsePlatformJson(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            Assertions.fail(MessageFormat.format(COULD_NOT_LOAD_PLATFORM, e.getMessage()));
            return null;
        }
    }

    public static DeploymentDescriptor loadDeploymentDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseDeploymentDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            Assertions.fail(MessageFormat.format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

    public static ExtensionDescriptor loadExtensionDescriptor(String filePath, Class<?> testClass) {
        try {
            return DESCRIPTOR_PARSER_FACADE.parseExtensionDescriptor(TestUtil.getResourceAsString(filePath, testClass));
        } catch (Exception e) {
            Assertions.fail(MessageFormat.format(COULD_NOT_LOAD_DEPLOYMENT_DESCRIPTOR, e.getMessage()));
            return null;
        }
    }

}
