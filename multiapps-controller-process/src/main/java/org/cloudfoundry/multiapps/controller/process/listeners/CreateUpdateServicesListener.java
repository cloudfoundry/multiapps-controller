package org.cloudfoundry.multiapps.controller.process.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.process.Constants;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;

@Named("createUpdateServicesListener")
public class CreateUpdateServicesListener extends AbstractProcessExecutionListener {

    private static final long serialVersionUID = 1L;

    @Override
    protected void notifyInternal(DelegateExecution execution) throws Exception {
        List<CloudServiceInstanceExtended> services = VariableHandling.get(execution, Variables.SERVICES_TO_CREATE);
        setCloudServiceInstancesInParentProcess(execution, services);
        setDynamicResolvableParametersInParentProcess(execution, services);
    }

    private void setCloudServiceInstancesInParentProcess(DelegateExecution execution, List<CloudServiceInstanceExtended> services) {
        for (var service : services) {
            var variableName = DetermineServiceCreateUpdateActionsListener.buildExportedVariableName(service.getName());
            setVariableInParentProcess(execution, variableName, StepsUtil.getObject(execution, variableName));
        }
    }

    private void setDynamicResolvableParametersInParentProcess(DelegateExecution execution, List<CloudServiceInstanceExtended> services) {
        Set<DynamicResolvableParameter> dynamicResolvableParametersFromSubProcesses = getDynamicResolvableParametersFromSubProcesses(execution,
                                                                                                                                     services);

        Set<DynamicResolvableParameter> resolvedParameters = new HashSet<>(VariableHandling.get(execution,
                                                                                                Variables.DYNAMIC_RESOLVABLE_PARAMETERS));

        setValueInDynamicResolvableParameters(execution, dynamicResolvableParametersFromSubProcesses, resolvedParameters);

        setVariableInParentProcess(execution, Variables.DYNAMIC_RESOLVABLE_PARAMETERS.getName(),
                                   Variables.DYNAMIC_RESOLVABLE_PARAMETERS.getSerializer()
                                                                          .serialize(resolvedParameters));
        execution.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS.getName(), Variables.DYNAMIC_RESOLVABLE_PARAMETERS.getSerializer()
                                                                                                                        .serialize(resolvedParameters));
    }

    private Set<DynamicResolvableParameter> getDynamicResolvableParametersFromSubProcesses(DelegateExecution execution,
                                                                                           List<CloudServiceInstanceExtended> services) {
        return services.stream()
                       .map(CloudServiceInstanceExtended::getName)
                       .map(serviceName -> Constants.VAR_SERVICE_INSTANCE_GUID_PREFIX + serviceName)
                       .map(serviceGuidConstant -> StepsUtil.getObject(execution, serviceGuidConstant))
                       .filter(Objects::nonNull)
                       .map(dynamicResolvableParameterObject -> Variables.DYNAMIC_RESOLVABLE_PARAMETER.getSerializer()
                                                                                                      .deserialize(dynamicResolvableParameterObject))
                       .collect(Collectors.toSet());
    }

    private void setValueInDynamicResolvableParameters(DelegateExecution execution,
                                                       Set<DynamicResolvableParameter> dynamicResolvableParametersFromSubProcesses,
                                                       Set<DynamicResolvableParameter> resolvedParameters) {
        DynamicResolvableParametersHelper helper = new DynamicResolvableParametersHelper(dynamicResolvableParametersFromSubProcesses);
        for (DynamicResolvableParameter parameter : VariableHandling.get(execution, Variables.DYNAMIC_RESOLVABLE_PARAMETERS)) {
            DynamicResolvableParameter resolvedParameter = helper.findDynamicResolvableParameter(parameter.getParameterName(),
                                                                                                 parameter.getRelationshipEntityName());
            if (resolvedParameter != null) {
                resolvedParameters.remove(parameter);
                resolvedParameters.add(resolvedParameter);
            }
        }
    }

}
