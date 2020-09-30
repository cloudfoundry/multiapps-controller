package org.cloudfoundry.multiapps.controller.persistence.query;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;

public interface HistoricOperationEventQuery extends Query<HistoricOperationEvent, HistoricOperationEventQuery> {

    HistoricOperationEventQuery id(Long id);

    HistoricOperationEventQuery processId(String processId);

    HistoricOperationEventQuery type(HistoricOperationEvent.EventType type);

    HistoricOperationEventQuery olderThan(Date time);

}