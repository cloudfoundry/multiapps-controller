package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Set;

public interface PortAllocator {

    int allocatePort();

    int allocateTcpPort(boolean tcps);

    void freeAll();

    void freeAllExcept(Set<Integer> ports);

    Set<Integer> getAllocatedPorts();

    void setAllocatedPorts(Set<Integer> allocatedPorts);

}
