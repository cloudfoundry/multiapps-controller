package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessConflictPreventer;

public class PrepareToUndeployStepTest extends AbstractStepTest<PrepareToUndeployStep> {

    private static final String MTA_ID = "com.sap.xs2.samples.helloworld";

    @Before
    public void setUp() throws Exception {
        context.setVariable(Constants.PARAM_MTA_ID, MTA_ID);

        step.conflictPreventerSupplier = (dao) -> mock(ProcessConflictPreventer.class);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        assertEquals(Collections.emptyList(), StepsUtil.getDependenciesToPublish(context));
        assertEquals(Collections.emptyList(), StepsUtil.getServiceUrlsToRegister(context));
        assertEquals(Collections.emptyList(), StepsUtil.getAppsToDeploy(context));
        assertEquals(Collections.emptyList(), StepsUtil.getServiceBrokersToCreate(context));
        assertEquals(Collections.emptySet(), StepsUtil.getMtaModules(context));
        assertEquals(Collections.emptyList(), StepsUtil.getPublishedEntries(context));
    }

    @Override
    protected PrepareToUndeployStep createStep() {
        return new PrepareToUndeployStep();
    }

}
