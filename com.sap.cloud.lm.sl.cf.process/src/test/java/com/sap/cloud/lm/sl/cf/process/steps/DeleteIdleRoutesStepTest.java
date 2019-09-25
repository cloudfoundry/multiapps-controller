package com.sap.cloud.lm.sl.cf.process.steps;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

public class DeleteIdleRoutesStepTest extends SyncFlowableStepTest<DeleteIdleRoutesStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
        // @formatter:off
            // (1) One old URI is replaced with a new one in redeploy:
            Arguments.of("existing-app-1.json", "app-to-deploy-1.json", Arrays.asList("module-1.domain.com", "module-1.domain.com/with/path"), null, null, StepPhase.DONE),
            // (2) There are no differences between old and new URIs:
            Arguments.of("existing-app-2.json", "app-to-deploy-2.json", Collections.emptyList(), null, null, StepPhase.DONE),
            // (3) The new URIs are a subset of the old:
            Arguments.of("existing-app-3.json", "app-to-deploy-3.json", Arrays.asList("tcp://test.domain.com:51052", "tcp://test.domain.com:51054"), null, null, StepPhase.DONE),
            // (4) There is no previous version of app:
            Arguments.of(null, "app-to-deploy-3.json", Collections.emptyList(), null, null, StepPhase.DONE),
            // (5) Not Found Exception is thrown
            Arguments.of("existing-app-1.json", "app-to-deploy-1.json", Arrays.asList("module-1.domain.com", "module-1.domain.com/with/path"), new CloudOperationException(HttpStatus.NOT_FOUND), new CloudOperationException(HttpStatus.NOT_FOUND), StepPhase.DONE),
            // (6) Conflict Exception is thrown
            Arguments.of("existing-app-1.json", "app-to-deploy-1.json", Arrays.asList("module-1.domain.com", "module-1.domain.com/with/path"), new CloudOperationException(HttpStatus.CONFLICT), new CloudOperationException(HttpStatus.CONFLICT), StepPhase.DONE)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testExecute(String existingAppFile, String appToDeployFile, List<String> urisToDelete,
                            CloudOperationException exceptionThrownByClient, CloudOperationException expectedException,
                            StepPhase expectedStepPhase)
        throws Throwable {
        prepareContext(existingAppFile, appToDeployFile, exceptionThrownByClient);
        step.execute(context);
        assertStepPhaseMatch(expectedStepPhase);
        verifyClient(urisToDelete);
    }

    private void prepareContext(String existingAppFile, String appToDeployFile, CloudOperationException exceptionThrownByClient) {
        prepareClient(exceptionThrownByClient);
        StepsUtil.setDeleteIdleUris(context, true);
        setExistingAppInContext(existingAppFile);
        CloudApplicationExtended appToDeploy = JsonUtil.fromJson(TestUtil.getResourceAsString(appToDeployFile, getClass()),
                                                                 new TypeReference<CloudApplicationExtended>() {
                                                                 });
        StepsUtil.setApp(context, appToDeploy);
    }

    private void prepareClient(CloudOperationException exceptionThrownByClient) {
        if (exceptionThrownByClient != null) {
            Mockito.doThrow(exceptionThrownByClient)
                   .when(client)
                   .deleteRoute(anyString(), anyString(), anyString());
        }
    }

    private void setExistingAppInContext(String existingAppFile) {
        if (existingAppFile == null) {
            return;
        }
        CloudApplicationExtended existingApp = JsonUtil.fromJson(TestUtil.getResourceAsString(existingAppFile, getClass()),
                                                                 new TypeReference<CloudApplicationExtended>() {
                                                                 });
        StepsUtil.setExistingApp(context, existingApp);
    }

    private void assertStepPhaseMatch(StepPhase stepPhase) {
        Assertions.assertEquals(stepPhase.toString(), getExecutionStatus());
    }

    private void verifyClient(List<String> urisToDelete) {
        if (CollectionUtils.isEmpty(urisToDelete)) {
            verify(client, never()).deleteRoute(anyString(), anyString(), anyString());
            return;
        }

        for (String uri : urisToDelete) {
            ApplicationURI parsedUri = new ApplicationURI(uri);
            verify(client, times(1)).deleteRoute(parsedUri.getHost(), parsedUri.getDomain(), parsedUri.getPath());
        }
    }

    @Test
    public void testErrorMessage() {
        Assertions.assertEquals(Messages.ERROR_DELETING_IDLE_ROUTES, step.getStepErrorMessage(context));
    }

    @Test
    public void testIfNotHandledExceptionIsThrown() {
        prepareContext("existing-app-1.json", "app-to-deploy-1.json", new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR));
        Assertions.assertThrows(SLException.class, () -> step.execute(context));
    }

    @Override
    protected DeleteIdleRoutesStep createStep() {
        return new DeleteIdleRoutesStep();
    }

}
