package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.EnvMtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.HookPhase;
import org.cloudfoundry.multiapps.controller.process.util.HooksExecutor;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseBuilder;
import org.cloudfoundry.multiapps.controller.process.util.HooksPhaseGetter;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class DeleteApplicationRoutesStepTest extends UndeployAppStepTest {

    @Mock
    private MtaMetadataParser mtaMetadataParser;
    @Mock
    private EnvMtaMetadataParser envMtaMetadataParser;
    @Mock
    private HooksPhaseGetter hooksPhaseGetter;
    @Mock
    private HooksExecutor hooksExecutor;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private HooksPhaseBuilder hooksPhaseBuilder;

    @BeforeEach
    void setUp() {
        Mockito.when(processTypeParser.getProcessType(context.getExecution()))
               .thenReturn(ProcessType.DEPLOY);
    }

    @Override
    protected void performValidation(CloudApplication cloudApplication) {
        if (!cloudApplication.getUris()
                             .isEmpty()) {
            verify(client).updateApplicationUris(cloudApplication.getName(), Collections.emptyList());
        }
    }

    @Override
    protected void performAfterUndeploymentValidation() {
        assertRoutesWereDeleted();
    }

    private void assertRoutesWereDeleted() {
        int routesToDeleteCount = stepOutput.expectedRoutesToDelete.size();
        verify(client, times(routesToDeleteCount)).deleteRoute(any(), any(), any());
        for (Route route : stepOutput.expectedRoutesToDelete) {
            verify(client).deleteRoute(route.host, route.domain, route.path);
            routesToDeleteCount--;
        }
        assertEquals("A number of routes were not deleted: ", 0, routesToDeleteCount);
    }

    @Test
    void testGetHookPhaseBefore() {
        Mockito.when(hooksPhaseBuilder.buildHookPhases(Arrays.asList(HookPhase.BEFORE_UNMAP_ROUTES,
                                                                     HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES),
                                                       context))
               .thenReturn(Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_LIVE,
                                         HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES));
        List<HookPhase> expectedPhases = Arrays.asList(HookPhase.BLUE_GREEN_APPLICATION_BEFORE_UNMAP_ROUTES_LIVE,
                                                       HookPhase.APPLICATION_BEFORE_UNMAP_ROUTES);
        List<HookPhase> hookPhasesBeforeStep = ((BeforeStepHookPhaseProvider) step).getHookPhasesBeforeStep(context);
        assertEquals(expectedPhases, hookPhasesBeforeStep);
    }

    @Override
    protected UndeployAppStep createStep() {
        return new DeleteApplicationRoutesStep();
    }

}
