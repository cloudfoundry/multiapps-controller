package org.cloudfoundry.multiapps.controller.persistence.query;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.LockOwnerEntry;

import java.time.LocalDateTime;
import java.util.List;

public interface LockOwnersQuery extends Query<LockOwnerEntry, LockOwnersQuery> {

    LockOwnersQuery id(Long id);

    LockOwnersQuery lockOwner(String lockOwner);

    LockOwnersQuery withLockOwnerAnyOf(List<String> lockOwners);

    LockOwnersQuery olderThan(LocalDateTime timestamp);

    LockOwnersQuery orderByTimestamp(OrderDirection orderDirection);
}
