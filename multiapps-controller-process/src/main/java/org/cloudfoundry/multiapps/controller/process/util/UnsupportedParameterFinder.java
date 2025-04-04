package org.cloudfoundry.multiapps.controller.process.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.ReferencesFinder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("UnsupportedParameterFinder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnsupportedParameterFinder {

    @Inject
    private SupportedParameterChecker supportedParameterChecker;

    public List<String> findUnsupportedParameters(DeploymentDescriptor descriptor) {
        List<String> unknownParametersContainer = findUnsupportedParametersInDescriptor(descriptor);
        Set<String> references = findReferencesInDescriptor(descriptor);
        return filterUnsupportedButReferencedParameters(unknownParametersContainer, references);
    }

    private List<String> findUnsupportedParametersInDescriptor(DeploymentDescriptor descriptor) {
        List<String> unknownParametersContainer = new ArrayList<>();
        supportedParameterChecker.fillListWithUnmatched(descriptor, unknownParametersContainer);
        return unknownParametersContainer;
    }

    private Set<String> findReferencesInDescriptor(DeploymentDescriptor descriptor) {
        Set<String> referencedParametersContainer = new HashSet<>();
        new ReferencesFinder().fillWithReferences(descriptor, referencedParametersContainer);
        return referencedParametersContainer;
    }

    private List<String> filterUnsupportedButReferencedParameters(List<String> unsupportedParameters, Set<String> references) {
        return unsupportedParameters.stream()
                                    .filter(param -> !references.contains(param))
                                    .collect(Collectors.toList());
    }

}
