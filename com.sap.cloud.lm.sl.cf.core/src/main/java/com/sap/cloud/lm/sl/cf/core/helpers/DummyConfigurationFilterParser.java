package com.sap.cloud.lm.sl.cf.core.helpers;

import org.cloudfoundry.multiapps.mta.model.Resource;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;

public class DummyConfigurationFilterParser extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser {

    private final ConfigurationFilter filter;

    public DummyConfigurationFilterParser(ConfigurationFilter filter) {
        super(null, null, null);
        this.filter = filter;
    }

    @Override
    public ConfigurationFilter parse(Resource resource) {
        return filter;
    }

}
