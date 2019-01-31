package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.CollectionUtils;

public class PortAllocatorMock implements PortAllocator {

    private Map<String, Set<Integer>> allocatedPorts = new HashMap<String, Set<Integer>>();
    private int minPort;

    public PortAllocatorMock(int minPort) {
        this.minPort = minPort;
    }

    @Override
    public void freeUnusedForModule(String module, Set<Integer> ports) {
        Set<Integer> allocatedPortsForModule = getAllocatedPortsForModule(module);
        allocatedPortsForModule.retainAll(ports);
        allocatedPorts.put(module, allocatedPortsForModule);
    }

    @Override
    public void freeAll() {
        allocatedPorts.clear();
    }

    @Override
    public int allocatePort(String module) {
        int allocatedPort = minPort++;
        addPortForModule(module, allocatedPort);
        return allocatedPort;
    }

    @Override
    public void setAllocatedPorts(Map<String, Set<Integer>> allocatedPorts) {
        this.allocatedPorts = allocatedPorts;
    }

    @Override
    public Map<String, Set<Integer>> getAllocatedPorts() {
        return allocatedPorts;
    }

    @Override
    public int allocateTcpPort(String module, boolean tcps) {
        return allocatePort(module);
    }

    private Set<Integer> getAllocatedPortsForModule(String module) {
        if (CollectionUtils.isEmpty(allocatedPorts.get(module))) {
            return new TreeSet<Integer>();
        }
        return allocatedPorts.get(module);
    }
    
    private void addPortForModule(String module, int port) {
        Set<Integer> allocatedPortsForModule = getAllocatedPortsForModule(module);
        allocatedPortsForModule.add(port);
        allocatedPorts.put(module, allocatedPortsForModule);
    }

}
