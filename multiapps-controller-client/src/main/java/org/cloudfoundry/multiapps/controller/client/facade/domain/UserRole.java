package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.roles.RoleType;

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

    public static UserRole fromRoleType(RoleType roleType) {
        UserRole userRole = NAMES_TO_VALUES.get(roleType.getValue());
        if (userRole == null) {
            throw new IllegalArgumentException("Unknown user role: " + roleType.getValue());
        }
        return userRole;
    }

    public String getName() {
        return name().toLowerCase();
    }

}
