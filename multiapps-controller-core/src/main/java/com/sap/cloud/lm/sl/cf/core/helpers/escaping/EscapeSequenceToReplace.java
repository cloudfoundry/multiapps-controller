package com.sap.cloud.lm.sl.cf.core.helpers.escaping;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

public class EscapeSequenceToReplace {

    private static final String ESCAPE_SEQUENCE_REGEX_TEMPLATE = "(?<escapeCharacters>\\Q%s\\E*)(?<markerCharacter>\\Q%s\\E)";

    private final CharacterToReplace escapeCharacter;
    /**
     * The marker character identifies the escape sequence (the 'n' in '\n', for example).
     */
    private final CharacterToReplace markerCharacter;

    public EscapeSequenceToReplace(CharacterToReplace escapeCharacter, CharacterToReplace markerCharacter) {
        this.escapeCharacter = escapeCharacter;
        this.markerCharacter = markerCharacter;
    }

    public CharacterToReplace getEscapeCharacter() {
        return escapeCharacter;
    }

    public CharacterToReplace getMarkerCharacter() {
        return markerCharacter;
    }

    public String getMatchReplacement(Matcher matcher) {
        String escapeCharacters = matcher.group("escapeCharacters");
        String replacement = StringUtils.repeat(escapeCharacter.getReplacement(), getEscapedEscapeCharactersCount(escapeCharacters));
        // If there is an unescaped escape character then the character following it is escaped and
        // should be replaced with its proper value. Otherwise it should be left untouched:
        if (hasUnescapedEscapeCharacter(escapeCharacters)) {
            return replacement + (markerCharacter.getReplacement());
        } else {
            return replacement + "${markerCharacter}";
        }
    }

    private int getEscapedEscapeCharactersCount(String escapeCharacters) {
        return escapeCharacters.length() / 2; // Escape characters can also be escaped by preceding
                                              // them with another escape character.
    }

    private boolean hasUnescapedEscapeCharacter(String escapeCharacters) {
        return escapeCharacters.length() % 2 != 0;
    }

    public String getEscapeSequenceRegex() {
        return String.format(ESCAPE_SEQUENCE_REGEX_TEMPLATE, escapeCharacter.getValue(), markerCharacter.getValue());
    }

}
