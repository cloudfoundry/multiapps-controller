package org.cloudfoundry.multiapps.controller.web.configuration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

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
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseSuffixPatternMatch(false);
        UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setUrlDecode(false);
        configurer.setUrlPathHelper(urlPathHelper);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.favorPathExtension(false);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        converters.add(createJsonHttpMessageConverter());
        converters.add(new XmlNamespaceIgnoringHttpMessageConverter());
    }

    private MappingJackson2HttpMessageConverter createJsonHttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(JsonUtil.getObjectMapper(JsonSerializationStrategy.DEFAULT));
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(buildSupportedMediaTypes(mappingJackson2HttpMessageConverter));
        return mappingJackson2HttpMessageConverter;
    }

    private List<MediaType> buildSupportedMediaTypes(MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        List<MediaType> supportedMediaTypes = new ArrayList<>(mappingJackson2HttpMessageConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        return supportedMediaTypes;
    }

}
