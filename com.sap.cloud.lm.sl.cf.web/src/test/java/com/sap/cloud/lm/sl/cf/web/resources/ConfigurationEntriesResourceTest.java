package com.sap.cloud.lm.sl.cf.web.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dto.serialization.ConfigurationFilterDto;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestCase;
import com.sap.cloud.lm.sl.common.util.TestInput;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;

@RunWith(Parameterized.class)
public class ConfigurationEntriesResourceTest {

    private TestCase<TestInput> test;

    public ConfigurationEntriesResourceTest(TestCase<TestInput> test) {
        this.test = test;
    }

    @Parameters
    public static List<Object[]> getParameters() throws Exception {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (1)
            {
                new SearchRequestTest(new SearchRequestTestInput(Arrays.asList("foo:bar", "baz:qux"), "parsed-properties-01.json"),
                    new Expectation(Expectation.Type.RESOURCE, "configuration-entries-resource-test-output-08.json")),
            },
            // (2)
            {
                new SearchRequestTest(new SearchRequestTestInput(Arrays.asList("{\"foo\":\"bar\",\"baz\":\"qux\"}"), "parsed-properties-01.json"),
                    new Expectation(Expectation.Type.RESOURCE, "configuration-entries-resource-test-output-08.json")),
            },
            // (3)
            {
                new SearchRequestTest(new SearchRequestTestInput(Arrays.asList("a"), "parsed-properties-01.json"),
                    new Expectation(Expectation.Type.EXCEPTION, "Could not parse content query parameter as JSON or list")),
            },
// @formatter:on
        });
    }

    @Test
    public void test() throws Exception {
        test.run();
    }

    private static class SearchRequestTestInput extends TestInput {

        private List<String> requiredContent;
        private Map<String, Object> parsedRequiredContent;

        public SearchRequestTestInput(List<String> requiredContent, String parsedRequiredContentLocation) throws Exception {
            this.requiredContent = requiredContent;
            this.parsedRequiredContent = JsonUtil.convertJsonToMap(SearchRequestTestInput.class.getResourceAsStream(parsedRequiredContentLocation),
                                                                   new TypeToken<Map<String, String>>() {
                                                                   }.getType());
        }

        public Map<String, Object> getParsedRequiredContent() {
            return parsedRequiredContent;
        }

        public List<String> getRequiredContent() {
            return requiredContent;
        }
    }

    private static class SearchRequestTest extends TestCase<SearchRequestTestInput> {

        private static final String PROVIDER_NID = "N";
        private static final CloudTarget TARGET_SPACE = new CloudTarget("O", "S");
        private static final String PROVIDER_VERSION = "V";
        private static final String PROVIDER_ID = "I";

        @Mock
        private CloudControllerClient client;
        @Mock
        private CloudControllerClientProvider clientProvider;
        @Mock
        private ConfigurationEntryDao dao;
        @Mock
        private UserInfo userInfo;
        @Mock
        private ApplicationConfiguration configuration;
        @InjectMocks
        private ConfigurationEntriesResource resource = new ConfigurationEntriesResource();

        public SearchRequestTest(SearchRequestTestInput input, Expectation expectation) {
            super(input, expectation);
        }

        @Override
        protected void test() throws Exception {
            TestUtil.test(() -> {

                return new RestResponse(resource.getConfigurationEntries(new ConfigurationFilterDto(PROVIDER_NID,
                                                                                                    PROVIDER_ID,
                                                                                                    PROVIDER_VERSION,
                                                                                                    TARGET_SPACE,
                                                                                                    input.getRequiredContent())));

            }, expectation, getClass());
        }

        @Override
        protected void setUp() throws Exception {
            MockitoAnnotations.initMocks(this);
            resource.userInfoSupplier = () -> userInfo;
            when(userInfo.getName()).thenReturn("");
            when(clientProvider.getControllerClient("")).thenReturn(client);
            when(client.getSpaces()).thenReturn(Collections.emptyList());
            when(dao.find(eq(PROVIDER_NID), eq(PROVIDER_ID), eq(PROVIDER_VERSION), eq(TARGET_SPACE), eq(input.getParsedRequiredContent()),
                          any(), any())).thenReturn(Collections.emptyList());
        }
    }

}
