package com.sap.cloud.lm.sl.cf.core.helpers.escaping;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.cloud.lm.sl.mta.helpers.SimplePropertyVisitor;

public class EscapeSequencesReplacer implements SimplePropertyVisitor {

    private final List<EscapeSequenceToReplace> escapeSequences;

    public EscapeSequencesReplacer(List<EscapeSequenceToReplace> escapeSequences) {
        this.escapeSequences = escapeSequences;
    }

    public String replaceEscapeSequences(String input) {
        String result = input;
        for (EscapeSequenceToReplace escapeSequence : escapeSequences) {
            result = replaceEscapeSequence(escapeSequence, result);
        }
        return result;
    }

    private String replaceEscapeSequence(EscapeSequenceToReplace escapeSequence, String input) {
        Pattern pattern = Pattern.compile(escapeSequence.getEscapeSequenceRegex());
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = escapeSequence.getMatchReplacement(matcher);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public Object visit(String key, String value) {
        return replaceEscapeSequences(value);
    }

}