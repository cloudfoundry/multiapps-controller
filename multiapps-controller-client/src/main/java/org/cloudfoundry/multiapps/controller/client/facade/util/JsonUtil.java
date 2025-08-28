package org.cloudfoundry.multiapps.controller.client.facade.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.MediaType;

/**
 * Some JSON helper utilities used by the Cloud Foundry Java client.
 */
public class JsonUtil {

    public static final MediaType JSON_MEDIA_TYPE = new MediaType(MediaType.APPLICATION_JSON.getType(),
                                                                  MediaType.APPLICATION_JSON.getSubtype(),
                                                                  StandardCharsets.UTF_8);

    protected static final Log logger = LogFactory.getLog(JsonUtil.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static List<String> convertJsonToList(String json) {
        List<String> retList = new ArrayList<>();
        if (json != null) {
            try {
                retList = mapper.readValue(json, new TypeReference<List<String>>() {
                });
            } catch (IOException e) {
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
            } catch (IOException e) {
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
        if (value == null || writer.canSerialize(value.getClass())) {
            try {
                return writer.writeValueAsString(value);
            } catch (IOException e) {
                logger.warn("Error while serializing " + value + " to JSON", e);
                return null;
            }

        } else {
            throw new IllegalArgumentException("Value of type " + value.getClass()
                                                                       .getName()
                                                   + " can not be serialized to JSON.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> convertToJsonList(InputStream jsonInputStream) {
        try {
            return mapper.readValue(jsonInputStream, List.class);
        } catch (JsonParseException | JsonMappingException e) {
            logger.error("Unable to parse JSON from InputStream", e);
            throw new IllegalArgumentException("Unable to parse JSON from InputStream", e);
        } catch (IOException e) {
            logger.error("Unable to process InputStream", e);
            throw new IllegalArgumentException("Unable to parse JSON from InputStream", e);
        }
    }

    public static <T> T convertJsonStringToObject(Object jsonString, Class<T> type) {
        return mapper.convertValue(jsonString, type);
    }

}
