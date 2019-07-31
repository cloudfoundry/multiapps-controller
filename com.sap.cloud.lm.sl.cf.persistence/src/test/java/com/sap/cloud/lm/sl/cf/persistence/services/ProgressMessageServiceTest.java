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
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.common.util.TestDataSourceProvider;

public class ProgressMessageServiceTest {

    private static final String LIQUIBASE_CHANGELOG_LOCATION = "com/sap/cloud/lm/sl/cf/persistence/db/changelog/db-changelog.xml";
    private static final String TASK_ID_1 = "create-app";
    private static final String TASK_ID_2 = "update-app";
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
        progressMessage1 = ImmutableProgressMessage.builder()
                                                   .processId(PROCESS_INSTANCE_ID_1)
                                                   .taskId(TASK_ID_1)
                                                   .type(ProgressMessageType.ERROR)
                                                   .text(MESSAGE_TEXT_1)
                                                   .build();
        progressMessage2 = ImmutableProgressMessage.builder()
                                                   .processId(PROCESS_INSTANCE_ID_1)
                                                   .taskId(TASK_ID_2)
                                                   .type(ProgressMessageType.INFO)
                                                   .text(MESSAGE_TEXT_2)
                                                   .build();
        progressMessage3 = ImmutableProgressMessage.builder()
                                                   .processId(PROCESS_INSTANCE_ID_2)
                                                   .taskId(TASK_ID_1)
                                                   .type(ProgressMessageType.INFO)
                                                   .text(MESSAGE_TEXT_1)
                                                   .build();
        progressMessage4 = ImmutableProgressMessage.builder()
                                                   .processId(PROCESS_INSTANCE_ID_2)
                                                   .taskId(TASK_ID_2)
                                                   .type(ProgressMessageType.INFO)
                                                   .text(MESSAGE_TEXT_2)
                                                   .build();

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
    public void testUpdate() {
        final String PROCESS_ID = "test-update-processId";
        final String TASK_ID = "test-update-taskId";
        ProgressMessage progressMessage = ImmutableProgressMessage.builder()
                                                                  .processId(PROCESS_ID)
                                                                  .taskId(TASK_ID)
                                                                  .type(ProgressMessageType.INFO)
                                                                  .text("test-update-info-message")
                                                                  .timestamp(new Timestamp(System.currentTimeMillis()))
                                                                  .build();
        boolean insertSuccess = service.add(progressMessage);
        assertTrue(insertSuccess);

        List<ProgressMessage> allMessagesForProcessInstance1 = service.findByProcessId(PROCESS_INSTANCE_ID_1);
        assertEquals(2, allMessagesForProcessInstance1.size());

        List<ProgressMessage> allMessagesForProcessInstance2 = service.findByProcessId(PROCESS_INSTANCE_ID_2);
        assertEquals(2, allMessagesForProcessInstance2.size());

        List<ProgressMessage> messagesByProcessId = service.findByProcessId(PROCESS_ID);
        assertEquals(1, messagesByProcessId.size());
        ProgressMessage messageToUpdate = messagesByProcessId.get(0);
        ProgressMessage updatedProgressMessage = ImmutableProgressMessage.builder()
                                                                         .processId(PROCESS_ID)
                                                                         .taskId(TASK_ID)
                                                                         .type(ProgressMessageType.INFO)
                                                                         .text("test-update-new-info-message")
                                                                         .build();
        boolean updateSuccess = service.update(messageToUpdate.getId(), updatedProgressMessage);
        assertTrue(updateSuccess);

        List<ProgressMessage> updatedProgressMessages = service.findByProcessId(PROCESS_ID);
        assertEquals(1, updatedProgressMessages.size());

        ProgressMessage updateProgressMessageFromDatabase = updatedProgressMessages.get(0);

        assertSameProgressMessage(updatedProgressMessage, updateProgressMessageFromDatabase);
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
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getText(), actual.getText());
    }
}
