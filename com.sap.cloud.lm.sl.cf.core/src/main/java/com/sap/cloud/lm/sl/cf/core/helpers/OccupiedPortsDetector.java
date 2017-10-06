package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.util.UriUtil;

public class OccupiedPortsDetector {

    public List<Integer> detectOccupiedPorts(CloudApplication application) {
        List<Integer> occupiedPorts = new ArrayList<>();
        for (String uri : getApplicationUris(application)) {
            Integer port = UriUtil.getPort(uri);
            if (port != null) {
                occupiedPorts.add(port);
            }
        }
        return occupiedPorts;
    }

    private List<String> getApplicationUris(CloudApplication application) {
        List<String> uris = application.getUris();
        if (uris == null) {
            return Collections.emptyList();
        }
        return uris;
    }

}
