package org.cloudfoundry.multiapps.controller.core.util;

import java.util.Objects;
import java.util.function.Consumer;

import org.mockito.Answers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MockBuilder<T> {

    private T mock;
    private final Class<T> mockClass;

    @SuppressWarnings("unchecked")
    public MockBuilder(T mock) {
        this.mock = mock;
        this.mockClass = (Class<T>) mock.getClass();
    }

    public MockBuilder<T> on(MockMethodCall<T> mockMethodCall) {
        return on(mockMethodCall, null);
    }

    public MockBuilder<T> on(MockMethodCall<T> mockMethodCall, Consumer<InvocationOnMock> invocationConsumer) {
        T callResult = mockMethodCall.performOn(mock);
        if (!Objects.equals(callResult, mock)) {
            this.mock = callResult;
            return this;
        }
        initNewMock(mockMethodCall, invocationConsumer);
        return this;
    }

    private void initNewMock(MockMethodCall<T> mockMethodCall, Consumer<InvocationOnMock> invocationConsumer) {
        T newMock = Mockito.mock(mockClass, Answers.RETURNS_SELF);
        Mockito.when(mockMethodCall.performOn(mock))
               .thenAnswer(getAnswer(newMock, invocationConsumer));
        this.mock = newMock;
    }

    private Answer<T> getAnswer(T answer, Consumer<InvocationOnMock> invocationConsumer) {
        return invocation -> {
            if (invocationConsumer != null) {
                invocationConsumer.accept(invocation);
            }
            return answer;
        };
    }

    public T build() {
        return this.mock;
    }

    public interface MockMethodCall<T> {

        T performOn(T mock);
    }
}
