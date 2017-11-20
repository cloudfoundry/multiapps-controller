package com.sap.cloud.lm.sl.cf.process.steps;

public enum StepPhase {
    EXECUTE("execute"), POLL("poll"), RETRY("retry"), WAIT("wait");

    private String type;

    StepPhase(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static StepPhase fromValue(String value) {
        for (StepPhase stepType : StepPhase.values()) {
            if (stepType.type.equals(value)) {
                return stepType;
            }
        }
        return null;
    }
}
