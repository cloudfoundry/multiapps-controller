package org.cloudfoundry.multiapps.controller.process.flowable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.common.util.JsonSerializationStrategy;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.persistence.test.TestDataSourceProvider;
import org.cloudfoundry.multiapps.controller.process.variables.Serializer;
import org.cloudfoundry.multiapps.controller.process.variables.Variable;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.repository.ProcessDefinition;
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
class FlowableEngineTest {

    private static final String TEST_PROCESS_KEY = "mtaDeploymentTest";
    private static final String MTA_DEPLOYMENT_TEST_PROCESS = "MTA Deployment Test Process";

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
        processEngine = configuration.buildProcessEngine();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        deploymentId = repositoryService.createDeployment()
                                        .name("MTA Deployment Test")
                                        .addClasspathResource(
                                            "org/cloudfoundry/multiapps/controller/process/flowable/test-bpmn-diagram.bpmn")
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
        assertEquals(MTA_DEPLOYMENT_TEST_PROCESS, processDefinitions.get(0)
                                                                    .getName());
    }

    @Test
    void testProcessWithPredefinedVariables() {
        FlowableProcessTestData data = FlowableProcessTestDataUtils.predefinedScenario();
        runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY, buildVariablesMap(data));
        assertCapturedVariables(data, true);
    }

    @Test
    void testVariableUpdateAndRetrieval() {
        FlowableProcessTestDataUtils.UpdateScenario updateScenario = FlowableProcessTestDataUtils.updateScenario();
        testVariableTask.variablesToSetInContext = buildVariablesMap(updateScenario.updated());
        runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY, buildVariablesMap(updateScenario.initial()));
        // This assertion intentionally checks that the modified deployment descriptor is different from the initially set one because of flowable caching regression
        // https://github.com/flowable/flowable-engine/issues/4130
        // The original behaviour must be to have same deployment descriptor as it was set in the task. Modify the test to assert for matching values when the issue is fixed.
        assertCapturedVariables(updateScenario.updated(), false);
    }

    @Test
    void testSetVariablesInsideStep() {
        FlowableProcessTestData data = FlowableProcessTestDataUtils.insideStepScenario();
        testVariableTask.variablesToSetInContext = buildVariablesMap(data);
        runtimeService.startProcessInstanceByKey(TEST_PROCESS_KEY);
        assertCapturedVariables(data, true);
    }

    private Map<String, Object> buildVariablesMap(FlowableProcessTestData flowableProcessTestData) {
        Map<String, Object> vars = new HashMap<>();
        setSerializedValueInMap(vars, Variables.CORRELATION_ID, flowableProcessTestData.getCorrelationId());
        setSerializedValueInMap(vars, Variables.SPACE_GUID, flowableProcessTestData.getSpaceGuid());
        setSerializedValueInMap(vars, Variables.ORGANIZATION_GUID, flowableProcessTestData.getOrgGuid());
        setSerializedValueInMap(vars, Variables.USER, flowableProcessTestData.getUsername());
        setSerializedValueInMap(vars, Variables.USER_GUID, flowableProcessTestData.getUserGuid());
        setSerializedValueInMap(vars, Variables.MTA_ID, flowableProcessTestData.getMtaId());
        setSerializedValueInMap(vars, Variables.KEEP_FILES, flowableProcessTestData.keepFiles());
        setSerializedValueInMap(vars, Variables.DELETE_SERVICES, flowableProcessTestData.deleteServices());
        setSerializedValueInMap(vars, Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE, flowableProcessTestData.getAppsStageTimeout());
        setSerializedValueInMap(vars, Variables.MODULES_COUNT, flowableProcessTestData.getModulesCount());
        setSerializedValueInMap(vars, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS, flowableProcessTestData.getSmallDescriptor());
        setSerializedValueInMap(vars, Variables.DEPLOYMENT_DESCRIPTOR, flowableProcessTestData.getBigDescriptor());
        setSerializedValueInMap(vars, Variables.DEPLOYED_MTA, flowableProcessTestData.getDeployedMta());
        setSerializedValueInMap(vars, Variables.CURRENT_ROUTES, flowableProcessTestData.getRoutes());
        setSerializedValueInMap(vars, Variables.STEP_PHASE, flowableProcessTestData.getStepPhase());
        setSerializedValueInMap(vars, Variables.APP_STATE_ACTIONS_TO_EXECUTE, flowableProcessTestData.getAppActions());
        setSerializedValueInMap(vars, Variables.CLOUD_SERVICE_KEYS_TO_CREATE, flowableProcessTestData.getServiceKeys());
        setSerializedValueInMap(vars, Variables.MODULES_FOR_DEPLOYMENT, flowableProcessTestData.getModulesForDeployment());
        setSerializedValueInMap(vars, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN, flowableProcessTestData.getExtensionDescriptors());
        return vars;
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

    private void assertCapturedVariables(FlowableProcessTestData expected, boolean expectBigDescriptorMatch) {
        Map<String, Object> actual = testVariableTask.capturedVariables;
        assertEquals(expected.getCorrelationId(), getDeserializedValue(actual, Variables.CORRELATION_ID));
        assertEquals(expected.getSpaceGuid(), getDeserializedValue(actual, Variables.SPACE_GUID));
        assertEquals(expected.getOrgGuid(), getDeserializedValue(actual, Variables.ORGANIZATION_GUID));
        assertEquals(expected.getUsername(), getDeserializedValue(actual, Variables.USER));
        assertEquals(expected.getUserGuid(), getDeserializedValue(actual, Variables.USER_GUID));
        assertEquals(expected.getMtaId(), getDeserializedValue(actual, Variables.MTA_ID));
        assertEquals(expected.keepFiles(), getDeserializedValue(actual, Variables.KEEP_FILES));
        assertEquals(expected.deleteServices(), getDeserializedValue(actual, Variables.DELETE_SERVICES));
        assertEquals(expected.getAppsStageTimeout(), getDeserializedValue(actual, Variables.APPS_STAGE_TIMEOUT_PROCESS_VARIABLE));
        assertEquals(expected.getModulesCount(), getDeserializedValue(actual, Variables.MODULES_COUNT));
        assertEquals(JsonUtil.toJson(expected.getSmallDescriptor()),
                     JsonUtil.toJson(getDeserializedValue(actual, Variables.DEPLOYMENT_DESCRIPTOR_WITH_SYSTEM_PARAMETERS)));

        String expectedBigDescriptor = JsonUtil.toJson(expected.getBigDescriptor());
        String actualBigDescriptor = JsonUtil.toJson(getDeserializedValue(actual, Variables.DEPLOYMENT_DESCRIPTOR));
        assertBigDescriptor(expectBigDescriptorMatch, expectedBigDescriptor, actualBigDescriptor);

        assertEquals(expected.getDeployedMta(), getDeserializedValue(actual, Variables.DEPLOYED_MTA));
        assertEquals(expected.getRoutes(), getDeserializedValue(actual, Variables.CURRENT_ROUTES));
        assertEquals(expected.getStepPhase(), getDeserializedValue(actual, Variables.STEP_PHASE));
        assertEquals(expected.getAppActions(), getDeserializedValue(actual, Variables.APP_STATE_ACTIONS_TO_EXECUTE));
        assertEquals(expected.getServiceKeys(), getDeserializedValue(actual, Variables.CLOUD_SERVICE_KEYS_TO_CREATE));
        assertEquals(expected.getModulesForDeployment(), getDeserializedValue(actual, Variables.MODULES_FOR_DEPLOYMENT));
        assertEquals(JsonUtil.toJson(expected.getExtensionDescriptors(), JsonSerializationStrategy.ALLOW_NULLS),
                     JsonUtil.toJson(getDeserializedValue(actual, Variables.MTA_EXTENSION_DESCRIPTOR_CHAIN),
                                     JsonSerializationStrategy.ALLOW_NULLS));
    }

    private static void assertBigDescriptor(boolean expectBigDescriptorMatch, String expectedBigDescriptor, String actualBigDescriptor) {
        if (expectBigDescriptorMatch) {
            assertEquals(expectedBigDescriptor, actualBigDescriptor);
            return;
        }
        assertNotEquals(expectedBigDescriptor, actualBigDescriptor);

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
