package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("UnsupportedParameterFinder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnsupportedParameterFinder {

    @Inject
    private SupportedParameterChecker supportedParameterChecker;

    @Inject
    private ReferenceContainerMatcher referenceMatcher;

    @Inject
    private CustomParameterContainerProcessor containerProcessor;
    
    public Map<String, List<String>> findUnsupportedParameters(DeploymentDescriptor descriptor, List<ReferenceContainer> references) {
        List<CustomParameterContainer> unknownParametersContainer = supportedParameterChecker.getCustomParameters(descriptor);
        return collectUnsupportedParametersByOwner(unknownParametersContainer, references);
    }

    private Map<String, List<String>> collectUnsupportedParametersByOwner(List<CustomParameterContainer> containers,
                                                                          List<ReferenceContainer> references) {
        return containers.stream()
                         .map(container -> containerProcessor.processCustomParameterContainer(container, references, referenceMatcher))
                         .filter(entry -> !entry.getValue()
                                                .isEmpty())
                         .collect(unsupportedParametersCollector());
    }

    private Collector<Map.Entry<String, List<String>>, ?, Map<String, List<String>>> unsupportedParametersCollector() {
        return Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            this::mergeParameterLists
        );
    }

    private List<String> mergeParameterLists(List<String> existing, List<String> replacement) {
        existing.addAll(replacement);
        return existing;
    }

}
