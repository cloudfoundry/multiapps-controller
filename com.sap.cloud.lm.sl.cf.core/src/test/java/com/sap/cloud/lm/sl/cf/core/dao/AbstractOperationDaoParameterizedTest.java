package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;

import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;

import com.sap.cloud.lm.sl.cf.core.helpers.OperationFactory;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

public abstract class AbstractOperationDaoParameterizedTest {

    protected OperationDao dao = createDao();

    protected void addOperations(List<Operation> operations) {
        for (Operation operation : operations) {
            dao.add(operation);
        }
    }

    @AfterEach
    public void clearDatabase() {
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
