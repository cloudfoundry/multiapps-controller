package com.sap.cloud.lm.sl.cf.core.persistence.query.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import com.sap.cloud.lm.sl.cf.core.model.StepAnalyticsData;
import com.sap.cloud.lm.sl.cf.core.persistence.dto.StepAnalyticsDataDto;
import com.sap.cloud.lm.sl.cf.core.persistence.query.StepAnalyticsDataQuery;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.ImmutableQueryAttributeRestriction;
import com.sap.cloud.lm.sl.cf.core.persistence.query.criteria.QueryCriteria;
import com.sap.cloud.lm.sl.cf.core.persistence.service.StepAnalyticsDataService.StepAnalyticsDataMapper;

public class StepAnalyticsDataQueryImpl extends AbstractQueryImpl<StepAnalyticsData, StepAnalyticsDataQuery>
    implements StepAnalyticsDataQuery {

    private QueryCriteria queryCriteria = new QueryCriteria();
    private StepAnalyticsDataMapper stepAnalyticsDataMapper;

    public StepAnalyticsDataQueryImpl(EntityManager entityManager, StepAnalyticsDataMapper stepAnalyticsDataMapper) {
        super(entityManager);
        this.stepAnalyticsDataMapper = stepAnalyticsDataMapper;
    }

    @Override
    public StepAnalyticsDataQuery olderThan(Date olderThan) {
        queryCriteria.addRestriction(ImmutableQueryAttributeRestriction.<Long> builder()
                                                                       .attribute(StepAnalyticsDataDto.AttributeNames.EVENT_OCCURRENCE_TIME)
                                                                       .condition(getCriteriaBuilder()::lessThan)
                                                                       .value(olderThan.getTime())
                                                                       .build());
        return this;
    }

    @Override
    public StepAnalyticsData singleResult() throws NoResultException {
        StepAnalyticsDataDto dto = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                               StepAnalyticsDataDto.class).getSingleResult());
        return stepAnalyticsDataMapper.fromDto(dto);
    }

    @Override
    public List<StepAnalyticsData> list() {
        List<StepAnalyticsDataDto> dtos = executeInTransaction(manager -> createQuery(manager, queryCriteria,
                                                                                      StepAnalyticsDataDto.class).getResultList());
        return dtos.stream()
                   .map(stepAnalyticsDataMapper::fromDto)
                   .collect(Collectors.toList());
    }

    @Override
    public int delete() {
        return executeInTransaction(manager -> createDeleteQuery(manager, queryCriteria, StepAnalyticsDataDto.class).executeUpdate());
    }

}
