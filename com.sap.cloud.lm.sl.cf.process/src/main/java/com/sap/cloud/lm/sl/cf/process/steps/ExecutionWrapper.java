package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.client.CloudFoundryOperationsExtended;
import com.sap.cloud.lm.sl.cf.client.util.TimeoutExecutor;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;

public class ExecutionWrapper {
    private DelegateExecution context;
    private StepLogger stepLogger;
    private CloudFoundryClientProvider clientProvider;
    private ProcessLoggerProviderFactory processLoggerProviderFactory;
    private ContextExtensionDao contextExtensionDao;

    public ExecutionWrapper(DelegateExecution context, ContextExtensionDao contextExtensionDao, StepLogger stepLogger,
        CloudFoundryClientProvider clientProvider, ProcessLoggerProviderFactory processLoggerProviderFactory) {
        this.context = context;
        this.stepLogger = stepLogger;
        this.clientProvider = clientProvider;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
        this.contextExtensionDao = contextExtensionDao;
    }

    public DelegateExecution getContext() {
        return context;
    }

    public StepLogger getStepLogger() {
        return stepLogger;
    }

    public CloudFoundryOperations getCloudFoundryClient() throws SLException {
        CloudFoundryOperations cloudFoundryClientWithoutTimeout = getCloudFoundryClientWithoutTimeout();
        decorateClient(cloudFoundryClientWithoutTimeout);
        return cloudFoundryClientWithoutTimeout;
    }

    public CloudFoundryOperations getCloudFoundryClientWithoutTimeout() throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger);
    }

    public CloudFoundryOperations getCloudFoundryClient(String org, String space) throws SLException {
        CloudFoundryOperations cloudFoundryClientWithoutTimeout = getCloudFoundryClientWithoutTimeout(org, space);
        decorateClient(cloudFoundryClientWithoutTimeout);
        return cloudFoundryClientWithoutTimeout;
    }

    private void decorateClient(CloudFoundryOperations cloudfoundryOperations) {
        if (cloudfoundryOperations instanceof CloudFoundryOperationsExtended) {
            ((CloudFoundryOperationsExtended) cloudfoundryOperations).withTimeoutExecutor(TimeoutExecutor.getInstance());
        }
    }

    public CloudFoundryOperations getCloudFoundryClientWithoutTimeout(String org, String space) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger, org, space);
    }

    public ClientExtensions getClientExtensions() throws SLException {
        ClientExtensions clientExtensionsWithoutTimeout = getClientExtensionsWithoutTimeout();
        decorateWithTimeout(clientExtensionsWithoutTimeout);
        return clientExtensionsWithoutTimeout;
    }

    public ClientExtensions getClientExtensionsWithoutTimeout() throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger);
    }

    public ClientExtensions getClientExtensions(String org, String space) throws SLException {
        ClientExtensions clientExtensionsWithoutTimeout = getClientExtensionsWithoutTimeout(org, space);
        decorateWithTimeout(clientExtensionsWithoutTimeout);
        return clientExtensionsWithoutTimeout;
    }

    public ClientExtensions getClientExtensionsWithoutTimeout(String org, String space) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger, org, space);
    }

    private void decorateWithTimeout(ClientExtensions clientExtensions) {
        if (clientExtensions == null) {
            return;
        }

        clientExtensions.withTimeoutExecutor(TimeoutExecutor.getInstance());
    }

    public ProcessLoggerProviderFactory getProcessLoggerProviderFactory() {
        return processLoggerProviderFactory;
    }

    public ContextExtensionDao getContextExtensionDao() {
        return contextExtensionDao;
    }

}
