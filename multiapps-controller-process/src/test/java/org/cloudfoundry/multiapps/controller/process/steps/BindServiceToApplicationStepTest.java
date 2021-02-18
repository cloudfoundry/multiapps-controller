package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

class BindServiceToApplicationStepTest extends SyncFlowableStepTest<BindServiceToApplicationStep> {

    private static final String APPLICATION_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";
    private static final Map<String, Object> BINDING_PARAMETERS = Map.of("test-config", "test-value");
    private static final int CF_SERVICE_ALREADY_BOUND = 90003;

    @Test
    void testBndServiceStep() {
        prepareContext();

        step.execute(execution);

        assertStepFinishedSuccessfully();
        verify(client).bindServiceInstance(eq(APPLICATION_NAME), eq(SERVICE_NAME), eq(BINDING_PARAMETERS),
                                           any(ApplicationServicesUpdateCallback.class));
    }

    private void prepareContext() {
        CloudApplicationExtended application = ImmutableCloudApplicationExtended.builder()
                                                                                .name(APPLICATION_NAME)
                                                                                .build();
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_NAME);
        context.setVariable(Variables.SERVICE_BINDING_PARAMETERS, BINDING_PARAMETERS);
    }
    
    @Test
    void testDoNotThrowExceptionWhenServiceAlreadyBound() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(true)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleServiceAlreadyBoundErrorInCallback(customProcessContext));
    }

    @Test
    void testDoNotThrowExceptionOnOptionalService() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(true)
                                                                                                                  .build()));
        assertDoesNotThrow(() -> handleErrorInCallback(customProcessContext));
    }

    @Test
    void testThrowExceptionOnNotOptionalService() {
        ProcessContext customProcessContext = new ProcessContext(execution, stepLogger, clientProvider);
        customProcessContext.setVariable(Variables.SERVICES_TO_BIND, List.of(ImmutableCloudServiceInstanceExtended.builder()
                                                                                                                  .name(SERVICE_NAME)
                                                                                                                  .isOptional(false)
                                                                                                                  .build()));
        assertThrows(SLException.class, () -> handleErrorInCallback(customProcessContext));
    }

    private void handleErrorInCallback(ProcessContext customProcessContext) {
        new BindServiceToApplicationStep.DefaultApplicationServicesUpdateCallback(customProcessContext).onError(new CloudOperationException(HttpStatus.BAD_GATEWAY),
                                                                                                                APPLICATION_NAME,
                                                                                                                SERVICE_NAME);
    }

    private void handleServiceAlreadyBoundErrorInCallback(ProcessContext customProcessContext) {
        ClientV2Exception cause = new ClientV2Exception(HttpStatus.BAD_REQUEST.value(),
                                                        CF_SERVICE_ALREADY_BOUND,
                                                        "The app is already bound to the service.",
                                                        "CF-ServiceBindingAppServiceTaken");
        new BindServiceToApplicationStep.DefaultApplicationServicesUpdateCallback(customProcessContext).onError(new CloudOperationException(HttpStatus.BAD_REQUEST,
                                                                                                                                            "CF-ServiceBindingAppServiceTaken",
                                                                                                                                            "The app is already bound to the service.",
                                                                                                                                            cause),
                                                                                                                APPLICATION_NAME,
                                                                                                                SERVICE_NAME);
    }

    @Override
    protected BindServiceToApplicationStep createStep() {
        return new BindServiceToApplicationStep();
    }

}
