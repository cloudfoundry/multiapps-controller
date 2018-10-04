package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadPlatforms;
import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadTargets;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.function.BiFunction;

import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sap.cloud.lm.sl.cf.core.cf.HandlerFactory;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.MtaDescriptorPropertiesResolver;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;
import com.sap.cloud.lm.sl.mta.handlers.v1.ConfigurationParser;
import com.sap.cloud.lm.sl.mta.model.SystemParameters;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1.Platform;
import com.sap.cloud.lm.sl.mta.model.v1.Target;

public class ProcessDescriptorStepTest extends SyncActivitiStepTest<ProcessDescriptorStep> {

    private static final String SPACE_NAME = "initial";
    private static final String ORG_NAME = "initial";

    private static final ConfigurationParser CONFIGURATION_PARSER = new ConfigurationParser();

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 1;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor("node-hello-mtad.yaml",
        ProcessDescriptorStepTest.class);
    private static final Platform PLATFORM = loadPlatforms(CONFIGURATION_PARSER, "platform-types-01.json", ProcessDescriptorStepTest.class)
        .get(0);
    private static final Target TARGET = loadTargets(CONFIGURATION_PARSER, "platforms-01.json", ProcessDescriptorStepTest.class).get(0);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, Platform platformType,
            Target platform, SystemParameters systemParameters, ConfigurationEntryDao dao,
            BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget) {
            return resolver;
        }
    }

    @Mock
    private MtaDescriptorPropertiesResolver resolver;
    @Mock
    private SpaceGetter spaceGetter;

    @Before
    public void setUp() throws Exception {
        prepareContext();
    }

    private void prepareContext() throws Exception {
        StepsUtil.setSystemParameters(context,
            new SystemParameters(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        StepsUtil.setUnresolvedDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setXsPlaceholderReplacementValues(context, MapUtil.asMap(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, 999));

        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID, Constants.DEPLOY_SERVICE_ID);

        StepsUtil.setPlatform(context, PLATFORM);
        StepsUtil.setTarget(context, TARGET);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
    }

    @Test
    public void testExecute1() throws Exception {
        when(resolver.resolve(any())).thenReturn(DEPLOYMENT_DESCRIPTOR);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getSubscriptionsToCreate(context), "[]", getClass());

        TestUtil.test(() -> {

            return StepsUtil.getDeploymentDescriptor(context);

        }, "R:node-hello-mtad-1.yaml.json", getClass());
    }

    @Test(expected = SLException.class)
    public void testExecute2() throws Exception {
        when(resolver.resolve(any())).thenThrow(new SLException("Error!"));

        step.execute(context);
    }

    @Test
    public void testGetSpaceIdSupplier1() {
        CloudSpace space = new CloudSpace(new Meta(NameUtil.getUUID(SPACE_NAME), null, null), SPACE_NAME, null);

        when(spaceGetter.findSpace(client, ORG_NAME, SPACE_NAME)).thenReturn(space);

        assertEquals("cc51b819-7428-3ab7-9cef-9e94fe778cc9", step.getSpaceIdSupplier(client)
            .apply(ORG_NAME, SPACE_NAME));
    }

    @Test
    public void testGetSpaceIdSupplier2() {
        assertNull(step.getSpaceIdSupplier(client)
            .apply("not-initial", SPACE_NAME));
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock();
    }

}
