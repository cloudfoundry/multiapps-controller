package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.List;

import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AddDomainsStepTest extends SyncFlowableStepTest<AddDomainsStep> {

    @Test
    void testExecute() {
        List<String> customDomains = List.of("foo", "bar");
        prepareContext(customDomains);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        // Each custom domain is added individually; addDomain itself skips creation when the domain already exists.
        for (String customDomain : customDomains) {
            Mockito.verify(client)
                   .addDomain(customDomain);
        }
        // Existence is resolved by addDomain, so the step never lists all domains visible to the user.
        Mockito.verify(client, Mockito.never())
               .getDomains();
    }

    @Test
    void testExecuteWithoutCustomDomains() {
        prepareContext(List.of());

        step.execute(execution);

        assertStepFinishedSuccessfully();

        // With no custom domains the step short-circuits before touching the controller.
        Mockito.verifyNoInteractions(client);
    }

    @Test
    void testGetStepErrorMessage() {
        prepareContext(List.of("foo"));
        Assertions.assertEquals(Messages.ERROR_ADDING_DOMAINS, step.getStepErrorMessage(context));
    }

    private void prepareContext(List<String> customDomains) {
        context.setVariable(Variables.CUSTOM_DOMAINS, customDomains);
    }

    @Override
    protected AddDomainsStep createStep() {
        return new AddDomainsStep();
    }

}
