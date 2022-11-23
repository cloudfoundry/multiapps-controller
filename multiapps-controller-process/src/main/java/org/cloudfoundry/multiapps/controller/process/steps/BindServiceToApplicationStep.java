package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.DefaultApplicationServicesUpdateCallback;
import org.cloudfoundry.multiapps.controller.process.util.ServiceBindingPollingFactory;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.ApplicationServicesUpdateCallback;
import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;

@Named("bindServiceToApplicationStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BindServiceToApplicationStep extends AsyncFlowableStep {

    @Override
    protected StepPhase executeAsyncStep(ProcessContext context) throws Exception {
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);
        String service = context.getVariable(Variables.SERVICE_TO_UNBIND_BIND);
        Map<String, Object> serviceBindingParameters = context.getVariable(Variables.SERVICE_BINDING_PARAMETERS);
        getStepLogger().info(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1, service, app.getName());
        CloudControllerClient controllerClient = context.getControllerClient();
        Optional<String> jobId = controllerClient.bindServiceInstance(app.getName(), service, serviceBindingParameters,
                                                                      getApplicationServicesUpdateCallback(context, controllerClient));
        if (context.getVariable(Variables.USE_LAST_OPERATION_FOR_SERVICE_BINDING_CREATION)) {
            return StepPhase.POLL;
        }
        if (jobId.isEmpty()) {
            getStepLogger().infoWithoutProgressMessage(Messages.BINDING_SERVICE_INSTANCE_0_TO_APPLICATION_1_FINISHED, service,
                                                       app.getName());
            return StepPhase.DONE;
        }
        context.setVariable(Variables.SERVICE_BINDING_JOB_ID, jobId.get());
        return StepPhase.POLL;
    }

    private ApplicationServicesUpdateCallback getApplicationServicesUpdateCallback(ProcessContext context,
                                                                                   CloudControllerClient controllerClient) {
        return new DefaultApplicationServicesUpdateCallback(context, controllerClient);
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        AsyncExecution pollingExecution = createServiceBindingPollingFactory(context).createPollingExecution();
        return List.of(pollingExecution);
    }

    private ServiceBindingPollingFactory createServiceBindingPollingFactory(ProcessContext context) {
        return new ServiceBindingPollingFactory(context, ServiceCredentialBindingOperation.Type.CREATE);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_BINDING_SERVICE_INSTANCE_TO_APPLICATION,
                                    context.getVariable(Variables.SERVICE_TO_UNBIND_BIND), context.getVariable(Variables.APP_TO_PROCESS)
                                                                                                  .getName());
    }

}
