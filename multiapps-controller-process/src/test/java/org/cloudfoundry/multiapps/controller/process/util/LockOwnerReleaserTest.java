package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;

import org.cloudfoundry.multiapps.controller.process.flowable.commands.ClearJobLockOwnersCmd;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class LockOwnerReleaserTest {

    private static final String LOCK_OWNER = "owner-1";

    @Mock
    private ProcessEngine processEngine;
    @Mock
    private ProcessEngineConfigurationImpl processEngineConfiguration;
    @Mock
    private CommandExecutor commandExecutor;
    @Mock
    private AsyncExecutor asyncExecutor;
    @Mock
    private JobServiceConfiguration jobServiceConfiguration;
    @Mock
    private ManagementService managementService;
    @Mock
    private JobQuery jobQuery;
    @Mock
    private Job job;

    private LockOwnerReleaser lockOwnerReleaser;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(processEngine.getProcessEngineConfiguration())
               .thenReturn(processEngineConfiguration);
        Mockito.when(processEngineConfiguration.getCommandExecutor())
               .thenReturn(commandExecutor);
        Mockito.when(processEngineConfiguration.getAsyncExecutor())
               .thenReturn(asyncExecutor);
        Mockito.when(asyncExecutor.getJobServiceConfiguration())
               .thenReturn(jobServiceConfiguration);
        Mockito.when(processEngine.getManagementService())
               .thenReturn(managementService);
        Mockito.when(managementService.createJobQuery())
               .thenReturn(jobQuery);
        Mockito.when(jobQuery.lockOwner(LOCK_OWNER))
               .thenReturn(jobQuery);

        lockOwnerReleaser = new LockOwnerReleaser(processEngine);
    }

    @Test
    void testReleaseClearsProcessInstanceLockTimesAndJobLockOwnersWhenStaleJobsExist() {
        Mockito.when(jobQuery.list())
               .thenReturn(List.of(job));

        lockOwnerReleaser.release(LOCK_OWNER);

        Mockito.verify(commandExecutor)
               .execute(Mockito.any(ClearProcessInstanceLockTimesCmd.class));
        Mockito.verify(managementService)
               .executeCommand(Mockito.any(ClearJobLockOwnersCmd.class));
    }

    @Test
    void testReleaseSkipsClearJobLockOwnersWhenNoStaleJobs() {
        Mockito.when(jobQuery.list())
               .thenReturn(List.of());

        lockOwnerReleaser.release(LOCK_OWNER);

        Mockito.verify(commandExecutor)
               .execute(Mockito.any(ClearProcessInstanceLockTimesCmd.class));
        Mockito.verify(managementService, Mockito.never())
               .executeCommand(Mockito.<Command<Object>> any());
    }

}
