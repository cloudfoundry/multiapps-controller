package org.cloudfoundry.multiapps.controller.core.cf.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.detect.AppSuffixDeterminer;
import org.cloudfoundry.multiapps.controller.core.helpers.ModuleToDeployHelper;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.UserMessageLogger;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.resolvers.ResolverBuilder;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApplicationCloudModelBuilderCfMetadataTest {

    private static final AppSuffixDeterminer NO_SUFFIX = new AppSuffixDeterminer(false, false);
    private static final ModuleToDeployHelper MODULE_HELPER = new ModuleToDeployHelper();

    private ApplicationCloudModelBuilder buildBuilder(DeploymentDescriptor descriptor) {
        DeploymentDescriptor resolved = new DescriptorReferenceResolver(descriptor, new ResolverBuilder(), new ResolverBuilder(),
                                                                        Collections.emptySet()).resolve();
        CloudControllerClient client = Mockito.mock(CloudControllerClient.class);
        Mockito.when(client.getApplicationRoutes(Mockito.any()))
               .thenReturn(Collections.emptyList());
        return new ApplicationCloudModelBuilder.Builder().deploymentDescriptor(resolved)
                                                         .prettyPrinting(false)
                                                         .deployId("test-deploy")
                                                         .userMessageLogger(Mockito.mock(UserMessageLogger.class))
                                                         .appSuffixDeterminer(NO_SUFFIX)
                                                         .client(client)
                                                         .build();
    }

    private static DeploymentDescriptor descriptorWithModule(Module module) {
        DeploymentDescriptor descriptor = DeploymentDescriptor.createV3();
        descriptor.setId("test-mta");
        descriptor.setVersion("1.0.0");
        descriptor.setModules(List.of(module));
        return descriptor;
    }

    @Test
    void cfMetadataLabelsFlowThroughToUserCfMetadata() {
        Module module = Module.createV3()
                              .setName("my-app")
                              .setType("application")
                              .setParameters(Map.of(SupportedParameters.CF_METADATA,
                                                    Map.of("labels", Map.of("env", "prod"),
                                                           "annotations", Map.of("owner", "team-a"))));

        ApplicationCloudModelBuilder builder = buildBuilder(descriptorWithModule(module));
        CloudApplicationExtended app = builder.build(module, MODULE_HELPER);

        assertEquals(Map.of("env", "prod"), app.getUserCfMetadata()
                                               .getLabels());
        assertEquals(Map.of("owner", "team-a"), app.getUserCfMetadata()
                                                   .getAnnotations());
    }

    @Test
    void absentCfMetadataProducesEmptyUserCfMetadata() {
        Module module = Module.createV3()
                              .setName("my-app")
                              .setType("application");

        ApplicationCloudModelBuilder builder = buildBuilder(descriptorWithModule(module));
        CloudApplicationExtended app = builder.build(module, MODULE_HELPER);

        assertTrue(app.getUserCfMetadata()
                      .getLabels()
                      .isEmpty());
        assertTrue(app.getUserCfMetadata()
                      .getAnnotations()
                      .isEmpty());
    }

}
