package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.BooleanUtils;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.listeners.ManageAppServiceBindingEndListener;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("determineVcapServicesPropertiesChangedStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class DetermineVcapServicesPropertiesChangedStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        List<String> services = context.getVariable(Variables.SERVICES_TO_UNBIND_BIND);
        CloudApplicationExtended app = context.getVariable(Variables.APP_TO_PROCESS);

        boolean changedVcapServicesProperties = isVcapServicesPropertiesChanged(services, app, context.getExecution());

        getStepLogger().debug(Messages.VCAP_SERVICES_PROPERTIES_FOR_APPLICATION_CHANGED, app.getName(), changedVcapServicesProperties);
        context.setVariable(Variables.VCAP_SERVICES_PROPERTIES_CHANGED, changedVcapServicesProperties);
        return StepPhase.DONE;
    }

    private boolean isVcapServicesPropertiesChanged(List<String> services, CloudApplicationExtended app, DelegateExecution execution) {
        return services.stream()
                       .map(service -> ManageAppServiceBindingEndListener.buildExportedVariableName(app.getName(), service))
                       .map(variableName -> StepsUtil.getObject(execution, variableName, false))
                       .anyMatch(BooleanUtils::isTrue);
    }

    @Override
    protected String getStepErrorMessage(ProcessContext context) {
        return MessageFormat.format(Messages.ERROR_WHILE_DETERMINE_VCAP_SERVICES_PROPERTIES_CHANGED_FOR_APPLICATION,
                                    context.getVariable(Variables.APP_TO_PROCESS)
                                           .getName());
    }

}
