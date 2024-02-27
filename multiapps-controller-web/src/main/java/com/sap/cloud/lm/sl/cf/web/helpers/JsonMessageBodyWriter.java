package com.sap.cloud.lm.sl.cf.web.helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

/**
 * The standard MessageBodyWriter for application/json uses JAXB annotations to generate JSON strings. However, it doesn't take into
 * consideration the XmlElementWrapper annotation. For example, if we have the following field in some class:
 * 
 * <pre>
 *   <code>
 *     &#64;XmlElementWrapper(name = "modules")
 *     &#64;XmlElement(name = "module")
 *     private List&#60;DeployedMtaModule&#62; modules;
 *   </code>
 * </pre>
 * 
 * Then the standard MessageBodyWriter would generate the following JSON:
 * 
 * <pre>
 *   <code>
 *     "module": [
 *       ...
 *     ]
 *   </code>
 * </pre>
 * 
 * Instead of:
 * 
 * <pre>
 *   <code>
 *     "modules": [
 *       ...
 *     ]
 *   </code>
 * </pre>
 * 
 * Because of that, this class is used to replace the standard JSON serializer with Gson.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JsonMessageBodyWriter<T> implements MessageBodyWriter<T> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public long getSize(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException {

        String json = JsonUtil.toJson(t);
        entityStream.write(json.getBytes(getCharset(mediaType)));
    }

    private Charset getCharset(MediaType mediaType) {
        String charsetName = mediaType.getParameters()
                                      .get(MediaType.CHARSET_PARAMETER);
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

}
