package com.sap.cloud.lm.sl.cf.core.dao.filters;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.OperationDto;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class OperationFilter {

    private Date startedBefore;
    private Date endedBefore;
    private Date endedAfter;
    private String spaceId;
    private String mtaId;
    private String user;
    private boolean inNonFinalState;
    private boolean inFinalState;
    private boolean withoutAcquiredLock;
    private boolean withAcquiredLock;
    private List<State> states;

    private String orderAttribute;
    private OrderDirection orderDirection;
    private Integer firstElement;
    private Integer maxResults;

    protected OperationFilter(Builder builder) {
        this.startedBefore = builder.startedBefore;
        this.endedBefore = builder.endedBefore;
        this.endedAfter = builder.endedAfter;
        this.spaceId = builder.spaceId;
        this.mtaId = builder.mtaId;
        this.user = builder.user;
        this.inNonFinalState = builder.inNonFinalState;
        this.inFinalState = builder.inFinalState;
        this.withoutAcquiredLock = builder.withoutAcquiredLock;
        this.withAcquiredLock = builder.withAcquiredLock;
        this.orderAttribute = builder.orderAttribute;
        this.orderDirection = builder.orderDirection;
        this.firstElement = builder.firstElement;
        this.maxResults = builder.maxResults;
        this.states = builder.states;
    }

    public Date getStartedBefore() {
        return startedBefore;
    }

    public Date getEndedBefore() {
        return endedBefore;
    }

    public Date getEndedAfter() {
        return endedAfter;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getUser() {
        return user;
    }

    public boolean isInNonFinalState() {
        return inNonFinalState;
    }

    public boolean isInFinalState() {
        return inFinalState;
    }

    public boolean isWithoutAcquiredLock() {
        return withoutAcquiredLock;
    }

    public boolean isWithAcquiredLock() {
        return withAcquiredLock;
    }

    public List<State> getStates() {
        return states;
    }

    public String getOrderAttribute() {
        return orderAttribute;
    }

    public OrderDirection getOrderDirection() {
        return orderDirection;
    }

    public Integer getFirstElement() {
        return firstElement;
    }

    public Integer getMaxResults() {
        return maxResults;
    }

    @Override
    public boolean equals(Object object) {
        return EqualsBuilder.reflectionEquals(this, object);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public static class Builder {

        private Date startedBefore;
        private Date endedBefore;
        private Date endedAfter;
        private String spaceId;
        private String mtaId;
        private String user;
        private boolean inFinalState;
        private boolean inNonFinalState;
        private boolean withoutAcquiredLock;
        private boolean withAcquiredLock;
        private List<State> states;

        private String orderAttribute;
        private OrderDirection orderDirection = OrderDirection.ASCENDING;
        private Integer firstElement;
        private Integer maxResults;

        public Builder startedBefore(Date startedBefore) {
            this.startedBefore = startedBefore;
            return this;
        }

        public Builder endedBefore(Date endedBefore) {
            this.endedBefore = endedBefore;
            return this;
        }

        public Builder endedAfter(Date endedAfter) {
            this.endedAfter = endedAfter;
            return this;
        }

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder mtaId(String mtaId) {
            this.mtaId = mtaId;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder inNonFinalState() {
            this.inNonFinalState = true;
            return this;
        }

        public Builder inFinalState() {
            this.inFinalState = true;
            return this;
        }

        public Builder withoutAcquiredLock() {
            this.withoutAcquiredLock = true;
            return this;
        }

        public Builder withAcquiredLock() {
            this.withAcquiredLock = true;
            return this;
        }

        public Builder state(State state) {
            this.states = Arrays.asList(state);
            return this;
        }

        public Builder stateIn(State... states) {
            this.states = Arrays.asList(states);
            return this;
        }

        public Builder stateIn(List<State> states) {
            this.states = states;
            return this;
        }

        public Builder orderByProcessId() {
            this.orderAttribute = OperationDto.AttributeNames.PROCESS_ID;
            return this;
        }

        public Builder orderByStartTime() {
            this.orderAttribute = OperationDto.AttributeNames.STARTED_AT;
            return this;
        }

        public Builder orderByEndTime() {
            this.orderAttribute = OperationDto.AttributeNames.ENDED_AT;
            return this;
        }

        public Builder firstElement(Integer firstElement) {
            this.firstElement = firstElement;
            return this;
        }

        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public Builder descending() {
            this.orderDirection = OrderDirection.DESCENDING;
            return this;
        }

        public Builder ascending() {
            this.orderDirection = OrderDirection.ASCENDING;
            return this;
        }

        public OperationFilter build() {
            return new OperationFilter(this);
        }

    }

}
