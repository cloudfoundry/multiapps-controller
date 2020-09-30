package org.cloudfoundry.multiapps.controller.process.util;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.test.Tester;
import org.cloudfoundry.multiapps.common.test.Tester.Expectation;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.ApplicationColor;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMta;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.Phase;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ApplicationColorDetectorTest {

    private static final String FAKE_CORRELATION_ID = "39170ac4-57ad-11e9-8647-d663bd873d93";
    private static final String FAKE_MTA_ID = "mta_id";
    private static final String BLUE = "BLUE";
    private static final String GREEN = "GREEN";
    private static final String EXPECTED_EXCEPTION_MESSAGE = "There are both blue and green applications already deployed for MTA \"com.sap.sample.mta.consumer\"";
    private static final String FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID = "123123123";
    private static final String FAKE_PROCESS_ID = "abc";

    static Stream<Arguments> detectLiveApplicationColorMtaColorAndPhase() {
        return Stream.of(Arguments.of("deployed-mta-01.json", GREEN, Operation.State.FINISHED, Operation.State.ABORTED, GREEN),
                         Arguments.of("deployed-mta-01.json", BLUE, Operation.State.FINISHED, Operation.State.ABORTED, BLUE),
                         Arguments.of("deployed-mta-02.json", GREEN, Operation.State.FINISHED, Operation.State.ABORTED, GREEN),
                         Arguments.of("deployed-mta-02.json", BLUE, Operation.State.FINISHED, Operation.State.ABORTED, BLUE),
                         Arguments.of("deployed-mta-02.json", GREEN, Operation.State.FINISHED, Operation.State.FINISHED, BLUE),
                         Arguments.of("deployed-mta-03.json", BLUE, Operation.State.FINISHED, Operation.State.ABORTED, BLUE),
                         Arguments.of("deployed-mta-03.json", GREEN, Operation.State.FINISHED, Operation.State.ABORTED, GREEN),
                         Arguments.of("deployed-mta-04.json", BLUE, Operation.State.FINISHED, Operation.State.ABORTED, BLUE),
                         Arguments.of("deployed-mta-04.json", GREEN, Operation.State.FINISHED, Operation.State.ABORTED, GREEN));
    }

    // This method is used to initialize method parameters -> @MethodSource
    static Stream<Arguments> detectLiveApplicationColor() {
        return Stream.of(Arguments.of("deployed-mta-02.json", new Expectation(GREEN)),
                         Arguments.of("deployed-mta-01.json", new Expectation(BLUE)),
                         Arguments.of("deployed-mta-03.json", new Expectation(BLUE)),
                         Arguments.of("deployed-mta-04.json", new Expectation(GREEN)),
                         Arguments.of("deployed-mta-05.json", new Expectation(null)),
                         Arguments.of("deployed-mta-06.json", new Expectation(GREEN)));
    }

    private final Tester tester = Tester.forClass(getClass());

    @Mock
    private OperationService operationService;

    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;

    @Mock
    private FlowableFacade flowableFacade;

    @InjectMocks
    private ApplicationColorDetector applicationColorDetector;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void detectSingularDeployedAppNullDeployedMta() {
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(null), new Expectation(null));
    }

    @Test
    void detectSingularDeployedApp3ModulesGreenSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            List.of(createMtaApplication("app-1", "app-1-green", parseDate("2019-04-04")),
                                                    createMtaApplication("app-2", "app-2-green", parseDate("2019-04-05")),
                                                    createMtaApplication("app-3", "app-3-green", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(GREEN);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @Test
    void detectSingularDeployedApp3ModulesBlueSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            List.of(createMtaApplication("app-1", "app-1-blue", parseDate("2019-04-04")),
                                                    createMtaApplication("app-2", "app-2-blue", parseDate("2019-04-05")),
                                                    createMtaApplication("app-3", "app-3-blue", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(BLUE);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @Test
    void detectSingularDeployedApp3ModulesNoSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            List.of(createMtaApplication("app-1", "app-1", parseDate("2019-04-04")),
                                                    createMtaApplication("app-2", "app-2", parseDate("2019-04-05")),
                                                    createMtaApplication("app-3", "app-3", parseDate("2019-04-06"))));
        Expectation expectation = new Expectation(BLUE);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @Test
    void detectSingularDeployedApp3ModulesGreenBlueSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            List.of(createMtaApplication("app-1", "app-1-blue", parseDate("2016-15-10")),
                                                    createMtaApplication("app-2", "app-2-green", parseDate("2016-10-10")),
                                                    createMtaApplication("app-3", "app-3-blue", parseDate("2016-15-10"))));
        Expectation expectation = new Expectation(Expectation.Type.EXCEPTION, EXPECTED_EXCEPTION_MESSAGE);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @Test
    void detectSingularDeployedAppNoModule() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(), Collections.emptyList());
        Expectation expectation = new Expectation(null);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @Test
    void detectSingularDeployedApp2ModuleBlueGreenSuffix() {
        DeployedMta deployedMta = createMta("com.sap.sample.mta.consumer", Collections.emptySet(),
                                            List.of(createMtaApplication("consumer", "consumer-green", parseDate("2016-15-10")),
                                                    createMtaApplication("consumer", "consumer-blue", parseDate("2016-10-10"))));
        Expectation expectation = new Expectation(Expectation.Type.EXCEPTION, EXPECTED_EXCEPTION_MESSAGE);
        tester.test(() -> applicationColorDetector.detectSingularDeployedApplicationColor(deployedMta), expectation);
    }

    @ParameterizedTest
    @MethodSource
    void detectLiveApplicationColor(String deployedMtaJsonLocation, Expectation expectations) {
        mockOperationService(createFakeOperation(Operation.State.RUNNING, FAKE_PROCESS_ID),
                             createFakeOperation(Operation.State.FINISHED, FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID));
        tester.test(() -> detectLiveApplicationColor(readResource(deployedMtaJsonLocation, DeployedMta.class)), expectations);
    }

    @Test
    void detectLiveApplicationColorNullDeployedMta() {
        tester.test(() -> detectLiveApplicationColor(null), new Expectation(null));
    }

    @ParameterizedTest
    @MethodSource
    void detectLiveApplicationColorMtaColorAndPhase(String deployedMtaJsonLocation, String expectedColor,
                                                    Operation.State currentOperationState, Operation.State lastOperationState,
                                                    String lastDeployedColor) {
        Expectation expectation = new Expectation(expectedColor);
        mockOperationService(createFakeOperation(currentOperationState, FAKE_PROCESS_ID),
                             createFakeOperation(lastOperationState, FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID));
        mockHistoricVariableInstanceColor(lastDeployedColor);
        mockHistoricVariableInstancePhase();
        tester.test(() -> detectLiveApplicationColor(readResource(deployedMtaJsonLocation, DeployedMta.class)), expectation);
    }

    @Test
    void detectLiveApplicationColorPhaseNotFound() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationService(createFakeOperation(Operation.State.RUNNING, FAKE_PROCESS_ID),
                             createFakeOperation(Operation.State.ABORTED, FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID));
        mockHistoricVariableInstanceColor(BLUE);
        tester.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation);
    }

    @Test
    void detectLiveApplicationColorMtaColoNotFound() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationService(createFakeOperation(Operation.State.RUNNING, FAKE_PROCESS_ID),
                             createFakeOperation(Operation.State.ABORTED, FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID));
        mockHistoricVariableInstancePhase();
        tester.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation);
    }

    @Test
    void detectLiveApplicationColorNoOperations() {
        Expectation expectation = new Expectation(GREEN);
        mockOperationServiceNoOtherOperations(createFakeOperation(Operation.State.RUNNING, FAKE_PROCESS_ID));
        tester.test(() -> detectLiveApplicationColor(readResource("deployed-mta-02.json", DeployedMta.class)), expectation);
    }

    private DeployedMta createMta(String id, Set<String> services, List<DeployedMtaApplication> deployedMtaApplications) {
        List<DeployedMtaService> deployedMtaServices = services.stream()
                                                               .map(serviceName -> ImmutableDeployedMtaService.builder()
                                                                                                              .name(serviceName)
                                                                                                              .build())
                                                               .collect(Collectors.toList());
        MtaMetadata mtaMetadata = ImmutableMtaMetadata.builder()
                                                      .id(id)
                                                      .build();
        return ImmutableDeployedMta.builder()
                                   .metadata(mtaMetadata)
                                   .applications(deployedMtaApplications)
                                   .services(deployedMtaServices)
                                   .build();
    }

    private Date parseDate(String date) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    private DeployedMtaApplication createMtaApplication(String moduleName, String appName, Date createdAt) {
        return ImmutableDeployedMtaApplication.builder()
                                              .name(appName)
                                              .moduleName(moduleName)
                                              .metadata(ImmutableCloudMetadata.builder()
                                                                              .createdAt(createdAt)
                                                                              .build())
                                              .build();
    }

    private void mockOperationServiceNoOtherOperations(Operation currentOperation) {
        when(operationService.createQuery()).thenReturn(operationQuery);
        doReturn(currentOperation).when(operationQuery)
                                  .singleResult();
        doReturn(Collections.emptyList()).when(operationQuery)
                                         .list();
    }

    private <T> T readResource(String deployMtaJsonLocation, Class<T> clazz) {
        return JsonUtil.fromJson(TestUtil.getResourceAsString(deployMtaJsonLocation, ApplicationColorDetectorTest.class), clazz);
    }

    private ApplicationColor detectLiveApplicationColor(DeployedMta deployedMta) {
        return applicationColorDetector.detectLiveApplicationColor(deployedMta, FAKE_CORRELATION_ID);
    }

    private void mockHistoricVariableInstancePhase() {
        HistoricVariableInstance historicVariableInstancePhase = mock(HistoricVariableInstance.class);
        when(historicVariableInstancePhase.getValue()).thenReturn(Phase.UNDEPLOY.toString());
        when(flowableFacade.getHistoricVariableInstance(FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID,
                                                        Variables.PHASE.getName())).thenReturn(historicVariableInstancePhase);
    }

    private void mockHistoricVariableInstanceColor(String color) {
        HistoricVariableInstance historicVariableInstanceColor = mock(HistoricVariableInstance.class);
        when(historicVariableInstanceColor.getValue()).thenReturn(color);
        when(flowableFacade.getHistoricVariableInstance(FAKE_BLUE_GREEN_DEPLOY_HISTORIC_PROCESS_INSTANCE_ID,
                                                        Variables.IDLE_MTA_COLOR.getName())).thenReturn(historicVariableInstanceColor);
    }

    private void mockOperationService(Operation currentOperation, Operation lastOperation) {
        when(operationService.createQuery()).thenReturn(operationQuery);
        doReturn(currentOperation).when(operationQuery)
                                  .singleResult();
        doReturn(List.of(lastOperation)).when(operationQuery)
                                        .list();
    }

    private Operation createFakeOperation(Operation.State state, String processId) {
        Operation operation = mock(Operation.class);
        when(operation.getState()).thenReturn(state);
        when(operation.getMtaId()).thenReturn(FAKE_MTA_ID);
        when(operation.getProcessId()).thenReturn(processId);
        return operation;
    }

}
