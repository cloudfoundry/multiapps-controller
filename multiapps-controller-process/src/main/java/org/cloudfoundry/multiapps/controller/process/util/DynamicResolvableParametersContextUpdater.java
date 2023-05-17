package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Set;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.DynamicResolvableParametersHelper;
import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDynamicResolvableParameter;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

public class DynamicResolvableParametersContextUpdater {

    private final ProcessContext context;

    public DynamicResolvableParametersContextUpdater(ProcessContext context) {
        this.context = context;
    }

    public void updateServiceGuid(CloudServiceInstanceExtended serviceInstance) {
        updateServiceGuid(serviceInstance, null);
    }

    public void updateServiceGuid(CloudServiceInstanceExtended serviceInstance, CloudServiceInstance existingService) {
        DynamicResolvableParameter dynamicResolvableParameter = getDynamicResolvableParameter(serviceInstance);
        if (dynamicResolvableParameter != null) {
            String serviceGuid = getServiceGuid(serviceInstance, existingService);
            dynamicResolvableParameter = ImmutableDynamicResolvableParameter.copyOf(dynamicResolvableParameter)
                                                                            .withValue(serviceGuid);
            context.setVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETER, dynamicResolvableParameter);
        }
    }

    private DynamicResolvableParameter getDynamicResolvableParameter(CloudServiceInstanceExtended serviceInstance) {
        Set<DynamicResolvableParameter> dynamicResolvableParameters = context.getVariable(Variables.DYNAMIC_RESOLVABLE_PARAMETERS);
        DynamicResolvableParametersHelper helper = new DynamicResolvableParametersHelper(dynamicResolvableParameters);
        return helper.findDynamicResolvableParameter(SupportedParameters.SERVICE_GUID, serviceInstance.getResourceName());
    }

    private String getServiceGuid(CloudServiceInstance serviceInstance, CloudServiceInstance existingService) {
        if (existingService == null) {
            return context.getControllerClient()
                          .getRequiredServiceInstanceGuid(serviceInstance.getName())
                          .toString();
        }
        return existingService.getGuid()
                              .toString();
    }

}
