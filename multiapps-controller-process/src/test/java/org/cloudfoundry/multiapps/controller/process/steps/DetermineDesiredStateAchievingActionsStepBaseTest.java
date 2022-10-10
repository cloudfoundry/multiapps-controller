package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.RestartParameters;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupState;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStartupStateCalculator;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerData;

public abstract class DetermineDesiredStateAchievingActionsStepBaseTest
    extends SyncFlowableStepTest<DetermineDesiredStateAchievingActionsStep> {

    protected static final UUID FAKE_UUID = UUID.fromString("3e31fdaa-4a4e-11e9-8646-d663bd873d93");
    protected static final String DUMMY = "dummy";

    @Mock
    protected ApplicationStartupStateCalculator appStateCalculator;
    @Mock
    protected ApplicationConfiguration configuration;

    protected void initializeParameters(ApplicationStartupState currentAppState,
                      ApplicationStartupState desiredAppState, boolean hasAppChanged,
                      boolean hasCloudPacakge) {
        prepareContext(hasAppChanged);
        prepareAppStepCalculator(currentAppState, desiredAppState);
        prepareStep();
        prepareClient(hasCloudPacakge);
    }

    @Override
    protected DetermineDesiredStateAchievingActionsStep createStep() {
        return new DetermineDesiredStateAchievingActionsStep();
    }

    protected abstract RestartParameters getRestartParameters();

    private void prepareStep() {
        step.configuration = configuration;
    }

    private void prepareContext(boolean hasAppChanged) {
        context.setVariable(Variables.APP_CONTENT_CHANGED, hasAppChanged);
        context.setVariable(Variables.NO_START, false);
    }

    private void prepareAppStepCalculator(ApplicationStartupState currentAppState, ApplicationStartupState desiredAppState) {
        when(appStateCalculator.computeCurrentState(any(), any(), any())).thenReturn(currentAppState);
        when(appStateCalculator.computeDesiredState(any(), any(), eq(false))).thenReturn(desiredAppState);
    }

    private void prepareClient(boolean hasCloudPacakge) {
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
        if (hasCloudPacakge) {
            context.setVariable(Variables.CLOUD_PACKAGE, ImmutableCloudPackage.builder()
                                                                              .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                                              .type(CloudPackage.Type.DOCKER)
                                                                              .data(ImmutableDockerData.builder()
                                                                                                       .image("cloudfoundry/test")
                                                                                                       .build())
                                                                              .build());
        }
    }
}
