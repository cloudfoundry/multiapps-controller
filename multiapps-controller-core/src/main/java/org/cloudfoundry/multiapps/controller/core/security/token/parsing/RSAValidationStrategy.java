package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;

import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;

public class RSAValidationStrategy extends ValidationStrategy {

    @Override
    protected JWSVerifier getVerifier(String publicKey) {
        String encodedKey = removeX509Wrapper(publicKey);
        byte[] encodedPublicKeyValue = Base64.getDecoder()
                                             .decode(encodedKey);
        KeyFactory rsaKeyFactory = getRSAKeyFactory();
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(encodedPublicKeyValue);
        RSAPublicKey rsaPublicKey = getRSAPublicKey(rsaKeyFactory, x509EncodedKeySpec);
        return new RSASSAVerifier(rsaPublicKey);
    }

    private String removeX509Wrapper(String publicKey) {
        return publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replace("\n", "");
    }

    private KeyFactory getRSAKeyFactory() {
        try {
            return KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

    private RSAPublicKey getRSAPublicKey(KeyFactory keyFactory, X509EncodedKeySpec x509EncodedKeySpec) {
        try {
            return (RSAPublicKey) keyFactory.generatePublic(x509EncodedKeySpec);
        } catch (InvalidKeySpecException e) {
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
