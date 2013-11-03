package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    public static void selfTest() {
    	/*
         System.out.println("---------------------PrioritySchedulerTest--------------------");
         System.out.println("----------------------Test1------------------------------------");
         System.out.println("testing basic priorityqueue, with simple single donation.............................");
         
         PriorityScheduler donatingScheduler = new PriorityScheduler ();
         PriorityQueue donatingQ = (PriorityQueue) donatingScheduler.newThreadQueue(true);
         PriorityQueue donatingQ2 = (PriorityQueue) donatingScheduler.newThreadQueue(true);
         
         KThread thread1 = new KThread();
         KThread thread2 = new KThread();
         KThread thread3 = new KThread();
         KThread thread4 = new KThread();
         KThread thread5 = new KThread();
         
         
         thread1.setName("thread1");
         donatingScheduler.getThreadState(thread1).setPriority(1);
         
         thread2.setName("thread2");
         donatingScheduler.getThreadState(thread2).setPriority(2);
         
         thread3.setName("thread3");
         donatingScheduler.getThreadState(thread3).setPriority(3);
         
         thread4.setName("thread4");
         donatingScheduler.getThreadState(thread4).setPriority(4);
         
         thread5.setName("thread5");
         donatingScheduler.getThreadState(thread5).setPriority(5);
         
         
         boolean intStatus = Machine.interrupt().disable();
         
         
         donatingQ.acquire(thread1);
         donatingQ.waitForAccess(thread3);
         
         donatingQ2.acquire(thread3);
         donatingQ2.waitForAccess(thread4);
         donatingQ2.waitForAccess(thread5);
         donatingScheduler.getThreadState(thread1).print();
         donatingScheduler.getThreadState(thread3).print();
         System.out.println("Effective priority of thread 1 is: " + donatingScheduler.getThreadState(thread1).getEffectivePriority());
         System.out.println("Effective priority of thread 3 is: " + donatingScheduler.getThreadState(thread3).getEffectivePriority());
         System.out.println("Current owner is: " + donatingQ.ownerThreadState.thread);
         System.out.println("Next out of the queue: (should be thread5)");
         System.out.println(donatingQ.nextThread());
         System.out.println("Next out of the queue: (should be thread4)");
         System.out.println(donatingQ.nextThread());
         */
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        return new PriorityQueue(transferPriority);
    }
    
    public int getPriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        
        return getThreadState(thread).getPriority();
    }
    
    public int getEffectivePriority(KThread thread) {
        Lib.assertTrue(Machine.interrupt().disabled());
        
        return getThreadState(thread).getEffectivePriority();
    }
    
    public void setPriority(KThread thread, int priority) {
        Lib.assertTrue(Machine.interrupt().disabled());
        
        Lib.assertTrue(priority >= priorityMinimum &&
                       priority <= priorityMaximum);
        
        getThreadState(thread).setPriority(priority);
    }
    
    public boolean increasePriority() {
        boolean intStatus = Machine.interrupt().disable();
        
        KThread thread = KThread.currentThread();
        
        int priority = getPriority(thread);
        if (priority == priorityMaximum)
            return false;
        
        setPriority(thread, priority+1);
        
        Machine.interrupt().restore(intStatus);
        return true;
    }
    
    public boolean decreasePriority() {
        boolean intStatus = Machine.interrupt().disable();
        
        KThread thread = KThread.currentThread();
        
        int priority = getPriority(thread);
        if (priority == priorityMinimum)
            return false;
        
        setPriority(thread, priority-1);
        
        Machine.interrupt().restore(intStatus);
        return true;
    }
    
    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;
    
    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);
        
        return (ThreadState) thread.schedulingState;
    }
    
    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority.
     */
    protected class PriorityQueue extends ThreadQueue {
        PriorityQueue(boolean transferPriority) {
            this.transferPriority = transferPriority;
        }
        
        public void waitForAccess(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).waitForAccess(this);
            priorityQ.add(getThreadState(thread));
        }
        
        public void acquire(KThread thread) {
            Lib.assertTrue(Machine.interrupt().disabled());
            getThreadState(thread).acquire(this);
            ownerThreadState = getThreadState(thread);
        }
        
        public KThread nextThread() {
            Lib.assertTrue(Machine.interrupt().disabled());
            /*
             if (ownerThreadState != null) {
             ownerThreadState.release(this);
             }
             ownerThreadState = priorityQ.poll();
             if (ownerThreadState != null) {
             ownerThreadState.acquire(this);
             return ownerThreadState.thread;
             }
             return null;
             */
            if(priorityQ.isEmpty()){
                return null;
            }else{
                acquire(priorityQ.poll().thread);
                return this.ownerThreadState.thread;
            }
            
            
        }
        
        /**
         * Return the next thread that <tt>nextThread()</tt> would return,
         * without modifying the state of this queue.
         *
         * @return	the next thread that <tt>nextThread()</tt> would
         *		return.
         */
        protected ThreadState pickNextThread() {
            if (priorityQ.peek() != null) {
                return priorityQ.peek();
            }
            return null;
        }
        
        public void print() {
            Lib.assertTrue(Machine.interrupt().disabled());
            // implement me (if you want)
        }
        
        /**
         * <tt>true</tt> if this queue should transfer priority from waiting
         * threads to the owning thread.
         */
        public boolean transferPriority;
        public ThreadState ownerThreadState;
        public java.util.PriorityQueue<ThreadState> priorityQ = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator<ThreadState>(this));
        
        protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> {
			protected ThreadStateComparator(PriorityQueue pq) {
				priorityQueue = pq;
			}
			
			@Override
			public int compare(T first, T second) {
				//first compare by effective priority
				int first_ep = first.getEffectivePriority();
				int second_ep = second.getEffectivePriority();
				if (first_ep > second_ep) {
					return -1;
				} else if (first_ep < second_ep) {
					return 1;
				} else {
					//compare by the times these threads have spent in this queue
					long first_age = first.ownedQueuesList.get(priorityQueue);
					long second_age = second.ownedQueuesList.get(priorityQueue);
					
					if (first_age < second_age) {
						return -1;
					} else if(first_age > second_age) {
						return 1;
					} else {
						return 0;
					}
				}
			}
			
			private PriorityQueue priorityQueue;
			
		}
        
    }
    
    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queue
     * it's waiting for, if any.
     *
     * @see	nachos.threads.KThread#schedulingState
     */
    protected class ThreadState {
    	
    	/** The thread with which this object is associated. */
    	protected KThread thread;
    	/** The priority of the associated thread. */
    	protected int priority;
    	/** The effective priority of the associated thread. */
    	protected int effectivePriority;
    	/** A Hashmap of all the PriorityQueues this ThreadState is waiting on mapped to the time they were waiting on them*/
    	protected HashMap<PriorityQueue,Long> ownedQueuesList = new HashMap<PriorityQueue,Long>();
    	/** The Set the thread has aquired */
    	protected HashSet<PriorityQueue> onQueue = new HashSet<PriorityQueue>();
        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param	thread	the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            this.thread = thread;
            
            setPriority(priorityDefault);
        }
        
        
        /**
         * Return the priority of the associated thread.
         *
         * @return	the priority of the associated thread.
         */
        public int getPriority() {
            return priority;
        }
        
        /**
         * Return the effective priority of the associated thread.
         *
         * @return	the effective priority of the associated thread.
         */
        public int getEffectivePriority() {
            return effectivePriority;
        }
        
        /**
         * Set the priority of the associated thread to the specified value.
         *
         * @param	priority	the new priority.
         */
        public void setPriority(int priority) {
            if (this.priority == priority)
                return;
            
            this.priority = priority;
            
            updateEffectivePriority();
        }
        
        /**
         * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
         * the associated thread) is invoked on the specified priority queue.
         * The associated thread is therefore waiting for access to the
         * resource guarded by <tt>waitQueue</tt>. This method is only called
         * if the associated thread cannot immediately obtain access.
         *
         * @param	waitQueue	the queue that the associated thread is
         *				now waiting on.
         *
         * @see	nachos.threads.ThreadQueue#waitForAccess
         */
        
        public void waitForAccess(PriorityQueue waitQueue) {
            /*
             onQueue = waitQueue;
             if (onQueue.ownerThreadState != null) {
             onQueue.ownerThreadState.updateEffectivePriority();
             }
             */
            //if we currently own it, release and add again to update value
            if(!ownedQueuesList.containsKey(waitQueue)){
                this.release(waitQueue);
                
            }
            //add it to the waiting queue here with the time it was added
            this.ownedQueuesList.put(waitQueue, Machine.timer().getTime());
            //add it to the PriorityQ for donations
            waitQueue.priorityQ.add(this);
            //update the owner of this priorityqueue if it has a owner
            if(waitQueue.ownerThreadState != null){
                waitQueue.ownerThreadState.updateEffectivePriority();
            }
            
            
        }
        
        
        /**
         * Called when the associated thread has acquired access to whatever is
         * guarded by <tt>waitQueue</tt>. This can occur either as a result of
         * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
         * <tt>thread</tt> is the associated thread), or as a result of
         * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
         *
         * @see	nachos.threads.ThreadQueue#acquire
         * @see	nachos.threads.ThreadQueue#nextThread
         */
        
        public void acquire(PriorityQueue waitQueue) {
            //ownedQueuesList.add(waitQueue);
            /*if there currently is an owner, remove him*/
            if(waitQueue.ownerThreadState !=null){
                waitQueue.ownerThreadState.release(waitQueue);
            }
            //remove this thread if it exisit in priorityQ since we want to update value and now is owner
            waitQueue.priorityQ.remove(this);
            
            //make the owner this
            waitQueue.ownerThreadState = this;
            //put it on the aquired list
            this.onQueue.add(waitQueue);
            //remove it from the waitlist
            this.ownedQueuesList.remove(waitQueue);
            
            
            //update accordingly
            updateEffectivePriority();
        }
        
        
        public void release(PriorityQueue waitQueue) {
            //ownedQueuesList.remove(waitQueue);
            
            /*if there is something to release, make sure remove the owner, and update */
            if(this.onQueue.remove(waitQueue)){
                waitQueue.ownerThreadState = null;
                updateEffectivePriority();
            }
            
            //else there was nothing to release
            
            
        }
        
        
        public void updateEffectivePriority () {
            /*
             if (onQueue != null && onQueue.transferPriority) {
             //** Pull effectivePriority from all resources this thread owns*/
			/*
             for (PriorityQueue resourceQueue: ownedQueuesList) {
             for (ThreadState ts: resourceQueue.priorityQ) {
             if (ts.getEffectivePriority() > effectivePriority) {
             effectivePriority = ts.getEffectivePriority();
             System.out.println("updating..");
             
             }
             }
             }
             }	
             else {
             effectivePriority = priority;
             }
             */
            
            //first pull out all prioritys that are currently aquired
            for(PriorityQueue pq :  ownedQueuesList.keySet()){
                pq.priorityQ.remove(this);
                
            }
            //this is this temp state current priority
            int tempP = this.priority;
            
            //for each thread that is waing, check if it can transfer
            for(PriorityQueue pq : onQueue){
                if(pq.transferPriority){
                    //if it can, the first one from the priorityqueue is the best priority
                    ThreadState beststate = pq.pickNextThread();
                    if(beststate !=null){
                        //if it exsist, compair with current priority
                        int bestEP = beststate.getEffectivePriority();
                        if(bestEP > tempP){
                            tempP = bestEP;
                        }
                    }
                }
            }
            //check if priority donations already happened owners
            boolean transfering = (tempP != this.getEffectivePriority());
            this.effectivePriority = tempP;
            //return the owner to reset values
            for(PriorityQueue pq: ownedQueuesList.keySet()){
                pq.priorityQ.add(this);
            }
            //if owner Effect was different than it should have been
            if(transfering){
                for(PriorityQueue pq : ownedQueuesList.keySet()){
                    //recalculate the owner
                    if(pq.transferPriority && pq.ownerThreadState !=null){
                        pq.ownerThreadState.updateEffectivePriority();
                    }
                }
            }
            
            
        }
        
        /*
         
         public int compareTo(ThreadState ts) {
         if (ts == null){
         return -1;
         }else if (this.getEffectivePriority() < ts.getEffectivePriority()){
         return 1;
         }else if (this.getEffectivePriority() > ts.getEffectivePriority()){
         return -1;
         }
         else{
         long this_age = this.ownedQueuesList.get(this.holder);
         long other_age = ts.ownedQueuesList.get(ts.holder); 
         if(this_age < other_age){
         return -1;
         } else if (this_age > other_age){
         return 1;
         }else{
         return 0;
         }
         
         }
         }
         
         */
        
        
    }
    
    
}



