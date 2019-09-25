package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild.ImmutableDropletInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.RestartParameters;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupState;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStartupStateCalculator;
import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStateAction;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public abstract class DetermineDesiredStateAchievingActionsStepBaseTest
    extends SyncFlowableStepTest<DetermineDesiredStateAchievingActionsStep> {

    protected static final String FAKE_ERROR = "Fake Error";
    protected static final UUID FAKE_UUID = UUID.fromString("3e31fdaa-4a4e-11e9-8646-d663bd873d93");
    protected static final String DUMMY = "dummy";
    protected static final String DATE_PATTERN = "dd-MM-yyyy";

    protected final Set<ApplicationStateAction> expectedAppStateActions;

    private final ApplicationStartupState currentAppState;
    private final ApplicationStartupState desiredAppState;
    private final boolean hasAppChanged;
    private final List<CloudBuild> cloudBuilds;

    @Mock
    protected ApplicationStartupStateCalculator appStateCalculator;

    @Mock
    protected ApplicationConfiguration configuration;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    public DetermineDesiredStateAchievingActionsStepBaseTest(ApplicationStartupState currentAppState,
                                                             ApplicationStartupState desiredAppState, boolean hasAppChanged,
                                                             Set<ApplicationStateAction> expectedAppStateActions,
                                                             List<CloudBuild> cloudBuilds) {
        this.currentAppState = currentAppState;
        this.desiredAppState = desiredAppState;
        this.hasAppChanged = hasAppChanged;
        this.expectedAppStateActions = expectedAppStateActions;
        this.cloudBuilds = cloudBuilds;
    }

    @Before
    public void setUp() {
        prepareContext();
        prepareAppStepCalculator();
        prepareStep();
        prepareClient();
    }

    @Override
    protected DetermineDesiredStateAchievingActionsStep createStep() {
        return new DetermineDesiredStateAchievingActionsStep();
    }

    protected static CloudBuild createCloudBuild(CloudBuild.State state, Date createdAt, String error) {
        return ImmutableCloudBuild.builder()
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                  .guid(FAKE_UUID)
                                                                  .createdAt(createdAt)
                                                                  .build())
                                  .dropletInfo(ImmutableDropletInfo.builder()
                                                                   .guid(FAKE_UUID)
                                                                   .build())
                                  .state(state)
                                  .error(error)
                                  .build();
    }

    protected static Date parseDate(String date) {
        try {
            return new SimpleDateFormat(DATE_PATTERN).parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid Date!");
        }
    }

    protected abstract RestartParameters getRestartParameters();

    private void prepareStep() {
        step.appStateCalculatorSupplier = () -> appStateCalculator;
        step.configuration = configuration;
    }

    private void prepareContext() {
        context.setVariable(Constants.VAR_APP_CONTENT_CHANGED, Boolean.toString(hasAppChanged));
        context.setVariable(Constants.PARAM_NO_START, false);
    }

    private void prepareAppStepCalculator() {
        when(appStateCalculator.computeCurrentState(any())).thenReturn(currentAppState);
        when(appStateCalculator.computeDesiredState(any(), eq(false))).thenReturn(desiredAppState);
    }

    private void prepareClient() {
        RestartParameters restartParameters = getRestartParameters();
        CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                       .guid(FAKE_UUID)
                                                       .build();

        CloudApplicationExtended app = ImmutableCloudApplicationExtended.builder()
                                                                        .metadata(metadata)
                                                                        .name(DUMMY)
                                                                        .restartParameters(restartParameters)
                                                                        .build();
        context.setVariable(Constants.VAR_APP_TO_PROCESS, JsonUtil.toJson(app));
        when(client.getApplication(anyString())).thenReturn(app);
        when(client.getBuildsForApplication(app.getMetadata()
                                               .getGuid())).thenReturn(cloudBuilds);
    }
}
