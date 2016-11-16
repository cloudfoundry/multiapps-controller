package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Set;
import java.util.TreeSet;

public class PortAllocatorMock implements PortAllocator {

    private Set<Integer> allocatedPorts = new TreeSet<Integer>();
    private int minPort;

    public PortAllocatorMock(int minPort, int maxPort) {
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

}
