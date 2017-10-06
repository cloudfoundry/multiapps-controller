package com.sap.cloud.lm.sl.cf.core.files;

public class FilePart {
    private String name;
    private String path;

    public FilePart(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

}
