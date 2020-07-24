package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.Date;

import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent;
import com.sap.cloud.lm.sl.cf.core.model.HistoricOperationEvent.EventType;

public interface HistoricOperationEventQuery extends Query<HistoricOperationEvent, HistoricOperationEventQuery> {

    HistoricOperationEventQuery id(Long id);

    HistoricOperationEventQuery processId(String processId);

    HistoricOperationEventQuery type(EventType type);

    HistoricOperationEventQuery olderThan(Date time);

}