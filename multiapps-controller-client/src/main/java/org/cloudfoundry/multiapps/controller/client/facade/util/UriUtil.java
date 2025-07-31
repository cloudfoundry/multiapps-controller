package org.cloudfoundry.multiapps.controller.client.facade.util;

import org.springframework.web.util.UriUtils;

import java.nio.charset.Charset;
import java.util.List;

public class UriUtil {

    private UriUtil() {
        // prevents initialization
    }

    public static String encodeChars(String queryParam, List<String> charsToEncode) {
        for (String charToEncode: charsToEncode) {
            queryParam = queryParam.replaceAll(charToEncode, UriUtils.encode(charToEncode, Charset.defaultCharset()));
        }
        return queryParam;
    }
}
