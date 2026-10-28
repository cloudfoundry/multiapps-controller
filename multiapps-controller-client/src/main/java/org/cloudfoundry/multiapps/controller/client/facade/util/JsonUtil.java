package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Some JSON helper utilities used by the Cloud Foundry Java client.
 *
 */
public class JsonUtil {

    public static final MediaType JSON_MEDIA_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
                                                                  MediaType.APPLICATION_JSON.getSubtype(),
                                                                  StandardCharsets.UTF_8);

    protected static final Log logger = LogFactory.getLog(JsonUtil.class);

    private static final ObjectMapper mapper = JsonMapper.builder()
                                                         .build();

    public static List<String> convertJsonToList(String json) {
        List<String> retList = new ArrayList<>();
        if (json != null) {
            try {
                retList = mapper.readValue(json, new TypeReference<List<String>>() {
                });
            } catch (JacksonException e) {
                logger.warn("Error while reading Java List from JSON response: " + json, e);
            }
        }
        return retList;
    }

    public static Map<String, Object> convertJsonToMap(String json) {
        Map<String, Object> retMap = new HashMap<>();
        if (json != null) {
            try {
                retMap = mapper.readValue(json, new TypeReference<Map<String, Object>>() {
                });
            } catch (JacksonException e) {
                logger.warn("Error while reading Java Map from JSON response: " + json, e);
            }
        }
        return retMap;
    }

    public static String convertToJson(Object value) {
        return convertToJson(value, false);
    }

    public static String convertToJson(Object value, boolean shouldPrettyPrint) {
        if (shouldPrettyPrint) {
            return convertToJsonStringUsingWriter(mapper.writerWithDefaultPrettyPrinter(), value);
        }

        return convertToJsonStringUsingWriter(mapper.writer(), value);
    }

    private static String convertToJsonStringUsingWriter(ObjectWriter writer, Object value) {
        try {
            return writer.writeValueAsString(value);
        } catch (JacksonException e) {
            logger.warn("Error while serializing " + value + " to JSON", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> convertToJsonList(InputStream jsonInputStream) {
        try {
            return mapper.readValue(jsonInputStream, List.class);
        } catch (JacksonException e) {
            logger.error("Unable to parse JSON from InputStream", e);
            throw new IllegalArgumentException("Unable to parse JSON from InputStream", e);
        }
    }

}
