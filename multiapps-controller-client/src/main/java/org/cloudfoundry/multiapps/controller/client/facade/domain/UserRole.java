package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum UserRole {

    ORGANIZATION_AUDITOR,
    ORGANIZATION_BILLING_MANAGER,
    ORGANIZATION_MANAGER,
    ORGANIZATION_USER,
    SPACE_AUDITOR,
    SPACE_DEVELOPER,
    SPACE_MANAGER;

    private static final Map<String, UserRole> NAMES_TO_VALUES = Arrays.stream(values())
                                                                       .collect(Collectors.toMap(UserRole::getName,
                                                                                                 roleType ->  roleType));

    /**
     * Resolve from the CF v3 role {@code type} wire value (e.g. {@code space_developer}). Takes the raw string rather than the OSS
     * {@code RoleType} enum, so the domain no longer depends on cf-java-client.
     */
    public static UserRole fromRoleType(String roleTypeValue) {
        UserRole userRole = NAMES_TO_VALUES.get(roleTypeValue);
        if (userRole == null) {
            throw new IllegalArgumentException("Unknown user role: " + roleTypeValue);
        }
        return userRole;
    }

    public String getName() {
        return name().toLowerCase();
    }

}
