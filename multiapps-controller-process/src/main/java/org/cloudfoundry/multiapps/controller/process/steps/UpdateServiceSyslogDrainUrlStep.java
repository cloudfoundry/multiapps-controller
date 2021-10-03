package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.util.OperationExecutionState;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation.Type;

@Named("updateServiceSyslogUrlStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UpdateServiceSyslogDrainUrlStep extends ServiceStep {

    @Override
    protected OperationExecutionState executeOperation(ProcessContext context, CloudControllerClient client,
                                                       CloudServiceInstanceExtended service) {
        if (service.shouldSkipSyslogUrlUpdate()) {
            getStepLogger().warn(Messages.WILL_NOT_UPDATE_SYSLOG_URL, service.getName());
            return OperationExecutionState.FINISHED;
        }
        getStepLogger().info(Messages.UPDATING_SERVICE_SYSLOG_URL, service.getName());

        client.updateServiceSyslogDrainUrl(service.getName(), service.getSyslogDrainUrl());

        getStepLogger().debug(Messages.SERVICE_SYSLOG_URL_UPDATED, service.getName());
        return OperationExecutionState.EXECUTING;
    }

    @Override
    protected List<AsyncExecution> getAsyncStepExecutions(ProcessContext context) {
        return Collections.singletonList(new PollServiceCreateOrUpdateOperationsExecution(getServiceOperationGetter(),
                                                                                          getServiceProgressReporter()));
    }

    @Override
    protected Type getOperationType() {
        return ServiceOperation.Type.UPDATE;
    }
}
