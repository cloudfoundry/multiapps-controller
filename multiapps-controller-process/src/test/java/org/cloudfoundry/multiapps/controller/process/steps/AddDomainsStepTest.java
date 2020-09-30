package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections4.ListUtils;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class AddDomainsStepTest extends SyncFlowableStepTest<AddDomainsStep> {

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0) Attempt to add non-existing custom domains:
            Arguments.of(List.of("foo", "bar"), List.of("baz", "qux")),
            // (1) Attempt to add existing custom domains:
            Arguments.of(List.of("foo", "bar"), List.of("foo", "bar")),
            // (2) Attempt to add a mix of existing and non-existing custom domains:
            Arguments.of(List.of("foo", "bar"), List.of("foo", "baz"))
// @formatter:on
        );
    }

    public AddDomainsStepTest() {

    }

    @ParameterizedTest
    @MethodSource
    void testExecute(List<String> existingDomains, List<String> customDomains) {
        prepareContext(customDomains);
        Mockito.when(client.getDomains())
               .thenReturn(getExistingDomainsList(existingDomains));
        List<String> nonExistingCustomDomains = getNonExistingDomainsList(existingDomains, customDomains);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        for (String nonExistingCustomDomain : nonExistingCustomDomains) {
            Mockito.verify(client, Mockito.times(1))
                   .addDomain(nonExistingCustomDomain);
        }
    }

    private void prepareContext(List<String> customDomains) {
        context.setVariable(Variables.CUSTOM_DOMAINS, customDomains);
    }

    private List<CloudDomain> getExistingDomainsList(List<String> existingDomains) {
        List<CloudDomain> result = new ArrayList<>();
        for (String existingDomain : existingDomains) {
            result.add(ImmutableCloudDomain.builder()
                                           .name(existingDomain)
                                           .build());
        }
        return result;
    }

    private List<String> getNonExistingDomainsList(List<String> existingDomains, List<String> customDomains) {
        return ListUtils.removeAll(customDomains, existingDomains);
    }

    @Override
    protected AddDomainsStep createStep() {
        return new AddDomainsStep();
    }

}
