package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Set;

import org.cloudfoundry.client.lib.CloudOperationException;

public interface PortAllocator {

    int allocatePort() throws CloudOperationException;

    int allocateTcpPort(boolean tcps) throws CloudOperationException;

    void freeAll();

    void freeAllExcept(Set<Integer> ports);

    Set<Integer> getAllocatedPorts();

    void setAllocatedPorts(Set<Integer> allocatedPorts);

}
