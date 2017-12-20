package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@RunWith(Parameterized.class)
public class DetermineDesiredStateAchievingActionsStepTest extends AbstractStepTest<DetermineDesiredStateAchievingActionsStep> {

    @Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, false, Collections.emptySet(),
            },
            // (1)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (2)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP)),
            },
            // (3)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, false, Collections.emptySet(),
            },
            // (4)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP)),
            },
            // (5)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (6)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STOPPED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE)),
            },
            // (7)
            {
                ApplicationStartupState.STOPPED, ApplicationStartupState.STARTED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (8)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STOPPED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
            },
            // (9)
            {
                ApplicationStartupState.STARTED, ApplicationStartupState.STARTED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (10)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STOPPED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
            },
            // (11)
            {
                ApplicationStartupState.INCONSISTENT, ApplicationStartupState.STARTED, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // @formatter:on
        });
    }

    private final ApplicationStartupState currentAppState;
    private final ApplicationStartupState desiredAppState;
    private final boolean hasAppChanged;
    private final Set<ApplicationStateAction> expectedAppStateActions;

    @Mock
    private ApplicationStartupStateCalculator appStateCalculator;

    public DetermineDesiredStateAchievingActionsStepTest(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState,
        boolean hasAppChanged, Set<ApplicationStateAction> expectedAppStateActions) {
        this.currentAppState = currentAppState;
        this.desiredAppState = desiredAppState;
        this.hasAppChanged = hasAppChanged;
        this.expectedAppStateActions = expectedAppStateActions;
    }

    @Before
    public void setUp() {
        context.setVariable(Constants.VAR_HAS_APP_CHANGED, Boolean.toString(hasAppChanged));
        context.setVariable(Constants.PARAM_NO_START, false);
        when(appStateCalculator.computeCurrentState(any())).thenReturn(currentAppState);
        when(appStateCalculator.computeDesiredState(any(), eq(false))).thenReturn(desiredAppState);
        step.appStateCalculatorSupplier = () -> appStateCalculator;
        CloudApplicationExtended app = new CloudApplicationExtended(null, "dummy");
        context.setVariable(Constants.VAR_APP_TO_DEPLOY, JsonUtil.toJson(app));
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(expectedAppStateActions, StepsUtil.getAppStateActionsToExecute(context));
    }

    @Override
    protected DetermineDesiredStateAchievingActionsStep createStep() {
        return new DetermineDesiredStateAchievingActionsStepMock();
    }

    private class DetermineDesiredStateAchievingActionsStepMock extends DetermineDesiredStateAchievingActionsStep {
        @Override
        protected boolean determineHasAppChanged(DelegateExecution context) {
            return hasAppChanged;
        }
    }

}
