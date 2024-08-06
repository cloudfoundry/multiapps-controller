package org.cloudfoundry.multiapps.controller.persistence.services;

public enum ProcessLoggerMethodNamesEnum {

    INFO("info"), WARN("warn"), TRACE("trace"), ERROR("error"), DEBUG("debug");

    private final String name;
    ProcessLoggerMethodNamesEnum(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
