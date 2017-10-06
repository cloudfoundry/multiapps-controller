package com.sap.cloud.lm.sl.cf.core.helpers.v3_0;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.mta.model.v3_0.Resource.ResourceBuilder;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao) {
        super(dao);
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

}
