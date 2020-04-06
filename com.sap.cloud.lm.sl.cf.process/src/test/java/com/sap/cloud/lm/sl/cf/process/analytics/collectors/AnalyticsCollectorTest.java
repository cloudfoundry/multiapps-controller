package com.sap.cloud.lm.sl.cf.process.analytics.collectors;

import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.net.URL;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sap.cloud.lm.sl.cf.core.model.ServiceOperation;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
import com.sap.cloud.lm.sl.cf.process.variables.VariableHandling;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.Tester;
import com.sap.cloud.lm.sl.common.util.Tester.Expectation;
import com.sap.cloud.lm.sl.mta.model.Module;

public class AnalyticsCollectorTest {
    protected static final String PROCESS_ID = "process-instance-id";
    protected static final String ARCHIVE_ID = "archive-id";
    protected static final String MTA_ID = "mta-id";
    protected static final String ORG_NAME = "org";
    protected static final String SPACE_NAME = "space";
    protected static final String CONTROLLER_URL = "http://example.com/";
    protected static final String TIME_ZONE = "Europe/Berlin";
    protected static final Map<String, ServiceOperation.Type> TRIGGERED_SERVICE_OPERATIONS = new HashMap<>();
    protected static final String MODULE_A = "module-a";

    static {
        TRIGGERED_SERVICE_OPERATIONS.put("foo", ServiceOperation.Type.CREATE);
        TRIGGERED_SERVICE_OPERATIONS.put("bar", ServiceOperation.Type.CREATE);
        TRIGGERED_SERVICE_OPERATIONS.put("baz", ServiceOperation.Type.UPDATE);
        TRIGGERED_SERVICE_OPERATIONS.put("qux", ServiceOperation.Type.CREATE);
    }

    private final Tester tester = Tester.forClass(getClass());

    protected final DelegateExecution execution = com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution.createSpyInstance();

    protected final FileService fileService = Mockito.mock(FileService.class);
    @Mock
    protected ApplicationConfiguration configuration;

    @Spy
    public DeployProcessAttributesCollector deployProcessAttributesCollector = new DeployProcessAttributesCollector(fileService);

    @Spy
    public UndeployProcessAttributesCollector undeployProcessAttributesCollector = new UndeployProcessAttributesCollector();

    @Mock
    private ProcessTypeParser processTypeParser;

    @Mock
    private ProcessEngineConfiguration processEngineConfiguration;

    @InjectMocks
    protected AnalyticsCollector collector;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockProcessStartTime();
        collector.endTimeSupplier = () -> 149543224L;
        when(configuration.getControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        collector.timeZoneSupplier = () -> ZoneId.of(TIME_ZONE);
        mockMtaSize();
        prepareContextForDeploy();
    }

    public void mockProcessStartTime() {
        HistoricProcessInstanceQuery query = Mockito.mock(HistoricProcessInstanceQuery.class);
        HistoryService service = Mockito.mock(HistoryService.class);
        when(service.createHistoricProcessInstanceQuery()).thenReturn(query);
        when(query.processInstanceId(Mockito.anyString())).thenReturn(query);
        when(processEngineConfiguration.getHistoryService()).thenReturn(service);
        HistoricProcessInstance processInstance = Mockito.mock(HistoricProcessInstance.class);
        when(processInstance.getStartTime()).thenReturn(new Date(0));
        when(query.singleResult()).thenReturn(processInstance);
    }

    public void mockMtaSize() throws Exception {
        VariableHandling.set(execution, Variables.APP_ARCHIVE_ID, ARCHIVE_ID);
        FileEntry fileEntry = Mockito.mock(FileEntry.class);
        when(fileService.getFile(VariableHandling.get(execution, Variables.SPACE_ID), ARCHIVE_ID)).thenReturn(fileEntry);
        when(fileEntry.getSize()).thenReturn(BigInteger.valueOf(1234));
    }

    private void prepareContextForDeploy() {
        when(execution.getProcessInstanceId()).thenReturn(PROCESS_ID);
        VariableHandling.set(execution, Variables.MTA_ID, MTA_ID);
        VariableHandling.set(execution, Variables.SPACE, SPACE_NAME);
        VariableHandling.set(execution, Variables.ORG, ORG_NAME);

        when(execution.getVariable(Constants.VAR_CUSTOM_DOMAINS)).thenReturn(mockedListAsBytesWithStrings(2));
        when(execution.getVariable(Constants.VAR_SERVICES_TO_CREATE)).thenReturn(mockedListWithStrings(4));
        when(execution.getVariable(Constants.VAR_APPS_TO_DEPLOY)).thenReturn(mockedListAsBytesWithStrings(1));
        when(execution.getVariable(Constants.VAR_PUBLISHED_ENTRIES)).thenReturn(mockedListWithObjects(1));
        when(execution.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE)).thenReturn(mockedListWithObjects(3));
        when(execution.getVariable(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS)).thenReturn(JsonUtil.toJsonBinary(TRIGGERED_SERVICE_OPERATIONS));
        when(execution.getVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE)).thenReturn(JsonUtil.toJsonBinary(Collections.emptyMap()));
        when(execution.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE)).thenReturn(mockedListWithObjects(2));
        when(execution.getVariable(Constants.VAR_DELETED_ENTRIES)).thenReturn(mockedListWithObjects(1));
        when(execution.getVariable(Constants.VAR_APPS_TO_UNDEPLOY)).thenReturn(mockAppsToUndeploy(3));
        when(execution.getVariable(Constants.VAR_SERVICES_TO_DELETE)).thenReturn(mockedListWithStrings(3));
        when(execution.getVariable(Constants.VAR_UPDATED_SUBSCRIBERS)).thenReturn(mockedListWithObjects(1));
        when(execution.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS)).thenReturn(mockedListWithObjects(2));
    }

    private byte[] mockedListWithObjects(int size) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(Collections.emptyMap());
        }
        return JsonUtil.toJsonBinary(list);
    }

    private List<String> mockedListWithStrings(int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add("{}");
        }
        return list;
    }

    private byte[] mockedListAsBytesWithStrings(int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add("{}");
        }
        return JsonUtil.toJsonBinary(list);
    }

    private List<String> mockAppsToUndeploy(int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            CloudApplication app = ImmutableCloudApplication.builder()
                                                            .name(Integer.toString(i))
                                                            .build();
            list.add(JsonUtil.toJson(app));
        }
        return list;
    }

    @Test
    public void collectAttributesDeployTest() {
        when(processTypeParser.getProcessType(execution)).thenReturn(ProcessType.DEPLOY);
        tester.test(() -> collector.collectAnalyticsData(execution), new Expectation(Expectation.Type.JSON, "AnalyticsDeploy.json"));
    }

    @Test
    public void collectAttributesUndeployTest() {
        when(processTypeParser.getProcessType(execution)).thenReturn(ProcessType.UNDEPLOY);
        tester.test(() -> collector.collectAnalyticsData(execution), new Expectation(Expectation.Type.JSON, "AnalyticsUndeploy.json"));
    }

}
