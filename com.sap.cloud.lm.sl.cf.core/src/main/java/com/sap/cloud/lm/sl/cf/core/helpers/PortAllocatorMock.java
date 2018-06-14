package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Set;
import java.util.TreeSet;

import org.cloudfoundry.client.lib.CloudFoundryException;

public class PortAllocatorMock implements PortAllocator {

    private Set<Integer> allocatedPorts = new TreeSet<>();
    private int minPort;

    public PortAllocatorMock(int minPort) {
        this.minPort = minPort;
    }

    @Override
    public void freeAllExcept(Set<Integer> ports) {
        allocatedPorts.retainAll(ports);
    }

    @Override
    public void freeAll() {
        allocatedPorts.clear();
    }

    @Override
    public int allocatePort() {
        int allocatedPort = minPort++;
        allocatedPorts.add(allocatedPort);
        return allocatedPort;
    }

    @Override
    public void setAllocatedPorts(Set<Integer> allocatedPorts) {
        this.allocatedPorts = allocatedPorts;
    }

    @Override
    public Set<Integer> getAllocatedPorts() {
        return allocatedPorts;
    }

    @Override
    public int allocateTcpPort(boolean tcps) throws CloudFoundryException {
        return allocatePort();
    }

}
