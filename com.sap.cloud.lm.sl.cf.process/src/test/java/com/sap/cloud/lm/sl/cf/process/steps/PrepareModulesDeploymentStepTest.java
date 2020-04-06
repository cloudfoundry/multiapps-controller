package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.mta.model.Module;

@RunWith(Parameterized.class)
public class PrepareModulesDeploymentStepTest extends SyncFlowableStepTest<PrepareModulesDeploymentStep> {

    @Mock
    private ProcessTypeParser processTypeParser;

    private final int count;
    private final ProcessType processType;
    private final boolean skipUpdateConfigurations;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
         // @formatter:off
            { 1, ProcessType.DEPLOY, false }, 
            { 2, ProcessType.DEPLOY, false }, 
            { 3, ProcessType.DEPLOY, false }, 
            { 4, ProcessType.BLUE_GREEN_DEPLOY, true }, 
            { 5, ProcessType.UNDEPLOY, false }
         // @formatter:on    
        });
    }

    public PrepareModulesDeploymentStepTest(int count, ProcessType processType, boolean skipUpdateConfigurations) {
        this.count = count;
        this.processType = processType;
        this.skipUpdateConfigurations = skipUpdateConfigurations;
    }

    @Before
    public void setUp() {
        prepareContext();
        Mockito.when(configuration.getStepPollingIntervalInSeconds())
               .thenReturn(ApplicationConfiguration.DEFAULT_STEP_POLLING_INTERVAL_IN_SECONDS);
        when(processTypeParser.getProcessType(execution)).thenReturn(processType);
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        assertEquals((Integer) count, context.getVariable(Variables.MODULES_COUNT));
        assertEquals((Integer) 0, context.getVariable(Variables.MODULES_INDEX));
        assertEquals(Constants.VAR_MODULES_INDEX, context.getVariable(Variables.INDEX_VARIABLE_NAME));
        assertTrue(context.getVariable(Variables.REBUILD_APP_ENV));
        assertTrue(context.getVariable(Variables.SHOULD_UPLOAD_APPLICATION_CONTENT));
        assertTrue(context.getVariable(Variables.EXECUTE_ONE_OFF_TASKS));

        assertEquals(skipUpdateConfigurations, context.getVariable(Variables.SKIP_UPDATE_CONFIGURATION_ENTRIES));
    }

    private DelegateExecution prepareContext() {
        context.setVariable(Variables.ALL_MODULES_TO_DEPLOY, getDummyModules());
        return execution;
    }

    private List<Module> getDummyModules() {
        List<Module> modules = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            modules.add(Module.createV2());
        }
        return modules;
    }

    @Override
    protected PrepareModulesDeploymentStep createStep() {
        return new PrepareModulesDeploymentStep();
    }

}
