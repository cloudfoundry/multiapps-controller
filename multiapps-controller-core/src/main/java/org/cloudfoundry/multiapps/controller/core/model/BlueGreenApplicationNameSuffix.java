package org.cloudfoundry.multiapps.controller.core.model;

import java.util.stream.Stream;

public class BlueGreenApplicationNameSuffix {

    public static final BlueGreenApplicationNameSuffix LIVE = new BlueGreenApplicationNameSuffix("live");
    public static final BlueGreenApplicationNameSuffix IDLE = new BlueGreenApplicationNameSuffix("idle");

    private final String value;

    private BlueGreenApplicationNameSuffix(String value) {
        this.value = value;
    }

    public String asSuffix() {
        return "-" + value;
    }

    public static boolean isSuffixContainedIn(String name) {
        return Stream.of(LIVE, IDLE)
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
