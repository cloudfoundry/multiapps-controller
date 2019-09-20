package com.sap.cloud.lm.sl.cf.web.configuration;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.sap.cloud.lm.sl.cf.web.util.XmlNamespaceIgnoringHttpMessageConverter;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Configuration
@EnableWebMvc
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new StringHttpMessageConverter());
        converters.add(createJsonHttpMessageConverter());
        converters.add(new XmlNamespaceIgnoringHttpMessageConverter());
    }

    private MappingJackson2HttpMessageConverter createJsonHttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter(JsonUtil.getObjectMapper());
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(buildSupportedMediaTypes(mappingJackson2HttpMessageConverter));
        return mappingJackson2HttpMessageConverter;
    }

    private List<MediaType> buildSupportedMediaTypes(MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter) {
        List<MediaType> supportedMediaTypes = new ArrayList<>(mappingJackson2HttpMessageConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        return supportedMediaTypes;
    }

}
