package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class SecureSerializer<E extends Element> {

    protected final SecureSerializerConfiguration configuration;

    public SecureSerializer() {
        this(new SecureSerializerConfiguration());
    }

    public SecureSerializer(SecureSerializerConfiguration configuration) {
        this.configuration = configuration;
    }

    public String serialize(Object object) {
        E objectTree = toTree(object);
        modifySensitiveElements(objectTree);
        return toString(objectTree);
    }

    private boolean hasSensitiveValues(Element element) {
        return element.isScalar() && isSensitive(element.asString());
    }

    private boolean hasSensitiveNaming(Element element) {
        return isSensitive(element.getName());
    }

    private boolean isSensitive(Element element) {
        return hasSensitiveNaming(element) || hasSensitiveValues(element);
    }

    private boolean isSensitive(String value) {
        return configuration.isSensitive(value);
    }

    private void modifySensitiveElements(CompositeElement element) {
        List<Element> elementsToModify = new LinkedList<>();
        for (Element nestedElement : element.getMembers()) {
            if (isSensitive(nestedElement)) {
                elementsToModify.add(0, nestedElement);
            } else {
                modifySensitiveElements(nestedElement);
            }
        }
        modifyElements(element, elementsToModify);
    }

    private void modifyElements(CompositeElement element, Collection<Element> elementsToModify) {
        if (element.isMap()) {
            modifyElements(element.asMapElement(), elementsToModify);
        } else {
            modifyElements(element.asListElement(), elementsToModify);
        }
    }

    private void modifyElements(MapElement mapElement, Collection<Element> membersToModify) {
        for (Element element : membersToModify) {
            mapElement.add(element.getName(), SecureSerializerConfiguration.SECURE_SERIALIZATION_MASK);
        }
    }

    private void modifyElements(ListElement seqElement, Collection<Element> membersToModify) {
        for (Element element : membersToModify) {
            seqElement.remove(Integer.parseInt(element.getName()));
            seqElement.add(SecureSerializerConfiguration.SECURE_SERIALIZATION_MASK);
        }
    }

    private void modifySensitiveElements(Element element) {
        if (element.isMap()) {
            modifySensitiveElements(element.asMapElement());
            return;
        }
        if (element.isList()) {
            modifySensitiveElements(element.asListElement());
        }
    }

    protected abstract String toString(E element);

    protected abstract E toTree(Object object);

}
