package com.sap.cloud.lm.sl.cf.persistence.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class ProgressMessageServiceTest {

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/persistence/db/changelog/db-changelog.xml";
    private static final String TASK_ID_1 = "create-app";
    private static final String TASK_ID_2 = "update-app";
    private static final String TASK_EXECUTION_ID_1 = "backend";
    private static final String TASK_EXECUTION_ID_2 = "ui";
    private static final String MESSAGE_TEXT_1 = "Creating application \"backend\"...";
    private static final String MESSAGE_TEXT_2 = "Updating application \"ui\"...";
    private static final String PROCESS_INSTANCE_ID_1 = "100";
    private static final String PROCESS_INSTANCE_ID_2 = "200";

    private ProgressMessageService service;

    private DataSourceWithDialect testDataSource;
    private ProgressMessage progressMessage1;
    private ProgressMessage progressMessage2;
    private ProgressMessage progressMessage3;
    private ProgressMessage progressMessage4;

    @Before
    public void setUp() throws Exception {
        setUpConnection();
        initializeData();
    }

    private void setUpConnection() throws Exception {
        testDataSource = new DataSourceWithDialect(TestDataSourceProvider.getDataSource(LIQUIBASE_CHANGELOG_LOCATION));
        service = new ProgressMessageService(testDataSource);
    }

    private void initializeData() {
        progressMessage1 = new ProgressMessage(PROCESS_INSTANCE_ID_1, TASK_ID_1, TASK_EXECUTION_ID_1, ProgressMessageType.ERROR,
            MESSAGE_TEXT_1, new Timestamp(System.currentTimeMillis()));
        progressMessage2 = new ProgressMessage(PROCESS_INSTANCE_ID_1, TASK_ID_2, TASK_EXECUTION_ID_2, ProgressMessageType.INFO,
            MESSAGE_TEXT_2, new Timestamp(System.currentTimeMillis()));
        progressMessage3 = new ProgressMessage(PROCESS_INSTANCE_ID_2, TASK_ID_1, TASK_EXECUTION_ID_1, ProgressMessageType.INFO,
            MESSAGE_TEXT_1, new Timestamp(System.currentTimeMillis()));
        progressMessage4 = new ProgressMessage(PROCESS_INSTANCE_ID_2, TASK_ID_2, TASK_EXECUTION_ID_2, ProgressMessageType.INFO,
            MESSAGE_TEXT_2, new Timestamp(System.currentTimeMillis()));

        List<ProgressMessage> messages = Arrays.asList(progressMessage1, progressMessage2, progressMessage3, progressMessage4);
        for (ProgressMessage message : messages) {
            service.add(message);
        }
    }

    @After
    public void tearDown() throws Exception {
        service.removeByProcessId(PROCESS_INSTANCE_ID_1);
        service.removeByProcessId(PROCESS_INSTANCE_ID_2);
        service.removeByProcessId("test-processId");
        JdbcUtil.closeQuietly(testDataSource.getDataSource()
            .getConnection());
    }

    @Test
    public void testInsert() {
        ProgressMessage progressMessage = new ProgressMessage("test-processId", "test-taskId", "test-executionId",
            ProgressMessageType.ERROR, "test-error-message", new Timestamp(System.currentTimeMillis()));
        boolean insertSuccess = service.add(progressMessage);
        assertTrue(insertSuccess);

        List<ProgressMessage> allMessages = service.findAll();
        assertEquals(5, allMessages.size());
        assertSameProgressMessage(progressMessage, allMessages.get(allMessages.size() - 1));
    }

    @Test
    public void testUpdate() {
        final String PROCESS_ID = "test-update-processId";
        final String TASK_ID = "test-update-taskId";
        final String TASK_EXECUTION_ID = "test-update-executionId";

        ProgressMessage progressMessage = new ProgressMessage(PROCESS_ID, TASK_ID, TASK_EXECUTION_ID, ProgressMessageType.INFO,
            "test-update-info-message", new Timestamp(System.currentTimeMillis()));
        boolean insertSuccess = service.add(progressMessage);
        assertTrue(insertSuccess);

        List<ProgressMessage> allMessages = service.findAll();
        assertEquals(5, allMessages.size());

        List<ProgressMessage> messagesByProcessId = service.findByProcessId(PROCESS_ID);
        assertEquals(1, messagesByProcessId.size());
        ProgressMessage messageToUpdate = messagesByProcessId.get(0);

        ProgressMessage updatedProgressMessage = new ProgressMessage(PROCESS_ID, TASK_ID, TASK_EXECUTION_ID, ProgressMessageType.INFO,
            "test-update-new-info-message", new Timestamp(System.currentTimeMillis()));
        boolean updateSuccess = service.update(messageToUpdate.getId(), updatedProgressMessage);
        assertTrue(updateSuccess);

        allMessages = service.findAll();
        assertEquals(5, allMessages.size());
        assertSameProgressMessage(updatedProgressMessage, allMessages.get(allMessages.size() - 1));
    }

    @Test
    public void testFindByProcessId() {
        List<ProgressMessage> messages = service.findByProcessId(PROCESS_INSTANCE_ID_1);
        assertEquals(2, messages.size());
    }

    @Test
    public void testDeleteByProcessId() {
        int deletedMessages = service.removeByProcessId(PROCESS_INSTANCE_ID_1);
        assertEquals(2, deletedMessages);

        List<ProgressMessage> messages = service.findByProcessId(PROCESS_INSTANCE_ID_1);
        assertEquals(0, messages.size());

        List<ProgressMessage> allMessages = service.findAll();
        assertEquals(2, allMessages.size());
        assertSameProgressMessage(progressMessage3, allMessages.get(0));
        assertSameProgressMessage(progressMessage4, allMessages.get(1));
    }

    @Test
    public void testDeleteByProcessIdTaskIdAndTaskExecutionId() {
        int deletedMessages = service.removeByProcessIdTaskIdAndTaskExecutionId(PROCESS_INSTANCE_ID_1, TASK_ID_1, TASK_EXECUTION_ID_1);
        assertEquals(1, deletedMessages);
        assertTrue(service.findByProcessIdTaskIdAndTaskExecutionId(PROCESS_INSTANCE_ID_1, TASK_ID_1, TASK_EXECUTION_ID_1)
            .isEmpty());
    }

    @Test
    public void testFindAll() {
        List<ProgressMessage> messages = service.findAll();
        assertEquals(4, messages.size());
    }

    @Test
    public void testFindByProcessIdTaskIdAndTaskExecutionId() {
        List<ProgressMessage> messages = service.findByProcessIdTaskIdAndTaskExecutionId(PROCESS_INSTANCE_ID_1, TASK_ID_1,
            TASK_EXECUTION_ID_1);
        assertEquals(1, messages.size());
        assertSameProgressMessage(progressMessage1, messages.get(0));
    }

    @Test
    public void testFindByProcessIdTaskIdTaskExecutionIdAndType() {
        List<ProgressMessage> messagesEmpty = service.findByProcessIdTaskIdTaskExecutionIdAndType(PROCESS_INSTANCE_ID_1, TASK_ID_1,
            TASK_EXECUTION_ID_1, ProgressMessageType.INFO);
        assertTrue(messagesEmpty.isEmpty());

        List<ProgressMessage> messagesError = service.findByProcessIdTaskIdTaskExecutionIdAndType(PROCESS_INSTANCE_ID_1, TASK_ID_1,
            TASK_EXECUTION_ID_1, ProgressMessageType.ERROR);
        assertEquals(1, messagesError.size());
        assertSameProgressMessage(progressMessage1, messagesError.get(0));
    }

    @Test
    public void testRemoveOlderThan() {
        int removedProgressMessages = service.removeOlderThan(new Date(System.currentTimeMillis() + 1));
        assertEquals(4, removedProgressMessages);
    }

    private static void assertSameProgressMessage(ProgressMessage expected, ProgressMessage actual) {
        assertNotNull(actual.getId());
        assertEquals(expected.getProcessId(), actual.getProcessId());
        assertEquals(expected.getTaskId(), actual.getTaskId());
        assertEquals(expected.getTaskExecutionId(), actual.getTaskExecutionId());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getText(), actual.getText());
    }
}
