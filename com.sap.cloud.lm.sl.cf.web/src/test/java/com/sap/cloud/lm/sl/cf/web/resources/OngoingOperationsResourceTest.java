package com.sap.cloud.lm.sl.cf.web.resources;

import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.dao.OngoingOperationDao;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation;
import com.sap.cloud.lm.sl.cf.core.model.OngoingOperation.SlpTaskStates;
import com.sap.cloud.lm.sl.cf.core.model.OperationsBean;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.slp.activiti.ActivitiProcess;
import com.sap.lmsl.slp.SlpTaskState;

@RunWith(Parameterized.class)
public class OngoingOperationsResourceTest {

    private TestCase<TestInput> test;

    private final static String PROCESS_SPACE_ID = "testProcessSpaceId";

    public OngoingOperationsResourceTest(TestCase<TestInput> test) {
        this.test = test;
    }

    @Parameters
    public static List<Object[]> getParameters() throws Exception {
        return Arrays.asList(new Object[][] {
      //@formatter:off
                // (0)
                { 
                    new GetOngoingOperationTest(new GetOngoingOperationTestInput("100", "ongoing-operation-1.json"),"R:ongoing-operations-resource-test-output-1.json") 
                },
                // (1) With ongoing operations and empty filter
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Collections.emptyList()), "ongoing-operations-2.json"),"R:ongoing-operations-resource-test-output-2.json") 
                },
                // (2) With only active operations and requested finished operations
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Arrays.asList("SLP_TASK_STATE_FINISHED", "SLP_TASK_STATE_ABORTED")), "ongoing-operations-2.json"),"R:ongoing-operations-resource-test-output-5.json") 
                },
                // (3) With only active operations and requested active operations
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Arrays.asList("SLP_TASK_STATE_RUNNING", "SLP_TASK_STATE_ERROR")), "ongoing-operations-3.json"),"R:ongoing-operations-resource-test-output-3.json") 
                },
                // (4) With ongoing operations and requested the last two
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(2, Collections.emptyList()), "ongoing-operations-4.json"),"R:ongoing-operations-resource-test-output-4.json") 
                },
                // (5) With only finished operations and requested active operations
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Arrays.asList("SLP_TASK_STATE_RUNNING", "SLP_TASK_STATE_ERROR")), "ongoing-operations-5.json"),"R:ongoing-operations-resource-test-output-5.json") 
                },
                // (6) With only active and finished operations and requested active and finished operations
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Arrays.asList("SLP_TASK_STATE_RUNNING", "SLP_TASK_STATE_FINISHED")), "ongoing-operations-6.json"),"R:ongoing-operations-resource-test-output-6.json") 
                },
                // (7) With only active and finished operations and requested the last 2 operations and the running and finished operations
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(2, Arrays.asList("SLP_TASK_STATE_RUNNING", "SLP_TASK_STATE_FINISHED")), "ongoing-operations-6.json"),"R:ongoing-operations-resource-test-output-7.json") 
                },
                // (8) 
                { 
                    new GetAllOngoingOperationsTest(new GetAllOngoingOperationsTestInput(new OperationsBean(null, Collections.emptyList()), "ongoing-operations-7.json"),"R:ongoing-operations-resource-test-output-8.json") 
                },
            }
        );
      //@formatter:on
    }

    @Test
    public void test() throws Exception {
        test.run();
    }

    private static class GetOngoingOperationTestInput extends TestInput {

        private String id;
        private OngoingOperation operation;

        public GetOngoingOperationTestInput(String id, String operationJsonLocation) throws Exception {
            this.id = id;
            this.operation = loadJsonInput(operationJsonLocation, OngoingOperation.class, getClass());
        }

        public String getId() {
            return id;
        }

        public OngoingOperation getOperation() {
            return operation;
        }

    }

    private static class GetAllOngoingOperationsTestInput extends TestInput {

        private List<OngoingOperation> operations;
        private OperationsBean filter;

        public GetAllOngoingOperationsTestInput(OperationsBean filter, String operationJsonLocation) throws Exception {
            this.filter = filter;
            this.operations = loadJsonInput(operationJsonLocation, new TypeToken<List<OngoingOperation>>() {
            }.getType(), getClass());
        }

        public List<OngoingOperation> getOperations() {
            return operations;
        }

        public OperationsBean getFilter() {
            return filter;
        }

    }

    private static class GetOngoingOperationTest extends TestCase<GetOngoingOperationTestInput> {
        @Mock
        private OngoingOperationDao dao;
        @InjectMocks
        private OngoingOperationsResourceMock resource;

        public GetOngoingOperationTest(GetOngoingOperationTestInput input, String expected) {
            super(input, expected);
            resource = new OngoingOperationsResourceMock();
        }

        @Override
        protected void test() throws Exception {
            TestUtil.test(() -> {

                return resource.getOngoingOperation(input.getId());

            }, expected, getClass());

        }

        @Override
        protected void setUp() throws Exception {
            MockitoAnnotations.initMocks(this);
            when(dao.findRequired(input.getId())).thenReturn(input.getOperation());
        }

    }

    private static class GetAllOngoingOperationsTest extends TestCase<GetAllOngoingOperationsTestInput> {
        @Mock
        private OngoingOperationDao dao;
        @InjectMocks
        private OngoingOperationsResourceMock resource;

        public GetAllOngoingOperationsTest(GetAllOngoingOperationsTestInput input, String expected) {
            super(input, expected);
            resource = new OngoingOperationsResourceMock();
        }

        @Override
        protected void test() throws Exception {
            TestUtil.test(() -> {

                return resource.getOngoingOperations(input.getFilter()).getOngoingOperations();

            }, expected, getClass());

        }

        @Override
        protected void setUp() throws Exception {
            MockitoAnnotations.initMocks(this);
            prepareDao();
        }

        @SuppressWarnings("unchecked")
        private void prepareDao() {
            when(dao.findAllInSpace(Mockito.anyString())).thenReturn(input.getOperations());

            when(dao.findOperationsByStatus(Mockito.anyList(), Mockito.anyString())).thenCallRealMethod();

            when(dao.findActiveOperations(Mockito.anyString(), Mockito.anyList())).thenReturn(
                input.getOperations().stream().filter(op -> SlpTaskStates.getActiveSlpTaskStates().contains(op.getFinalState())).collect(
                    Collectors.toList()));

            when(dao.findFinishedOperations(Mockito.anyString(), Mockito.anyList())).thenReturn(
                input.getOperations().stream().filter(op -> SlpTaskStates.getFinishedSlpTaskStates().contains(op.getFinalState())).collect(
                    Collectors.toList()));

            if (input.filter.getStatusList() != null) {
                List<SlpTaskState> slpTaskStates = input.filter.getStatusList().stream().map(
                    status -> SlpTaskState.valueOf(status)).collect(Collectors.toList());
                when(dao.findAllInSpaceByStatus(Mockito.eq(slpTaskStates), Mockito.anyString())).thenReturn(
                    input.operations.stream().filter(oo -> slpTaskStates.contains(oo.getFinalState())).collect(Collectors.toList()));
            }

            if (input.filter.getLastRequestedOperationsCount() != null) {
                try {
                    int lastRequestedOperations = input.filter.getLastRequestedOperationsCount();
                    when(dao.findLastOperations(Mockito.eq(lastRequestedOperations), Mockito.anyString())).thenReturn(
                        input.getOperations().subList(lastRequestedOperations, input.getOperations().size()));
                } catch (NumberFormatException e) {
                    when(dao.findLastOperations(Mockito.anyInt(), Mockito.anyString())).thenReturn(Collections.emptyList());
                }
            } else {
                when(dao.findLastOperations(Mockito.anyInt(), Mockito.anyString())).thenReturn(input.getOperations());
            }
        }

    }

    private static class OngoingOperationsResourceMock extends OngoingOperationsResource {

        @Override
        protected ActivitiProcess getProcessForOperation(OngoingOperation ongoingOperation) throws SLException {
            ActivitiProcess processMock = Mockito.mock(ActivitiProcess.class);
            Mockito.when(processMock.getCurrentState()).thenReturn(SlpTaskState.SLP_TASK_STATE_RUNNING);
            return processMock;
        }

        @Override
        protected String getSpaceId() throws SLException {
            return PROCESS_SPACE_ID;
        }

        @Override
        protected String getSpaceIdByProcessId(String processId) throws SLException {
            return PROCESS_SPACE_ID;
        }

    }
}
