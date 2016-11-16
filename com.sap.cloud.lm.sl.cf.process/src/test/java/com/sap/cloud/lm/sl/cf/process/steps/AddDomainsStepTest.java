package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.util.ContextUtil;
import com.sap.cloud.lm.sl.cf.process.Constants;

@RunWith(Parameterized.class)
public class AddDomainsStepTest extends AbstractStepTest<AddDomainsStep> {

    private final List<String> existingDomains;
    private final List<String> customDomains;

    private List<String> nonExistingCustomDomains;

    private CloudFoundryOperations client = Mockito.mock(CloudFoundryOperations.class);

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) Attempt to add non-existing custom domains:
            {
                Arrays.asList("foo", "bar"), Arrays.asList("baz", "qux"),
            },
            // (1) Attempt to add existing custom domains:
            {
                Arrays.asList("foo", "bar"), Arrays.asList("foo", "bar"),
            },
            // (2) Attempt to add a mix of existing and non-existing custom domains:
            {
                Arrays.asList("foo", "bar"), Arrays.asList("foo", "baz"),
            },
// @formatter:on
        });
    }

    public AddDomainsStepTest(List<String> existingDomains, List<String> customDomains) {
        this.existingDomains = existingDomains;
        this.customDomains = customDomains;
    }

    @Before
    public void setUp() throws Exception {
        prepareContext();
        Mockito.when(client.getDomainsForOrg()).thenReturn(getExistingDomainsList());
        nonExistingCustomDomains = getNonExistingDomainsList();
        step.clientSupplier = (context) -> client;
    }

    @Test
    public void testExecute() throws Exception {
        step.execute(context);

        assertEquals(ExecutionStatus.SUCCESS.toString(),
            context.getVariable(com.sap.activiti.common.Constants.STEP_NAME_PREFIX + step.getLogicalStepName()));

        for (String nonExistingCustomDomain : nonExistingCustomDomains) {
            Mockito.verify(client, Mockito.times(1)).addDomain(nonExistingCustomDomain);
        }
    }

    private void prepareContext() {
        ContextUtil.setArrayVariableFromCollection(context, Constants.VAR_CUSTOM_DOMAINS, customDomains);
    }

    private List<CloudDomain> getExistingDomainsList() {
        List<CloudDomain> result = new ArrayList<>();
        for (String existingDomain : existingDomains) {
            result.add(new CloudDomain(null, existingDomain, null));
        }
        return result;
    }

    private List<String> getNonExistingDomainsList() {
        List<String> result = new ArrayList<>();
        for (String customDomain : customDomains) {
            if (!existingDomains.contains(customDomain)) {
                result.add(customDomain);
            }
        }
        return result;
    }

    @Override
    protected AddDomainsStep createStep() {
        return new AddDomainsStep();
    }

}
