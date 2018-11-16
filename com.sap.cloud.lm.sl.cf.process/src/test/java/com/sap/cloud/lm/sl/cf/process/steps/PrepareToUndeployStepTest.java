package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;

public class PrepareToUndeployStepTest extends SyncFlowableStepTest<PrepareToUndeployStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";

    @Before
    public void setUp() throws Exception {
        context.setVariable(Constants.PARAM_MTA_ID, MTA_ID);

        step.conflictPreventerSupplier = (dao) -> mock(ProcessConflictPreventer.class);
        Mockito.when(flowableFacadeFacade.getHistoricSubProcessIds(Mockito.any()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(Collections.emptyList(), StepsUtil.getServiceUrlsToRegister(context));
        assertEquals(Collections.emptyList(), StepsUtil.getAppsToDeploy(context));
        assertEquals(Collections.emptyList(), StepsUtil.getServiceBrokersToCreate(context));
        assertEquals(Collections.emptySet(), StepsUtil.getMtaModules(context));
        assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntriesFromSubProcesses(context, flowableFacadeFacade));
    }

    @Override
    protected PrepareToUndeployStep createStep() {
        return new PrepareToUndeployStep();
    }

}
