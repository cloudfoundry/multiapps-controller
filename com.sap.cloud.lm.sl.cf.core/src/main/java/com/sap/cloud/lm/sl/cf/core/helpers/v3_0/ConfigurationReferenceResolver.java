package com.sap.cloud.lm.sl.cf.core.helpers.v3_0;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.v3_0.Resource.Builder;

public class ConfigurationReferenceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationReferenceResolver {

    public ConfigurationReferenceResolver(ConfigurationEntryDao dao, ApplicationConfiguration configuration) {
        super(dao, configuration);
    }

    @Override
    protected Builder getResourceBuilder() {
        return new Builder();
    }

}
