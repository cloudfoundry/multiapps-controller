package org.cloudfoundry.multiapps.controller.persistence.query;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;

public interface HistoricOperationEventQuery extends Query<HistoricOperationEvent, HistoricOperationEventQuery> {

    HistoricOperationEventQuery id(Long id);

    HistoricOperationEventQuery processId(String processId);

    HistoricOperationEventQuery type(HistoricOperationEvent.EventType type);

    HistoricOperationEventQuery olderThan(LocalDateTime time);

    HistoricOperationEventQuery orderByTimestamp(OrderDirection orderDirection);

}