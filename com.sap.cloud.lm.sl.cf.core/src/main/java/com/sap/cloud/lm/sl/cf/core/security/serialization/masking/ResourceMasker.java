package com.sap.cloud.lm.sl.cf.core.security.serialization.masking;

import com.sap.cloud.lm.sl.mta.model.Resource;

public class ResourceMasker extends AbstractMasker<Resource> {

    public void mask(Resource resource) {
        maskParameters(resource);
        maskProperties(resource);
    }
}
