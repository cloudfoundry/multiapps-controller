package org.cloudfoundry.multiapps.controller.core.helpers;

import org.cloudfoundry.multiapps.controller.core.model.ConfigurationFilter;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class DummyConfigurationFilterParser extends org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationFilterParser {

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
