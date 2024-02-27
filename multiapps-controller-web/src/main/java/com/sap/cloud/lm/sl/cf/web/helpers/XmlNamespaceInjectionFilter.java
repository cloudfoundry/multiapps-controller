package com.sap.cloud.lm.sl.cf.web.helpers;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class XmlNamespaceInjectionFilter extends XMLFilterImpl {

    private String namespaceToSet;

    public XmlNamespaceInjectionFilter(XMLReader reader, String namespaceToSet) {
        super(reader);
        this.namespaceToSet = namespaceToSet;
    }

    @Override
    public void startElement(String namespace, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(namespaceToSet, localName, qName, attributes);
    }

    @Override
    public void endElement(String namespace, String localName, String qName) throws SAXException {
        super.endElement(namespaceToSet, localName, qName);
    }

}
