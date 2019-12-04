package com.sap.cloud.lm.sl.cf.core.model;

import java.util.stream.Stream;

public class BlueGreenApplicationNameSuffix {

    public static final BlueGreenApplicationNameSuffix OLD = new BlueGreenApplicationNameSuffix("old");
    public static final BlueGreenApplicationNameSuffix NEW = new BlueGreenApplicationNameSuffix("new");
    
    private final String value;

    private BlueGreenApplicationNameSuffix(String value) {
        this.value = value;
    }

    public String asSuffix() {
        return "-" + value;
    }

    public static boolean isSuffixContainedIn(String name) {
        return Stream.of(OLD, NEW)
                     .map(BlueGreenApplicationNameSuffix::asSuffix)
                     .anyMatch(name::endsWith);
    }

    public static String removeSuffix(String name) {
        if (isSuffixContainedIn(name)) {
            return name.substring(0, name.lastIndexOf('-'));
        }
        return name;
    }

}
