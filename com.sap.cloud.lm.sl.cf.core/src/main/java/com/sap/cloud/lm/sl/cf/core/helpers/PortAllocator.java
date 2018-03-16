package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Set;

import org.cloudfoundry.client.lib.CloudFoundryException;

public interface PortAllocator {

    int allocatePort() throws CloudFoundryException;

    int allocateTcpPort(boolean tcps) throws CloudFoundryException;

    void freeAll();

    void freeAllExcept(Set<Integer> ports);

    Set<Integer> getAllocatedPorts();

    void setAllocatedPorts(Set<Integer> allocatedPorts);

}
