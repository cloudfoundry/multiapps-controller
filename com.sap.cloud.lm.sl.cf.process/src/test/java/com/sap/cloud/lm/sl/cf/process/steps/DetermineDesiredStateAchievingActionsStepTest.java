package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@RunWith(Parameterized.class)
public class DetermineDesiredStateAchievingActionsStepTest extends SyncFlowableStepTest<DetermineDesiredStateAchievingActionsStep> {

    private static final String FAKE_ERROR = "Fake Error";
    private static final UUID FAKE_UUID = UUID.fromString("3e31fdaa-4a4e-11e9-8646-d663bd873d93");
    private static final String DUMMY = "dummy";

    @Parameters
    public static List<Object[]> getParameters() throws ParseException {
        return Arrays.asList(new Object[][] {
            // @formatter:off
            // (0)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, Collections.emptySet(), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build(), new CloudBuild.Builder().buildState(CloudBuild.BuildState.FAILED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("18-03-2019"), null)).build()),
            },
            // (1)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STARTED, false, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)), Collections.emptyList(),
            },
            // (2)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.FAILED).error(FAKE_ERROR).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).build()),
            },
            // (3)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STARTED, false, false, false, false, Collections.emptySet(), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).build()),
            },
            // (4)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (5)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STARTED, false, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (6)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).build()),
            },
            // (7)
            {
                ApplicationStartupState.STOPPED, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (8)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (9)
            {
                ApplicationStartupState.STARTED, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (10)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STOPPED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.FAILED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (11)
            {
                ApplicationStartupState.INCONSISTENT, true, true, true, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (12)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.EXECUTED, false, false, false, false, Collections.emptySet(), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).build()),
            },
            // (13)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.STOPPED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE)), Collections.emptyList(),
            },
            // (14)
            {
                ApplicationStartupState.EXECUTED, true, true, true, ApplicationStartupState.STARTED, false, false, false, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STAGE, ApplicationStateAction.START)), Collections.emptyList(),
            },
            // (15)
            {
                ApplicationStartupState.STARTED, false, true, true, ApplicationStartupState.STARTED, false, false, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Collections.emptyList(),
            },
            // (16)
            {
                ApplicationStartupState.STARTED, false, false, false, ApplicationStartupState.STARTED, false, true, true, true, Collections.emptySet(), Arrays.asList(new CloudBuild.Builder().buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).build()),
            },
            // (17)
            {
                ApplicationStartupState.STARTED, false, false, false, ApplicationStartupState.STARTED, true, true, true, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (18)
            {
                ApplicationStartupState.STARTED, false, true, false, ApplicationStartupState.STARTED, false, false, true, false, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
            },
            // (19)
            {
                ApplicationStartupState.STARTED, false, false, true, ApplicationStartupState.STARTED, false, false, false, true, new HashSet<>(Arrays.asList(ApplicationStateAction.STOP, ApplicationStateAction.STAGE, ApplicationStateAction.START)), Arrays.asList(new CloudBuild.Builder().meta(new Meta(FAKE_UUID, new SimpleDateFormat("dd-MM-yyyy").parse("20-03-2019"), null)).buildState(CloudBuild.BuildState.STAGED).droplet(new CloudBuild.Droplet(FAKE_UUID, null)).build()),
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
    private final List<CloudBuild> cloudBuilds;
    private RestartParameters appRestartParameters;

    @Mock
    private ApplicationStartupStateCalculator appStateCalculator;

    @Mock
    private ApplicationConfiguration configuration;

    public DetermineDesiredStateAchievingActionsStepTest(ApplicationStartupState currentAppState, boolean shouldRestartOnVcapAppChange,
        boolean shouldRestartOnVcapServicesChange, boolean shouldRestartOnUserProvidedChange, ApplicationStartupState desiredAppState,
        boolean hasAppChanged, boolean hasAppPropertiesChanged, boolean hasServicesPropertiesChanged, boolean hasUserPropertiesChanged,
        Set<ApplicationStateAction> expectedAppStateActions, List<CloudBuild> cloudBuilds) {
        this.currentAppState = currentAppState;
        this.appRestartParameters = new RestartParameters(shouldRestartOnVcapAppChange, shouldRestartOnVcapServicesChange,
            shouldRestartOnUserProvidedChange);
        this.desiredAppState = desiredAppState;
        this.hasAppChanged = hasAppChanged;
        this.hasAppPropertiesChanged = hasAppPropertiesChanged;
        this.hasServicesPropertiesChanged = hasServicesPropertiesChanged;
        this.hasUserPropertiesChanged = hasUserPropertiesChanged;
        this.expectedAppStateActions = expectedAppStateActions;
        this.cloudBuilds = cloudBuilds;
    }

    @Before
    public void setUp() {
        context.setVariable(Constants.VAR_APP_CONTENT_CHANGED, Boolean.toString(hasAppChanged));
        context.setVariable(Constants.VAR_VCAP_APP_PROPERTIES_CHANGED, hasAppPropertiesChanged);
        context.setVariable(Constants.VAR_VCAP_SERVICES_PROPERTIES_CHANGED, hasServicesPropertiesChanged);
        context.setVariable(Constants.VAR_USER_PROPERTIES_CHANGED, hasUserPropertiesChanged);
        context.setVariable(Constants.PARAM_NO_START, false);
        when(configuration.getPlatformType()).thenReturn(PlatformType.CF);
        when(appStateCalculator.computeCurrentState(any())).thenReturn(currentAppState);
        when(appStateCalculator.computeDesiredState(any(), eq(false))).thenReturn(desiredAppState);
        step.appStateCalculatorSupplier = () -> appStateCalculator;
        step.configuration = configuration;
        CloudEntity.Meta meta = new CloudEntity.Meta(FAKE_UUID, null, null);
        CloudApplicationExtended app = new CloudApplicationExtended(meta, DUMMY);
        app.setRestartParameters(appRestartParameters);
        context.setVariable(Constants.VAR_APP_TO_DEPLOY, JsonUtil.toJson(app));
        when(client.getApplication(anyString())).thenReturn(app);
        when(client.getBuildsForApplication(app.getMeta()
            .getGuid())).thenReturn(cloudBuilds);
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        assertEquals(expectedAppStateActions, StepsUtil.getAppStateActionsToExecute(context));
    }

    @Override
    protected DetermineDesiredStateAchievingActionsStep createStep() {
        return new DetermineDesiredStateAchievingActionsStep();
    }

}
