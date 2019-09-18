package com.sap.cloud.lm.sl.cf.core.persistence.query;

 import java.util.Date;
import java.util.List;

import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.ProcessType;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

 public interface OperationQuery extends Query<Operation, OperationQuery> {

     OperationQuery processId(String processId);

     OperationQuery processType(ProcessType processType);

     OperationQuery spaceId(String spaceId);

     OperationQuery mtaId(String mtaId);

     OperationQuery user(String user);

     OperationQuery acquiredLock(Boolean acquiredLock);

     OperationQuery state(State finalState);

     OperationQuery startedBefore(Date startedBefore);

     OperationQuery endedAfter(Date endedAfter);

     OperationQuery inNonFinalState();

     OperationQuery inFinalState();

     OperationQuery withStateAnyOf(List<State> states);

     OperationQuery orderByProcessId(OrderDirection orderDirection);

     OperationQuery orderByEndTime(OrderDirection orderDirection);

     OperationQuery orderByStartTime(OrderDirection orderDirection);

 }