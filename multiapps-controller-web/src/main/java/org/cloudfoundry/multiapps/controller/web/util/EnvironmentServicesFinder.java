package org.cloudfoundry.multiapps.controller.web.util;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import io.pivotal.cfenv.core.CfService;
import io.pivotal.cfenv.jdbc.CfJdbcEnv;
import io.pivotal.cfenv.jdbc.CfJdbcService;

@Named
public class EnvironmentServicesFinder {

    private final CfJdbcEnv env;

    public EnvironmentServicesFinder() {
        this(new CfJdbcEnv());
    }

    public EnvironmentServicesFinder(CfJdbcEnv env) {
        this.env = env;
    }

    public CfJdbcService findJdbcService(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        try {
            return env.findJdbcServiceByName(name);
        } catch (IllegalArgumentException e) { // Thrown when there are 0 or more matches for the specified name.
            return null;
        }
    }

    public CfService findService(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        /*
         * findServiceByName throws an exception when there are 0 or more matches for the specified name. We're using findServicesByName
         * (plural) to avoid catching that exception.
         */
        return CollectionUtils.firstElement(env.findServicesByName(name));
    }

}
