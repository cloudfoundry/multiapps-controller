package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.Constants;
import org.cloudfoundry.multiapps.controller.client.facade.util.UriUtil;

public final class V3Uri {

    private static final List<String> CHARS_TO_ENCODE = List.of(Constants.COMMA);

    private final String path;

    private final List<String> queryParameters = new ArrayList<>();

    private V3Uri(String path) {
        this.path = path;
    }

    public static V3Uri path(String template, Object... args) {
        return new V3Uri(fillPlaceholders(template, args));
    }

    public V3Uri perPage(int size) {
        queryParameters.add(Constants.PER_PAGE + size);
        return this;
    }

    public V3Uri query(String key, Object value) {
        if (value != null) {
            queryParameters.add(key + Constants.EQUALS + value);
        }

        return this;
    }

    public V3Uri queryEncoded(String key, Object value) {
        if (value != null) {
            queryParameters.add(key + Constants.EQUALS + encodeCommas(String.valueOf(value)));
        }

        return this;
    }

    public V3Uri query(String key, Collection<?> values) {
        if (values != null && !values.isEmpty()) {
            queryParameters.add(key + Constants.EQUALS + values.stream()
                                                               .map(String::valueOf)
                                                               .map(V3Uri::encodeCommas)
                                                               .collect(Collectors.joining(Constants.COMMA)));
        }

        return this;
    }

    public String build() {
        if (queryParameters.isEmpty()) {
            return path;
        }

        return path + Constants.QUESTION_MARK + String.join(Constants.AMPERSAND, queryParameters);
    }

    private static String encodeCommas(String value) {
        return UriUtil.encodeChars(value, CHARS_TO_ENCODE);
    }

    private static String fillPlaceholders(String template, Object... args) {
        StringBuilder result = new StringBuilder();

        int argumentIndex = 0;
        int nextSearchStart = 0;
        int openBraceIndex;

        while ((openBraceIndex = template.indexOf(Constants.OPEN_BRACE_CHAR, nextSearchStart)) >= 0) {
            int closeBraceIndex = template.indexOf(Constants.CLOSE_BRACE_CHAR, openBraceIndex);

            if (closeBraceIndex < 0) {
                break;
            }

            result.append(template, nextSearchStart, openBraceIndex)
                  .append(args[argumentIndex++]);

            nextSearchStart = closeBraceIndex + 1;
        }

        result.append(template, nextSearchStart, template.length());
        return result.toString();
    }

}
