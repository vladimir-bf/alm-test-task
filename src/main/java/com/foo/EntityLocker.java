package com.foo;

import java.util.concurrent.TimeUnit;

/**
 * Provides functionality to perform locking by entity id.
 * Note that lock is reentrant.
 * @param <T> entity id type
 */
public interface EntityLocker<T> {
    /**
     * Acquires lock by entity id
     * @param id entity id
     */
    void lock(T id);

    /**
     * Acquires the lock by entity id unless the current thread is interrupted.
     * @param id entity id
     * @throws InterruptedException if thread is interrupted
     */
    void lockInterruptibly(T id) throws InterruptedException;

    /**
     * Acquires lock by entity id with a timeout in interruptible manner
     * @param id entity id
     * @param timeout timeout value
     * @param timeUnit timeout's timeunit
     * @return true if acquired successfully, otherwise false
     * @throws InterruptedException if thread is interrupted
     */
    boolean lock(T id, long timeout, TimeUnit timeUnit) throws InterruptedException;

    /**
     * Releases lock by entity id
     * @param id entity id
     */
    void unlock(T id);

    /**
     * Acquires global lock.
     * Waits for all other locks to be released before acquiring.
     * Only one thread can hold the global lock at moment.
     */
    void acquireGlobalLock();

    /**
     * Releases global lock.
     */
    void releaseGlobalLock();
}
