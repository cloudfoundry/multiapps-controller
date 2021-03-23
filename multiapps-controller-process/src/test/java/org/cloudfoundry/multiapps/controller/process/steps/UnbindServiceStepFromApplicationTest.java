package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

import java.util.List;

class UnbindServiceStepFromApplicationTest extends SyncFlowableStepTest<UnbindServiceFromApplicationStep> {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";

    @Test
    void testUnbindServiceStep() {
        prepareContext();

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).unbindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), any(ApplicationServicesUpdateCallback.class));
    }

    @Test
    void testDoNotThrowExceptionWhenServiceBindingAlreadyDeleted() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        assertDoesNotThrow(() -> handleErrorInCallback(customProcessContext, HttpStatus.NOT_FOUND));
    }

    @Test
    void testDoNotThrowExceptionOnOptionalServiceWhenServerError() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(true)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleErrorInCallback(customProcessContext, HttpStatus.BAD_GATEWAY));
    }

    @Test
    void testThrowExceptionOnNotOptionalServiceWhenServerError() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(false)
                                                                                                                  .build()));
        assertThrows(SLException.class, () -> handleErrorInCallback(customProcessContext, HttpStatus.BAD_GATEWAY));
    }

    private void handleErrorInCallback(ProcessContext customProcessContext, HttpStatus httpStatus) {
        new UnbindServiceFromApplicationStep.UnbindServiceFromApplicationCallback(customProcessContext).onError(new CloudOperationException(httpStatus),
                                                                                                                APPLICATION_NAME,
                                                                                                                SERVICE_NAME);
    }

    private void prepareContext() {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APPLICATION_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
    }

    @Override
    protected UnbindServiceFromApplicationStep createStep() {
        return new UnbindServiceFromApplicationStep();
    }

}
