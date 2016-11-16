package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.message.Messages;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class XsPlaceholderResolverInvokerTest {

    private static final String DEFAULT_DOMAIN = "localhost";
    private static final String PROTOCOL = "http";
    private static final String _AUTH_ENDPOINT = "http://localhost:9999/uaa";
    private static final String DS_SERVICE_URL = "http://localhost:8888";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) MTA spec version 1.0.0:
            {
                "xs-placeholder-mtad-00.yaml", 1, "R:xs-placeholder-resolved-mtad-00.json",
            },
            // (1) MTA spec version 2.0.0:
            {
                "xs-placeholder-mtad-01.yaml", 2, "R:xs-placeholder-resolved-mtad-01.json",
            },
// @formatter:on
        });
    }

    private Integer schemaVersion;
    private String expected;
    private String descriptorLocation;

    public XsPlaceholderResolverInvokerTest(String descriptorLocation, Integer schemaVersion, String expected) {
        this.schemaVersion = schemaVersion;
        this.expected = expected;
        this.descriptorLocation = descriptorLocation;
    }

    @Test
    public void testInvoke() throws Exception {
        String descriptorString = TestUtil.getResourceAsString(descriptorLocation, getClass());
        DeploymentDescriptor descriptor = getDescriptorParser().parseDeploymentDescriptorYaml(descriptorString);
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDeployServiceUrl(DS_SERVICE_URL);
        xsPlaceholderResolver.setAuthorizationEndpoint(_AUTH_ENDPOINT);
        xsPlaceholderResolver.setProtocol(PROTOCOL);
        xsPlaceholderResolver.setDefaultDomain(DEFAULT_DOMAIN);
        TestUtil.test(() -> {

            descriptor.accept(new XsPlaceholderResolverInvoker(schemaVersion, xsPlaceholderResolver));
            return descriptor;

        } , expected, getClass());
    }

    private DescriptorParser getDescriptorParser() {
        switch (schemaVersion) {
            case 2:
                return new com.sap.cloud.lm.sl.mta.handlers.v2_0.DescriptorParser();
            case 1:
                return new com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser();
            default:
                throw new UnsupportedOperationException(MessageFormat.format(Messages.UNSUPPORTED_VERSION, schemaVersion));
        }
    }

}
