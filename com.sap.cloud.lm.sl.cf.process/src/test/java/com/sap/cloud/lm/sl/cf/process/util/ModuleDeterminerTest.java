package com.sap.cloud.lm.sl.cf.process.util;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.ProcessContext;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

class ModuleDeterminerTest {

    private final ProcessContext context = createContext();
    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;

    public ModuleDeterminerTest() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testDetermineModuleToDeployModuleIsSet() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        context.setVariable(Variables.MODULE_TO_DEPLOY, createModule("test-module"));
        Module module = moduleDeterminer.determineModuleToDeploy(context);
        Assertions.assertEquals("test-module", module.getName());
    }

    @Test
    void testDetermineModuleIfModuleIsNotSetAnywhere() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        Assertions.assertNull(moduleDeterminer.determineModuleToDeploy(context));
    }

    @Test
    void testDetermineModuleIfModuleIsNotSet() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        DeploymentDescriptor completeDeploymentDescriptor = createDeploymentDescriptor();
        Module moduleToDeploy = createModule("some-module-name");
        completeDeploymentDescriptor.setModules(Collections.singletonList(moduleToDeploy));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        context.setVariable(Variables.APP_TO_PROCESS, createApplication("app-to-process", "some-module-name", null));
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        Module module = moduleDeterminer.determineModuleToDeploy(context);
        Assertions.assertEquals("some-module-name", module.getName());
    }

    @Test
    void testDetermineModuleIfModuleIsNotInDeploymentCompleteDeploymentDescriptorEnvOnly() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        DeploymentDescriptor completeDeploymentDescriptor = createDeploymentDescriptor();
        Module moduleToDeploy = createModule("some-module-name");
        completeDeploymentDescriptor.setModules(Collections.singletonList(moduleToDeploy));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        CloudApplicationExtended cloudApplicationExtended = createApplication("app-to-process", null, null);
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
        Mockito.when(envMtaMetadataParser.parseDeployedMtaApplication(cloudApplicationExtended))
               .thenReturn(createDeployedMtaApplication("app-to-process", "some-module-name"));
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        Module module = moduleDeterminer.determineModuleToDeploy(context);
        Assertions.assertEquals("some-module-name", module.getName());
    }

    @Test
    void testDetermineModuleIfModuleIsNotInDeploymentCompleteDeploymentDescriptorMetadataOnly() {
        ModuleDeterminer moduleDeterminer = createModuleDeterminer();
        DeploymentDescriptor completeDeploymentDescriptor = createDeploymentDescriptor();
        Module moduleToDeploy = createModule("some-module-name");
        completeDeploymentDescriptor.setModules(Collections.singletonList(moduleToDeploy));
        context.setVariable(Variables.COMPLETE_DEPLOYMENT_DESCRIPTOR, completeDeploymentDescriptor);
        Map<String, String> labels = MapUtil.asMap("mta_id", "123");
        Map<String, String> annotations = MapUtil.asMap("mta_id", "mta-id");
        CloudApplicationExtended cloudApplicationExtended = createApplication("app-to-process", null,
                                                                              createV3Metadata(labels, annotations));
        Mockito.when(envMtaMetadataParser.parseDeployedMtaApplication(cloudApplicationExtended))
               .thenReturn(createDeployedMtaApplication("app-to-process", "some-module-name"));
        context.setVariable(Variables.APP_TO_PROCESS, cloudApplicationExtended);
        context.setVariable(Variables.MTA_MAJOR_SCHEMA_VERSION, 3);
        Module module = moduleDeterminer.determineModuleToDeploy(context);
        Assertions.assertEquals("some-module-name", module.getName());
    }

    protected ModuleDeterminer createModuleDeterminer() {
        return ImmutableModuleDeterminer.builder()
                                        .context(context)
                                        .mtaMetadataParser(mtaMetadataParser)
                                        .envMtaMetadataParser(envMtaMetadataParser)
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
