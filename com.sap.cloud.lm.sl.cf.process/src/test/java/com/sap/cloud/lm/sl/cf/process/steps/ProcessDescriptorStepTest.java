package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.cloud.lm.sl.cf.process.steps.StepsTestUtil.loadDeploymentDescriptor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

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
import com.sap.cloud.lm.sl.common.util.TestUtil.Expectation;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;

public class ProcessDescriptorStepTest extends SyncFlowableStepTest<ProcessDescriptorStep> {

    private static final String SPACE_NAME = "initial";
    private static final String ORG_NAME = "initial";

    private static final Integer MTA_MAJOR_SCHEMA_VERSION = 2;

    private static final DeploymentDescriptor DEPLOYMENT_DESCRIPTOR = loadDeploymentDescriptor("node-hello-mtad.yaml",
        ProcessDescriptorStepTest.class);

    private class ProcessDescriptorStepMock extends ProcessDescriptorStep {

        @Override
        protected MtaDescriptorPropertiesResolver getMtaDescriptorPropertiesResolver(HandlerFactory factory, ConfigurationEntryDao dao,
            BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget, boolean useNamespaces,
            boolean useNamespacesForServices) {
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
        StepsUtil.setCompleteDeploymentDescriptor(context, DEPLOYMENT_DESCRIPTOR);
        StepsUtil.setXsPlaceholderReplacementValues(context, MapUtil.asMap(SupportedParameters.XSA_ROUTER_PORT_PLACEHOLDER, 999));

        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SERVICE_ID, Constants.DEPLOY_SERVICE_ID);
        context.setVariable(Constants.PARAM_USE_NAMESPACES, false);
        context.setVariable(Constants.PARAM_USE_NAMESPACES_FOR_SERVICES, false);

        context.setVariable(Constants.VAR_MTA_MAJOR_SCHEMA_VERSION, MTA_MAJOR_SCHEMA_VERSION);
    }

    @Test
    public void testExecute1() throws Exception {
        when(resolver.resolve(any())).thenAnswer((invocation) -> invocation.getArguments()[0]);

        step.execute(context);

        assertStepFinishedSuccessfully();

        TestUtil.test(() -> StepsUtil.getSubscriptionsToCreate(context), new Expectation("[]"), getClass());

        TestUtil.test(() -> StepsUtil.getCompleteDeploymentDescriptor(context),
            new Expectation(Expectation.Type.RESOURCE, "node-hello-mtad-1.yaml.json"), getClass());
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

    @Test(expected = IllegalStateException.class)
    public void testWithInvalidModulesSpecifiedForDeployment() {
        when(resolver.resolve(any())).thenReturn(DEPLOYMENT_DESCRIPTOR);
        when(context.getVariable(Constants.PARAM_MODULES_FOR_DEPLOYMENT)).thenReturn("foo,bar");

        step.execute(context);
    }

    @Override
    protected ProcessDescriptorStep createStep() {
        return new ProcessDescriptorStepMock();
    }

}
