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
import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sap.cloud.lm.sl.cf.core.cf.PlatformType;
import com.sap.cloud.lm.sl.cf.core.cf.services.ServiceOperationType;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.model.FileEntry;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.ProcessTypeParser;
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
    protected static final PlatformType PLATFORM_TYPE = PlatformType.CF;
    protected static final Map<String, ServiceOperationType> TRIGGERED_SERVICE_OPERATIONS = new HashMap<>();
    protected static final String MODULE_A = "module-a";

    static {
        TRIGGERED_SERVICE_OPERATIONS.put("foo", ServiceOperationType.CREATE);
        TRIGGERED_SERVICE_OPERATIONS.put("bar", ServiceOperationType.CREATE);
        TRIGGERED_SERVICE_OPERATIONS.put("baz", ServiceOperationType.UPDATE);
        TRIGGERED_SERVICE_OPERATIONS.put("qux", ServiceOperationType.CREATE);
    }

    private Tester tester = Tester.forClass(getClass());

    protected DelegateExecution context = com.sap.cloud.lm.sl.cf.process.mock.MockDelegateExecution.createSpyInstance();

    protected FileService fileService = Mockito.mock(FileService.class);
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

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mockProcessStartTime();
        collector.endTimeSupplier = () -> 149543224L;
        when(configuration.getControllerUrl()).thenReturn(new URL(CONTROLLER_URL));
        collector.timeZoneSupplier = () -> ZoneId.of(TIME_ZONE);
        mockMtaSize();
        prepareContextForDeploy();
    }

    public void mockProcessStartTime() throws Exception {
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
        when(context.getVariable(Constants.PARAM_APP_ARCHIVE_ID)).thenReturn(ARCHIVE_ID);
        FileEntry fileEntry = Mockito.mock(FileEntry.class);
        when(fileService.getFile(StepsUtil.getSpaceId(context), ARCHIVE_ID)).thenReturn(fileEntry);
        when(fileEntry.getSize()).thenReturn(BigInteger.valueOf(1234));
    }

    private void prepareContextForDeploy() throws Exception {
        when(context.getProcessInstanceId()).thenReturn(PROCESS_ID);
        when(context.getVariable(Constants.PARAM_MTA_ID)).thenReturn(MTA_ID);
        when(configuration.getPlatformType()).thenReturn(PLATFORM_TYPE);
        context.setVariable(Constants.VAR_SPACE, SPACE_NAME);
        context.setVariable(Constants.VAR_ORG, ORG_NAME);

        when(context.getVariable(Constants.VAR_MODULES_TO_DEPLOY_CLASSNAME)).thenReturn(Module.class.getName());
        when(context.getVariable(Constants.VAR_ALL_MODULES_TO_DEPLOY)).thenReturn(mockModulesToDeploy(2));
        when(context.getVariable(Constants.VAR_CUSTOM_DOMAINS)).thenReturn(mockedListAsBytesWithStrings(2));
        when(context.getVariable(Constants.VAR_SERVICES_TO_CREATE)).thenReturn(mockedListWithStrings(4));
        when(context.getVariable(Constants.VAR_APPS_TO_DEPLOY)).thenReturn(mockedListAsBytesWithStrings(1));
        when(context.getVariable(Constants.VAR_PUBLISHED_ENTRIES)).thenReturn(mockedListWithObjects(1));
        when(context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_CREATE)).thenReturn(mockedListWithObjects(3));
        when(context.getVariable(Constants.VAR_TRIGGERED_SERVICE_OPERATIONS))
            .thenReturn(JsonUtil.toJsonBinary(TRIGGERED_SERVICE_OPERATIONS));
        when(context.getVariable(Constants.VAR_SERVICE_KEYS_TO_CREATE)).thenReturn(JsonUtil.toJsonBinary(Collections.emptyMap()));

        when(context.getVariable(Constants.VAR_SUBSCRIPTIONS_TO_DELETE)).thenReturn(mockedListWithObjects(2));
        when(context.getVariable(Constants.VAR_DELETED_ENTRIES)).thenReturn(mockedListWithObjects(1));
        when(context.getVariable(Constants.VAR_APPS_TO_UNDEPLOY)).thenReturn(mockAppsToUndeploy(3));
        when(context.getVariable(Constants.VAR_SERVICES_TO_DELETE)).thenReturn(mockedListAsBytesWithStrings(3));
        when(context.getVariable(Constants.VAR_UPDATED_SUBSCRIBERS)).thenReturn(mockedListWithObjects(1));
        when(context.getVariable(Constants.VAR_UPDATED_SERVICE_BROKER_SUBSCRIBERS)).thenReturn(mockedListWithObjects(2));
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

    private List<byte[]> mockModulesToDeploy(int size) {
        List<byte[]> list = new ArrayList<>();
        Module module = Module.createV2();
        for (int i = 0; i < size; i++) {
            module.setName(Integer.toString(i));
            list.add(JsonUtil.toJsonBinary(module));
        }
        return list;
    }

    private List<String> mockAppsToUndeploy(int size) {
        List<String> list = new ArrayList<>();
        CloudApplication app = new CloudApplication(null, null);
        for (int i = 0; i < size; i++) {
            app.setName(Integer.toString(i));
            list.add(JsonUtil.toJson(app));
        }
        return list;
    }

    @Test
    public void collectAttributesDeployTest() throws Exception {
        when(processTypeParser.getProcessType(context)).thenReturn(ProcessType.DEPLOY);
        tester.test(() -> {
            return collector.collectAnalyticsData(context);
        }, new Expectation(Expectation.Type.JSON, "AnalyticsDeploy.json"));
    }

    @Test
    public void collectAttributesUndeployTest() throws Exception {
        when(processTypeParser.getProcessType(context)).thenReturn(ProcessType.UNDEPLOY);
        tester.test(() -> {
            return collector.collectAnalyticsData(context);
        }, new Expectation(Expectation.Type.JSON, "AnalyticsUndeploy.json"));
    }

}
