package com.sap.cloud.lm.sl.cf.core.filters;

import static com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil.TARGET_DELIMITER;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TargetWildcardFilter implements BiFunction<String, String, Boolean> {
    public static final String ANY_TARGET_WILDCARD = "\\*";
    public static final String ANY_ORG_REGEX = "(" + ANY_TARGET_WILDCARD + TARGET_DELIMITER + ")(.*)"; // '* space'
    public static final String ANY_SPACE_REGEX = "(.*)(" + TARGET_DELIMITER + ANY_TARGET_WILDCARD + ")";// 'org *'
    public static final String ANY_TARGET_REGEX = "(" + ANY_ORG_REGEX + ")|(" + ANY_SPACE_REGEX + ")";

    @Override
    public Boolean apply(String actualEntryTarget, String requestedTarget) {
        if (requestedTarget == null) {
            return true;
        }
        Matcher spaceMatcher = Pattern.compile(ANY_SPACE_REGEX).matcher(requestedTarget);
        Matcher orgMatcher = Pattern.compile(ANY_ORG_REGEX).matcher(requestedTarget);

        if (!orgMatcher.matches() && !spaceMatcher.matches()) {
            return actualEntryTarget.equals(requestedTarget);
        }
        if (orgMatcher.matches() && spaceMatcher.matches()) {
            // * *
            return true;
        }
        if (orgMatcher.matches()) {
            // * abc
            return actualEntryTarget.endsWith(TARGET_DELIMITER + orgMatcher.group(2));
        }
        // abc *
        return actualEntryTarget.startsWith(spaceMatcher.group(1) + TARGET_DELIMITER);
    }
}
