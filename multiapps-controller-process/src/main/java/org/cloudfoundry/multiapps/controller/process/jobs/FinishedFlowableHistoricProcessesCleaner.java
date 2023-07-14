package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Named
public class FinishedFlowableHistoricProcessesCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinishedFlowableHistoricProcessesCleaner.class);
    private static final Marker LOG_MARKER = MarkerFactory.getMarker("finished-flowable-historic-processes-cleaner");
    private static final int SELECTED_INSTANCE_FOR_CLEAN_UP = 1;
    private static final int MAX_OPERATIONS_PER_PAGE = 100;

    private final ApplicationConfiguration applicationConfiguration;
    private final OperationService operationService;
    private final HistoryService historyService;

    @Inject
    public FinishedFlowableHistoricProcessesCleaner(ApplicationConfiguration applicationConfiguration, OperationService operationService,
                                                    HistoryService historyService) {
        this.applicationConfiguration = applicationConfiguration;
        this.operationService = operationService;
        this.historyService = historyService;
    }

    @Scheduled(fixedRateString = "#{@applicationConfiguration.getExecutionTimeForFinishedProcesses()}")
    public void cleanUp() throws ExecutionException, InterruptedException {
        if (applicationConfiguration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEAN_UP) {
            return;
        }
        LOGGER.info(LOG_MARKER, Messages.CLEAN_UP_JOB_FOR_FINISHED_OPERATIONS_HAS_STARTED);
        deleteFinishedProcesses();
        LOGGER.info(LOG_MARKER, Messages.CLEAN_UP_JOB_FOR_FINISHED_OPERATIONS_HAS_FINISHED);
    }

    private void deleteFinishedProcesses() throws InterruptedException, ExecutionException {
        LocalDateTime endedBefore = LocalDateTime.now()
                                                 .minusHours(2)
                                                 .minusMinutes(30);
        LocalDateTime endedAfter = LocalDateTime.now()
                                                .minusHours(5);
        int page = 0;
        int deletedProcesses = 0;
        List<Operation> operations = getFinishedOperationsByPage(page, endedBefore, endedAfter);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        while (!operations.isEmpty()) {
            int batchOfDeletedProcesses = 0;
            List<Callable<Boolean>> historicProcessesDeletionTasks = getHistoricProcessInstances(operations).stream()
                                                                                                            .map(historicProcessInstance -> (Callable<Boolean>) () -> deleteHistoricProcess(historicProcessInstance))
                                                                                                            .collect(Collectors.toList());
            List<Future<Boolean>> executedDeletionTasks = executorService.invokeAll(historicProcessesDeletionTasks);
            for (Future<Boolean> executedDeletionTask : executedDeletionTasks) {
                if (executedDeletionTask.get()) {
                    deletedProcesses++;
                    batchOfDeletedProcesses++;
                }
            }
            LOGGER.info(MessageFormat.format(Messages.BATCH_OF_PROCESSES_DELETED, batchOfDeletedProcesses));
            operations = getFinishedOperationsByPage(++page, endedBefore, endedAfter);
        }
        executorService.shutdown();
        LOGGER.info(LOG_MARKER, format(Messages.DELETED_HISTORIC_PROCESSES_0, deletedProcesses));
    }

    private List<Operation> getFinishedOperationsByPage(int page, LocalDateTime endedBefore, LocalDateTime endedAfter) {
        return operationService.createQuery()
                               .limitOnSelect(MAX_OPERATIONS_PER_PAGE)
                               .offsetOnSelect(page * MAX_OPERATIONS_PER_PAGE)
                               .endedBefore(endedBefore)
                               .endedAfter(endedAfter)
                               .state(Operation.State.FINISHED)
                               .list();
    }

    private List<HistoricProcessInstance> getHistoricProcessInstances(List<Operation> operations) {
        return historyService.createHistoricProcessInstanceQuery()
                             .finished()
                             .processInstanceIds(getOperationProcessIds(operations))
                             .list();
    }

    private Set<String> getOperationProcessIds(List<Operation> operations) {
        return operations.stream()
                         .map(Operation::getProcessId)
                         .collect(Collectors.toSet());
    }

    private boolean deleteHistoricProcess(HistoricProcessInstance historicProcessInstance) {
        try {
            LOGGER.debug(MessageFormat.format(Messages.DELETING_PROCESS_WITH_ID_0, historicProcessInstance.getId()));
            historyService.deleteHistoricProcessInstance(historicProcessInstance.getId());
            LOGGER.debug(MessageFormat.format(Messages.PROCESS_WAS_DELETED_0, historicProcessInstance.getId()));
            return true;
        } catch (Exception e) {
            LOGGER.warn(LOG_MARKER, format(Messages.COULD_NOT_DELETE_HISTORIC_PROCESS_0, historicProcessInstance), e);
            return false;
        }
    }

}
