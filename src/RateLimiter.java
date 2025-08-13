import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token-bucket-like RateLimiter to throttle operations.
 * Allows up to "permitsPerInterval" operations per interval.
 */
public class RateLimiter {
    private final long intervalNanos;
    private final int permitsPerInterval;
    private final AtomicLong nextAvailable = new AtomicLong(0);
    private final AtomicLong permitsLeft = new AtomicLong(0);

    /**
     * Create a RateLimiter.
     * @param permitsPerInterval max operations per interval
     * @param interval the interval length
     * @param unit time unit of interval
     */
    public RateLimiter(int permitsPerInterval, long interval, TimeUnit unit) {
        if (permitsPerInterval <= 0) throw new IllegalArgumentException("permitsPerInterval must be > 0");
        if (interval <= 0) throw new IllegalArgumentException("interval must be > 0");
        this.permitsPerInterval = permitsPerInterval;
        this.intervalNanos = unit.toNanos(interval);
        this.permitsLeft.set(permitsPerInterval);
    }

    /**
     * Acquire a permit, blocking minimally to honor the configured rate.
     */
    public void acquire() {
        while (true) {
            long now = System.nanoTime();
            long availableTime = nextAvailable.get();

            // Refill window if interval elapsed
            if (now >= availableTime) {
                // Set next window start
                nextAvailable.set(now + intervalNanos);
                permitsLeft.set(permitsPerInterval);
            }

            long left = permitsLeft.get();
            if (left > 0 && permitsLeft.compareAndSet(left, left - 1)) {
                return; // permit granted
            }

            // No permits left: sleep until next window
            long waitNanos = Math.max(1_000_000, availableTime - now); // at least 1ms
            try {
                long ms = TimeUnit.NANOSECONDS.toMillis(waitNanos);
                long ns = waitNanos - TimeUnit.MILLISECONDS.toNanos(ms);
                Thread.sleep(ms, (int) ns);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Convenience factory: limit to N per second.
     */
    public static RateLimiter perSecond(int permitsPerSecond) {
        return new RateLimiter(permitsPerSecond, 1, TimeUnit.SECONDS);
    }
}
