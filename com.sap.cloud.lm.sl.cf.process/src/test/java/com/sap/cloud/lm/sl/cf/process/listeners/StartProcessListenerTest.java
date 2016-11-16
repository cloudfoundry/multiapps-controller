package com.sap.cloud.lm.sl.cf.process.listeners;

import java.util.Arrays;

import org.activiti.engine.delegate.DelegateExecution;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.slf4j.LoggerFactory;

import com.sap.activiti.common.impl.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.ProcessType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ArgumentMatcherProvider;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.slp.services.ProcessLogsPersistenceService;

@RunWith(Parameterized.class)
public class StartProcessListenerTest {

    private static final String USER = "current-user";
    private static final String SPACE_ID = "test-space-id";

    private final String processInstanceId;
    private final String serviceId;
    private final String exceptionMessage;

    private DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private OngoingOperationDao dao;
    @Spy
    @InjectMocks
    protected ProcessLoggerProviderFactory processLoggerProviderFactory = new ProcessLoggerProviderFactory();
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @Mock
    protected ProgressMessageService progressMessageService;

    @InjectMocks
    private StartProcessListener listener = new StartProcessListener();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Create OngoingOperation for process undeploy
            {
                "process-instance-id", Constants.UNDEPLOY_SERVICE_ID, null
            },
            // (1) Create OngoingOperation for process deploy
            {
                "process-instance-id", Constants.DEPLOY_SERVICE_ID, null
            },
            // (0) Create OngoingOperation for process undeploy
            {
                "process-instance-id", "unknown-service-id", "Unknown service id \"unknown-service-id\""
            },
// @formatter:on
        });
    }

    public StartProcessListenerTest(String processInstanceId, String serviceId, String exceptionMessage) {
        this.serviceId = serviceId;
        this.processInstanceId = processInstanceId;
        this.exceptionMessage = exceptionMessage;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        loadParameters();
        prepareContext();
    }

    @Test
    public void testVerify() throws Exception {
        listener.notify(context);

        verifyOngoingOperationInsertion();
    }

    private void prepareContext() {
        Mockito.when(context.getProcessInstanceId()).thenReturn(processInstanceId);
        context.setVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SERVICE_ID, serviceId);
        context.setVariable(com.sap.cloud.lm.sl.slp.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        context.setVariable(Constants.VAR_USER, USER);
    }

    private void loadParameters() {
        if (exceptionMessage != null) {
            exception.expectMessage(exceptionMessage);
            exception.expect(SLException.class);
        }
    }

    private void verifyOngoingOperationInsertion() throws SLException, ConflictException {
        ProcessType type = (serviceId.equals("xs2-undeploy")) ? ProcessType.UNDEPLOY : ProcessType.DEPLOY;
        String user = StepsUtil.determineCurrentUser(context, LoggerFactory.getLogger(StartProcessListenerTest.class),
            processLoggerProviderFactory);
        Mockito.verify(dao).add(Mockito.argThat(ArgumentMatcherProvider.getOngoingOpMatcher(
            new OngoingOperation(processInstanceId, type, null, SPACE_ID, null, user, false, null))));
    }

}
