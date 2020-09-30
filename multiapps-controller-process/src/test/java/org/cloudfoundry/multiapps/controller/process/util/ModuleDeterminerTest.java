package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ModuleDeterminerTest {

    private final ProcessContext context = createContext();
    @Mock
    private MtaMetadataParser mtaMetadataParser;

    public ModuleDeterminerTest() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testDetermineModuleToDeployModuleIsSet() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        context.setVariable(Variables.MODULE_TO_DEPLOY, createModule("test-module"));
        Module module = moduleDeterminer.determineModuleToDeploy();
        Assertions.assertEquals("test-module", module.getName());
    }

    @Test
    void testDetermineModuleIfModuleIsNotSetAnywhere() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        Assertions.assertNull(moduleDeterminer.determineModuleToDeploy());
    }

    @Test
    void testDetermineModuleIfModuleIsNotSet() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        DeploymentDescriptor completeDeploymentDescriptor = createDeploymentDescriptor();
        Module moduleToDeploy = createModule("some-module-name");
        completeDeploymentDescriptor.setModules(List.of(moduleToDeploy));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        context.setVariable(Variables.APP_TO_PROCESS, createApplication("app-to-process", "some-module-name", null));
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        Module module = moduleDeterminer.determineModuleToDeploy();
        Assertions.assertEquals("some-module-name", module.getName());
    }

    void testDetermineModuleIfModuleIsNotInDeploymentCompleteDeploymentDescriptorMetadataOnly() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        DeploymentDescriptor completeDeploymentDescriptor = createDeploymentDescriptor();
        Module moduleToDeploy = createModule("some-module-name");
        completeDeploymentDescriptor.setModules(List.of(moduleToDeploy));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        Map<String, String> labels = Map.of("mta_id", "123");
        Map<String, String> annotations = Map.of("mta_id", "mta-id");
        CloudApplicationExtended cloudApplicationExtended = createApplication("app-to-process", null,
                                                                              createV3Metadata(labels, annotations));
        Mockito.when(mtaMetadataParser.parseDeployedMtaApplication(cloudApplicationExtended))
               .thenReturn(createDeployedMtaApplication("app-to-process", "some-module-name"));
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        Module module = moduleDeterminer.determineModuleToDeploy();
        Assertions.assertEquals("some-module-name", module.getName());
    }

    protected ModuleDeterminer createModuleDeterminer() {
        return ImmutableModuleDeterminer.builder()
                                        .context(context)
                                        .mtaMetadataParser(mtaMetadataParser)
                                        .build();
    }

    private DeploymentDescriptor createDeploymentDescriptor() {
        return DeploymentDescriptor.createV3();
    }

    private CloudApplicationExtended createApplication(String appName, String moduleName, Metadata v3Metadata) {
        return ImmutableCloudApplicationExtended.builder()
                                                .name(appName)
                                                .moduleName(moduleName)
                                                .v3Metadata(v3Metadata)
                                                .build();
    }

    private Metadata createV3Metadata(Map<String, String> labels, Map<String, String> annotations) {
        return Metadata.builder()
                       .labels(labels)
                       .annotations(annotations)
                       .build();
    }

    private DeployedMtaApplication createDeployedMtaApplication(String appName, String moduleName) {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(appName)
                                              .moduleName(moduleName)
                                              .build();
    }

    private Module createModule(String moduleName) {
        return Module.createV3()
                     .setName(moduleName);
    }

    private ProcessContext createContext() {
        DelegateExecution delegateExecution = MockDelegateExecution.createSpyInstance();
        StepLogger stepLogger = Mockito.mock(StepLogger.class);
        CloudControllerClientProvider cloudControllerClientProvider = Mockito.mock(CloudControllerClientProvider.class);
        return new ProcessContext(delegateExecution, stepLogger, cloudControllerClientProvider);
    }

}
