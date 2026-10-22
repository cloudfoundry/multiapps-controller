package org.cloudfoundry.multiapps.controller.process.client;

import java.util.ArrayList;
import java.util.List;

public class DryRunChangeSet {

    public List<String> appsCreate = new ArrayList<>();
    public List<String> appsUpdate = new ArrayList<>();
    public List<String> appsDelete = new ArrayList<>();
    public List<String> servicesCreate = new ArrayList<>();
    public List<String> servicesUpdate = new ArrayList<>();
    public List<String> servicesDelete = new ArrayList<>();
    public List<String> routesCreate = new ArrayList<>();
    public List<String> routesDelete = new ArrayList<>();

    public DryRunChangeSet() {
    }
}
