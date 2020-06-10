package org.cloudfoundry.multiapps.controller.web.timers;

import java.util.TimerTask;

public interface RegularTimer {

    public TimerTask getTimerTask();

    public long getDelay();

    public long getPeriod();

}
