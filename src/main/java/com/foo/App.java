package com.foo;

import java.util.concurrent.Executors;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello World!");

        EntityLocker<Integer> locker = new EntityLocker<>();

        locker.unlock(1);

        System.out.println("done");

    }
}
