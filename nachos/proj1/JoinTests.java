package nachos.proj1;

import java.util.LinkedList;
import java.util.Queue;

import nachos.threads.KThread;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class JoinTests {
	/**
	 * Queue for storing ordered results from test runs
	 */
	Queue<String> resultsQ = new LinkedList<String>();

	/**
	 * Simple worker thread that does nothing but yield() in a loop for 100
	 * iterations. Usable by lots of tests, so instantiated as a class variable
	 */
	final KThread simpleWorker = new KThread(new Runnable() {
		@Override
		public void run() {
			resultsQ.add(simpleWorker.getName() + ":start");
			for (int i = 0; i < 100; i++)
				KThread.yield();
			resultsQ.add(simpleWorker.getName() + ":finish");
		}
	});

	@Test
	public void basicTest() {

		UnitTests.enqueueJob(new Runnable() {

			@Override
			public void run() {
				simpleWorker.setName("basicTestWorker");
				resultsQ.add("master:start");
				simpleWorker.fork();
				simpleWorker.join();
				resultsQ.add("master:finish");
			}

		});
		assertTrue("Master start not the first item!",
				resultsQ.poll().equals("master:start"));
		assertTrue("Worker start not the next item!",
				resultsQ.poll().equals("basicTestWorker:start"));
		assertTrue("Worker finish not the next item!",
				resultsQ.poll().equals("basicTestWorker:finish"));
		assertTrue("Master finish not the last item!",
				resultsQ.poll().equals("master:finish"));
		assertTrue("Results queue not empty!", resultsQ.poll() == null);
	}
}
