package com.sap.cloud.lm.sl.cf.web.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;

import javax.servlet.http.HttpServletRequest;

import com.sap.cloud.lm.sl.cf.web.message.Messages;

public class ServletUtils {

    public static String getDecodedURI(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return decode(uri);
    }

    public static String decode(String uri) {
        try {
            return URLDecoder.decode(uri, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(MessageFormat.format(Messages.COULD_NOT_DECODE_URI_0, uri), e);
        }
    }

}
