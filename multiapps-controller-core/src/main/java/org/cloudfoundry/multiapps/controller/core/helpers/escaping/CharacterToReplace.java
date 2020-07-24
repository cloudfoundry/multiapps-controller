package org.cloudfoundry.multiapps.controller.core.helpers.escaping;

public class CharacterToReplace {

    private final String replacement;
    private final char value;

    public CharacterToReplace(char value, String replacement) {
        this.replacement = replacement;
        this.value = value;
    }

    public String getReplacement() {
        return replacement;
    }

    public char getValue() {
        return value;
    }

}
