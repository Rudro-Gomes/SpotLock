package concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

// Gives out one lock per slot id so that two threads cannot run the booking or
// checkout flow for the SAME slot at the same time inside this JVM. The lock is
// a first line of defence; the authoritative guard is still the atomic SQL
// UPDATE in SlotDAO. The map is concurrent so lock lookup itself is thread-safe.
public class SlotLockManager {

    private final ConcurrentHashMap<Integer, ReentrantLock> locksBySlotId =
            new ConcurrentHashMap<>();

    // How long a thread waits for a slot's lock before giving up.
    private static final long LOCK_TIMEOUT_SECONDS = 2;

    // Tries to acquire the lock for one slot. Returns false if it could not be
    // taken within the timeout (the caller should treat this as "slot busy").
    public boolean tryLock(int slotId) {
        ReentrantLock lock = locksBySlotId.computeIfAbsent(slotId, id -> new ReentrantLock());
        try {
            return lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // Releases the lock for one slot. Safe to call only after a successful
    // tryLock; callers always do this in a finally block.
    public void unlock(int slotId) {
        ReentrantLock lock = locksBySlotId.get(slotId);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
