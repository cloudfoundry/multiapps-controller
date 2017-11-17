package com.sap.cloud.lm.sl.cf.core.dao.filters;

import java.util.Date;

public class OperationFilter {

    private Date endTimeUpperBound;
    private Date endTimeLowerBound;
    private String spaceId;
    private String mtaId;
    private String user;
    private boolean inNonFinalState;
    private boolean inFinalState;

    protected OperationFilter(Builder builder) {
        this.endTimeUpperBound = builder.endTimeUpperBound;
        this.endTimeLowerBound = builder.endTimeLowerBound;
        this.spaceId = builder.spaceId;
        this.mtaId = builder.mtaId;
        this.user = builder.user;
        this.inNonFinalState = builder.inNonFinalState;
        this.inFinalState = builder.inFinalState;
    }

    public Date getEndTimeUpperBound() {
        return endTimeUpperBound;
    }

    public Date getEndTimeLowerBound() {
        return endTimeLowerBound;
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

    public static class Builder {

        private Date endTimeUpperBound;
        private Date endTimeLowerBound;
        private String spaceId;
        private String mtaId;
        private String user;
        private boolean inFinalState;
        private boolean inNonFinalState;

        public Builder endedBefore(Date endTimeUpperBound) {
            this.endTimeUpperBound = endTimeUpperBound;
            return this;
        }

        public Builder endedAfter(Date endTimeLowerBound) {
            this.endTimeLowerBound = endTimeLowerBound;
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

        public OperationFilter build() {
            return new OperationFilter(this);
        }

    }

}
