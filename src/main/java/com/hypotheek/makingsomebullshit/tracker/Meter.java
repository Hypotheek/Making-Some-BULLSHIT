package com.hypotheek.makingsomebullshit.tracker;

/**
 * Turns successive readings of a "remaining" counter into a spending rate. Project Expansion's links only
 * count these down and refill them to their maximum once a second, so a reading that goes up means a refill
 * happened.
 */
final class Meter {
    private static final int WINDOW_TICKS = 100;
    private static final double TICKS_PER_SECOND = 20.0;

    private final long[] spent = new long[WINDOW_TICKS];
    private int next;
    private int filled;
    private long total;
    private long previous;
    private boolean hasPrevious;

    /** Call once per tick with the counter's current value and its maximum. */
    void sample(long remaining, long max) {
        if (hasPrevious) {
            record(spentSince(previous, remaining, max));
        }
        previous = remaining;
        hasPrevious = true;
    }

    /** Alternative to sample() for amounts that are reported directly: call once per tick with that tick's amount. */
    void add(long amount) {
        record(amount);
    }

    /** Average amount spent per second over the last few seconds, or over fewer ticks while warming up. */
    double perSecond() {
        return filled == 0 ? 0 : total * TICKS_PER_SECOND / filled;
    }

    static long spentSince(long previous, long remaining, long max) {
        if (remaining <= previous) return previous - remaining;

        // Refilled to the maximum, then spent down to the current value.
        return Math.max(0, max - remaining);
    }

    private void record(long value) {
        total += value - spent[next];
        spent[next] = value;
        next = (next + 1) % WINDOW_TICKS;
        if (filled < WINDOW_TICKS) filled++;
    }
}
