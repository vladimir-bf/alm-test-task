package com.foo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EntityLockerImpl<T> implements EntityLocker<T> {

    private final Map<T, Sync> syncMap = new ConcurrentHashMap<>();
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    @Override
    public void lock(T id) {
        lockInternal(id);
    }

    @Override
    public void lockInterruptibly(T id) throws InterruptedException {
        lockInternalInterruptibly(id, null, null);
    }

    @Override
    public boolean lock(T id, long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (timeUnit == null) {
            throw new IllegalArgumentException("TimeUnit cannot be null");
        }
        return lockInternalInterruptibly(id, timeout, timeUnit);
    }

    @Override
    public void unlock(T id) {
        Sync sync = syncMap.get(id);
        if (sync == null || Thread.currentThread().getId() != sync.threadId) {
            throw new IllegalMonitorStateException();
        }

        sync.holdCount--;

        if (sync.holdCount == 0) {
            syncMap.remove(id);
            sync.latch.countDown();
        }

        globalLock.readLock().unlock();
    }

    @Override
    public void acquireGlobalLock() {
        globalLock.writeLock().lock();
    }

    @Override
    public void releaseGlobalLock() {
        globalLock.writeLock().unlock();
    }

    private void lockInternal(T id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity id cannot be null");
        }

        long threadId = Thread.currentThread().getId();
        Sync sync = new Sync(threadId);

        Sync previous = syncMap.putIfAbsent(id, sync);

        while (previous != null && previous.threadId != threadId) {
            try {
                previous.latch.await();
            } catch (InterruptedException ignore) {
                continue;
            }
            previous = syncMap.putIfAbsent(id, sync);
        }

        Sync actual = previous == null ? sync : previous;
        actual.holdCount++;

        globalLock.readLock().lock();
    }

    private boolean lockInternalInterruptibly(T id, Long timeout, TimeUnit timeUnit) throws InterruptedException {
        if (id == null) {
            throw new IllegalArgumentException("Entity id cannot be null");
        }

        long threadId = Thread.currentThread().getId();
        Sync sync = new Sync(threadId);

        Sync previous = syncMap.putIfAbsent(id, sync);

        while (previous != null && previous.threadId != threadId) {
            if (timeout != null && timeUnit != null) {
                boolean timedWaitSuccessful = previous.latch.await(timeout, timeUnit);
                if (!timedWaitSuccessful) {
                    return false;
                }
            } else {
                previous.latch.await();
            }
            previous = syncMap.putIfAbsent(id, sync);
        }

        Sync actual = previous == null ? sync : previous;
        actual.holdCount++;

        globalLock.readLock().lockInterruptibly();
        return true;
    }

    private class Sync {
        private final long threadId;
        private final CountDownLatch latch;
        private int holdCount;

        Sync(long threadId) {
            this.threadId = threadId;
            this.latch = new CountDownLatch(1);
            this.holdCount = 0;
        }
    }

}
