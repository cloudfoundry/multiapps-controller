package org.cloudfoundry.multiapps.controller.core.cf.v3;

import java.util.Arrays;

import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.controller.core.cf.CloudHandlerFactory;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class CloudModelBuilderTest extends org.cloudfoundry.multiapps.controller.core.cf.v2.CloudModelBuilderTest {

    @Mock
    private UserMessageLogger userMessageLogger;

    public CloudModelBuilderTest(String deploymentDescriptorLocation, String extensionDescriptorLocation, String platformsLocation,
                                 String deployedMtaLocation, String namespace, boolean applyNamespace, String[] mtaArchiveModules,
                                 String[] mtaModules, String[] deployedApps, Expectation expectedServices, Expectation expectedApps,
                                 AppSuffixDeterminer appSuffixDeterminer)
        throws Exception {
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
              expectedApps,
              appSuffixDeterminer);
        MockitoAnnotations.openMocks(this)
                          .close();
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
                DEFAULT_APP_SUFFIX_DETERMINER,
            },
// @formatter:on
        });
    }

    @Override
    protected CloudHandlerFactory getHandlerFactory() {
        return CloudHandlerFactory.forSchemaVersion(3);
    }

    @Override
    protected ServicesCloudModelBuilder getServicesCloudModelBuilder(DeploymentDescriptor deploymentDescriptor) {
        return new ServicesCloudModelBuilder(deploymentDescriptor, namespace);
    }

    @Override
    protected ApplicationCloudModelBuilder getApplicationCloudModelBuilder(DeploymentDescriptor deploymentDescriptor,
                                                                           boolean prettyPrinting, DeployedMta deployedMta,
                                                                           AppSuffixDeterminer appSuffixDeterminer) {
        deploymentDescriptor = new DescriptorReferenceResolver(deploymentDescriptor,
                                                               new ResolverBuilder(),
                                                               new ResolverBuilder()).resolve();
        return new org.cloudfoundry.multiapps.controller.core.cf.v2.ApplicationCloudModelBuilder.Builder().deploymentDescriptor(deploymentDescriptor)
                                                                                                          .prettyPrinting(prettyPrinting)
                                                                                                          .deployedMta(deployedMta)
                                                                                                          .deployId(DEPLOY_ID)
                                                                                                          .namespace(namespace)
                                                                                                          .userMessageLogger(Mockito.mock(UserMessageLogger.class))
                                                                                                          .appSuffixDeterminer(appSuffixDeterminer)
                                                                                                          .build();
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
