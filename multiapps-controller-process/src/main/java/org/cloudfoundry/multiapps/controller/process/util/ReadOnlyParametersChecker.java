package org.cloudfoundry.multiapps.controller.process.util;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_MODULE_PARAMETERS;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_RESOURCE_PARAMETERS;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_SYSTEM_PARAMETERS;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MiscUtil;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.NamedParametersContainer;

@Named
public class ReadOnlyParametersChecker {

    private static final String LIVE_ROUTE_PARAMETER_NAME = "live-route";
    private static final String ROUTES_PARAMETER_NAME = "routes";

    public void check(DeploymentDescriptor descriptor) {
        Map<String, Set<String>> detectedReadOnlyParameters = new LinkedHashMap<>();
        checkForCommonParameters(new GlobalParameters(descriptor), READ_ONLY_SYSTEM_PARAMETERS, detectedReadOnlyParameters);
        checkCollectionForCommonParameters(descriptor.getModules(), READ_ONLY_MODULE_PARAMETERS, detectedReadOnlyParameters);
        checkModulesForLiveParameter(descriptor.getModules(), detectedReadOnlyParameters);
        checkCollectionForCommonParameters(descriptor.getResources(), READ_ONLY_RESOURCE_PARAMETERS, detectedReadOnlyParameters);
        if (!detectedReadOnlyParameters.isEmpty()) {
            throw new SLException(getFormattedOutput(detectedReadOnlyParameters));
        }
    }

    private void checkForCommonParameters(NamedParametersContainer namedParametersContainer, Set<String> readOnlyParameters,
                                          Map<String, Set<String>> commonReadOnlyParameters) {
        Set<String> commonParameters = SetUtils.intersection(namedParametersContainer.getParameters()
                                                                                     .keySet(), readOnlyParameters);
        if (!commonParameters.isEmpty()) {
            commonReadOnlyParameters.put(namedParametersContainer.getName(), commonParameters);
        }
    }

    private void checkModulesForLiveParameter(List<? extends NamedParametersContainer> namedElementsWithParametersContainers, Map<String, Set<String>> commonReadOnlyParameters) {
        for (NamedParametersContainer namedElement : namedElementsWithParametersContainers) {
            checkForLiveParameter(namedElement, commonReadOnlyParameters);
        }
    }

    private void checkForLiveParameter(NamedParametersContainer namedParametersContainer, Map<String, Set<String>> commonReadOnlyParameters) {
        Object routes = namedParametersContainer.getParameters().get(ROUTES_PARAMETER_NAME);

        if (Objects.nonNull(routes)) {
            for (var route: MiscUtil.<List<Map<String, Object>>>cast(routes)) {
                if (Objects.nonNull(route.get(LIVE_ROUTE_PARAMETER_NAME))) {
                    commonReadOnlyParameters.put(namedParametersContainer.getName(), Set.of(LIVE_ROUTE_PARAMETER_NAME));
                    return;
                }
            }
        }
    }

    private void checkCollectionForCommonParameters(List<? extends NamedParametersContainer> namedElementsWithParametersContainers,
                                                    Set<String> readOnlyParameters, Map<String, Set<String>> commonReadOnlyParameters) {
        for (NamedParametersContainer namedElement : namedElementsWithParametersContainers) {
            checkForCommonParameters(namedElement, readOnlyParameters, commonReadOnlyParameters);
        }
    }

    private String getFormattedOutput(Map<String, Set<String>> readOnlyParameters) {
        return System.lineSeparator() + readOnlyParameters.entrySet()
                                                          .stream()
                                                          .map(entry -> MessageFormat.format(Messages.PARAMETERS_HAVE_READ_ONLY_ELEMENTS,
                                                                                             entry.getKey(), entry.getValue()))
                                                          .collect(Collectors.joining(System.lineSeparator()));
    }

}
