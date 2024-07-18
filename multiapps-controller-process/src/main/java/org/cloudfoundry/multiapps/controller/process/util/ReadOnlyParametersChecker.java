package org.cloudfoundry.multiapps.controller.process.util;

import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_MODULE_PARAMETERS;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_RESOURCE_PARAMETERS;
import static org.cloudfoundry.multiapps.controller.core.model.SupportedParameters.READ_ONLY_SYSTEM_PARAMETERS;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Named;

import org.apache.commons.collections4.SetUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.NamedParametersContainer;

@Named("readOnlyParametersChecker")
public class ReadOnlyParametersChecker {

    public void check(DeploymentDescriptor descriptor) {
        Map<String, Set<String>> detectedReadOnlyParameters = new LinkedHashMap<>();
        checkForCommonParameters(new GlobalParameters(descriptor), getReadOnlySystemParameters(), detectedReadOnlyParameters);
        checkCollectionForCommonParameters(descriptor.getModules(), getReadOnlyModuleParameters(), detectedReadOnlyParameters);
        checkCollectionForCommonParameters(descriptor.getResources(), getReadOnlyResourceParameters(), detectedReadOnlyParameters);
        if (!detectedReadOnlyParameters.isEmpty()) {
            throw new SLException(getFormattedOutput(detectedReadOnlyParameters));
        }
    }

    protected Set<String> getReadOnlySystemParameters() {
        return READ_ONLY_SYSTEM_PARAMETERS;
    }

    protected Set<String> getReadOnlyModuleParameters() {
        return READ_ONLY_MODULE_PARAMETERS;
    }

    protected Set<String> getReadOnlyResourceParameters() {
        return READ_ONLY_RESOURCE_PARAMETERS;
    }

    private void checkForCommonParameters(NamedParametersContainer namedParametersContainer, Set<String> readOnlyParameters,
                                          Map<String, Set<String>> commonReadOnlyParameters) {
        Set<String> commonParameters = SetUtils.intersection(namedParametersContainer.getParameters()
                                                                                     .keySet(),
                                                             readOnlyParameters);
        if (!commonParameters.isEmpty()) {
            commonReadOnlyParameters.put(namedParametersContainer.getName(), commonParameters);
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
