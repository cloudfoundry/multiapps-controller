package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort.ApplicationPortType;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;

@Component("reserveRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReserveRoutesStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException, FileStorageException {
        // Get the next cloud application from the context:
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            CloudFoundryOperations client = execution.getCloudFoundryClient();
            boolean portBasedRouting = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.VAR_PORT_BASED_ROUTING, false);
            if (!(client instanceof ClientExtensions) || !portBasedRouting) {
                return StepPhase.DONE;
            }
            Set<Integer> allocatedPorts = StepsUtil.getAllocatedPorts(execution.getContext());
            getStepLogger().debug(Messages.ALLOCATED_PORTS, allocatedPorts);
            List<String> domains = app.getDomains();
            ClientExtensions clientExtended = (ClientExtensions) client;

            for (ApplicationPort applicationPort : app.getApplicationPorts()) {
                if (shouldReserveTcpPort(allocatedPorts, applicationPort)) {
                    reservePortInDomains(clientExtended, applicationPort, domains);
                }
            }
            return StepPhase.DONE;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RESERVING_ROUTES, app.getName());
            throw e;
        } catch (CloudFoundryException cfe) {
            CloudControllerException e = new CloudControllerException(cfe);
            getStepLogger().error(e, Messages.ERROR_RESERVING_ROUTES, app.getName());
            throw e;
        }
    }

    private void reservePortInDomains(ClientExtensions clientExtended, ApplicationPort applicationPort, List<String> domains) {
        boolean isTcps = ApplicationPortType.TCPS.equals(applicationPort.getPortType());
        for (String domain : domains) {
            clientExtended.reserveTcpPort(applicationPort.getPort(), domain, isTcps);
        }
    }

    private boolean shouldReserveTcpPort(Set<Integer> allocatedPorts, ApplicationPort applicationPort) {
        return !allocatedPorts.contains(applicationPort.getPort()) && !ApplicationPortType.HTTP.equals(applicationPort.getPortType());
    }
}
