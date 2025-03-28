package org.cloudfoundry.multiapps.controller.core.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.SystemParameters;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class NameUtil {

    private NameUtil() {
    }

    public static String computeValidApplicationName(String applicationName, String namespace, boolean applyNamespace,
                                                     boolean applyNamespaceAsSuffix) {
        return computeNamespacedNameWithLength(applicationName, namespace, applyNamespace, applyNamespaceAsSuffix,
                                               NameRequirements.APP_NAME_MAX_LENGTH);
    }

    public static String computeValidServiceName(String serviceName, String namespace, boolean applyNamespace,
                                                 boolean applyNamespaceAsSuffix) {
        return computeNamespacedNameWithLength(serviceName, namespace, applyNamespace, applyNamespaceAsSuffix,
                                               NameRequirements.SERVICE_NAME_MAX_LENGTH);
    }

    public static String computeValidContainerName(String organization, String space, String serviceName) {
        String properOrganization = organization.toUpperCase(Locale.US)
                                                .replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        String properSpace = space.toUpperCase(Locale.US)
                                  .replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        String properServiceName = serviceName.toUpperCase(Locale.US)
                                              .replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        return getNameWithProperLength(String.format("%s_%s_%s", properOrganization, properSpace, properServiceName),
                                       NameRequirements.CONTAINER_NAME_MAX_LENGTH).toUpperCase(Locale.US);
    }

    public static String computeValidXsAppName(String serviceName) {
        if (serviceName.startsWith("sap_system")) {
            serviceName = serviceName.replaceFirst("sap_system", "");
        }
        if (serviceName.length() == 0) {
            serviceName = "_";
        }
        return getNameWithProperLength(serviceName.replaceAll(NameRequirements.XS_APP_NAME_ILLEGAL_CHARACTERS, "_"),
                                       NameRequirements.XS_APP_NAME_MAX_LENGTH);
    }

    public static String getNameWithProperLength(String name, int maxLength) {
        if (name.length() > maxLength) {
            return getShortenedName(name, maxLength);
        }
        return name;
    }

    public static String computeNamespacedNameWithLength(String name, String namespace, boolean applyNamespace,
                                                         boolean applyNamespaceAsSuffix, int maxLength) {
        if (StringUtils.isNotEmpty(namespace) && applyNamespace) {
            if (applyNamespaceAsSuffix) {
                name = getNameWithNamespaceSuffix(name, namespace, maxLength);
            } else {
                name = getNamespacePrefix(namespace) + name;
            }
        }
        return getNameWithProperLength(name, maxLength);
    }

    private static String getNameWithNamespaceSuffix(String name, String namespace, int maxLength) {
        String namespaceSuffix = getNamespaceSuffix(namespace);
        String shortenedName = getNameWithProperLength(name,
                                                       calculateNameLengthWithoutNamespaceAndBlueGreenSuffix(namespaceSuffix, maxLength));

        return correctNameSuffix(shortenedName, name, namespaceSuffix);
    }

    private static int calculateNameLengthWithoutNamespaceAndBlueGreenSuffix(String namespaceSuffix, int maxLengthWithSuffix) {
        //Here we use the "green" suffix because it is the longest out of all
        return maxLengthWithSuffix - (namespaceSuffix.length() + SystemParameters.GREEN_HOST_SUFFIX.length());
    }

    private static String correctNameSuffix(String name, String nameWithoutShortening, String namespaceSuffix) {
        if (nameWithoutShortening.endsWith(SystemParameters.IDLE_HOST_SUFFIX)) {
            name = placeNamespaceBeforeBlueGreenSuffix(name, namespaceSuffix, SystemParameters.IDLE_HOST_SUFFIX);
        } else if (nameWithoutShortening.endsWith(SystemParameters.BLUE_HOST_SUFFIX)) {
            name = placeNamespaceBeforeBlueGreenSuffix(name, namespaceSuffix, SystemParameters.BLUE_HOST_SUFFIX);
        } else if (nameWithoutShortening.endsWith(SystemParameters.GREEN_HOST_SUFFIX)) {
            name = placeNamespaceBeforeBlueGreenSuffix(name, namespaceSuffix, SystemParameters.GREEN_HOST_SUFFIX);
        } else {
            name += namespaceSuffix;
        }

        return name;
    }

    private static String placeNamespaceBeforeBlueGreenSuffix(String name, String namespaceSuffix, String blueGreenSuffix) {
        int lastOccurrenceOfBlueGreenSuffix = name.lastIndexOf(blueGreenSuffix);
        if (lastOccurrenceOfBlueGreenSuffix > -1) {
            name = name.substring(0, lastOccurrenceOfBlueGreenSuffix);
        }
        name += namespaceSuffix + blueGreenSuffix;
        return name;
    }

    public static String getNamespacePrefix(String namespace) {
        return namespace + Constants.NAMESPACE_SEPARATOR;
    }

    public static String getNamespaceSuffix(String namespace) {
        return Constants.NAMESPACE_SEPARATOR + namespace;
    }

    public static String computeUserNamespaceWithSystemNamespace(String systemNamespace, String userNamespace) {
        if (StringUtils.isNotEmpty(userNamespace)) {
            return systemNamespace + Constants.NAMESPACE_SEPARATOR + userNamespace;
        }
        return systemNamespace;
    }

    private static String getShortenedName(String name, int maxLength) {
        String nameHashCode = getHashCodeAsHexString(name);
        if (maxLength < nameHashCode.length()) {
            throw new SLException(Messages.CANNOT_SHORTEN_NAME_TO_N_CHARACTERS, name, maxLength);
        }
        return name.substring(0, maxLength - nameHashCode.length()) + nameHashCode;
    }

    private static String getHashCodeAsHexString(String s) {
        int hashCode = s.hashCode();
        if (hashCode == Integer.MIN_VALUE) {
            hashCode++;
        }
        return Integer.toString(Math.abs(hashCode), 16);
    }

    public static UUID getUUID(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    public static String getIndexedName(String resourceName, int index, int entriesCnt, String delimiter) {
        if (entriesCnt > 1) {
            return org.cloudfoundry.multiapps.mta.util.NameUtil.getPrefixedName(resourceName, Integer.toString(index), delimiter);
        }
        return resourceName;
    }

    public static String getApplicationName(Module module) {
        return (String) module.getParameters()
                              .get(SupportedParameters.APP_NAME);
    }

    public static String getServiceName(Resource resource) {
        return (String) resource.getParameters()
                                .get(SupportedParameters.SERVICE_NAME);
    }

    public static class NameRequirements {

        public static final String XS_APP_NAME_PATTERN = "(?!sap_system)[a-zA-Z0-9\\._\\-\\\\/]{1,240}";
        public static final String CONTAINER_NAME_PATTERN = "[A-Z0-9][_A-Z0-9]{0,63}";
        public static final int XS_APP_NAME_MAX_LENGTH = 240;
        public static final int APP_NAME_MAX_LENGTH = 1024;
        public static final int SERVICE_NAME_MAX_LENGTH = 50; // TODO: Make this configurable.
        public static final int CONTAINER_NAME_MAX_LENGTH = 64;
        public static final String ENVIRONMENT_NAME_ILLEGAL_CHARACTERS = "[^_a-zA-Z0-9]";
        public static final String XS_APP_NAME_ILLEGAL_CHARACTERS = "[^a-zA-Z0-9._\\-\\\\/]";
        public static final String CONTAINER_NAME_ILLEGAL_CHARACTERS = "[^_A-Z0-9]";

        private NameRequirements() {
        }

    }

}
