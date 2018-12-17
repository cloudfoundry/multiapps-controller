package com.sap.cloud.lm.sl.cf.core.cf.v3;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelConfiguration;
import com.sap.cloud.lm.sl.cf.core.helpers.XsPlaceholderResolver;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelBuilderTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
        String deployedMtaLocation, boolean useNamespaces, boolean useNamespacesForServices, String[] mtaArchiveModules,
        String[] mtaModules, String[] deployedApps, Expectation expectedServices, Expectation expectedApps) {
        super(deploymentDescriptorLocation, extensionDescriptorLocation, platformsLocation, deployedMtaLocation, useNamespaces,
            useNamespacesForServices, mtaArchiveModules, mtaModules, deployedApps, expectedServices, expectedApps);
        MockitoAnnotations.initMocks(this);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Test missing resource type definition:
            {
                "mtad-missing-resource-type-definition.yaml", "config-01.mtaext", "/mta/cf-platform-v2.json", null,
                false, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.RESOURCE, "apps-01.json"),
            },
// @formatter:on
        });
    }

    @Override
    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(3);
    }

    @Override
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, getPropertiesAccessor(), configuration);
    }

    @Override
    protected ApplicationsCloudModelBuilder getApplicationsCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
        CloudModelConfiguration configuration, DeployedMta deployedMta, SystemParameters systemParameters,
        XsPlaceholderResolver xsPlaceholderResolver) {
        deploymentDescriptor = new DescriptorReferenceResolver((com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor) deploymentDescriptor,
            new ResolverBuilder(), new ResolverBuilder()).resolve();
        return new com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationsCloudModelBuilder(
            (com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor) deploymentDescriptor, configuration, deployedMta, systemParameters,
            xsPlaceholderResolver, DEPLOY_ID);
    }

    @Override
    protected UserMessageLogger getUserMessageLogger() {
        return userMessageLogger;
    }

    @Test
    public void testWarnMessage() {
        resourcesCalculator.calculateContentForBuilding(deploymentDescriptor.getResources2());
        Mockito.verify(userMessageLogger)
            .warn(Mockito.anyString(), Mockito.any());
    }
}
