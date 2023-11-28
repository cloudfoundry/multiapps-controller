package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;
import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;

public interface OperationQuery extends Query<Operation, OperationQuery> {

    OperationQuery processId(String processId);

    OperationQuery processType(ProcessType processType);

    OperationQuery spaceId(String spaceId);

    OperationQuery mtaId(String mtaId);

    OperationQuery namespace(String namespace);

    OperationQuery user(String user);

    OperationQuery acquiredLock(Boolean acquiredLock);

    OperationQuery state(Operation.State state);

    OperationQuery startedBefore(LocalDateTime startedBefore);

    OperationQuery startedAfter(LocalDateTime timestamp);

    OperationQuery endedBefore(LocalDateTime endedBefore);

    OperationQuery endedAfter(LocalDateTime endedAfter);

    OperationQuery inNonFinalState();

    OperationQuery inFinalState();

    OperationQuery withStateAnyOf(List<Operation.State> states);

    OperationQuery orderByProcessId(OrderDirection orderDirection);

    OperationQuery orderByEndTime(OrderDirection orderDirection);

    OperationQuery orderByStartTime(OrderDirection orderDirection);

}