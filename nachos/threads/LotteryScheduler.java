package nachos.threads;

import nachos.machine.*;
import nachos.threads.PriorityScheduler.PriorityQueue;
import nachos.threads.PriorityScheduler.ThreadState;

import java.util.*;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends Scheduler {
    /**
     * Allocate a new lottery scheduler.
     */
	public static void selfTest() {
		
		System.out.println("---------------------LotterySchedulerTest--------------------");
        System.out.println("----------------------Test1------------------------------------");
        System.out.println("testing basic lotteryqueue, with simple single donation.............................");
        
        LotteryScheduler donatingScheduler = new LotteryScheduler ();
        LotteryQueue donatingQ = (LotteryQueue) donatingScheduler.newThreadQueue(true);
        LotteryQueue donatingQ2 = (LotteryQueue) donatingScheduler.newThreadQueue(true);
        
        KThread thread1 = new KThread();
        KThread thread2 = new KThread();
        KThread thread3 = new KThread();
        KThread thread4 = new KThread();
        KThread thread5 = new KThread();
        
        
        thread1.setName("thread1");
       donatingScheduler.getLotThreadState(thread1).setTickets(1);
        
        thread2.setName("thread2");
        donatingScheduler.getLotThreadState(thread2).setTickets(2);
        
        thread3.setName("thread3");
        donatingScheduler.getLotThreadState(thread3).setTickets(3);
        
        thread4.setName("thread4");
        donatingScheduler.getLotThreadState(thread4).setTickets(4);
        
        thread5.setName("thread5");
        donatingScheduler.getLotThreadState(thread5).setTickets(5);
        
        
        
        
        donatingQ.acquire(thread1);
        donatingQ.waitForAccess(thread3);
        System.out.println("Effective priority of thread 1 should be 4: " + donatingScheduler.getLotThreadState(thread1).donatedTickets);
        donatingQ2.acquire(thread3);
        donatingQ2.waitForAccess(thread4);
        donatingQ2.waitForAccess(thread5);
        System.out.println("Effective priority of thread 1 is: " + donatingScheduler.getLotThreadState(thread1).donatedTickets);
        System.out.println("Effective priority of thread 3 is: " + donatingScheduler.getLotThreadState(thread3).donatedTickets);
        System.out.println("Current owner is: " + donatingQ.masterThread.thread);
        System.out.println("Next out of the queue: (should be thread3)");
        
        System.out.println(donatingQ.nextThread());
        System.out.println("Next out of the queue: (should be none)");
        System.out.println(donatingQ.nextThread());
        
        System.out.println("----------------------Test1_is_done------------------------------------");
        System.out.println("Next out of the queue for second queue: (should be thread5 or 4 at least once)");
        System.out.println(donatingQ2.nextThread());
        System.out.println("Next out of the queue: (should be thread4 or 5 at least once)");
        System.out.println(donatingQ2.nextThread());
        
        System.out.println("----------------------Test2_is_done------------------------------------");
       
	}
	
	
	
	static final int priorityDefault = 1;
	//default priority
	static final int priorityMinimum = 1;

	static final int priorityMaximum = Integer.MAX_VALUE;
	//updated priority maximum
    public LotteryScheduler() {
    	
    }
    public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getLotThreadState(thread).getTickets();
	}
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());
		return getLotThreadState(thread).getFullyDonated();
	}
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());
		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
		getLotThreadState(thread).setTickets(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable(), returnBool = true;

		KThread thread = KThread.currentThread();

		int ticket = getPriority(thread);
		if (ticket == priorityMaximum)
			returnBool = false;
		else
			setPriority(thread, ticket + 1);

		Machine.interrupt().restore(intStatus);
		return returnBool;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable(), returnBool = true;

		KThread thread = KThread.currentThread();

		int ticket = getPriority(thread);
		if (ticket == priorityMinimum)
			returnBool = false;
		else
			setPriority(thread, ticket - 1);

		Machine.interrupt().restore(intStatus);
		return returnBool;
	}

    protected LotteryThreadState getLotThreadState(KThread thread) {
        if (thread.schedulingState == null)
                thread.schedulingState = new LotteryThreadState(thread);
        return (LotteryThreadState) thread.schedulingState;
}

    /**
     * Allocate a new lottery thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer tickets from waiting threads
     *					to the owning thread.
     * @return	a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	// implement me
	return new LotteryQueue(transferPriority);
    }
    protected class LotteryQueue extends ThreadQueue {
    	/**
		 * A random number generator for the lottery.
		 */
		Random randomGenerator = new Random();
		//need this for random lottery
		private LotteryThreadState masterThread; 
		//holds now the tickets instead of priority
    	private boolean transferTickets;
    	//see if priority donates happen I assume
    	private HashSet<LotteryThreadState> queue = new HashSet<LotteryThreadState>();
    	//a datastructure to hold the waiting threads
    	private int maxTickets;
    	//knows the value of the ticket max

    	LotteryQueue(boolean transferTickets2) {
			transferTickets = transferTickets2;
		}

		@Override
		public void waitForAccess(KThread thread) {
			// TODO Auto-generated method stub
			LotteryThreadState state = getLotThreadState(thread);
			state.waitForAccess(this);
		}
		private void revaluate(){
			for(LotteryThreadState val : this.queue){
				val.updateSumTicket();
			}
		}
		private int findMax(){
			int maxValue = 1;
			for (LotteryThreadState value : this.queue) {
				int temp = value.donatedTickets;
    			if (temp > maxValue) {
        			maxValue = temp;
    			}	
			}
			return maxValue;
		}

		@Override
		public KThread nextThread() {
			//System.out.println(this.queue.size()+"how many to remove"+this.queue.isEmpty());
			if(!this.queue.isEmpty()){
				//pick the value that represents the max of all the threads in queue
				
				int winning_ticket = this.randomGenerator.nextInt(this.findMax());
				//System.out.println("this is winning ticket"+winning_ticket);
				//randomly pick that number
				//go through an iterative loop, find that element, and remove it
				for(LotteryThreadState winner: this.queue){
					//System.out.println(winning_ticket+"wiiner vs ticket holder"+winner.donatedTickets);
					if(winning_ticket < winner.donatedTickets){
						KThread temp = winner.thread;
						//update ticketmax
						this.acquire(temp);
						this.revaluate();
						return temp;
					}
				}
			}
			//if none, return null
			return null;

		}
		@Override
		public void acquire(KThread thread) {
			// TODO Auto-generated method stub
			getLotThreadState(thread).acquire(this);
		}
		@Override
		public void print() {
			// TODO Auto-generated method stub
			
		}
		void removeAndUpdate(LotteryThreadState temp) {
            if (this.queue.remove(temp)) {
            	this.revaluate();
            	if (this.masterThread != null){
                   this.masterThread.updateSumTicket();
            	}
            }
    }
		
    }
    protected static class LotteryThreadState {
    	
    	LotteryThreadState(KThread thread2) {
			this.thread = thread2;
		}

		void acquire(LotteryQueue lotteryQueue) {
			if(lotteryQueue.masterThread != null){
				lotteryQueue.masterThread.release(lotteryQueue);
			}
			lotteryQueue.removeAndUpdate(this);
			lotteryQueue.masterThread = this;
			this.acquired.add(lotteryQueue);
			this.waiting.remove(lotteryQueue);
			this.updateSumTicket();

		}

		void waitForAccess(LotteryQueue lotteryQueue) {

			if(this.acquired.contains(lotteryQueue)){
                this.release(lotteryQueue);
            }
            this.waiting.add(lotteryQueue);
            lotteryQueue.queue.add(this);
            if(lotteryQueue.masterThread != null){
                lotteryQueue.masterThread.updateSumTicket();
            }
            this.updateSumTicket();
		}

		private void release(LotteryQueue lotteryQueue) {
			if(this.acquired.remove(lotteryQueue)){
				lotteryQueue.masterThread = null;
				this.updateSumTicket();
			}
		}
		void setTickets(int tickets2) {
			if(this.tickets == tickets2){
				return;
			}
			tickets = tickets2;
			this.updateSumTicket();
		}
		int getTickets() {
			return tickets;
		}

		void updateSumTicket(){
			//look at all my waiting, and add up and set that to me :D
			int temp = this.tickets;
			for(LotteryQueue lot: this.acquired){
				if(lot.transferTickets){
					for(LotteryThreadState waitingtotrans: lot.queue){
						temp += waitingtotrans.donatedTickets;
					}
				}
			}
			this.donatedTickets = temp;

			for(LotteryQueue wait : this.waiting){
				if(wait.masterThread!=null && wait.transferTickets){
					wait.masterThread.updateSumTicket();
				}
			}

		}
		public int getFullyDonated() {
			return this.donatedTickets;
		}




		
		
		private HashSet<LotteryQueue> acquired = new HashSet<LotteryQueue>();

		
		private HashSet<LotteryQueue> waiting = new HashSet<LotteryQueue>();

		
		private int tickets = priorityDefault;

		
		private int donatedTickets = priorityDefault;

		
		private KThread thread;
	}
}
