package com.foo;

import org.junit.Test;

import java.util.concurrent.*;

import static junit.framework.TestCase.fail;

public class EntityLockerTest {

    @Test
    public void it_must_lock_and_unlock_entities_by_id() {
        EntityLocker<Integer> locker = new EntityLocker<>();
        int entityId = 1;
        try {
            locker.lock(entityId);
        } finally {
            locker.unlock(entityId);
        }
    }

    @Test
    public void it_must_provide_reentrancy() {
        EntityLocker<Integer> locker = new EntityLocker<>();
        int entityId = 1;

        locker.lock(entityId);
        locker.lock(entityId);
        locker.lock(entityId);

        locker.unlock(entityId);
        locker.unlock(entityId);
        locker.unlock(entityId);
    }

    @Test
    public void it_must_throw_exception_when_unlocking_lock_held_by_other_thread() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        EntityLocker<Integer> locker = new EntityLocker<>();
        int entityId = 1;

        executorService.submit(() -> {
            locker.lock(entityId);
            latch.countDown();
        });

        latch.await();
        try {
            locker.unlock(entityId);
            fail();
        } catch (IllegalMonitorStateException e) {
        }
    }

    @Test
    public void it_must_acquire_and_release_global_lock() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        EntityLocker<Integer> locker = new EntityLocker<>();
        locker.acquireGlobalLock();

        try {
            executorService.submit(() -> locker.lock(1)).get(10, TimeUnit.SECONDS);
            fail();
        } catch (TimeoutException e) {
        }
        locker.releaseGlobalLock();
    }


}