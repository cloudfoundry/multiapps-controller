package org.cloudfoundry.multiapps.controller.core.persistence.query;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.core.model.HistoricOperationEvent.EventType;

public interface HistoricOperationEventQuery extends Query<HistoricOperationEvent, HistoricOperationEventQuery> {

    HistoricOperationEventQuery id(Long id);

    HistoricOperationEventQuery processId(String processId);

    HistoricOperationEventQuery type(EventType type);

    HistoricOperationEventQuery olderThan(Date time);

}