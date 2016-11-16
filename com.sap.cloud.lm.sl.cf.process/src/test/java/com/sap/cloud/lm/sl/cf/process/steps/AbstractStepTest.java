package com.sap.cloud.lm.sl.cf.process.steps;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sap.activiti.common.impl.AbstractActivitiStep;
import com.sap.activiti.common.impl.MockDelegateExecution;
import com.sap.cloud.lm.sl.persistence.services.FileService;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.slp.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.slp.services.TaskExtensionService;

public abstract class AbstractStepTest<T extends AbstractActivitiStep> {

    protected DelegateExecution context = MockDelegateExecution.createSpyInstance();
    @Spy
    @InjectMocks
    protected ProcessLoggerProviderFactory processLoggerProviderFactory = new ProcessLoggerProviderFactory();
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @Mock
    protected ProgressMessageService progressMessageService;
    @Mock
    protected FileService fileService;
    @Mock
    protected TaskExtensionService taskExtensionService;
    @InjectMocks
    protected final T step = createStep();

    protected abstract T createStep();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

}
