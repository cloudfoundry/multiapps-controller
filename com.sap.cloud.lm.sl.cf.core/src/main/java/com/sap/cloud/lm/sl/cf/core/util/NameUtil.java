package com.sap.cloud.lm.sl.cf.core.util;

import static com.sap.cloud.lm.sl.mta.util.ValidatorUtil.getPrefixedName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public class NameUtil {

    public static class NameRequirements {

        public static final String ENVIRONMENT_NAME_PATTERN = "[_a-zA-Z][_a-zA-Z0-9]{0,63}";
        public static final String XS_APP_NAME_PATTERN = "(?!sap_system)[a-zA-Z0-9\\._\\-\\\\/]{1,240}";
        public static final String CONTAINER_NAME_PATTERN = "[A-Z0-9][_A-Z0-9]{0,63}";

        public static final int ENVIRONMENT_NAME_MAX_LENGTH = 64;
        public static final int XS_APP_NAME_MAX_LENGTH = 240;
        public static final int APP_NAME_MAX_LENGTH = 1024;
        public static final int SERVICE_NAME_MAX_LENGTH = 50; // TODO: Make this configurable.
        public static final int CONTAINER_NAME_MAX_LENGTH = 64;

        public static final String ENVIRONMENT_NAME_ILLEGAL_CHARACTERS = "[^_a-zA-Z0-9]";
        public static final String XS_APP_NAME_ILLEGAL_CHARACTERS = "[^a-zA-Z0-9\\._\\-\\\\/]";
        public static final String CONTAINER_NAME_ILLEGAL_CHARACTERS = "[^_A-Z0-9]";

    }

    public static String getNamespacePrefix(String namespaceId) {
        return namespaceId + ".";
    }

    public static String getWithoutNamespacePrefix(String name) {
        int i = name.lastIndexOf(".");
        return (i >= 0) ? name.substring(i + 1) : name;
    }

    public static boolean isValidName(String name, String namePattern) {
        return name.matches(namePattern);
    }

    public static void ensureValidEnvName(String name, boolean shouldAllowInvalidEnvNames) {
        if (name.matches(NameRequirements.ENVIRONMENT_NAME_PATTERN) || shouldAllowInvalidEnvNames) {
            return;
        }
        throw new ContentException(Messages.INVALID_ENVIRONMENT_VARIABLE_NAME, name);
    }

    public static String getApplicationName(String moduleName, String mtaId, boolean useNamespaces) throws SLException {
        String prefix = (useNamespaces ? getNamespacePrefix(mtaId) : "");
        return prefix + getNameWithProperLength(moduleName, NameRequirements.APP_NAME_MAX_LENGTH - prefix.length());
    }

    public static String getServiceName(String resourceName, String mtaId, boolean useNamespaces, boolean useNamespacesForServices)
        throws SLException {
        String prefix = (useNamespaces && useNamespacesForServices ? getNamespacePrefix(mtaId) : "");
        return prefix + getNameWithProperLength(resourceName, NameRequirements.SERVICE_NAME_MAX_LENGTH - prefix.length());
    }

    public static String createValidContainerName(String organization, String space, String serviceName) throws SLException {
        String properOrganization = organization.toUpperCase(Locale.US).replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        String properSpace = space.toUpperCase(Locale.US).replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        String properServiceName = serviceName.toUpperCase(Locale.US).replaceAll(NameRequirements.CONTAINER_NAME_ILLEGAL_CHARACTERS, "_");
        return getNameWithProperLength(String.format("%s_%s_%s", properOrganization, properSpace, properServiceName),
            NameRequirements.CONTAINER_NAME_MAX_LENGTH).toUpperCase(Locale.US);
    }

    public static String createValidXsAppName(String serviceName) throws SLException {
        if (serviceName.startsWith("sap_system")) {
            serviceName = serviceName.replaceFirst("sap_system", "");
        }
        if (serviceName.length() == 0) {
            serviceName = "_";
        }
        return getNameWithProperLength(serviceName.replaceAll(NameRequirements.XS_APP_NAME_ILLEGAL_CHARACTERS, "_"),
            NameRequirements.XS_APP_NAME_MAX_LENGTH);
    }

    public static String getNameWithProperLength(String name, int maxLength) throws SLException {
        if (name.length() > maxLength) {
            return getShortenedName(name, maxLength);
        }
        return name;
    }

    private static String getShortenedName(String name, int maxLength) throws SLException {
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
            return getPrefixedName(resourceName, Integer.toString(index), delimiter);
        }
        return resourceName;
    }
    
    public static List<String> splitFilesIds(List<String> fileIds) {
        List<String> allFileIds = new ArrayList<>();
        for (String fileId : fileIds) {
            if (fileId == null || !fileId.contains(",")) {
                allFileIds.add(fileId);
            } else {
                List<String> fileInChunks = Arrays.asList(fileId.split(","));
                allFileIds.addAll(fileInChunks);
            }
        }
        return allFileIds;
    }
}
