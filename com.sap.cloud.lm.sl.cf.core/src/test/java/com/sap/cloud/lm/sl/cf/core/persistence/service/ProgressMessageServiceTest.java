package com.sap.cloud.lm.sl.cf.core.persistence.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.persistence.query.ProgressMessageQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService.ProgressMessageMapper;
import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.common.ConflictException;

public class ProgressMessageServiceTest {

    private static final ProgressMessage PROGRESS_MESSAGE_1 = createProgressMessage(1L, "1", "taskId", ProgressMessageType.INFO, "text",
                                                                                    new Date());
    private static final ProgressMessage PROGRESS_MESSAGE_2 = createProgressMessage(2L, "2", "taskId2", ProgressMessageType.ERROR,
                                                                                    "error text", new Date());

    private final ProgressMessageService progressMessageService = createProgressMessageService();

    @AfterEach
    public void cleanUp() {
        progressMessageService.createQuery()
                              .delete();
    }

    @Test
    public void testAdd() {
        progressMessageService.add(PROGRESS_MESSAGE_1);
        assertEquals(1, progressMessageService.createQuery()
                                              .list()
                                              .size());
        assertEquals(PROGRESS_MESSAGE_1.getId(), progressMessageService.createQuery()
                                                                       .id(PROGRESS_MESSAGE_1.getId())
                                                                       .singleResult()
                                                                       .getId());

    }

    @Test
    public void testAddWithNonEmptyDatabase() {
        addProgressMessages(Arrays.asList(PROGRESS_MESSAGE_1, PROGRESS_MESSAGE_2));

        assertProgressMessageExists(PROGRESS_MESSAGE_1.getId());
        assertProgressMessageExists(PROGRESS_MESSAGE_2.getId());

        assertEquals(2, progressMessageService.createQuery()
                                              .list()
                                              .size());
    }

    @Test
    public void testAddWithAlreadyExistingMessage() {
        progressMessageService.add(PROGRESS_MESSAGE_1);
        Exception exception = assertThrows(ConflictException.class, () -> progressMessageService.add(PROGRESS_MESSAGE_1));
        String expectedExceptionMessage = MessageFormat.format(Messages.PROGRESS_MESSAGE_ALREADY_EXISTS, PROGRESS_MESSAGE_1.getProcessId(),
                                                               PROGRESS_MESSAGE_1.getId());
        assertEquals(expectedExceptionMessage, exception.getMessage());
    }

    @Test
    public void testQueryById() {
        testQueryByCriteria((query, message) -> query.id(message.getId()));
    }

    @Test
    public void testQueryByProcessId() {
        testQueryByCriteria((query, message) -> query.processId(message.getProcessId()));
    }

    @Test
    public void testQueryByTaskId() {
        testQueryByCriteria((query, message) -> query.taskId(message.getTaskId()));
    }

    @Test
    public void testQueryByType() {
        testQueryByCriteria((query, message) -> query.type(message.getType()));
    }

    @Test
    public void testQueryByTypeNot() {
        testQueryByCriteria((query, message) -> query.typeNot(message.getType()));
    }

    @Test
    public void testQueryByText() {
        testQueryByCriteria((query, message) -> query.text(message.getText()));
    }

    private void testQueryByCriteria(ProgressMessageQueryBuilder progressMessageQueryBuilder) {
        addProgressMessages(Arrays.asList(PROGRESS_MESSAGE_1, PROGRESS_MESSAGE_2));
        assertEquals(1, progressMessageQueryBuilder.build(progressMessageService.createQuery(), PROGRESS_MESSAGE_1)
                                                   .list()
                                                   .size());
        assertEquals(1, progressMessageQueryBuilder.build(progressMessageService.createQuery(), PROGRESS_MESSAGE_1)
                                                   .delete());
        assertProgressMessageExists(PROGRESS_MESSAGE_2.getId());
    }

    private interface ProgressMessageQueryBuilder {

        ProgressMessageQuery build(ProgressMessageQuery progressMessageQuery, ProgressMessage progressMessage);
    }

    private void addProgressMessages(List<ProgressMessage> messages) {
        messages.forEach(progressMessageService::add);
    }

    private static ProgressMessage createProgressMessage(long id, String processId, String taskId, ProgressMessageType type, String text,
                                                         Date timestamp) {
        return ImmutableProgressMessage.builder()
                                       .id(id)
                                       .processId(processId)
                                       .taskId(taskId)
                                       .type(type)
                                       .text(text)
                                       .timestamp(timestamp)
                                       .build();
    }

    private void assertProgressMessageExists(Long id) {
        // If does not exist, will throw NoResultException
        progressMessageService.createQuery()
                              .id(id);
    }

    private ProgressMessageService createProgressMessageService() {
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("TestDefault");
        ProgressMessageService progressMessageService = new ProgressMessageService(entityManagerFactory);
        progressMessageService.progressMessageMapper = new ProgressMessageMapper();
        return progressMessageService;
    }
}
