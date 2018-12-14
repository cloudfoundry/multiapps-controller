package com.sap.cloud.lm.sl.cf.core.model;

import java.util.HashSet;
import java.util.Set;

public class ModuleToDeploy {

    private String name;
    private String type;
    private Set<String> deployedAfter = new HashSet<>();
    
    public ModuleToDeploy(String name, String type) {
        this.name = name;
        this.type = type;
    }
    
    public ModuleToDeploy(String name, String type, Set<String> deployedAfter) {
        this.name = name;
        this.type = type;
        this.deployedAfter = deployedAfter;
    }
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<String> getDeployedAfter() {
        return deployedAfter;
    }

    public void setDeployedAfter(Set<String> deployedAfter) {
        this.deployedAfter = deployedAfter;
    }
    
}
