package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
	    this.conditionLock = conditionLock;
        waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        Machine.interrupt().disable();
        conditionLock.release(); // release lock automatically
        KThread current = KThread.currentThread();
        waitQueue.waitForAccess(current);
        current.sleep(); // put current thread to sleep
        conditionLock.acquire(); // re-acquire lock
        Machine.interrupt().enable();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	    Lib.assertTrue(conditionLock.isHeldByCurrentThread());

        Machine.interrupt().disable();
        KThread next = waitQueue.nextThread();
        if (next != null) {
            next.ready(); // if there is a next thread, ready next thread
        }
        Machine.interrupt().enable();
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
    
    Machine.interrupt().disable();
    KThread next = waitQueue.nextThread();
    while (next != null) { // wakes all threads on the waitQueue
        wake();
        next = waitQueue.nextThread();
    }
    Machine.interrupt().enable();
    }
    private ThreadQueue waitQueue;
    private Lock conditionLock;
}