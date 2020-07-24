package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class PrepareToUndeployStepTest extends SyncFlowableStepTest<PrepareToUndeployStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";

    @BeforeEach
    public void setUp() {
        context.setVariable(Variables.MTA_ID, MTA_ID);

        step.conflictPreventerSupplier = service -> mock(ProcessConflictPreventer.class);
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
               .thenReturn(Collections.emptyList());
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();
        Assertions.assertEquals(Collections.emptyList(), context.getVariable(Variables.APPS_TO_DEPLOY));
        Assertions.assertEquals(Collections.emptySet(), context.getVariable(Variables.MTA_MODULES));
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacadeFacade));
    }

    @Test
    public void testErrorMessage() {
        Assertions.assertEquals(Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY, step.getStepErrorMessage(context));
    }

    @Test
    public void testExecuteDeployedModuleNotNull() {
        context.setVariable(Variables.DEPLOYED_MTA, createDeployedMta());
        step.execute(execution);

        assertStepFinishedSuccessfully();
        Assertions.assertEquals(Collections.emptyList(), context.getVariable(Variables.APPS_TO_DEPLOY));
        Assertions.assertEquals(getMtaModulesNames(createDeployedMtaApplications()), context.getVariable(Variables.MTA_MODULES));
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacadeFacade));
    }

    private DeployedMta createDeployedMta() {
        return ImmutableDeployedMta.builder()
                                   .metadata(ImmutableMtaMetadata.builder()
                                                                 .id("test")
                                                                 .build())
                                   .applications(createDeployedMtaApplications())
                                   .build();
    }

    private List<DeployedMtaApplication> createDeployedMtaApplications() {
        return Arrays.asList(createDeployedMtaApplication("module_1"), createDeployedMtaApplication("module_2"));
    }

    private DeployedMtaApplication createDeployedMtaApplication(String name) {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(name)
                                              .moduleName(name)
                                              .build();
    }

    private Set<String> getMtaModulesNames(List<DeployedMtaApplication> deployedMtaApplications) {
        return deployedMtaApplications.stream()
                                      .map(DeployedMtaApplication::getModuleName)
                                      .collect(Collectors.toSet());
    }

    @Override
    protected PrepareToUndeployStep createStep() {
        return new PrepareToUndeployStep();
    }

}
