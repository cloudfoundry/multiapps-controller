package org.cloudfoundry.multiapps.controller.process.steps;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("collectBlueGreenSystemParametersStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class CollectBlueGreenSystemParametersStep extends CollectSystemParametersStep {

    @Override
    protected StepPhase executeStep(ProcessContext context) {
        return executeStepInternal(context, true);
    }

    @Override
    protected boolean shouldStartApplications(ProcessContext context) {
        if (context.getVariable(Variables.SKIP_IDLE_START)) {
            getStepLogger().info(MessageFormat.format(Messages.SKIPPING_START_OF_IDLE_APPLICATIONS, Variables.SKIP_IDLE_START.getName(),
                                                      true));
            return false;
        }
        return true;
    }

}
