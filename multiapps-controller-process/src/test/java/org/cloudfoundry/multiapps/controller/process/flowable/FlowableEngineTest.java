package org.cloudfoundry.multiapps.controller.process.flowable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.common.util.JsonSerializationStrategy;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudRoute;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.core.cf.apps.ApplicationStateAction;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.cloudfoundry.multiapps.controller.process.steps.StepPhase;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionDescriptor;
import org.cloudfoundry.multiapps.mta.model.ExtensionModule;
import org.cloudfoundry.multiapps.mta.model.ExtensionResource;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.Version;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Integration test for Flowable engine with in-memory H2 database. Tests real BPMN process creation, variable setting and retrieval using
 * the multiapps-controller variable handling system.
 */
//@FlowableTest
//@ConfigurationResource("flowable-config.xml")
class FlowableEngineTest {

    private static final String TEST_PROCESS_KEY = "mtaDeploymentTest";
    private static final String TEST_BPMN_CONTENT = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:flowable="http://flowable.org/bpmn"
                     targetNamespace="http://flowable.org/test">
          <process id="mtaDeploymentTest" name="MTA Deployment Test Process" isExecutable="true">
            <startEvent id="start"/>
            <serviceTask id="processVariablesTask" name="Process Variables Task" 
                         flowable:delegateExpression="${testVariableTask}"/>
            <endEvent id="end"/>
            <sequenceFlow id="flow1" sourceRef="start" targetRef="processVariablesTask"/>
            <sequenceFlow id="flow2" sourceRef="processVariablesTask" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Mock
    private FlowableMailClient flowableMailClient;

    private ProcessEngine processEngine;
    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private String deploymentId;
    private TestVariableTask testVariableTask;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        testVariableTask = new TestVariableTask();
        DataSource dataSource = TestDataSourceProvider.getDataSource();
        ProcessEngineConfiguration configuration = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setDefaultMailClient(flowableMailClient);
        configuration.setDatabaseSchemaUpdate("create-drop");
        configuration.setBeans(Map.of("testVariableTask", testVariableTask));
        configuration.setAsyncExecutorActivate(false);
        processEngine = configuration.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();

        // Deploy test process
        deploymentId = repositoryService.createDeployment()
                                        .name("MTA Deployment Test")
                                        .addString("mta-deployment-test.bpmn20.xml", TEST_BPMN_CONTENT)
                                        .deploy()
                                        .getId();
    }

    @AfterEach
    void tearDown() {
        if (deploymentId != null) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
        if (processEngine != null) {
            processEngine.close();
        }
    }

    @Test
    void testProcessDeployment() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                                                                      .processDefinitionKey(TEST_PROCESS_KEY)
                                                                      .list();

        assertEquals(1, processDefinitions.size());
        assertEquals(TEST_PROCESS_KEY, processDefinitions.get(0)
                                                         .getKey());
        assertEquals("MTA Deployment Test Process", processDefinitions.get(0)
                                                                      .getName());
    }

    @Test
    void testProcessWithPredefinedVariables() {
        String correlationId = UUID.randomUUID()
                                   .toString();
        String spaceGuid = UUID.randomUUID()
                               .toString();
        String orgGuid = UUID.randomUUID()
                             .toString();
        String userName = "test-user@example.com";
        String userGuid = UUID.randomUUID()
                              .toString();
        String mtaId = "test-mta";
        String mtaVersion = "1.0.0";
        boolean keepFiles = true;
        Duration appsStageTimeout = Duration.ofMinutes(15);
        int modulesCount = 5;
        DeploymentDescriptor smallDeploymentDescriptor = createSmallDeploymentDescriptor(mtaId, mtaVersion);
        DeploymentDescriptor bigDeploymentDescriptor = createBigDeploymentDescriptor(mtaId, mtaVersion);
        DeployedMta deployedMta = createDeployedMta(mtaId, mtaVersion);
        List<CloudRoute> cloudRoutes = createCloudRoutes("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com");
        StepPhase stepPhase = StepPhase.DONE;
        List<ApplicationStateAction> appStateActionsToExecute = List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START);
        List<CloudServiceKey> serviceKeysToCreate = createServiceKeysToCreate();
        List<String> modulesForDeployment = List.of("module-1", "module-2", "module-3");
        List<ExtensionDescriptor> extensionDescriptors = createExtensionDescriptorsWithNullValues(mtaId);

        // Start process instance with variables using multiapps-controller variable names
        Map<String, Object> variables = new HashMap<>();
        setSerializedValueInMap(variables, Variables.CORRELATION_ID, correlationId);
        setSerializedValueInMap(variables, Variables.SPACE_GUID, spaceGuid);
        setSerializedValueInMap(variables, Variables.ORGANIZATION_GUID, orgGuid);
        setSerializedValueInMap(variables, Variables.USER, userName);
        setSerializedValueInMap(variables, Variables.USER_GUID, userGuid);
        setSerializedValueInMap(variables, Variables.MTA_ID, mtaId);
        setSerializedValueInMap(variables, Variables.KEEP_FILES, keepFiles);
        setSerializedValueInMap(variables, Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE, appsStageTimeout);
        setSerializedValueInMap(variables, Variables.MODULES_COUNT, modulesCount);
        setSerializedValueInMap(variables, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, smallDeploymentDescriptor);
        setSerializedValueInMap(variables, Variables.DEPLOYMENT_DESCRIPTOR, bigDeploymentDescriptor);
        setSerializedValueInMap(variables, Variables.DEPLOYED_MTA, deployedMta);
        setSerializedValueInMap(variables, Variables.CURRENT_ROUTES, cloudRoutes);
        setSerializedValueInMap(variables, Variables.STEP_PHASE, stepPhase);
        setSerializedValueInMap(variables, Variables.APP_STATE_ACTIONS_TO_EXECUTE, appStateActionsToExecute);
        setSerializedValueInMap(variables, Variables.CLOUD_SERVICE_KEYS_TO_CREATE, serviceKeysToCreate);
        setSerializedValueInMap(variables, Variables.MODULES_FOR_DEPLOYMENT, modulesForDeployment);
        setSerializedValueInMap(variables, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, extensionDescriptors);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY, variables);

        assertEquals(TEST_PROCESS_KEY, processInstance.getProcessDefinitionKey());

        // Verify variables are set and retrievable using multiapps variable system
        assertEquals(correlationId, getDeserializedValue(testVariableTask.capturedVariables, Variables.CORRELATION_ID));
        assertEquals(spaceGuid, getDeserializedValue(testVariableTask.capturedVariables, Variables.SPACE_GUID));
        assertEquals(orgGuid, getDeserializedValue(testVariableTask.capturedVariables, Variables.ORGANIZATION_GUID));
        assertEquals(userName, getDeserializedValue(testVariableTask.capturedVariables, Variables.USER));
        assertEquals(userGuid, getDeserializedValue(testVariableTask.capturedVariables, Variables.USER_GUID));
        assertEquals(mtaId, getDeserializedValue(testVariableTask.capturedVariables, Variables.MTA_ID));
        assertEquals(keepFiles, getDeserializedValue(testVariableTask.capturedVariables, Variables.KEEP_FILES));
        assertEquals(appsStageTimeout,
                     getDeserializedValue(testVariableTask.capturedVariables, Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE));
        assertEquals(modulesCount, getDeserializedValue(testVariableTask.capturedVariables, Variables.MODULES_COUNT));
        assertEquals(JsonUtil.toJson(smallDeploymentDescriptor), JsonUtil.toJson(
            getDeserializedValue(testVariableTask.capturedVariables, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)));
        assertEquals(JsonUtil.toJson(bigDeploymentDescriptor),
                     JsonUtil.toJson(getDeserializedValue(testVariableTask.capturedVariables, Variables.DEPLOYMENT_DESCRIPTOR)));
        assertEquals(deployedMta, getDeserializedValue(testVariableTask.capturedVariables, Variables.DEPLOYED_MTA));
        assertEquals(cloudRoutes, getDeserializedValue(testVariableTask.capturedVariables, Variables.CURRENT_ROUTES));
        assertEquals(stepPhase, getDeserializedValue(testVariableTask.capturedVariables, Variables.STEP_PHASE));
        assertEquals(appStateActionsToExecute,
                     getDeserializedValue(testVariableTask.capturedVariables, Variables.APP_STATE_ACTIONS_TO_EXECUTE));
        assertEquals(serviceKeysToCreate, getDeserializedValue(testVariableTask.capturedVariables, Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(modulesForDeployment, getDeserializedValue(testVariableTask.capturedVariables, Variables.MODULES_FOR_DEPLOYMENT));
        assertEquals(JsonUtil.toJson(extensionDescriptors, JsonSerializationStrategy.ALLOW_NULLS),
                     JsonUtil.toJson(getDeserializedValue(testVariableTask.capturedVariables, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN),
                                     JsonSerializationStrategy.ALLOW_NULLS));
    }

    @Test
    void testVariableUpdateAndRetrieval() {
        String correlationId = UUID.randomUUID()
                                   .toString();
        Boolean deleteServices = true;
        String mtaId = "test-mta-2";
        String mtaVersion = "2.0.0";
        DeploymentDescriptor smallDeploymentDescriptor = createSmallDeploymentDescriptor(mtaId, mtaVersion);
        DeploymentDescriptor bigDeploymentDescriptor = createBigDeploymentDescriptor(mtaId, mtaVersion);
        DeploymentDescriptor modifiedBigDeploymentDescriptor = DeploymentDescriptor.copyOf(bigDeploymentDescriptor)
                                                                                   .setParameters(Map.of("new-param", "new-value"));
        DeployedMta oldDeployedMta = createDeployedMta(mtaId, "1.0.0");
        DeployedMta newDeployedMta = createDeployedMta(mtaId, mtaVersion);
        List<CloudRoute> idleRoutes = createCloudRoutes("route-1-idle.example.com/bar-foo", "route-2-idle.example.com",
                                                        "route-3-idle.example.com");
        List<CloudRoute> liveRoutes = createCloudRoutes("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com");
        StepPhase pollingPhase = StepPhase.POLL;
        StepPhase donePhase = StepPhase.DONE;
        List<ApplicationStateAction> idleAppStateActionsToExecute = List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START);
        List<ApplicationStateAction> liveAppStateActionsToExecute = List.of(ApplicationStateAction.EXECUTE, ApplicationStateAction.STOP);
        List<CloudServiceKey> serviceKeysToCreate = createServiceKeysToCreate();
        List<String> modulesForDeployment = List.of("module-1", "module-2", "module-3");
        List<ExtensionDescriptor> extensionDescriptors = createExtensionDescriptorsWithNullValues(mtaId);

        Map<String, Object> initialVariables = new HashMap<>();
        setSerializedValueInMap(initialVariables, Variables.CORRELATION_ID, correlationId);
        setSerializedValueInMap(initialVariables, Variables.DELETE_SERVICES, deleteServices);
        setSerializedValueInMap(initialVariables, Variables.MTA_ID, mtaId);
        setSerializedValueInMap(initialVariables, Variables.DEPLOYMENT_DESCRIPTOR, bigDeploymentDescriptor);
        setSerializedValueInMap(initialVariables, Variables.DEPLOYED_MTA, oldDeployedMta);
        setSerializedValueInMap(initialVariables, Variables.CURRENT_ROUTES, idleRoutes);
        setSerializedValueInMap(initialVariables, Variables.STEP_PHASE, pollingPhase);
        setSerializedValueInMap(initialVariables, Variables.APP_STATE_ACTIONS_TO_EXECUTE, idleAppStateActionsToExecute);
        setSerializedValueInMap(initialVariables, Variables.CLOUD_SERVICE_KEYS_TO_CREATE, serviceKeysToCreate);
        setSerializedValueInMap(initialVariables, Variables.MODULES_FOR_DEPLOYMENT, modulesForDeployment);
        setSerializedValueInMap(initialVariables, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, extensionDescriptors);

        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CORRELATION_ID, correlationId);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DELETE_SERVICES, deleteServices);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MTA_ID, mtaId);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS,
                                smallDeploymentDescriptor);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYMENT_DESCRIPTOR, modifiedBigDeploymentDescriptor);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYED_MTA, newDeployedMta);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CURRENT_ROUTES, liveRoutes);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.STEP_PHASE, donePhase);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.APP_STATE_ACTIONS_TO_EXECUTE,
                                liveAppStateActionsToExecute);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CLOUD_SERVICE_KEYS_TO_CREATE, serviceKeysToCreate);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MODULES_FOR_DEPLOYMENT, modulesForDeployment);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, extensionDescriptors);

        runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY, initialVariables);

        String retrievedCorrelationId = getDeserializedValue(testVariableTask.capturedVariables, Variables.CORRELATION_ID);
        Boolean retrievedDeleteServices = getDeserializedValue(testVariableTask.capturedVariables, Variables.DELETE_SERVICES);
        String retrievedMtaId = getDeserializedValue(testVariableTask.capturedVariables, Variables.MTA_ID);
        DeploymentDescriptor retrievedSmallDeploymentDescriptor = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                       Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        DeploymentDescriptor retrievedBigDeploymentDescriptor = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                     Variables.DEPLOYMENT_DESCRIPTOR);
        DeployedMta retrievedDeployedMta = getDeserializedValue(testVariableTask.capturedVariables, Variables.DEPLOYED_MTA);
        List<CloudRoute> retrievedCloudRoutes = getDeserializedValue(testVariableTask.capturedVariables, Variables.CURRENT_ROUTES);
        StepPhase retrievedStepPhase = getDeserializedValue(testVariableTask.capturedVariables, Variables.STEP_PHASE);
        List<ApplicationStateAction> retrievedAppStateActionsToExecute = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                              Variables.APP_STATE_ACTIONS_TO_EXECUTE);
        List<CloudServiceKey> retrievedServiceKeysToCreate = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                  Variables.CLOUD_SERVICE_KEYS_TO_CREATE);
        List<String> retrievedModulesForDeployment = getDeserializedValue(testVariableTask.capturedVariables,
                                                                          Variables.MODULES_FOR_DEPLOYMENT);
        List<ExtensionDescriptor> retrievedExtensionDescriptors = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                       Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);

        assertEquals(correlationId, retrievedCorrelationId);
        assertEquals(deleteServices, retrievedDeleteServices);
        assertEquals(mtaId, retrievedMtaId);
        assertEquals(JsonUtil.toJson(smallDeploymentDescriptor), JsonUtil.toJson(retrievedSmallDeploymentDescriptor));
        // This assertion intentionally checks that the modified deployment descriptor is different from the initially set one because of flowable caching regression
        // https://github.com/flowable/flowable-engine/issues/4130
        // The original behaviour must be to have same deployment descriptor as it was set in the task. Modify the test to assert for matching values when the issue is fixed.
        assertNotEquals(JsonUtil.toJson(modifiedBigDeploymentDescriptor), JsonUtil.toJson(retrievedBigDeploymentDescriptor));
        assertEquals(newDeployedMta, retrievedDeployedMta);
        assertEquals(liveRoutes, retrievedCloudRoutes);
        assertEquals(donePhase, retrievedStepPhase);
        assertEquals(liveAppStateActionsToExecute, retrievedAppStateActionsToExecute);
        assertEquals(serviceKeysToCreate, retrievedServiceKeysToCreate);
        assertEquals(modulesForDeployment, retrievedModulesForDeployment);
        assertEquals(JsonUtil.toJson(extensionDescriptors, JsonSerializationStrategy.ALLOW_NULLS),
                     JsonUtil.toJson(retrievedExtensionDescriptors, JsonSerializationStrategy.ALLOW_NULLS));
    }

    @Test
    void testSetVariablesInsideStep() {
        String correlationId = UUID.randomUUID()
                                   .toString();
        Boolean deleteServices = true;
        String mtaId = "test-mta-2";
        String mtaVersion = "2.0.0";
        DeploymentDescriptor smallDeploymentDescriptor = createSmallDeploymentDescriptor(mtaId, mtaVersion);
        DeploymentDescriptor bigDeploymentDescriptor = createBigDeploymentDescriptor(mtaId, mtaVersion);
        DeployedMta deployedMta = createDeployedMta(mtaId, mtaVersion);
        List<CloudRoute> cloudRoutes = createCloudRoutes("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com");
        StepPhase stepPhase = StepPhase.DONE;
        List<ApplicationStateAction> appStateActionsToExecute = List.of(ApplicationStateAction.STAGE, ApplicationStateAction.START);
        List<CloudServiceKey> serviceKeysToCreate = createServiceKeysToCreate();
        List<String> modulesForDeployment = List.of("module-1", "module-2", "module-3");
        List<ExtensionDescriptor> extensionDescriptors = createExtensionDescriptorsWithNullValues(mtaId);

        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CORRELATION_ID, correlationId);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DELETE_SERVICES, deleteServices);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MTA_ID, mtaId);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS,
                                smallDeploymentDescriptor);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYMENT_DESCRIPTOR, bigDeploymentDescriptor);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.DEPLOYED_MTA, deployedMta);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CURRENT_ROUTES, cloudRoutes);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.STEP_PHASE, stepPhase);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.APP_STATE_ACTIONS_TO_EXECUTE, appStateActionsToExecute);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.CLOUD_SERVICE_KEYS_TO_CREATE, serviceKeysToCreate);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MODULES_FOR_DEPLOYMENT, modulesForDeployment);
        setSerializedValueInMap(testVariableTask.variablesToSetInContext, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, extensionDescriptors);

        runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY);

        String retrievedCorrelationId = getDeserializedValue(testVariableTask.capturedVariables, Variables.CORRELATION_ID);
        Boolean retrievedDeleteServices = getDeserializedValue(testVariableTask.capturedVariables, Variables.DELETE_SERVICES);
        String retrievedMtaId = getDeserializedValue(testVariableTask.capturedVariables, Variables.MTA_ID);
        DeploymentDescriptor retrievedSmallDeploymentDescriptor = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                       Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS);
        DeploymentDescriptor retrievedBigDeploymentDescriptor = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                     Variables.DEPLOYMENT_DESCRIPTOR);
        DeployedMta retrievedDeployedMta = getDeserializedValue(testVariableTask.capturedVariables, Variables.DEPLOYED_MTA);
        List<CloudRoute> retrievedCloudRoutes = getDeserializedValue(testVariableTask.capturedVariables, Variables.CURRENT_ROUTES);
        StepPhase retrievedStepPhase = getDeserializedValue(testVariableTask.capturedVariables, Variables.STEP_PHASE);
        List<ApplicationStateAction> retrievedAppStateActionsToExecute = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                              Variables.APP_STATE_ACTIONS_TO_EXECUTE);
        List<CloudServiceKey> retrievedServiceKeysToCreate = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                  Variables.CLOUD_SERVICE_KEYS_TO_CREATE);
        List<String> retrievedModulesForDeployment = getDeserializedValue(testVariableTask.capturedVariables,
                                                                          Variables.MODULES_FOR_DEPLOYMENT);
        List<ExtensionDescriptor> retrievedExtensionDescriptors = getDeserializedValue(testVariableTask.capturedVariables,
                                                                                       Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN);

        assertEquals(correlationId, retrievedCorrelationId);
        assertEquals(deleteServices, retrievedDeleteServices);
        assertEquals(mtaId, retrievedMtaId);
        assertEquals(JsonUtil.toJson(smallDeploymentDescriptor), JsonUtil.toJson(retrievedSmallDeploymentDescriptor));
        assertEquals(JsonUtil.toJson(bigDeploymentDescriptor), JsonUtil.toJson(retrievedBigDeploymentDescriptor));
        assertEquals(deployedMta, retrievedDeployedMta);
        assertEquals(cloudRoutes, retrievedCloudRoutes);
        assertEquals(stepPhase, retrievedStepPhase);
        assertEquals(appStateActionsToExecute, retrievedAppStateActionsToExecute);
        assertEquals(serviceKeysToCreate, retrievedServiceKeysToCreate);
        assertEquals(modulesForDeployment, retrievedModulesForDeployment);
        assertEquals(JsonUtil.toJson(extensionDescriptors, JsonSerializationStrategy.ALLOW_NULLS),
                     JsonUtil.toJson(retrievedExtensionDescriptors, JsonSerializationStrategy.ALLOW_NULLS));
    }

    private <T> void setSerializedValueInMap(Map<String, Object> variables, Variable<T> variable, T value) {
        if (value == null) {
            variables.put(variable.getName(), null);
            return;
        }
        Serializer<T> serializer = variable.getSerializer();
        variables.put(variable.getName(), serializer.serialize(value));
    }

    private <T> T getDeserializedValue(Map<String, Object> variables, Variable<T> variable) {
        Object serializedValue = variables.get(variable.getName());
        if (serializedValue == null) {
            return variable.getDefaultValue();
        }
        Serializer<T> serializer = variable.getSerializer();
        return serializer.deserialize(serializedValue);
    }

    private DeploymentDescriptor createSmallDeploymentDescriptor(String mtaId, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(mtaId)
                                   .setVersion(mtaVersion)
                                   .setModules(List.of(Module.createV3()
                                                             .setName("module-1")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-1-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db")))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("db")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-db-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free"))));
    }

    private DeploymentDescriptor createBigDeploymentDescriptor(String mtaId, String mtaVersion) {
        return DeploymentDescriptor.createV3()
                                   .setId(mtaId)
                                   .setVersion(mtaVersion)
                                   .setParameters(Map.of(SupportedParameters.ENABLE_PARALLEL_DEPLOYMENTS, true))
                                   .setModules(List.of(Module.createV3()
                                                             .setName("module-1")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-1-route.example.com"),
                                                                        SupportedParameters.TASKS,
                                                                        List.of(Map.of("name", "task-1", "command", "migrate-db.sh"))))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs"))),
                                                       Module.createV3()
                                                             .setName("module-2")
                                                             .setType("java")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "1G", SupportedParameters.DISK_QUOTA,
                                                                        "4096M", SupportedParameters.INSTANCES, 2,
                                                                        SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-2-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("cache"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("autoscaler"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs"))),
                                                       Module.createV3()
                                                             .setName("module-3")
                                                             .setType("javascript")
                                                             .setParameters(
                                                                 Map.of(SupportedParameters.MEMORY, "512M", SupportedParameters.DISK_QUOTA,
                                                                        "256M", SupportedParameters.ROUTES,
                                                                        List.of(SupportedParameters.ROUTE, "module-3-route.example.com")))
                                                             .setRequiredDependencies(List.of(RequiredDependency.createV3()
                                                                                                                .setName("db"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("cache"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName("autoscaler"),
                                                                                              RequiredDependency.createV3()
                                                                                                                .setName(
                                                                                                                    "application-logs")))
                                                             .setProvidedDependencies(List.of(ProvidedDependency.createV3()
                                                                                                                .setName("my-api")
                                                                                                                .setProperties(Map.of("url",
                                                                                                                                      "https://api.example.com"))))))
                                   .setResources(List.of(Resource.createV3()
                                                                 .setName("db")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-db-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free")),
                                                         Resource.createV3()
                                                                 .setName("cache")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "test-cache-service",
                                                                                       SupportedParameters.SERVICE_PLAN, "free")),
                                                         Resource.createV3()
                                                                 .setName("autoscaler")
                                                                 .setType("org.cloudfoundry.managed-service")
                                                                 .setParameters(Map.of(SupportedParameters.SERVICE, "app-autoscaler",
                                                                                       SupportedParameters.SERVICE_PLAN, "default")),
                                                         Resource.createV3()
                                                                 .setName("application-logs")
                                                                 .setType("org.cloudfoundry.user-provided-service")
                                                                 .setParameters(Map.of(SupportedParameters.SYSLOG_DRAIN_URL,
                                                                                       "syslog://logs.example.com:514"))));
    }

    private DeployedMta createDeployedMta(String mtaId, String mtaVersion) {
        List<DeployedMtaApplication> deployedMtaApplications = List.of("app-1", "app-2", "app-3")
                                                                   .stream()
                                                                   .map(appName -> ImmutableDeployedMtaApplication.builder()
                                                                                                                  .name(appName)
                                                                                                                  .moduleName(appName)
                                                                                                                  .build())
                                                                   .collect(Collectors.toList());
        List<DeployedMtaService> deployedMtaServices = List.of("service-1", "service-2", "service-3")
                                                           .stream()
                                                           .map(serviceName -> ImmutableDeployedMtaService.builder()
                                                                                                          .name(serviceName)
                                                                                                          .build())
                                                           .collect(Collectors.toList());
        return ImmutableDeployedMta.builder()
                                   .metadata(org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata.builder()
                                                                                                                        .id(mtaId)
                                                                                                                        .version(
                                                                                                                            Version.parseVersion(
                                                                                                                                mtaVersion))
                                                                                                                        .build())
                                   .applications(deployedMtaApplications)
                                   .services(deployedMtaServices)
                                   .build();
    }

    private List<CloudRoute> createCloudRoutes(String... routes) {
        //        return List.of("route-1.example.com/foo-bar", "route-2.example.com", "route-3.example.com")
        return Stream.of(routes)
                     .map(route -> new ApplicationURI(route, false, null).toCloudRoute())
                     .collect(Collectors.toList());
    }

    private List<CloudServiceKey> createServiceKeysToCreate() {
        return List.of(ImmutableCloudServiceKey.builder()
                                               .name("service-key-1")
                                               .build(), ImmutableCloudServiceKey.builder()
                                                                                 .name("service-key-2")
                                                                                 .build());
    }

    private List<ExtensionDescriptor> createExtensionDescriptorsWithNullValues(String mtaId) {
        Map<String, Object> moduleParams = new HashMap<>();
        moduleParams.put(SupportedParameters.MEMORY, null);
        moduleParams.put(SupportedParameters.DISK_QUOTA, "256M");
        moduleParams.put(SupportedParameters.ROUTES, List.of(SupportedParameters.ROUTE, "module-1-route.example.com"));

        Map<String, Object> resourceParams = new HashMap<>();
        resourceParams.put("test-parameter", null);
        return List.of(ExtensionDescriptor.createV3()
                                          .setId("test-extension")
                                          .setParentId(mtaId)
                                          .setModules(List.of(ExtensionModule.createV3()
                                                                             .setName("module-1")
                                                                             .setParameters(moduleParams)))
                                          .setResources(List.of(ExtensionResource.createV3()
                                                                                 .setName("db")
                                                                                 .setParameters(resourceParams))));
    }

    private class TestVariableTask implements JavaDelegate {

        Map<String, Object> variablesToSetInContext = new HashMap<>();
        Map<String, Object> capturedVariables = new HashMap<>();

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariables(variablesToSetInContext);
            capturedVariables = execution.getVariables();
        }
    }

}
