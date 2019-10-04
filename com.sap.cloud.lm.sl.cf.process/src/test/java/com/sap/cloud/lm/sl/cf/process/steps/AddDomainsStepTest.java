package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class AddDomainsStepTest extends SyncFlowableStepTest<AddDomainsStep> {

    private final List<String> existingDomains;
    private final List<String> customDomains;

    private List<String> nonExistingCustomDomains;

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
    public void setUp() {
        prepareContext();
        Mockito.when(client.getDomains())
               .thenReturn(getExistingDomainsList());
        nonExistingCustomDomains = getNonExistingDomainsList();
    }

    @Test
    public void testExecute() {
        step.execute(context);

        assertStepFinishedSuccessfully();

        for (String nonExistingCustomDomain : nonExistingCustomDomains) {
            Mockito.verify(client, Mockito.times(1))
                   .addDomain(nonExistingCustomDomain);
        }
    }

    private void prepareContext() {
        StepsUtil.setCustomDomains(context, customDomains);
    }

    private List<CloudDomain> getExistingDomainsList() {
        List<CloudDomain> result = new ArrayList<>();
        for (String existingDomain : existingDomains) {
            result.add(ImmutableCloudDomain.builder()
                                           .name(existingDomain)
                                           .build());
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
