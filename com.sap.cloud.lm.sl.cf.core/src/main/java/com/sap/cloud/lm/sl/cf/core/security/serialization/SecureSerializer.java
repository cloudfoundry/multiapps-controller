package com.sap.cloud.lm.sl.cf.core.security.serialization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class SecureSerializer<E extends Element> {

    protected SecureSerializerConfiguration configuration;

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
        return element.isSimpleElement() && isSensitive(element.asSimpleElement());
    }

    private boolean hasSensitiveNaming(Element element) {
        return isSensitive(element.getName());
    }

    private boolean isSensitive(Element element) {
        return hasSensitiveNaming(element) || hasSensitiveValues(element);
    }

    private boolean isSensitive(String value) {
        return configuration.apply(value);
    }

    private void modifySensitiveElements(CompositeElement element) {
        List<Element> elementsToModify = new ArrayList<>();
        element.getMembers().forEach((nestedElement) -> {
            if (isSensitive(nestedElement)) {
                elementsToModify.add(0, nestedElement);
            } else {
                modifySensitiveElements(nestedElement);
            }
        });
        modifyElements(element, elementsToModify);
    }

    private void modifyElements(CompositeElement element, Collection<Element> elementsToModify) {
        if (element.isMappingElement()) {
            modifyElements(element.asMappingElement(), elementsToModify);
        } else {
            modifyElements(element.asListingElement(), elementsToModify);
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
        if (element.isMappingElement()) {
            modifySensitiveElements(element.asMappingElement());
            return;
        }
        if (element.isListingElement()) {
            modifySensitiveElements(element.asListingElement());
            return;
        }
    }

    protected abstract String toString(E element);

    protected abstract E toTree(Object object);

}
