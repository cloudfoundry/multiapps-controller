package org.cloudfoundry.multiapps.controller.process.util;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.flowable.engine.delegate.DelegateExecution;

public class ResourcesGetter {

    private final DelegateExecution execution;

    public ResourcesGetter(DelegateExecution execution) {
        this.execution = execution;
    }

    public List<Resource> getResources() {
        return VariableHandling.get(execution, Variables.BATCHES_TO_PROCESS)
                               .stream()
                               .flatMap(List::stream)
                               .collect(Collectors.toList());
    }
}
