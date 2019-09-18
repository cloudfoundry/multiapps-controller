package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.Date;

import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;

public interface ProgressMessageQuery extends Query<ProgressMessage, ProgressMessageQuery> {

    ProgressMessageQuery id(Long id);

    ProgressMessageQuery processId(String processId);

    ProgressMessageQuery taskId(String taskId);

    ProgressMessageQuery type(ProgressMessageType type);

    ProgressMessageQuery typeNot(ProgressMessageType type);

    ProgressMessageQuery text(String text);

    ProgressMessageQuery olderThan(Date time);

}