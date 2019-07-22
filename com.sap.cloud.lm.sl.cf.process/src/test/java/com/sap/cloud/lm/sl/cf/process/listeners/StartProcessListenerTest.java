package com.sap.cloud.lm.sl.cf.process.listeners;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;

import org.flowable.engine.delegate.DelegateExecution;
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

import com.sap.cloud.lm.sl.cf.core.dao.HistoricOperationEventDao;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.metadata.ProcessTypeToOperationMetadataMapper;
import com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.ConflictException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.GenericArgumentMatcher;

@RunWith(Parameterized.class)
public class StartProcessListenerTest {

    private static final String TASK_ID = "test-task-id";
    private static final String USER = "current-user";
    private static final String SPACE_ID = "test-space-id";
    private static final ZonedDateTime START_TIME = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));

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
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @Mock
    private HistoricOperationEventDao historicOperationEventDao;
    @Spy
    private ProcessTypeToOperationMetadataMapper processTypeToServiceMetadataMapper = new ProcessTypeToOperationMetadataMapper();
    @Spy
    private ProcessLogsPersister processLogsPersister = new ProcessLogsPersister();
    @Mock
    private ApplicationConfiguration configuration;

    private Supplier<ZonedDateTime> currentTimeSupplier = () -> START_TIME;

    @InjectMocks
    private StartProcessListener listener = new StartProcessListener();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Create Operation for process undeploy
            {
                "process-instance-id", ProcessType.UNDEPLOY, null
            },
            // (1) Create Operation for process deploy
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
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
            .thenReturn(stepLogger);
        Mockito.doNothing()
            .when(processLogsPersister)
            .persistLogs(processInstanceId, TASK_ID);
    }

    @Test
    public void testVerify() throws Exception {
        listener.notify(context);

        verifyOperationInsertion();
    }

    private void prepareContext() {
        listener.currentTimeSupplier = currentTimeSupplier;
        Mockito.when(context.getProcessInstanceId())
            .thenReturn(processInstanceId);
        Mockito.when(context.getVariables())
            .thenReturn(Collections.emptyMap());
        Mockito.when(processTypeParser.getProcessType(context))
            .thenReturn(processType);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, SPACE_ID);
        context.setVariable(Constants.VAR_USER, USER);
        context.setVariable(Constants.VAR_CORRELATION_ID, processInstanceId);
        context.setVariable(Constants.TASK_ID, TASK_ID);
    }

    private void loadParameters() {
        if (exceptionMessage != null) {
            exception.expectMessage(exceptionMessage);
            exception.expect(SLException.class);
        }
    }

    private void verifyOperationInsertion() throws SLException, ConflictException {
        String user = StepsUtil.determineCurrentUser(context, stepLogger);
        Operation operation = new Operation().processId(processInstanceId)
            .processType(processType)
            .spaceId(SPACE_ID)
            .startedAt(START_TIME)
            .user(user)
            .acquiredLock(false);
        Mockito.verify(dao)
            .add(Mockito.argThat(GenericArgumentMatcher.forObject(operation)));
        Mockito.verify(processLogsPersister, Mockito.atLeastOnce())
            .persistLogs(processInstanceId, TASK_ID);
    }

}
