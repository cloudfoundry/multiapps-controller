package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;

public class PrepareToUndeployStepTest extends SyncFlowableStepTest<PrepareToUndeployStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";

    @BeforeEach
    public void setUp() throws Exception {
        context.setVariable(Constants.PARAM_MTA_ID, MTA_ID);

        step.conflictPreventerSupplier = service -> mock(ProcessConflictPreventer.class);
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
               .thenReturn(Collections.emptyList());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getAppsToDeploy(context));
        Assertions.assertEquals(Collections.emptySet(), StepsUtil.getMtaModules(context));
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacadeFacade));
    }

    @Test
    public void testErrorMessage() {
        Assertions.assertEquals(Messages.ERROR_DETECTING_COMPONENTS_TO_UNDEPLOY, step.getStepErrorMessage(context));
    }

    @Test
    public void testExecuteDeployedModuleNotNull() {
        StepsUtil.setDeployedMta(context, createDeployedMta());
        step.execute(context);

        assertStepFinishedSuccessfully();
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getAppsToDeploy(context));
        Assertions.assertEquals(getMtaModulesNames(createDeployedMtaModules()), StepsUtil.getMtaModules(context));
        Assertions.assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacadeFacade));
    }

    private DeployedMta createDeployedMta() {
        DeployedMta deployedMta = new DeployedMta();
        deployedMta.setModules(createDeployedMtaModules());
        return deployedMta;
    }

    private List<DeployedMtaModule> createDeployedMtaModules() {
        return Arrays.asList(createModule("module_1"), createModule("module_2"));
    }

    private DeployedMtaModule createModule(String name) {
        DeployedMtaModule module = new DeployedMtaModule();
        module.setModuleName(name);
        return module;
    }

    private Set<String> getMtaModulesNames(List<DeployedMtaModule> deployedMtaModules) {
        return deployedMtaModules.stream()
                                 .map(DeployedMtaModule::getModuleName)
                                 .collect(Collectors.toSet());
    }

    @Override
    protected PrepareToUndeployStep createStep() {
        return new PrepareToUndeployStep();
    }

}
