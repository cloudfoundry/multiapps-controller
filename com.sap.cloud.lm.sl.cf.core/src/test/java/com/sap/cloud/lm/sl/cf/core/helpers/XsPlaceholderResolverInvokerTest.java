package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.handlers.DescriptorParserFacade;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

@RunWith(Parameterized.class)
public class XsPlaceholderResolverInvokerTest {

    private static final String DEFAULT_DOMAIN = "localhost";
    private static final String PROTOCOL = "http";
    private static final String AUTH_ENDPOINT = "http://localhost:9999/uaa";
    private static final String DS_SERVICE_URL = "http://localhost:8888";

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) MTA spec version 2.0.0:
            {
                "xs-placeholder-mtad-01.yaml", new Expectation(Expectation.Type.RESOURCE, "xs-placeholder-resolved-mtad-01.json"),
            },
// @formatter:on
        });
    }

    private String descriptorLocation;
    private Expectation expectation;

    public XsPlaceholderResolverInvokerTest(String descriptorLocation, Expectation expectation) {
        this.descriptorLocation = descriptorLocation;
        this.expectation = expectation;
    }

    @Test
    public void testInvoke() throws Exception {
        String descriptorString = TestUtil.getResourceAsString(descriptorLocation, getClass());
        DescriptorParserFacade descriptorParserFacade = new DescriptorParserFacade();
        DeploymentDescriptor descriptor = descriptorParserFacade.parseDeploymentDescriptor(descriptorString);
        XsPlaceholderResolver xsPlaceholderResolver = new XsPlaceholderResolver();
        xsPlaceholderResolver.setDeployServiceUrl(DS_SERVICE_URL);
        xsPlaceholderResolver.setAuthorizationEndpoint(AUTH_ENDPOINT);
        xsPlaceholderResolver.setProtocol(PROTOCOL);
        xsPlaceholderResolver.setDefaultDomain(DEFAULT_DOMAIN);
        TestUtil.test(() -> {

            descriptor.accept(new XsPlaceholderResolverInvoker(xsPlaceholderResolver));
            return descriptor;

        }, expectation, getClass());
    }

}
