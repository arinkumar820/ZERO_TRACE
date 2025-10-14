package com.sameetasadullah.i180479_180531;

import org.json.JSONObject;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TransportScheduler
 * - Sends real messages from a queue at a constant cadence with randomized jitter
 * - When idle, sends cover packets via Sender.sendCover()
 */
public class TransportScheduler {

    public interface Sender {
        void send(JSONObject msg);
        void sendCover();
    }

    private final long baseIntervalMs;
    private final long jitterMs;
    private final Sender sender;

    private final BlockingQueue<JSONObject> realQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ScheduledExecutorService executor;

    public TransportScheduler(long baseIntervalMs, long jitterMs, Sender sender) {
        this.baseIntervalMs = Math.max(1, baseIntervalMs);
        this.jitterMs = Math.max(0, jitterMs);
        this.sender = sender;
    }

    /** Start ticking. Safe to call multiple times. */
    public synchronized void start() {
        if (running.get()) return;
        running.set(true);
        executor = Executors.newSingleThreadScheduledExecutor();
        scheduleNext();
    }

    /** Stop ticking and clear executor. Can be started again later. */
    public synchronized void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** Enqueue a real message to be sent on the next tick. */
    public void enqueue(JSONObject message) {
        if (message != null) {
            realQueue.offer(message);
        }
    }

    private void scheduleNext() {
        if (!running.get() || executor == null) return;
        long delay = computeDelayWithJitter();
        executor.schedule(this::tick, delay, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        try {
            JSONObject next = realQueue.poll();
            if (next != null) {
                sender.send(next);
            } else {
                sender.sendCover();
            }
        } catch (Throwable t) {
            // Swallow to keep scheduler alive
        } finally {
            if (running.get()) {
                scheduleNext();
            }
        }
    }

    private long computeDelayWithJitter() {
        if (jitterMs <= 0) return baseIntervalMs;
        long j = ThreadLocalRandom.current().nextLong(-jitterMs, jitterMs + 1);
        long d = baseIntervalMs + j;
        return Math.max(1, d);
    }
}
