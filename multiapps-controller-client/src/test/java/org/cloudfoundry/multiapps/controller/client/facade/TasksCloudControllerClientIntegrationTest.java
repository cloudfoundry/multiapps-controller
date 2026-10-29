package org.cloudfoundry.multiapps.controller.client.facade;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudPackage;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudTask;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableStaging;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ApplicationToCreateDto;
import org.cloudfoundry.multiapps.controller.client.facade.dto.ImmutableApplicationToCreateDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasksCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String APP_NAME = "test-task-app";
    private static final int MAX_POLL_ATTEMPTS = 60;

    @BeforeAll
    static void setUp() throws InterruptedException {
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(IntegrationTestConstants.STATICFILE_BUILDPACK)
                                          .build();

        ApplicationToCreateDto applicationDto = ImmutableApplicationToCreateDto.builder()
                                                                               .name(APP_NAME)
                                                                               .staging(staging)
                                                                               .diskQuotaInMb(IntegrationTestConstants.DISK_IN_MB)
                                                                               .memoryInMb(IntegrationTestConstants.MEMORY_IN_MB)
                                                                               .build();

        client.createApplication(applicationDto);
        CloudPackage cloudPackage = ApplicationUtil.uploadApplication(client, APP_NAME, getStaticfileApplicationContentPath());
        ApplicationUtil.stageApplication(client, APP_NAME, cloudPackage);
        ApplicationUtil.startApplication(client, APP_NAME);
    }

    @AfterAll
    static void tearDown() {
        client.deleteApplication(APP_NAME);
    }

    @Test
    @DisplayName("Run a task - then fetch it by GUID and then find it in the application's task list")
    void runTaskAndFetch() {
        CloudTask task = client.runTask(APP_NAME, ImmutableCloudTask.builder()
                                                                    .name("test-task")
                                                                    .command("echo hello")
                                                                    .build());
        assertNotNull(task);
        assertNotNull(task.getGuid());
        assertEquals("test-task", task.getName());
        assertNotNull(task.getState());

        CloudTask taskByGuid = client.getTask(task.getGuid());
        assertNotNull(taskByGuid);
        assertEquals(task.getGuid(), taskByGuid.getGuid());
        assertEquals("test-task", taskByGuid.getName());

        List<CloudTask> tasks = client.getTasks(APP_NAME);
        assertFalse(tasks.isEmpty());
        assertTrue(tasks.stream()
                        .anyMatch(currentTask -> currentTask.getGuid()
                                                            .equals(task.getGuid())),
                   MessageFormat.format("Expected the created task \"{0}\" to be in the application's task list", task.getName()));
    }

    @Test
    @DisplayName("Run a long task and cancel it")
    void runAndCancelTask() {
        CloudTask task = client.runTask(APP_NAME, ImmutableCloudTask.builder()
                                                                    .name("test-cancel-task")
                                                                    .command("sleep 300")
                                                                    .build());
        assertNotNull(task.getGuid());

        CloudTask cancelledTask = client.cancelTask(task.getGuid());
        assertNotNull(cancelledTask);
        assertEquals(task.getGuid(), cancelledTask.getGuid());

        CloudTask terminalStateTask = pollUntilInTerminalState(task.getGuid());
        assertTrue(terminalStateTask.getState() == CloudTask.State.CANCELING
                       || terminalStateTask.getState() == CloudTask.State.FAILED
                       || terminalStateTask.getState() == CloudTask.State.SUCCEEDED,
                   MessageFormat.format("Unexpected terminal task state \"{0}\"", terminalStateTask.getState()));
    }

    private CloudTask pollUntilInTerminalState(UUID taskGuid) {
        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            CloudTask task = client.getTask(taskGuid);

            if (task.getState() != CloudTask.State.PENDING && task.getState() != CloudTask.State.RUNNING) {
                return task;
            }

            MiscUtil.sleep(TimeUnit.SECONDS.toMillis(1));
        }

        throw new IllegalStateException(
            MessageFormat.format("Timeout was hit while waiting for task \"{0}\" to come into terminal state", taskGuid));
    }

}
