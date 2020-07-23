package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import org.cloudfoundry.multiapps.mta.model.Resource;

public class ResourceMasker extends AbstractMasker<Resource> {

    public void mask(Resource resource) {
        maskParameters(resource);
        maskProperties(resource);
    }
}
