package com.sap.cloud.lm.sl.cf.core.cf.v3;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.util.UserMessageLogger;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.ResolverBuilder;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

public class CloudModelBuilderTest extends com.sap.cloud.lm.sl.cf.core.cf.v2.CloudModelBuilderTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                                 String deployedMtaLocation, String namespace, boolean applyNamespace,
                                 String[] mtaArchiveModules, String[] mtaModules, String[] deployedApps, Expectation expectedServices,
                                 Expectation expectedApps) {
        super(deploymentDescriptorLocation,
              extensionDescriptorLocation,
              platformsLocation,
              deployedMtaLocation,
              namespace,
              applyNamespace,
              mtaArchiveModules,
              mtaModules,
              deployedApps,
              expectedServices,
              expectedApps);
        MockitoAnnotations.initMocks(this);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (00) Test missing resource type definition:
            {
                "mtad-missing-resource-type-definition.yaml", "config-01.mtaext", "/mta/cf-platform.json", null,
                null, false,
                new String[] { "foo" }, // mtaArchiveModules
                new String[] { "foo" }, // mtaModules
                new String[] {}, // deployedApps
                new Expectation("[]"),
                new Expectation(Expectation.Type.JSON, "apps-01.json"),
            },
// @formatter:on
        });
    }

    @Override
    protected HandlerFactory getHandlerFactory() {
        return new HandlerFactory(3);
    }

    @Override
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                           boolean prettyPrinting, DeployedMta deployedMta) {
        deploymentDescriptor = new DescriptorReferenceResolver(deploymentDescriptor,
                                                               new ResolverBuilder(),
                                                               new ResolverBuilder()).resolve();
        return new com.sap.cloud.lm.sl.cf.core.cf.v2.ApplicationCloudModelBuilder(deploymentDescriptor,
                                                                                  prettyPrinting,
                                                                                  deployedMta,
                                                                                  DEPLOY_ID,
                                                                                  namespace,
                                                                                  Mockito.mock(UserMessageLogger.class));
    }

    @Override
    protected UserMessageLogger getUserMessageLogger() {
        return userMessageLogger;
    }

    @Test
    public void testWarnMessage() {
        resourcesCalculator.calculateContentForBuilding(deploymentDescriptor.getResources());
        Mockito.verify(userMessageLogger)
               .warn(Mockito.anyString(), Mockito.any());
    }
}
