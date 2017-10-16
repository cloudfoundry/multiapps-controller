package com.sap.cloud.lm.sl.cf.process.listeners;

import java.util.Arrays;
import java.util.Collections;

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

import com.sap.activiti.common.impl.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToServiceMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ArgumentMatcherProvider;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;

@RunWith(Parameterized.class)
public class StartProcessListenerTest {

    private static final String USER = "current-user";
    private static final String SPACE_ID = "test-space-id";

    private final String processInstanceId;
    private final ProcessType processType;
    private final String exceptionMessage;

    private DelegateExecution context = MockDelegateExecution.createSpyInstance();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private OperationDao dao;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Spy
    private ProcessTypeToServiceMetadataMapper processTypeToServiceMetadataMapper = new ProcessTypeToServiceMetadataMapper();
    @Mock
    private Configuration configuration;

    @InjectMocks
    private StartProcessListener listener = new StartProcessListener();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Create OngoingOperation for process undeploy
            {
                "process-instance-id", ProcessType.UNDEPLOY, null
            },
            // (1) Create OngoingOperation for process deploy
            {
                "process-instance-id", ProcessType.DEPLOY, null
            },
// @formatter:on
        });
    }

    public StartProcessListenerTest(String processInstanceId, ProcessType processType, String exceptionMessage) {
        this.processType = processType;
        this.processInstanceId = processInstanceId;
        this.exceptionMessage = exceptionMessage;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        loadParameters();
        prepareContext();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(stepLogger);
    }

    @Test
    public void testVerify() throws Exception {
        listener.notify(context);

        verifyOngoingOperationInsertion();
    }

    private void prepareContext() {
        Mockito.when(context.getProcessInstanceId()).thenReturn(processInstanceId);
        Mockito.when(context.getVariables()).thenReturn(Collections.emptyMap());
        Mockito.when(processTypeParser.getProcessType(context)).thenReturn(processType);
        context.setVariable(com.sap.cloud.lm.sl.cf.api.activiti.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        context.setVariable(Constants.VAR_USER, USER);
    }

    private void loadParameters() {
        if (exceptionMessage != null) {
            exception.expectMessage(exceptionMessage);
            exception.expect(SLException.class);
        }
    }

    private void verifyOngoingOperationInsertion() throws SLException, ConflictException {
        String user = StepsUtil.determineCurrentUser(context, stepLogger);
        Mockito.verify(dao).add(Mockito.argThat(ArgumentMatcherProvider.getOngoingOpMatcher(
            new Operation(processInstanceId, processType, null, SPACE_ID, null, user, false, null))));
    }

}
