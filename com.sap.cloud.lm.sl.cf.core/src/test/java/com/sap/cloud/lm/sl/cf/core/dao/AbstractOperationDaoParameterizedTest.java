package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;

import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class AbstractOperationDaoParameterizedTest {

    private final String databaseContentJsonLocation;

    protected OperationDao dao = createDao();

    public AbstractOperationDaoParameterizedTest(String databaseContentJsonLocation) {
        this.databaseContentJsonLocation = databaseContentJsonLocation;
    }

    @Before
    public void importDatabaseContent() throws Throwable {
        List<Operation> operations = JsonUtil.convertJsonToList(getClass().getResourceAsStream(databaseContentJsonLocation),
            new TypeToken<List<Operation>>() {
            }.getType());

        for (Operation operation : operations) {
            dao.add(operation);
        }
    }

    @After
    public void clearDatabase() throws Throwable {
        for (Operation operation : dao.findAll()) {
            dao.remove(operation.getProcessId());
        }
    }

    public static OperationDao createDao() {
        OperationDao dao = new OperationDao();
        OperationDtoDao dtoDao = new OperationDtoDao();
        dtoDao.emf = Persistence.createEntityManagerFactory("OperationManagement");
        dao.dao = dtoDao;
        dao.operationFactory = new OperationFactory();
        return dao;
    }

}
