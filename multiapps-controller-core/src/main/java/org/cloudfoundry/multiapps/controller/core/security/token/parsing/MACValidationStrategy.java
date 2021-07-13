package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;

public class MACValidationStrategy extends ValidationStrategy {

    @Override
    protected JWSVerifier getVerifier(String key) {
        SecretKey secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), 0, key.length(), "HS256");
        return getMACVerifier(secretKey);
    }

    private MACVerifier getMACVerifier(SecretKey secretKey) {
        try {
            return new MACVerifier(secretKey);
        } catch (JOSEException e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

    @Override
    protected JWSObject getObject(String jwtTokenString) {
        try {
            return JWSObject.parse(jwtTokenString);
        } catch (ParseException e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

}
