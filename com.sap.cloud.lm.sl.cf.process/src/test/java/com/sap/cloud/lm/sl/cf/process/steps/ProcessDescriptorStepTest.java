package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatformTypes;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatforms;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.BiFunction;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.MtaSchemaVersionDetector;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.handlers.v1_0.DescriptorParser;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class ProcessDescriptorStepTest extends AbstractStepTest<ProcessDescriptorStep> {

    private static final String SPACE_NAME = "initial";
    private static final String ORG_NAME = "initial";

    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();
    private static final DescriptorParser DESCRIPTOR_PARSER = new DescriptorParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;
    private static final Integer MTA_MINOR_SCHEMA_VERSION = 0;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor(DESCRIPTOR_PARSER, "node-hello-mtad.yaml",
        ProcessDescriptorStepTest.class);
    private static final TargetPlatformType PLATFORM_TYPE = loadPlatformTypes(CONFIGURATION_PARSER, "platform-types-01.json",
        ProcessDescriptorStepTest.class).get(0);
    private static final TargetPlatform PLATFORM = loadPlatforms(CONFIGURATION_PARSER, "platforms-01.json",
        ProcessDescriptorStepTest.class).get(0);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory,
            TargetPlatformType platformType, TargetPlatform platform, SystemParameters systemParameters, ConfigurationEntryDao dao,
            BiFunction<String, String, String> spaceIdSupplier) {
            return resolverr;
        }

    }

    @Mock
    private CloudFoundryClientProvider clientProvider;
    @Mock
    private MtaDescriptorPropertiesResolver resolverr;
    @Mock
    private CloudFoundryOperations client;
    @Mock
    private MtaSchemaVersionDetector versionDetector;

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() throws Exception {
        StepsUtil.setSystemParameters(context,
            new SystemParameters(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        StepsUtil.setDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setXsPlaceholderReplacementValues(context, MapUtil.asMap(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, 999));

        context.setVariable(Constants.VAR_SPACE, SPACE_NAME);
        context.setVariable(Constants.VAR_ORG, ORG_NAME);

        StepsUtil.setPlatformType(context, PLATFORM_TYPE);
        StepsUtil.setPlatform(context, PLATFORM);

        when(clientProvider.getCloudFoundryClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
        context.setVariable(Constants.VAR_MTA_MINOR_SCHEMA_VERSION, MTA_MINOR_SCHEMA_VERSION);

        context.setVariable(Constants.VAR_USER, "XSMASTER");
    }

    @Test
    public void testExecute1() throws Exception {
        when(resolverr.resolve(any())).thenReturn(DEPLOYMENT_DESCRIPTOR);

        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        TestUtil.test(() -> StepsUtil.getSubscriptionsToCreate(context), "[]", getClass());

        TestUtil.test(() -> {

            return StepsUtil.getDeploymentDescriptor(context);

        } , "R:node-hello-mtad-1.yaml.json", getClass());
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(resolverr.resolve(any())).thenThrow(new SLException("Error!"));

        step.execute(context);
    }

    @Test
    public void testGetSpaceIdSupplier1() {
        CloudOrganization org = new CloudOrganization(new Meta(null, null, null), ORG_NAME);
        CloudSpace space = new CloudSpace(new Meta(NameUtil.getUUID(SPACE_NAME), null, null), SPACE_NAME, org);

        when(client.getSpaces()).thenReturn(Arrays.asList(space));

        assertEquals("cc51b819-7428-3ab7-9cef-9e94fe778cc9", step.getSpaceIdSupplier(client).apply(ORG_NAME, SPACE_NAME));
    }

    @Test
    public void testGetSpaceIdSupplier2() {
        CloudOrganization org = new CloudOrganization(new Meta(null, null, null), ORG_NAME);
        CloudSpace space = new CloudSpace(new Meta(NameUtil.getUUID(SPACE_NAME), null, null), SPACE_NAME, org);

        when(client.getSpace(SPACE_NAME)).thenReturn(space);

        assertNull(step.getSpaceIdSupplier(client).apply("not-initial", SPACE_NAME));
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock();
    }

}
