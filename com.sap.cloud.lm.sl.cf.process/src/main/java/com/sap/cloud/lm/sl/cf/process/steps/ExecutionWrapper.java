package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
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

    protected CloudFoundryOperations getCloudFoundryClient() throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger);
    }

    protected CloudFoundryOperations getCloudFoundryClient(String org, String space) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, stepLogger, org, space);
    }

    protected ClientExtensions getClientExtensions() throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger);
    }

    protected ClientExtensions getClientExtensions(String org, String space) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, stepLogger, org, space);
    }

    public ProcessLoggerProviderFactory getProcessLoggerProviderFactory() {
        return processLoggerProviderFactory;
    }

    public ContextExtensionDao getContextExtensionDao() {
        return contextExtensionDao;
    }

}
