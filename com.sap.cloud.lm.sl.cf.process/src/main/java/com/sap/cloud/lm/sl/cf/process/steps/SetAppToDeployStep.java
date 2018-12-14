package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.ModuleToDeploy;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("setAppToDeployStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class SetAppToDeployStep extends SyncFlowableStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) {
        ModuleToDeploy moduleToDeploy = StepsUtil.getModuleToDeploy(execution.getContext());

        try {
            List<CloudApplicationExtended> appsToDeploy = StepsUtil.getAppsToDeploy(execution.getContext());
            CloudApplicationExtended appToDeploy = appsToDeploy.stream()
                .filter(app -> app.getModuleName()
                    .equals(moduleToDeploy.getName()))
                .findFirst()
                .get();
            
            StepsUtil.setApp(execution.getContext(), appToDeploy);
            
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_SETTING_APP, moduleToDeploy.getName());
            throw e;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_SETTING_APP, moduleToDeploy.getName());
            throw e;
        }
    }

}
