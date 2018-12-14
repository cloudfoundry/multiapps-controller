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

import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;

@RunWith(Parameterized.class)
public class PrepareAppsDeploymentStepTest extends SyncFlowableStepTest<PrepareModulesDeploymentStep> {

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

    public PrepareAppsDeploymentStepTest(int count, ProcessType processType, boolean skipUpdateConfigurations) {
        this.count = count;
        this.processType = processType;
        this.skipUpdateConfigurations = skipUpdateConfigurations;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        Mockito.when(configuration.getPlatformType())
            .thenReturn(ApplicationConfiguration.DEFAULT_TYPE);
        Mockito.when(configuration.getControllerPollingInterval())
            .thenReturn(ApplicationConfiguration.DEFAULT_CONTROLLER_POLLING_INTERVAL);
        when(processTypeParser.getProcessType(context)).thenReturn(processType);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(count, context.getVariable(Constants.VAR_MODULES_COUNT));
        assertEquals(0, context.getVariable(Constants.VAR_MODULES_INDEX));
        assertEquals(Constants.VAR_MODULES_INDEX, context.getVariable(Constants.VAR_INDEX_VARIABLE_NAME));
        assertEquals(ApplicationConfiguration.DEFAULT_CONTROLLER_POLLING_INTERVAL,
            context.getVariable(Constants.VAR_CONTROLLER_POLLING_INTERVAL));
        assertTrue((boolean) context.getVariable(Constants.REBUILD_APP_ENV));
        assertTrue((boolean) context.getVariable(Constants.SHOULD_UPLOAD_APPLICATION_CONTENT));
        assertTrue((boolean) context.getVariable(Constants.EXECUTE_ONE_OFF_TASKS));

        assertEquals(skipUpdateConfigurations, (boolean) context.getVariable(Constants.VAR_SKIP_UPDATE_CONFIGURATION_ENTRIES));
    }

    private DelegateExecution prepareContext() {
        StepsUtil.setAllModulesToDeploy(context, getDummyModules());
        return context;
    }
    
    private List<ModuleToDeploy> getDummyModules() {
        List<ModuleToDeploy> modules = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            modules.add(new ModuleToDeploy("module-" + i, "app"));
        }
        return modules;
    }

    @Override
    protected PrepareModulesDeploymentStep createStep() {
        return new PrepareModulesDeploymentStep();
    }

}
