package com.foo;

import org.junit.Test;

import java.util.concurrent.*;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

public class EntityLockerTest {

    @Test
    public void it_must_lock_and_unlock_entities_by_id() {
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        int entityId = 1;
        try {
            locker.lock(entityId);
        } finally {
            locker.unlock(entityId);
        }
    }

    @Test
    public void it_must_provide_independent_locking_for_different_entities() throws InterruptedException {
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        CountDownLatch latch = new CountDownLatch(2);
        Thread t = new Thread(() -> {
            locker.lock(2);
            latch.countDown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            locker.unlock(2);
        });

        t.start();

        locker.lock(1);
        latch.countDown();
        latch.await();
        locker.unlock(1);
    }

    @Test
    public void it_must_not_allow_null_arguments() throws InterruptedException {
        EntityLocker<Integer> locker = new EntityLockerImpl<>();

        try {
            locker.lock(null);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            locker.lock(null, 1, TimeUnit.SECONDS);
            fail();
        } catch (IllegalArgumentException e) {
        }

        try {
            locker.lock(1, 1, null);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void it_must_provide_reentrancy() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        int entityId = 1;

        locker.lock(entityId);
        locker.lock(entityId);
        locker.lock(entityId);

        locker.unlock(entityId);
        locker.unlock(entityId);
        locker.unlock(entityId);

        executorService.submit(() -> {
            locker.lock(entityId);
            locker.unlock(entityId);
        }).get(10, TimeUnit.SECONDS);
    }

    @Test
    public void it_must_throw_exception_when_unlocking_lock_held_by_other_thread() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        CountDownLatch latch = new CountDownLatch(1);
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
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
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        locker.acquireGlobalLock();

        try {
            executorService.submit(() -> locker.lock(1)).get(10, TimeUnit.SECONDS);
            fail();
        } catch (TimeoutException e) {
        }
        locker.releaseGlobalLock();
    }

    @Test
    public void it_must_provide_locking_with_timeout() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        int entityId = 1;

        locker.lock(entityId);

        executorService.submit(() -> {
            try {
                locker.lock(entityId, 5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).get(10, TimeUnit.SECONDS);

        locker.unlock(entityId);
    }

    @Test
    public void it_must_provide_interruptible_locking() {
        EntityLocker<Integer> locker = new EntityLockerImpl<>();
        int entityId = 1;

        locker.lock(entityId);

        Thread t = new Thread(() -> {
            try {
                locker.lockInterruptibly(entityId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        t.start();
        t.interrupt();
        assertTrue(t.isInterrupted());

        locker.unlock(entityId);
    }


}