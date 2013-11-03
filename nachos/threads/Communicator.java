package nachos.threads;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
   final Lock lock;
   final Condition speaker;
   final Condition listener;
   private Boolean word_used = false; // boolean condition to make sure that there is no waiting data queue
   private int word_shared = 0;
   private int waitqueue = 0;
    
    public Communicator() {
    lock = new Lock();
    speaker = new Condition(lock);  //initializing the conditions for speaker and listner
    listener = new Condition(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param   word    the integer to transfer.
     */
    public void speak(int word) {
    lock.acquire();
    while (waitqueue == 0){ // check if there is a listener waiting or received to wake up an active listener
        speaker.sleep();
    }
    while (word_used){  // checking if there is any active speaker prior to this.speaker
        speaker.sleep();
    }
    word_shared = word;
    word_used = true;
    listener.wake();
    lock.release();

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return  the integer transferred.
     */    
    public int listen() {
    lock.acquire();
    waitqueue++;
    speaker.wake();   //waking up the speaker sleeping in the sending queue
    listener.sleep();  
    int word = word_shared;
    word_used = false;
    waitqueue--;
    speaker.wake();
    lock.release();
    return word;
    }


 }
