package com.sap.cloud.lm.sl.cf.core.helpers;

import java.security.SecureRandom;

import org.apache.commons.lang3.ArrayUtils;

import com.sap.cloud.lm.sl.common.util.CommonUtil;

public class CredentialsGenerator {

    private static final char[] LEGAL_CHARACTERS = getLegalCharacters();

    private final SecureRandom randomGenerator;

    public CredentialsGenerator() {
        this(new SecureRandom());
    }

    protected CredentialsGenerator(SecureRandom randomGenerator) {
        this.randomGenerator = randomGenerator;
    }

    private static char[] getLegalCharacters() {
        char[] digits = CommonUtil.getCharacterRange('0', '9');
        char[] lowerCaseLetters = CommonUtil.getCharacterRange('a', 'z');
        char[] upperCaseLetters = CommonUtil.getCharacterRange('A', 'Z');
        char[] specialCharacters = new char[] { '_', '-', '@', '(', ')', '&', '#', '*', '[', ']', };

        char[] legalCharacters = ArrayUtils.addAll(lowerCaseLetters, upperCaseLetters);
        legalCharacters = ArrayUtils.addAll(legalCharacters, digits);
        legalCharacters = ArrayUtils.addAll(legalCharacters, specialCharacters);

        return legalCharacters;
    }

    public String next(int length) {
        StringBuilder credential = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = randomGenerator.nextInt(LEGAL_CHARACTERS.length);
            credential.append(LEGAL_CHARACTERS[randomIndex]);
        }
        return credential.toString();
    }

}
