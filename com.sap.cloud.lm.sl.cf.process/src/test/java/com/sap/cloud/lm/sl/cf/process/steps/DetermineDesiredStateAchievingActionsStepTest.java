package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.engine.delegate.DelegateExecution;
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
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@RunWith(Parameterized.class)
public class DetermineDesiredStateAchievingActionsStepTest extends SyncActivitiStepTest<DetermineDesiredStateAchievingActionsStep> {

    @Parameters
    public static List<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, Collections.emptySet(),
            },
            // (1)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STARTED, false, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (2)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP)),
            },
            // (3)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STARTED, false, false, false, false, Collections.emptySet(),
            },
            // (4)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP)),
            },
            // (5)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STARTED, false, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (6)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE)),
            },
            // (7)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (8)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
            },
            // (9)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (10)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)),
            },
            // (11)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (12)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.EXECUTED, false, false, false, false, Collections.emptySet(),
            },
            // (13)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, Collections.emptySet(),
            },
            // (14)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.STARTED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (15)
            {
                ApplicationStartupState.STARTED, false, true, true, ApplicationStartupState.STARTED, false, false, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (16)
            {
                ApplicationStartupState.STARTED, false, false, false, ApplicationStartupState.STARTED, false, true, true, true, Collections.emptySet(),
            },
            // (17)
            {
                ApplicationStartupState.STARTED, false, false, false, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (18)
            {
                ApplicationStartupState.STARTED, false, true, false, ApplicationStartupState.STARTED, false, false, true, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            },
            // (19)
            {
                ApplicationStartupState.STARTED, false, false, true, ApplicationStartupState.STARTED, false, false, false, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)),
            }
            // @formatter:on
        });
    }

    private final ApplicationStartupState currentAppState;
    private final ApplicationStartupState desiredAppState;
    private final boolean hasAppChanged;
    private final boolean hasAppPropertiesChanged;
    private final boolean hasServicesPropertiesChanged;
    private final boolean hasUserPropertiesChanged;
    private final Set<ApplicationStateAction> expectedAppStateActions;

    private Map<String, Boolean> appRestartParameters;

    @Mock
    private ApplicationStartupStateCalculator appStateCalculator;

    public DetermineDesiredStateAchievingActionsStepTest(ApplicationStartupState currentAppState, boolean shouldRestartOnVcapAppChange,
        boolean shouldRestartOnVcapServicesChange, boolean shouldRestartOnUserProvidedChange, ApplicationStartupState desiredAppState,
        boolean hasAppChanged, boolean hasAppPropertiesChanged, boolean hasServicesPropertiesChanged, boolean hasUserPropertiesChanged,
        Set<ApplicationStateAction> expectedAppStateActions) {
        this.currentAppState = currentAppState;
        initRestartParametersMap(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange, shouldRestartOnUserProvidedChange);
        this.desiredAppState = desiredAppState;
        this.hasAppChanged = hasAppChanged;
        this.hasAppPropertiesChanged = hasAppPropertiesChanged;
        this.hasServicesPropertiesChanged = hasServicesPropertiesChanged;
        this.hasUserPropertiesChanged = hasUserPropertiesChanged;
        this.expectedAppStateActions = expectedAppStateActions;
    }

    private void initRestartParametersMap(boolean shouldRestartOnVcapAppChange, boolean shouldRestartOnVcapServicesChange,
        boolean shouldRestartOnUserProvidedChange) {
        appRestartParameters = new HashMap<>();
        appRestartParameters.put(SupportedParameters.VCAP_APPLICATION_ENV, shouldRestartOnVcapAppChange);
        appRestartParameters.put(SupportedParameters.VCAP_SERVICES_ENV, shouldRestartOnVcapServicesChange);
        appRestartParameters.put(SupportedParameters.USER_PROVIDED_ENV, shouldRestartOnUserProvidedChange);
    }

    @Before
    public void setUp() {
        context.setVariable(Constants.VAR_HAS_APP_CHANGED, Boolean.toString(hasAppChanged));
        context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, hasAppPropertiesChanged);
        context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, hasServicesPropertiesChanged);
        context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, hasUserPropertiesChanged);
        context.setVariable(Constants.PARAM_NO_START, false);
        when(appStateCalculator.computeCurrentState(any())).thenReturn(currentAppState);
        when(appStateCalculator.computeDesiredState(any(), eq(false))).thenReturn(desiredAppState);
        step.appStateCalculatorSupplier = () -> appStateCalculator;
        CloudApplicationExtended app = new CloudApplicationExtended(null, "dummy");
        app.setRestartParameters(appRestartParameters);
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
