package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Collections;
import java.util.List;

import com.sap.cloud.lm.sl.cf.core.helpers.escaping.CharacterToReplace;
import com.sap.cloud.lm.sl.cf.core.helpers.escaping.EscapeSequenceToReplace;
import com.sap.cloud.lm.sl.cf.core.helpers.escaping.EscapeSequencesReplacer;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.helpers.VisitableObject;

public class ObjectToEnvironmentValueConverter {

    private final List<EscapeSequenceToReplace> customEscapeSequences;
    private final boolean prettyPrinting;

    public ObjectToEnvironmentValueConverter(boolean prettyPrinting) {
        this(getDefaultCustomEscapeSequences(), prettyPrinting);
    }

    public ObjectToEnvironmentValueConverter(List<EscapeSequenceToReplace> customEscapeSequences, boolean prettyPrinting) {
        this.customEscapeSequences = customEscapeSequences;
        this.prettyPrinting = prettyPrinting;
    }

    public String convert(Object object) {
        return object instanceof String ? (String) object : toJson(object);
    }

    private String toJson(Object object) {
        object = replaceCustomEscapeSequencesWithPlaceholders(object);
        String result = JsonUtil.toJson(object, prettyPrinting);
        result = replacePlaceholdersWithCustomEscapeSequences(result);
        return result;
    }

    private Object replaceCustomEscapeSequencesWithPlaceholders(Object value) {
        // If the custom escape sequences (like \$, for example) are not replaced with placeholders
        // the JSON serialization library might try to escape any symbols in them that have special
        // meaning in JSON documents (like the backslash, for example). That would break the escape
        // sequence, as instead of "\$", it would become "\\$".
        return new VisitableObject(value).accept(new EscapeSequencesReplacer(customEscapeSequences));
    }

    private String replacePlaceholdersWithCustomEscapeSequences(String value) {
        String result = value;
        for (EscapeSequenceToReplace escapeSequence : customEscapeSequences) {
            CharacterToReplace escapeCharacter = escapeSequence.getEscapeCharacter();
            CharacterToReplace markerCharacter = escapeSequence.getMarkerCharacter();
            result = result.replace(escapeCharacter.getReplacement(), "" + escapeCharacter.getValue() + escapeCharacter.getValue());
            result = result.replace(markerCharacter.getReplacement(), "" + escapeCharacter.getValue() + markerCharacter.getValue());
        }
        return result;
    }

    private static CharacterToReplace getDefaultEscapeCharacter() {
        return new CharacterToReplace('\\', getPlaceholder('\\'));
    }

    private static List<EscapeSequenceToReplace> getDefaultCustomEscapeSequences() {
        return Collections.singletonList(new EscapeSequenceToReplace(getDefaultEscapeCharacter(),
                                                                     new CharacterToReplace('$', getPlaceholder('$'))));
    }

    private static String getPlaceholder(char c) {
        return String.format("{__DEPLOY_SERVICE_ESCAPED_%s}", (int) c);
    }

}
