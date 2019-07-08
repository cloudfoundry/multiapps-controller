package com.sap.cloud.lm.sl.cf.core.util;

import java.util.function.Function;

import org.mockito.ArgumentMatcher;

public class LambdaArgumentMatcher<T> extends ArgumentMatcher<T> {

    private Function<Object, Boolean> matcher;

    public LambdaArgumentMatcher(Function<Object, Boolean> matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object argument) {
        return matcher.apply(argument);
    }
}