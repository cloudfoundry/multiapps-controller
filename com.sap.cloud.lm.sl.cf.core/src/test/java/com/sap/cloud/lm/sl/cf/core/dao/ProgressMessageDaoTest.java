package com.sap.cloud.lm.sl.cf.core.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.joda.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.provider.Arguments;

import com.sap.cloud.lm.sl.cf.persistence.model.ImmutableProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.common.ConflictException;

public class ProgressMessageDaoTest {

    private ProgressMessageDao dao = createDao();
    private static final EntityManagerFactory EMF = Persistence.createEntityManagerFactory("TestDefault");
    private static final ProgressMessage PROGRESS_MESSAGE_1 = getProgressMessage(1l, "1", "taskId", ProgressMessageType.INFO, "Text");
    private static final ProgressMessage PROGRESS_MESSAGE_1_1 = getProgressMessage(2, "1", "taskId1", ProgressMessageType.INFO, "Text1");
    private static final ProgressMessage PROGRESS_MESSAGE_1_2 = getProgressMessage(3, "1", "taskId2", ProgressMessageType.INFO, "Text2");

    static Stream<Arguments> arguments = Stream.of(Arguments.of());

    @AfterEach
    public void cleanUp() {
        for (ProgressMessage progressMessage : dao.findAll()) {
            dao.remove(progressMessage.getId());
        }
    }

    @Test
    public void testAddNonExisting() {
        dao.add(PROGRESS_MESSAGE_1);
        assertNotNull(dao.find(PROGRESS_MESSAGE_1.getId()));
    }

    @Test
    public void testAddAlreadyExisting() {
        dao.add(PROGRESS_MESSAGE_1);
        assertException(() -> dao.add(PROGRESS_MESSAGE_1), ConflictException.class,
            "Progress message for process \"1\" with ID \"1\" already exist");
    }

    public <T extends Throwable> void assertException(Executable executable, Class<T> expectedExceptionType, String message) {
        T exception = assertThrows(expectedExceptionType, executable);
        assertEquals(message, exception.getMessage());
    }

    @Test
    public void testRemove() {
        dao.add(PROGRESS_MESSAGE_1);
        assertNotNull(dao.find(PROGRESS_MESSAGE_1.getId()));
        dao.remove(PROGRESS_MESSAGE_1.getId());
        assertNull(dao.find(PROGRESS_MESSAGE_1.getId()));
    }

    @Test
    public void testUpdate() {
        dao.add(PROGRESS_MESSAGE_1);
        dao.update(PROGRESS_MESSAGE_1.getId(), PROGRESS_MESSAGE_1_2);
        ProgressMessage foundProgressMessage = dao.find(PROGRESS_MESSAGE_1.getId());
        assertEquals(PROGRESS_MESSAGE_1_2.getTaskId(), foundProgressMessage.getTaskId());
        assertEquals(PROGRESS_MESSAGE_1_2.getText(), foundProgressMessage.getText());
    }

    @Test
    public void testFindAll() {
        List<ProgressMessage> progressMessages = Arrays.asList(PROGRESS_MESSAGE_1, PROGRESS_MESSAGE_1_1);
        addAll(progressMessages);
        List<ProgressMessage> foundProgressMessages = dao.find(PROGRESS_MESSAGE_1.getProcessId());
        assertEquals(progressMessages.toString(), foundProgressMessages.toString());
    }

    private void addAll(List<ProgressMessage> messages) {
        for (ProgressMessage progressMessage : messages) {
            dao.add(progressMessage);
        }
    }

    @Test
    public void testRemoveByProcessId() {
        List<ProgressMessage> progressMessages = Arrays.asList(PROGRESS_MESSAGE_1, PROGRESS_MESSAGE_1_1);
        addAll(progressMessages);
        dao.removeBy(PROGRESS_MESSAGE_1.getProcessId());
        assertTrue(dao.find(PROGRESS_MESSAGE_1.getProcessId())
            .isEmpty());
    }

    @Test
    public void testRemoveOlderThan() throws InterruptedException {
        List<ProgressMessage> progressMessages = Arrays.asList(PROGRESS_MESSAGE_1, PROGRESS_MESSAGE_1_1);
        addAll(progressMessages);
        dao.removeOlderThan(new LocalDateTime().toDate());
        assertTrue(dao.findAll()
            .isEmpty());
    }

    @Test
    public void testRemoveByProcessIdTaskIdAndType() {
        dao.add(PROGRESS_MESSAGE_1);
        dao.removeBy(PROGRESS_MESSAGE_1.getProcessId(), PROGRESS_MESSAGE_1.getTaskId(), PROGRESS_MESSAGE_1.getType());
        assertTrue(dao.findAll()
            .isEmpty());
    }

    private static ProgressMessageDao createDao() {
        ProgressMessageDao dao = new ProgressMessageDao();
        dao.progressMessageDtoDao = new ProgressMessageDtoDao(EMF);
        return dao;
    }

    private static ProgressMessage getProgressMessage(long id, String processId, String taskId, ProgressMessageType type, String text) {
        return ImmutableProgressMessage.builder()
            .id(id)
            .processId(processId)
            .taskId(taskId)
            .type(type)
            .text(text)
            .build();
    }
}
