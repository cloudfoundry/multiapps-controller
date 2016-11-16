package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.util.UriUtil;

public class OccupiedPortsDetector {

    public Map<String, List<Integer>> detectOccupiedPorts(List<CloudApplication> applications) {
        Map<String, List<Integer>> occupiedPorts = new TreeMap<>();
        for (CloudApplication application : applications) {
            occupiedPorts.put(application.getName(), getOccupiedPorts(application));
        }
        return occupiedPorts;
    }

    private List<Integer> getOccupiedPorts(CloudApplication application) {
        List<Integer> occupiedPorts = new ArrayList<>();
        for (String uri : getApplicationUris(application)) {
            Integer port = getPort(uri);
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

    private Integer getPort(String uri) {
        try {
            int port = Integer.parseInt(UriUtil.getHostAndDomain(uri)._1);
            if (UriUtil.isValidPort(port)) {
                return port;
            }
            return null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
