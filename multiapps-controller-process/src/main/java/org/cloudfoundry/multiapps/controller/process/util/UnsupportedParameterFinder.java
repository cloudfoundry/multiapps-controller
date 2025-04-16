package org.cloudfoundry.multiapps.controller.process.util;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.resolvers.CustomParameterContainer;
import org.cloudfoundry.multiapps.mta.resolvers.Reference;
import org.cloudfoundry.multiapps.mta.resolvers.ReferenceContainer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@Named("UnsupportedParameterFinder")
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class UnsupportedParameterFinder {

    @Inject
    private SupportedParameterChecker supportedParameterChecker;

    private static final String GLOBAL_PARAMETER = "global-parameter";

    public Map<String, List<String>> findUnsupportedParameters(DeploymentDescriptor descriptor, List<ReferenceContainer> references) {
        List<CustomParameterContainer> unknownParametersContainer = findUnsupportedParametersInDescriptor(descriptor);
        return collectUnsupportedParametersByOwner(unknownParametersContainer, references);
    }

    private List<CustomParameterContainer> findUnsupportedParametersInDescriptor(DeploymentDescriptor descriptor) {
        return supportedParameterChecker.getCustomParameters(descriptor);
    }

    private Map<String, List<String>> collectUnsupportedParametersByOwner(List<CustomParameterContainer> containers,
                                                                          List<ReferenceContainer> references) {
        return containers.stream()
                         .map(container -> toFilteredEntry(container, references))
                         .filter(this::hasUnsupportedParameters)
                         .collect(unsupportedParametersCollector());
    }

    private Map.Entry<String, List<String>> toFilteredEntry(CustomParameterContainer container, List<ReferenceContainer> references) {
        List<String> unreferenced = filterUnreferencedParameters(container, references);
        String prefixedName = container.getPrefixedName() != null ? container.getPrefixedName() : GLOBAL_PARAMETER;
        return new AbstractMap.SimpleEntry<>(prefixedName, unreferenced);
    }

    private List<String> filterUnreferencedParameters(CustomParameterContainer container, List<ReferenceContainer> references) {
        return container.getParameters()
                        .stream()
                        .filter(parameter -> !isParameterReferenced(references, container, parameter))
                        .collect(Collectors.toList());
    }

    private boolean isParameterReferenced(List<ReferenceContainer> references,
                                          CustomParameterContainer container,
                                          String customParameter) {
        return references.stream()
                         .anyMatch(referenceContainer -> isReferenceContainerMatching(referenceContainer, container, customParameter));
    }

    private boolean isReferenceContainerMatching(ReferenceContainer referenceContainer,
                                                 CustomParameterContainer container,
                                                 String customParameter) {
        if (!hasMatchingReference(referenceContainer, container, customParameter)) {
            return false;
        }

        return isReferenceOwnerCompatible(referenceContainer.getReferenceOwner(), container.getParameterOwner());
    }

    private boolean hasMatchingReference(ReferenceContainer referenceContainer,
                                         CustomParameterContainer container,
                                         String customParameter) {
        return referenceContainer.getParameters()
                                 .stream()
                                 .anyMatch(reference -> isReferenceNameMatched(reference, customParameter)
                                     && isReferenceDependencyMatched(reference, container));
    }

    private boolean isReferenceOwnerCompatible(String referenceOwner, String parameterOwner) {
        return parameterOwner == null || parameterOwner.equals(referenceOwner);
    }

    private boolean isReferenceNameMatched(Reference reference, String customParameter) {
        return reference.getKey()
                        .equals(customParameter);
    }

    private boolean isReferenceDependencyMatched(Reference reference, CustomParameterContainer container) {
        return reference.getDependencyName() == null || reference.getDependencyName()
                                                                 .equals(container.getParameterOwner());
    }

    private boolean hasUnsupportedParameters(Map.Entry<String, List<String>> entry) {
        return !entry.getValue()
                     .isEmpty();
    }

    private Collector<Map.Entry<String, List<String>>, ?, Map<String, List<String>>> unsupportedParametersCollector() {
        return Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue,
            (existing, replacement) -> {
                existing.addAll(replacement);
                return existing;
            }
        );
    }

}
