package nachos.proj1;

import nachos.threads.Communicator;
import nachos.threads.KThread;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class CommunicatorTests {

	@Test
	public void testCommunicator() {
		final Communicator c = new Communicator();
		final Speaker s = new Speaker(0xdeadbeef, c);
		final Listener l = new Listener(c);

		final KThread thread1 = new KThread(s).setName("Speaker Thread");
		final KThread thread2 = new KThread(l).setName("Listener Thread");

		UnitTests.enqueueJob(new Runnable() {

			@Override
			public void run() {
				thread1.fork();
				thread2.fork();

				thread1.join();
				thread2.join();
			}

		});
		assertTrue("Incorrect Message recieved", 0xdeadbeef == l.getMessage());
	}

	private static class Listener implements Runnable {
		private int msg;
		private Communicator commu;
		private boolean hasRun;

		private Listener(Communicator commu) {
			this.commu = commu;
			this.hasRun = false;
		}

		public void run() {
			System.out.println("Listener Listening!");
			msg = commu.listen();
			System.out.println("Listener Return!");
			hasRun = true;
		}

		private int getMessage() {
			assertTrue("Listener has not finished running", hasRun);
			return msg;
		}
	}

	private static class Speaker implements Runnable {
		private int msg;
		private Communicator commu;

		private Speaker(int msg, Communicator commu) {
			this.msg = msg;
			this.commu = commu;
		}

		public void run() {
			System.out.println("Speaker Speaking!");
			commu.speak(msg);
			System.out.println("Speaker Return!");
		}
	}
}
