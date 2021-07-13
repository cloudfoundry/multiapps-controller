package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;

public abstract class ValidationStrategy {

    public boolean validateToken(String jwtTokenString, String key) {
        JWSVerifier jwsVerifier = getVerifier(key);
        JWSObject jwsObject = getObject(jwtTokenString);
        return verify(jwsVerifier, jwsObject);
    }

    private boolean verify(JWSVerifier jwsVerifier, JWSObject jwsObject) {
        try {
            return jwsObject.verify(jwsVerifier);
        } catch (JOSEException e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

    protected abstract JWSVerifier getVerifier(String key);

    protected abstract JWSObject getObject(String jwtTokenString);

}
