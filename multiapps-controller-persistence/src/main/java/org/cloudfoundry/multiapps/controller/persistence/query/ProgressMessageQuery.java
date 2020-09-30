package org.cloudfoundry.multiapps.controller.persistence.query;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.OrderDirection;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage;
import org.cloudfoundry.multiapps.controller.persistence.model.ProgressMessage.ProgressMessageType;

public interface ProgressMessageQuery extends Query<ProgressMessage, ProgressMessageQuery> {

    ProgressMessageQuery id(Long id);

    ProgressMessageQuery processId(String processId);

    ProgressMessageQuery taskId(String taskId);

    ProgressMessageQuery type(ProgressMessageType type);

    ProgressMessageQuery typeNot(ProgressMessageType type);

    ProgressMessageQuery text(String text);

    ProgressMessageQuery olderThan(Date time);

    ProgressMessageQuery orderById(OrderDirection orderDirection);

}