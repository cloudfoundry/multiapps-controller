package com.sap.cloud.lm.sl.cf.core.helpers;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class DummyConfigurationFilterParser extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser {

    private ConfigurationFilter filter;

    public DummyConfigurationFilterParser(ConfigurationFilter filter) {
        super(null, null);
        this.filter = filter;
    }

    @Override
    public ConfigurationFilter parse(Resource resource) {
        return filter;
    }

}
