package com.sap.cloud.lm.sl.cf.web.helpers;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;

@Provider
@Consumes(MediaType.APPLICATION_XML)
public class XmlNamespaceIgnoringMessageBodyReader<T> implements MessageBodyReader<T> {

    private final SAXParserFactory factory;

    public XmlNamespaceIgnoringMessageBodyReader(@Context SAXParserFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.getAnnotation(XmlRootElement.class) != null && mediaType.equals(MediaType.APPLICATION_XML_TYPE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {

        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(type)
                .createUnmarshaller();
            String declaredNamespaceForType = getDeclaredNamespaceForType(type);
            XMLFilter filter = createXmlFilter(unmarshaller, declaredNamespaceForType);

            return (T) (unmarshaller.unmarshal(asSource(entityStream, filter)));
        } catch (JAXBException e) {
            throw new BadRequestException(e);
        }
    }

    private XMLFilter createXmlFilter(Unmarshaller unmarshaller, String namespaceToSet) throws JAXBException {
        try {
            XMLFilter filter = new XmlNamespaceInjectionFilter(createXmlReader(), namespaceToSet);
            filter.setContentHandler(unmarshaller.getUnmarshallerHandler());
            return filter;
        } catch (SAXException | ParserConfigurationException e) {
            throw new JAXBException(e);
        }
    }

    private XMLReader createXmlReader() throws SAXException, ParserConfigurationException {
        return factory.newSAXParser()
            .getXMLReader();
    }

    private String getDeclaredNamespaceForType(Class<T> type) {
        XmlSchema xmlSchemaAnnotation = type.getPackage()
            .getAnnotation(XmlSchema.class);
        return xmlSchemaAnnotation == null ? null : xmlSchemaAnnotation.namespace();
    }

    private SAXSource asSource(InputStream entityStream, XMLReader reader) {
        return new SAXSource(reader, new InputSource(entityStream));
    }

}
