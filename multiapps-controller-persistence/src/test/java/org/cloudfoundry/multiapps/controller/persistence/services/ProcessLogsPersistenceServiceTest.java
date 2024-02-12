package org.cloudfoundry.multiapps.controller.persistence.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

class ProcessLogsPersistenceServiceTest {

    private static final String SPACE = "space-1";
    private static final String OPERATION = "operation-1";
    private static final String LOG_FILE_NAME = "log-1";
    private static final String LOGS_FIE = "logs.txt";
    private static final String LIQUIBASE_CHANGELOG_LOCATION = "org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog.xml";
    protected DataSourceWithDialect testDataSource;
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        this.testDataSource = createDataSource();
        this.processLogsPersistenceService = new ProcessLogsPersistenceService(testDataSource);
    }

    @AfterEach
    void cleanUp() throws Exception {
        processLogsPersistenceService.deleteBySpaceIds(List.of(SPACE));
        testDataSource.getDataSource()
                      .getConnection()
                      .close();
    }

    private DataSourceWithDialect createDataSource() throws Exception {
        return new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
    }

    @Test
    void getLogNamesTest() throws Exception {
        URL logsResource = getClass().getResource(LOGS_FIE);
        File logsFile = new File(logsResource.toURI());
        processLogsPersistenceService.persistLog(SPACE, OPERATION, logsFile, LOG_FILE_NAME);
        List<String> logNames = processLogsPersistenceService.getLogNames(SPACE, OPERATION);
        assertEquals(1, logNames.size());
        assertEquals(LOG_FILE_NAME, logNames.get(0));
    }

    @Test
    void getLogContentTest() throws Exception {
        URL logsResource = getClass().getResource(LOGS_FIE);
        File logsFile = new File(logsResource.toURI());
        processLogsPersistenceService.persistLog(SPACE, OPERATION, logsFile, LOG_FILE_NAME);
        String logContent = processLogsPersistenceService.getLogContent(SPACE, OPERATION, LOG_FILE_NAME);
        String actualFileContent = FileUtils.readFileToString(logsFile, StandardCharsets.UTF_8);
        assertEquals(actualFileContent, logContent);
    }

}
