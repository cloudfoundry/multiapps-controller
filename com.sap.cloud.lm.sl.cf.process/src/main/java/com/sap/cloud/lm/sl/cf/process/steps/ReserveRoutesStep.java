package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.List;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.XsCloudControllerClient;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ApplicationPort.ApplicationPortType;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;

@Component("reserveRoutesStep")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class ReserveRoutesStep extends SyncActivitiStep {

    @Override
    protected StepPhase executeStep(ExecutionWrapper execution) throws SLException, FileStorageException {
        // Get the next cloud application from the context:
        CloudApplicationExtended app = StepsUtil.getApp(execution.getContext());

        try {
            CloudControllerClient client = execution.getControllerClient();
            boolean portBasedRouting = StepsUtil.getVariableOrDefault(execution.getContext(), Constants.VAR_PORT_BASED_ROUTING, false);
            if (!(client instanceof XsCloudControllerClient) || !portBasedRouting) {
                return StepPhase.DONE;
            }
            Set<Integer> allocatedPorts = StepsUtil.getAllocatedPorts(execution.getContext());
            getStepLogger().debug(Messages.ALLOCATED_PORTS, allocatedPorts);
            List<String> domains = app.getDomains();
            XsCloudControllerClient xsClient = (XsCloudControllerClient) client;

            for (ApplicationPort applicationPort : app.getApplicationPorts()) {
                if (shouldReserveTcpPort(allocatedPorts, applicationPort)) {
                    reservePortInDomains(xsClient, applicationPort, domains);
                }
            }
            return StepPhase.DONE;
        } catch (CloudOperationException coe) {
            CloudControllerException e = new CloudControllerException(coe);
            getStepLogger().error(e, Messages.ERROR_RESERVING_ROUTES, app.getName());
            throw e;
        } catch (SLException e) {
            getStepLogger().error(e, Messages.ERROR_RESERVING_ROUTES, app.getName());
            throw e;
        }
    }

    private void reservePortInDomains(XsCloudControllerClient xsClient, ApplicationPort applicationPort, List<String> domains) {
        boolean isTcps = ApplicationPortType.TCPS.equals(applicationPort.getPortType());
        for (String domain : domains) {
            xsClient.reserveTcpPort(applicationPort.getPort(), domain, isTcps);
        }
    }

    private boolean shouldReserveTcpPort(Set<Integer> allocatedPorts, ApplicationPort applicationPort) {
        return !allocatedPorts.contains(applicationPort.getPort()) && !ApplicationPortType.HTTP.equals(applicationPort.getPortType());
    }
}
