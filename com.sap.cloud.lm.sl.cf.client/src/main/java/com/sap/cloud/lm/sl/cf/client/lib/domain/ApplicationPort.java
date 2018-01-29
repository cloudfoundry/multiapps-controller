package com.sap.cloud.lm.sl.cf.client.lib.domain;

public class ApplicationPort {
    private int port;
    private ApplicationPortType portType;

    public ApplicationPort(int port, ApplicationPortType portType) {
        this.port = port;
        this.portType = portType;
    }

    public int getPort() {
        return port;
    }

    public ApplicationPortType getPortType() {
        return portType;
    }

    public enum ApplicationPortType {
        HTTP, TCP, TCPS
    }
}
