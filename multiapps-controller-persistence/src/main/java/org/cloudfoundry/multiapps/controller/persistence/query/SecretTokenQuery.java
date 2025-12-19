package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.model.SecretToken;

public interface SecretTokenQuery extends Query<SecretToken, SecretTokenQuery> {

    SecretTokenQuery id(Long id);

    SecretTokenQuery processInstanceId(String processInstanceId);

    SecretTokenQuery variableName(String variableName);

    SecretTokenQuery olderThan(LocalDateTime time);

    SecretTokenQuery keyId(String keyId);

}
