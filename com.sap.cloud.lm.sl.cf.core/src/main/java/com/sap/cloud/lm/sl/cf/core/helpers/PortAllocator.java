package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.Set;

public interface PortAllocator {

    int allocatePort(String module);

    int allocateTcpPort(String module, boolean tcps);

    void freeAll();

    void freeUnusedForModule(String module, Set<Integer> usedPorts);

    Map<String, Set<Integer>> getAllocatedPorts();

    void setAllocatedPorts(Map<String, Set<Integer>> allocatedPorts);

}
