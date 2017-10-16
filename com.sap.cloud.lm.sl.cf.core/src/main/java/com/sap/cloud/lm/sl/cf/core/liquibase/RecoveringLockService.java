package com.sap.cloud.lm.sl.cf.core.liquibase;

import static java.text.MessageFormat.format;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;

import liquibase.exception.LockException;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.StandardLockService;

public class RecoveringLockService extends StandardLockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveringLockService.class);

    private long changeLogLockAttempts;
    private long changeLogLockDuration;

    public RecoveringLockService() {
        Configuration configuration = Configuration.getInstance();
        this.changeLogLockAttempts = configuration.getChangeLogLockAttempts();
        this.changeLogLockDuration = configuration.getChangeLogLockDuration();
        setChangeLogLockWaitTime(configuration.getChangeLogLockWaitTime());
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1; // Liquibase chooses which LockService to use based on its priority. This line makes sure that our
                                        // custom lock service has a higher priority than the standard one (which it extends).
    }

    @Override
    public void waitForLock() throws LockException {
        LockException releaseLockException = null;
        LockException acquireLockException = null;
        for (int attempt = 0; attempt < changeLogLockAttempts; attempt++) {
            releaseLockException = attemptToReleaseLock();
            if (releaseLockException != null) {
                LOGGER.warn(releaseLockException.getMessage(), releaseLockException);
            }
            acquireLockException = attemptToAcquireLock();
            if (acquireLockException != null) {
                LOGGER.warn(acquireLockException.getMessage(), acquireLockException);
            } else {
                return;
            }
        }
        throw acquireLockException;
    }

    private LockException attemptToReleaseLock() {
        try {
            if (lockIsStuck()) {
                LOGGER.info(Messages.ATTEMPTING_TO_RELEASE_STUCK_LOCK);
                releaseLock();
            }
            return null;
        } catch (LockException e) {
            return e;
        }
    }

    private LockException attemptToAcquireLock() {
        try {
            super.waitForLock();
            return null;
        } catch (LockException e) {
            return e;
        }
    }

    private boolean lockIsStuck() throws LockException {
        DatabaseChangeLogLock lock = getDatabaseChangeLogLock();
        if (lock == null) {
            return false;
        }
        Date lockGranted = lock.getLockGranted();
        Date currentDate = new Date();

        LOGGER.info(format(Messages.CURRENT_LOCK, lockGranted, lock.getLockedBy()));
        LOGGER.info(format(Messages.CURRENT_DATE, currentDate));
        return hasTimedOut(lockGranted, currentDate);
    }

    private DatabaseChangeLogLock getDatabaseChangeLogLock() throws LockException {
        DatabaseChangeLogLock[] locks = listLocks();
        return (locks.length > 0) ? locks[0] : null;
    }

    private boolean hasTimedOut(Date lockGrantedDate, Date currentDate) {
        return fromMillisToMinutes(currentDate.getTime() - lockGrantedDate.getTime()) >= changeLogLockDuration;
    }

    private long fromMillisToMinutes(long millis) {
        return TimeUnit.MILLISECONDS.toMinutes(millis);
    }

}
