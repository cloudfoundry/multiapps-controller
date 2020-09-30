package org.cloudfoundry.multiapps.controller.persistence.model.filters;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.apache.commons.collections4.MapUtils;
import org.cloudfoundry.multiapps.common.ParsingException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

public class ContentFilter implements BiPredicate<String, Map<String, Object>> {

    @Override
    public boolean test(String content, Map<String, Object> requiredProperties) {
        if (MapUtils.isEmpty(requiredProperties)) {
            return true;
        }
        Map<String, Object> parsedContent = getParsedContent(content);
        if (MapUtils.isEmpty(parsedContent)) {
            return false;
        }
        return requiredProperties.entrySet()
                                 .stream()
                                 .allMatch(requiredEntry -> exists(parsedContent, requiredEntry));
    }

    private boolean exists(Map<String, Object> content, Map.Entry<String, Object> requiredEntry) {
        Object actualValue = content.get(requiredEntry.getKey());
        return Objects.equals(actualValue, requiredEntry.getValue());
    }

    private Map<String, Object> getParsedContent(String content) {
        if (content == null) {
            return Collections.emptyMap();
        }
        try {
            return JsonUtil.convertJsonToMap(content);
        } catch (ParsingException e) {
            return null;
        }
    }

}