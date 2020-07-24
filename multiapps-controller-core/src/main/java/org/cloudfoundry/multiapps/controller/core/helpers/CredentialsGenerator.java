package org.cloudfoundry.multiapps.controller.core.helpers;

import java.security.SecureRandom;

import org.apache.commons.lang3.ArrayUtils;
import org.cloudfoundry.multiapps.common.util.MiscUtil;

public class CredentialsGenerator {

    private static final char[] LEGAL_CHARACTERS = getLegalCharacters();

    private final SecureRandom randomGenerator;

    public CredentialsGenerator() {
        this(new SecureRandom());
    }

    protected CredentialsGenerator(SecureRandom randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    public String next(int length) {
        StringBuilder credential = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = randomGenerator.nextInt(LEGAL_CHARACTERS.length);
            credential.append(LEGAL_CHARACTERS[randomIndex]);
        }
        return credential.toString();
    }

    private static char[] getLegalCharacters() {
        char[] digits = MiscUtil.getCharacterRange('0', '9');
        char[] lowerCaseLetters = MiscUtil.getCharacterRange('a', 'z');
        char[] upperCaseLetters = MiscUtil.getCharacterRange('A', 'Z');
        char[] specialCharacters = new char[] { '_', '-', '@', '(', ')', '&', '#', '*', '[', ']', };

        char[] legalCharacters = ArrayUtils.addAll(lowerCaseLetters, upperCaseLetters);
        legalCharacters = ArrayUtils.addAll(legalCharacters, digits);
        legalCharacters = ArrayUtils.addAll(legalCharacters, specialCharacters);

        return legalCharacters;
    }

}
