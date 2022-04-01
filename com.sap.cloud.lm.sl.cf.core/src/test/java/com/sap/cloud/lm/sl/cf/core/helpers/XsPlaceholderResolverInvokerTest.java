package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser;
import com.sap.cloud.lm.sl.mta.message.Messages;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class XsPlaceholderResolverInvokerTest {

    private static final String DEFAULT_DOMAIN = "localhost";
    private static final String PROTOCOL = "http";
    private static final String _AUTH_ENDPOINT = "http://localhost:9999/uaa";
    private static final String DS_SERVICE_URL = "http://localhost:8888";
    private Integer schemaVersion;
    private String descriptorLocation;
    private Expectation expectation;
    public XsPlaceholderResolverInvokerTest(String descriptorLocation, Integer schemaVersion, Expectation expectation) {
        this.schemaVersion = schemaVersion;
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) MTA spec version 2.0.0:
            {
                "xs-placeholder-mtad-01.yaml", 2, new Expectation(Expectation.Type.RESOURCE, "xs-placeholder-resolved-mtad-01.json"),
            },
// @formatter:on
        });
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

        }, expectation, getClass());
    }

    private DescriptorParser getDescriptorParser() {
        switch (schemaVersion) {
            case 2:
                return new com.sap.cloud.lm.sl.mta.handlers.v2.DescriptorParser();
            default:
                throw new UnsupportedOperationException(MessageFormat.format(Messages.UNSUPPORTED_VERSION, schemaVersion));
        }
    }

}
