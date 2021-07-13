package org.cloudfoundry.multiapps.controller.core.security.token.parsing;

import java.text.MessageFormat;

import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

@Named
public class TokenValidationStrategyFactory {

    public ValidationStrategy createStrategy(String algorithm) {
        switch (algorithm) {
            case "RS256":
            case "SHA256withRSA":
                return new RSAValidationStrategy();
            case "HS256":
            case "HMACSHA256":
                return new MACValidationStrategy();
            default:
                throw new InternalAuthenticationServiceException(MessageFormat.format(Messages.UNSUPPORTED_ALGORITHM_PROVIDED, algorithm));
        }
    }

}
