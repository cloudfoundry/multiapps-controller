package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
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
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public abstract class DetermineDesiredStateAchievingActionsStepBaseTest
    extends SyncFlowableStepTest<DetermineDesiredStateAchievingActionsStep> {

    protected static final UUID FAKE_UUID = UUID.fromString("3e31fdaa-4a4e-11e9-8646-d663bd873d93");
    protected static final String DUMMY = "dummy";

    protected final List<ApplicationStateAction> expectedAppStateActions;

    private final ApplicationStartupState currentAppState;
    private final ApplicationStartupState desiredAppState;
    private final boolean hasAppChanged;
    private final boolean hasUploadToken;

    @Mock
    protected ApplicationStartupStateCalculator appStateCalculator;

    @Mock
    protected ApplicationConfiguration configuration;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    public DetermineDesiredStateAchievingActionsStepBaseTest(ApplicationStartupState currentAppState,
                                                             ApplicationStartupState desiredAppState, boolean hasAppChanged,
                                                             List<ApplicationStateAction> expectedAppStateActions, boolean hasUploadToken) {
        this.currentAppState = currentAppState;
        this.desiredAppState = desiredAppState;
        this.hasAppChanged = hasAppChanged;
        this.expectedAppStateActions = expectedAppStateActions;
        this.hasUploadToken = hasUploadToken;
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

    protected abstract RestartParameters getRestartParameters();

    private void prepareStep() {
        step.configuration = configuration;
    }

    private void prepareContext() {
        context.setVariable(Variables.APP_CONTENT_CHANGED, hasAppChanged);
        context.setVariable(Variables.NO_START, false);
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
        context.setVariable(Variables.APP_TO_PROCESS, app);
        when(client.getApplication(anyString())).thenReturn(app);
        if (hasUploadToken) {
            context.setVariable(Variables.UPLOAD_TOKEN, ImmutableUploadToken.builder()
                                                                            .packageGuid(FAKE_UUID)
                                                                            .build());
        }
    }
}
