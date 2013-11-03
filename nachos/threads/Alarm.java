package nachos.threads;

import nachos.machine.*;
import java.lang.Long;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	    Machine.timer().setInterruptHandler(new Runnable() {
		    public void run() { timerInterrupt(); }
        });
        Comparator<WaitThread> comparator = new WaitThreadComparator();
        waitQueue = new PriorityQueue<WaitThread>(10, comparator);
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
	    Machine.interrupt().disable();
        WaitThread first = waitQueue.peek();
        while (first != null && first.getTime() <= Machine.timer().getTime()) { // wakes all threads that have wake time <= current time
            waitQueue.remove(first);
            first.getThread().ready();
            first = waitQueue.peek(); // since priority queue is ordered by wake time, we can stop the loop once we hit a waitThread with wake time > current time
        }
        Machine.interrupt().enable();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
	    Machine.interrupt().disable();
        long wakeTime = Machine.timer().getTime() + x;
        KThread current = KThread.currentThread();
        WaitThread waiting = new WaitThread(current, wakeTime);
        waitQueue.add(waiting); // add WaitThread to waitQueue, awoken by timerInterrupt
        current.sleep();
        Machine.interrupt().enable();
    }

    private PriorityQueue<WaitThread> waitQueue;
}


class WaitThread { // new class that holds a thread and wake time
    private KThread thread;
    private long wakeTime;
    
    public WaitThread() {
        thread = null;
        wakeTime = 0;
    }
    public WaitThread(KThread kt, long time) {
        thread = kt;
        wakeTime = time;
    }
    public KThread getThread() {
        return thread;
    }
    public long getTime() {
        return wakeTime;
    }
}

class WaitThreadComparator implements Comparator<WaitThread> {
    public int compare(WaitThread t1, WaitThread t2) {
        Long time1 = t1.getTime();
        Long time2 = t2.getTime();
        return time1.compareTo(time2);
    }
}
