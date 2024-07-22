package org.cloudfoundry.multiapps.controller.process.listeners;

import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.OperationMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ParameterMetadata;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.dynatrace.ImmutableDynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.util.OperationFileIdsUtil;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class StartProcessListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(StartProcessListener.class);
    protected ProcessTypeParser processTypeParser;
    Supplier<ZonedDateTime> currentTimeSupplier = ZonedDateTime::now;
    private final OperationService operationService;
    private final ProcessTypeToOperationMetadataMapper operationMetadataMapper;
    private final DynatracePublisher dynatracePublisher;
    private final FileService fileService;

    @Inject
    public StartProcessListener(ProgressMessageService progressMessageService, StepLogger.Factory stepLoggerFactory,
                                ProcessLoggerProvider processLoggerProvider, ProcessLogsPersister processLogsPersister,
                                HistoricOperationEventService historicOperationEventService, FlowableFacade flowableFacade,
                                ApplicationConfiguration configuration, ProcessTypeParser processTypeParser,
                                OperationService operationService, ProcessTypeToOperationMetadataMapper operationMetadataMapper,
                                DynatracePublisher dynatracePublisher, FileService fileService) {
        super(progressMessageService,
              stepLoggerFactory,
              processLoggerProvider,
              processLogsPersister,
              historicOperationEventService,
              flowableFacade,
              configuration);
        this.processTypeParser = processTypeParser;
        this.operationService = operationService;
        this.operationMetadataMapper = operationMetadataMapper;
        this.dynatracePublisher = dynatracePublisher;
        this.fileService = fileService;
    }

    @Override
    protected void notifyInternal(DelegateExecution execution) {
        if (!isRootProcess(execution)) {
            return;
        }
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        getStepLogger().info(Messages.OPERATION_ID, correlationId);
        ProcessType processType = processTypeParser.getProcessType(execution);

        if (operationDoesNotExist(correlationId)) {
            addOperation(execution, correlationId, processType);
        }

        updateOperationFiles(execution, correlationId);
        getHistoricOperationEventService().add(ImmutableHistoricOperationEvent.of(correlationId, HistoricOperationEvent.EventType.STARTED));
        logProcessEnvironment();
        logProcessVariables(execution, processType, correlationId);
        publishDynatraceEvent(execution, processType, correlationId);
    }

    private boolean operationDoesNotExist(String correlationId) {
        return operationService.createQuery()
                               .processId(correlationId)
                               .list()
                               .isEmpty();
    }

    private void addOperation(DelegateExecution execution, String correlationId, ProcessType processType) {
        Operation operation = ImmutableOperation.builder()
                                                .mtaId(VariableHandling.get(execution, Variables.MTA_ID))
                                                .processId(correlationId)
                                                .processType(processType)
                                                .startedAt(currentTimeSupplier.get())
                                                .spaceId(VariableHandling.get(execution, Variables.SPACE_GUID))
                                                .user(StepsUtil.determineCurrentUser(execution))
                                                .hasAcquiredLock(false)
                                                .namespace(VariableHandling.get(execution, Variables.MTA_NAMESPACE))
                                                .state(Operation.State.RUNNING)
                                                .build();
        operationService.add(operation);
    }

    private void logProcessEnvironment() {
        Map<String, String> environment = configuration.getNotSensitiveVariables();
        getStepLogger().debug(Messages.PROCESS_ENVIRONMENT, JsonUtil.toJson(environment, true));
    }

    private void logProcessVariables(DelegateExecution execution, ProcessType processType, String correlationId) {
        getStepLogger().debug(Messages.CURRENT_USER, StepsUtil.determineCurrentUser(execution));
        getStepLogger().debug(Messages.CLIENT_SPACE, VariableHandling.get(execution, Variables.SPACE_NAME));
        getStepLogger().debug(Messages.CLIENT_ORGANIZATION, VariableHandling.get(execution, Variables.ORGANIZATION_NAME));
        Map<String, Object> processVariables = findProcessVariables(execution, processType);
        LoggingUtil.logWithCorrelationId(correlationId,
                                         () -> getStepLogger().infoWithoutProgressMessage(Messages.PROCESS_VARIABLES,
                                                                                          JsonUtil.toJson(processVariables, true)));
    }

    private Map<String, Object> findProcessVariables(DelegateExecution execution, ProcessType processType) {
        OperationMetadata operationMetadata = operationMetadataMapper.getOperationMetadata(processType);
        Map<String, Object> result = new HashMap<>();
        for (ParameterMetadata parameterMetadata : operationMetadata.getParameters()) {
            if (execution.hasVariable(parameterMetadata.getId())) {
                Object variableValue = execution.getVariable(parameterMetadata.getId());
                result.put(parameterMetadata.getId(), variableValue);
            }
        }
        return result;
    }

    private void updateOperationFiles(DelegateExecution execution, String correlationId) {
        List<String> operationFileIds = OperationFileIdsUtil.getOperationFileIds(execution);
        try {
            LOGGER.info(MessageFormat.format(Messages.FILES_FOR_OPERATION_0_WERE_UPDATED_1, correlationId,
                                             fileService.updateFilesOperationId(operationFileIds, correlationId)));
        } catch (FileStorageException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SLException(MessageFormat.format(Messages.FAILED_TO_UPDATE_FILES_OF_OPERATION_0, correlationId));
        }
    }

    private void publishDynatraceEvent(DelegateExecution execution, ProcessType processType, String correlationId) {
        DynatraceProcessEvent startEvent = ImmutableDynatraceProcessEvent.builder()
                                                                         .processId(correlationId)
                                                                         .mtaId(VariableHandling.get(execution, Variables.MTA_ID))
                                                                         .spaceId(VariableHandling.get(execution, Variables.SPACE_GUID))
                                                                         .eventType(DynatraceProcessEvent.EventType.STARTED)
                                                                         .processType(processType)
                                                                         .build();
        dynatracePublisher.publishProcessEvent(startEvent, getLogger());
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

}