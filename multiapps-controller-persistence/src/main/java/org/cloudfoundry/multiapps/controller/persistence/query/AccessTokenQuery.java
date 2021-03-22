package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.AccessToken;

public interface AccessTokenQuery extends Query<AccessToken, AccessTokenQuery> {

    AccessTokenQuery id(Long id);

    AccessTokenQuery value(byte[] value);

    AccessTokenQuery username(String username);

    AccessTokenQuery expiresBefore(LocalDateTime expiresAt);

    AccessTokenQuery orderByExpiresAt(OrderDirection orderDirection);
}
