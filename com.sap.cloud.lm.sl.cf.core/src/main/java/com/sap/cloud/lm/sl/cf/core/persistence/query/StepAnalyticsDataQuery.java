package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.Date;

import com.sap.cloud.lm.sl.cf.core.model.StepAnalyticsData;

public interface StepAnalyticsDataQuery extends Query<StepAnalyticsData, StepAnalyticsDataQuery> {

    StepAnalyticsDataQuery olderThan(Date olderThan);

}
