package org.cloudfoundry.multiapps.controller.web.configuration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.cloudfoundry.multiapps.common.util.JsonSerializationStrategy;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.web.interceptors.CustomHandlerInterceptor;
import org.cloudfoundry.multiapps.controller.web.util.XmlNamespaceIgnoringHttpMessageConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebMvc
public class WebMvcConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcConfiguration.class);

    @Inject
    private List<CustomHandlerInterceptor> customHandlerInterceptors;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LOGGER.trace("Registering custom handler interceptors: {}", customHandlerInterceptors);
        customHandlerInterceptors.forEach(registry::addInterceptor);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        converters.add(createJsonHttpMessageConverter());
        converters.add(new XmlNamespaceIgnoringHttpMessageConverter());
    }

    private JacksonJsonHttpMessageConverter createJsonHttpMessageConverter() {
        JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter = new JacksonJsonHttpMessageConverter((JsonMapper) JsonUtil.getObjectMapper(JsonSerializationStrategy.DEFAULT));
        jacksonJsonHttpMessageConverter.setSupportedMediaTypes(buildSupportedMediaTypes(jacksonJsonHttpMessageConverter));
        return jacksonJsonHttpMessageConverter;
    }

    private List<MediaType> buildSupportedMediaTypes(JacksonJsonHttpMessageConverter jacksonJsonHttpMessageConverter) {
        List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonJsonHttpMessageConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        return supportedMediaTypes;
    }

}
