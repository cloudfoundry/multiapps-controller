package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;

public class ExecutionWrapper {
    private DelegateExecution context;
    private StepLogger stepLogger;
    private CloudFoundryClientProvider clientProvider;
    private ProcessLoggerProviderFactory processLoggerProviderFactory;

    public ExecutionWrapper(DelegateExecution context, StepLogger stepLogger, CloudFoundryClientProvider clientProvider,
        ProcessLoggerProviderFactory processLoggerProviderFactory) {
        this.context = context;
        this.stepLogger = stepLogger;
        this.clientProvider = clientProvider;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
    }

    public DelegateExecution getContext() {
        return context;
    }

    public StepLogger getStepLogger() {
        return stepLogger;
    }

    public CloudFoundryOperations getCloudFoundryClient() throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger);
    }

    public CloudFoundryOperations getCloudFoundryClient(String org, String space) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger, org, space);
    }

    public ClientExtensions getClientExtensions() throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger);
    }

    public ClientExtensions getClientExtensions(String org, String space) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger, org, space);
    }

    public ProcessLoggerProviderFactory getProcessLoggerProviderFactory() {
        return processLoggerProviderFactory;
    }

}
