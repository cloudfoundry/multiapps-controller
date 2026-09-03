package org.cloudfoundry.multiapps.controller.client.facade.domain;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.Messages;

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
                                                                                                 roleType -> roleType));

    public static UserRole fromRoleType(String roleTypeValue) {
        UserRole userRole = NAMES_TO_VALUES.get(roleTypeValue);

        if (userRole == null) {
            throw new IllegalArgumentException(MessageFormat.format(Messages.UNKNOWN_USER_ROLE_0, roleTypeValue));
        }

        return userRole;
    }

    public String getName() {
        return name().toLowerCase();
    }

}
