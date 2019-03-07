package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class ProcessLogsPersistenceServiceTest {

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/persistence/db/changelog/db-changelog.xml";

    private static final String SPACE_1 = "myspace";
    private static final String NAMESPACE_1 = "12312412";
    private static final String NAMESPACE_2 = "23423523";

    private static final String LOG_1 = "log1.txt";
    private static final String LOG_2 = "log2.txt";

    private ProcessLogsPersistenceService processLogsService;

    private DataSourceWithDialect testDataSource;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.testDataSource = createDataSource();
        this.processLogsService = new ProcessLogsPersistenceService(testDataSource, true);
    }

    @After
    public void tearDown() throws Exception {
        sweepFiles();
        tearDownConnection();
    }

    @Test
    public void testPersistListAndGetLog() throws Exception {
        String space = SPACE_1;
        String namespace = NAMESPACE_1;
        String logName = LOG_1;
        File file = getResourceAsFile(LOG_1);

        processLogsService.persistLog(space, namespace, file, LOG_1);

        List<String> logNames = processLogsService.getLogNames(space, namespace);
        assertEquals(1, logNames.size());
        String persistedLogName = logNames.get(0);
        assertEquals(logName, persistedLogName);

        String persistedLogContent = processLogsService.getLogContent(space, namespace, persistedLogName);
        assertEquals(buildExpectedContent(file), persistedLogContent);
    }

    @Test
    public void testMultipleFilesForSameLog() throws Exception {
        String space = SPACE_1;
        String namespace = NAMESPACE_1;
        String logName = LOG_1;
        File file1 = getResourceAsFile(LOG_1);
        File file2 = getResourceAsFile(LOG_2);

        processLogsService.persistLog(space, namespace, file1, logName);
        processLogsService.persistLog(space, namespace, file2, logName);

        List<String> logNames = processLogsService.getLogNames(space, namespace);
        assertEquals(1, logNames.size());
        String persistedLogName = logNames.get(0);
        assertEquals(logName, persistedLogName);

        String persistedLogContent = processLogsService.getLogContent(space, namespace, persistedLogName);

        assertEquals(buildExpectedContent(file1, file2), persistedLogContent);
    }

    @Test
    public void testDeleteByNamespace() throws Exception {
        String space = SPACE_1;
        String namespaceToKeep = NAMESPACE_1;
        String namespaceToDelete = NAMESPACE_2;
        String logName = LOG_1;
        File file1 = getResourceAsFile(LOG_1);
        File file2 = getResourceAsFile(LOG_2);

        processLogsService.persistLog(space, namespaceToKeep, file1, logName);
        processLogsService.persistLog(space, namespaceToDelete, file2, logName);

        processLogsService.deleteByNamespace(namespaceToDelete);

        List<String> logNames = processLogsService.getLogNames(space, namespaceToKeep);
        assertEquals(1, logNames.size());
        String persistedLogName = logNames.get(0);
        assertEquals(logName, persistedLogName);

        logNames = processLogsService.getLogNames(space, namespaceToDelete);
        assertTrue(logNames.isEmpty());

        String persistedLogContent = processLogsService.getLogContent(space, namespaceToDelete, persistedLogName);
        assertNull(persistedLogContent);
    }

    private String buildExpectedContent(File... files) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        for (File file : files) {
            byteStream.write(Files.readAllBytes(file.toPath()));
        }
        return byteStream.toString();
    }

    private File getResourceAsFile(String name) throws URISyntaxException {
        URL resource = Thread.currentThread()
            .getContextClassLoader()
            .getResource(name);
        return new File(resource.toURI());
    }

    private DataSourceWithDialect createDataSource() throws Exception {
        return new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
    }

    private void sweepFiles() throws FileStorageException, Exception {
        processLogsService.deleteBySpace(SPACE_1);
    }

    private void tearDownConnection() throws Exception {
        // actually close the connection
        testDataSource.getDataSource()
            .getConnection()
            .close();
    }

}
