package org.cloudfoundry.multiapps.controller.process.util;

import java.util.Comparator;

public class PriorityFutureComparator implements Comparator<Runnable> {

    @Override
    public int compare(Runnable r1, Runnable r2) {
        if (r1 == null && r2 == null) {
            return 0;
        } else if (r1 == null) {
            return -1;
        } else if (r2 == null) {
            return 1;
        }
        int p1 = ((PriorityFuture<?>) r1).getPriority();
        int p2 = ((PriorityFuture<?>) r2).getPriority();
        return Integer.compare(p1, p2);
    }
}
