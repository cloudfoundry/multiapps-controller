package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import java.util.Collections;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.DynamicResolvableParameter;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class DynamicResolvableParametersFactory {

    protected final DeploymentDescriptor descriptor;

    public DynamicResolvableParametersFactory(DeploymentDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public Set<DynamicResolvableParameter> create() {
        return Collections.emptySet();
    }

}
