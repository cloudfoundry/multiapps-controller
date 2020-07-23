package com.sap.cloud.lm.sl.cf.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.cloudfoundry.multiapps.common.ParsingException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

public class XmlNamespaceIgnoringHttpMessageConverter implements HttpMessageConverter<Object> {

    private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

    private final Jaxb2RootElementHttpMessageConverter delegate = new Jaxb2RootElementHttpMessageConverter();

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return delegate.getSupportedMediaTypes();
    }

    @Override
    public boolean canRead(Class<?> type, MediaType mediaType) {
        return delegate.canRead(type, mediaType);
    }

    @Override
    public Object read(Class<?> type, HttpInputMessage inputMessage) throws IOException {
        try {
            Unmarshaller unmarshaller = createUnmarshaller(type);
            Source source = createNamespaceSettingSource(type, inputMessage, unmarshaller);
            return unmarshaller.unmarshal(source);
        } catch (JAXBException | SAXException | ParserConfigurationException e) {
            throw new ParsingException(e, e.getMessage());
        }
    }

    private Unmarshaller createUnmarshaller(Class<?> type) throws JAXBException {
        return JAXBContext.newInstance(type)
                          .createUnmarshaller();
    }

    private Source createNamespaceSettingSource(Class<?> type, HttpInputMessage inputMessage, Unmarshaller unmarshaller)
        throws SAXException, ParserConfigurationException, IOException {
        return asSource(inputMessage.getBody(), createNamespaceSettingFilter(unmarshaller, type));
    }

    private XMLFilter createNamespaceSettingFilter(Unmarshaller unmarshaller, Class<?> type)
        throws SAXException, ParserConfigurationException {
        return createNamespaceSettingFilter(unmarshaller, getDeclaredNamespace(type));
    }

    private XMLFilter createNamespaceSettingFilter(Unmarshaller unmarshaller, String namespaceToSet)
        throws SAXException, ParserConfigurationException {
        XMLFilter filter = new XmlNamespaceSettingFilter(createXmlReader(), namespaceToSet);
        filter.setContentHandler(unmarshaller.getUnmarshallerHandler());
        return filter;
    }

    private String getDeclaredNamespace(Class<?> type) {
        XmlSchema xmlSchemaAnnotation = type.getPackage()
                                            .getAnnotation(XmlSchema.class);
        return xmlSchemaAnnotation == null ? null : xmlSchemaAnnotation.namespace();
    }

    private XMLReader createXmlReader() throws SAXException, ParserConfigurationException {
        return SAX_PARSER_FACTORY.newSAXParser()
                                 .getXMLReader();
    }

    private SAXSource asSource(InputStream inputStream, XMLReader reader) {
        return new SAXSource(reader, new InputSource(inputStream));
    }

    @Override
    public boolean canWrite(Class<?> type, MediaType mediaType) {
        return delegate.canWrite(type, mediaType);
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage) throws IOException {
        delegate.write(t, contentType, outputMessage);
    }

}
