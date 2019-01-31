package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.sap.cloud.lm.sl.cf.core.util.UriUtil;

public class OccupiedPortsDetector {

    public static Set<Integer> detectOccupiedPorts(List<String> applicationUris) {
        Set<Integer> occupiedPorts = new TreeSet<>();
        for (String uri : applicationUris) {
            Integer port = UriUtil.getPort(uri);
            if (port != null) {
                occupiedPorts.add(port);
            }
        }
        return occupiedPorts;
    }
}
