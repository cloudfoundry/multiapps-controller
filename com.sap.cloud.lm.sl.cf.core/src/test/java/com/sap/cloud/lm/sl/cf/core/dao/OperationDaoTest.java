package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.Callable;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Runnable;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class OperationDaoTest {

    private static enum DaoOperation {
        ADD, REMOVE, MERGE, FIND, FIND_ALL, FIND_ALL_IN_SPACE, FIND_LAST_IN_SPACE, FIND_ACTIVE_IN_SPACE, FIND_FINISHED_IN_SPACE
    }

    private static final int LAST_ONGOING_OPERATIONS_COUNT = 3;
    private static final String SPACE_ID = "1234";

    private DaoOperation operation;
    private String processId;
    private String databaseContent;
    private String expected;

    private OperationDao dao = createDao();

    public OperationDaoTest(DaoOperation operation, String processId, String databaseContent, String expected) {
        this.operation = operation;
        this.processId = processId;
        this.databaseContent = databaseContent;
        this.expected = expected;
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Find all ongoing deployments in non-empty database:
            {
                DaoOperation.FIND_ALL, null, "database-content-2.json", "R:database-content-2.json",
            },
            // (1) Find all ongoing deployments in empty database:
            {
                DaoOperation.FIND_ALL, null, "database-content-1.json", "R:database-content-1.json",
            },
            // (2) Add ongoing deployment to empty database:
            {
                DaoOperation.ADD, "1", "database-content-1.json", "",
            },
            // (3) Add ongoing deployment to non-empty database (conflict):
            {
                DaoOperation.ADD, "1", "database-content-2.json", "E:Ongoing MTA operation with id \"1\" already exists",
            },
            // (4) Add ongoing deployment to non-empty database:
            {
                DaoOperation.ADD, "3", "database-content-2.json", "",
            },
            // (5) Remove ongoing deployment from database (non-existing):
            {
                DaoOperation.REMOVE, "1", "database-content-1.json", "E:Ongoing MTA operation with id \"1\" does not exist",
            },
            // (6) Remove ongoing deployment from database:
            {
                DaoOperation.REMOVE, "1", "database-content-2.json", "",
            },
            // (7) Find ongoing deployment in database (non-existing):
            {
                DaoOperation.FIND, "1", "database-content-1.json", "E:Ongoing MTA operation with id \"1\" does not exist",
            },
            // (8) Find ongoing deployment in database:
            {
                DaoOperation.FIND, "2", "database-content-2.json", "",
            },
            // (9) Find all ongoing deployments in space in non-empty database:
            {
                DaoOperation.FIND_ALL_IN_SPACE, null, "database-content-3.json", "R:database-content-4.json",
            },
            // (10) Find last ongoing deployments in space in non-empty database:
            {
                DaoOperation.FIND_LAST_IN_SPACE, null, "database-content-5.json", "R:database-content-6.json",
            },
            // (11) Find active ongoing deployments in space in non-empty database:
            {
                DaoOperation.FIND_ACTIVE_IN_SPACE, null, "database-content-7.json", "R:database-content-8.json",
            },
            // (12) Find finished ongoing deployments in space in non-empty database:
            {
                DaoOperation.FIND_FINISHED_IN_SPACE, null, "database-content-7.json", "R:database-content-9.json",
            },
// @formatter:on
        });
    }

    @Before
    public void importDatabaseContent() throws Throwable {
        List<Operation> ongoingProcesses = JsonUtil.convertJsonToList(getClass().getResourceAsStream(databaseContent),
            new TypeToken<List<Operation>>() {
            }.getType());

        for (Operation ongoingProcess : ongoingProcesses) {
            dao.add(ongoingProcess);
        }
    }

    @Test
    public void testAdd() throws Throwable {
        assumeTrue(operation.equals(DaoOperation.ADD));
        TestUtil.test(new Runnable() {
            @Override
            public void run() throws Exception {
                int currentOngoingOperationsCnt = dao.findAll().size();
                Operation oo1 = new Operation(processId, null, null, null, null, null, false, null);
                dao.add(oo1);

                assertEquals(currentOngoingOperationsCnt + 1, dao.findAll().size());

                Operation oo2 = dao.findRequired(processId);

                assertEquals(oo1.getProcessId(), oo2.getProcessId());
            }
        }, expected);
    }

    @Test
    public void testRemove() throws Throwable {
        assumeTrue(operation.equals(DaoOperation.REMOVE));

        TestUtil.test(new Runnable() {
            @Override
            public void run() throws Exception {
                int currentOngoingOperationsCnt = dao.findAll().size();
                dao.remove(processId);

                assertEquals(currentOngoingOperationsCnt - 1, dao.findAll().size());
            }
        }, expected);
    }

    @Test
    public void testFind() throws Throwable {
        assumeTrue(operation.equals(DaoOperation.FIND));

        TestUtil.test(new Runnable() {
            @Override
            public void run() throws Exception {
                Operation oo1 = new Operation(processId, null, null, null, null, null, false, null);
                Operation oo2 = dao.findRequired(processId);

                assertEquals(oo1.getProcessId(), oo2.getProcessId());
            }
        }, expected);
    }

    @Test
    public void testFindAll() throws Throwable {
        assumeTrue(operation.equals(DaoOperation.FIND_ALL));

        TestUtil.test(new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                return dao.findAll();
            }
        }, expected, getClass());
    }

    @Test
    public void testFindAllInSpace() throws Throwable {
        assumeTrue(operation.equals(DaoOperation.FIND_ALL_IN_SPACE));

        TestUtil.test(new Callable<List<Operation>>() {
            @Override
            public List<Operation> call() throws Exception {
                return dao.findAllInSpace(SPACE_ID);
            }
        }, expected, getClass());
    }

    @Test
    public void testFindLastOperationsInSpace() {
        assumeTrue(operation.equals(DaoOperation.FIND_LAST_IN_SPACE));

        TestUtil.test(() -> {
            return dao.findLastOperations(LAST_ONGOING_OPERATIONS_COUNT, SPACE_ID);
        }, expected, getClass());
    }

    @Test
    public void testFindActiveOperationsInSpace() {
        assumeTrue(operation.equals(DaoOperation.FIND_ACTIVE_IN_SPACE));

        TestUtil.test(() -> {
            return dao.findActiveOperations(SPACE_ID, State.getActiveStates());
        }, expected, getClass());
    }

    @Test
    public void testFindFinishedOperationsInSpace() {
        assumeTrue(operation.equals(DaoOperation.FIND_FINISHED_IN_SPACE));

        TestUtil.test(() -> {
            return dao.findFinishedOperations(SPACE_ID, State.getFinishedStates());
        }, expected, getClass());
    }

    @After
    public void clearDatabase() throws Throwable {
        for (Operation ongoingOperation : dao.findAll()) {
            dao.remove(ongoingOperation.getProcessId());
        }
    }

    private static OperationDao createDao() {
        OperationDao dao = new OperationDao();
        OperationDtoDao dtoDao = new OperationDtoDao();
        dtoDao.emf = Persistence.createEntityManagerFactory("OperationManagement");
        dao.dao = dtoDao;
        dao.ongoingOperationFactory = new OperationFactory();
        return dao;
    }

}
