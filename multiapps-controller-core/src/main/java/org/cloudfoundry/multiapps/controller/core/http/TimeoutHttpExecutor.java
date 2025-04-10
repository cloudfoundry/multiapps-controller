package org.cloudfoundry.multiapps.controller.core.http;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.cloudfoundry.multiapps.common.SLException;

public class TimeoutHttpExecutor {

    private final HttpClient client;

    public TimeoutHttpExecutor(HttpClient client) {
        this.client = client;
    }

    public <T> T executeWithTimeout(HttpUriRequest request, long timeoutInMillis, Function<HttpResponse, T> responseHandler) {
        TimerTask abortRequestTask = buildAbortRequestTimerTask(request);
        Timer timeoutEnforcer = new Timer(true);
        timeoutEnforcer.schedule(abortRequestTask, timeoutInMillis);
        try {
            final HttpResponse response = client.execute(request);
            return responseHandler.apply(response);
        } catch (IOException e) {
            throw new SLException(e, e.getMessage());
        } finally {
            abortRequestTask.cancel();
            timeoutEnforcer.cancel();
        }
    }

    private TimerTask buildAbortRequestTimerTask(HttpUriRequest request) {
        return new TimerTask() {
            @Override
            public void run() {
                if (!request.isAborted()) {
                    request.abort();
                }
            }
        };
    }
}