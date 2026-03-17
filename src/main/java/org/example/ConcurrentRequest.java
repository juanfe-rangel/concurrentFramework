package org.example;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConcurrentRequest {
    public ExecutorService executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final int hilos;

    public ConcurrentRequest(int hilos) {
        this.hilos = hilos;
        this.executor = Executors.newFixedThreadPool(hilos);
    }

    public void initiateGracefulShutdown() {
        shutdown.set(true);
        executor.shutdown();
    }

    public boolean isShuttingDown() {
        return shutdown.get();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }
}
