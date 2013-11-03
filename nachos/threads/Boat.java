package nachos.threads;

import java.util.HashMap;

import nachos.ag.BoatGrader;

public class Boat {
	static BoatGrader bg;
	static int mChildren;
	static int mAdults;
	static int oChildren;
	static int oAdults;
	static Lock boat;
	static Condition molokai;
	static Condition oahu;
	static Lock awake;
	static Lock end;
	static Condition terminate;
	static String boatLocation;
	static HashMap<String, Condition> islands;
	static boolean passengerWaiting;
	static int k;
	public static void selfTest() {
		BoatGrader b = new BoatGrader();

		// System.out.println("\n ***Testing Boats with only 2 children***");
		//begin(0, 2, b);

		// System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		// begin(1, 2, b);

		// System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		 begin(100, 100, b);
	}

	public static void begin(int adults, int children, BoatGrader b0) {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b0;

		// Instantiate global variables here
		// the boat, first entry is the boat, second entry is boat status string
		islands = new HashMap<String, Condition>();

		boat = new Lock();
		molokai = new Condition(boat);
		oahu = new Condition(boat);
		awake = new Lock();
		end = new Lock();
		terminate = new Condition(end);
		islands.put("Oahu", oahu);
		islands.put("Molokai", molokai);
		// counts of people on islands
		mChildren = 0;
		mAdults = 0;
		oChildren = 0;
		oAdults = 0;
		k = 0;
		boatLocation = "Oahu";
		passengerWaiting = false;
		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		for (int a = 0; a < adults; a++) {
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.fork();
		}
		for (int b = 0; b < children; b++) {
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.fork();
		}
		end.acquire();
		while (mAdults < adults || mChildren < children) {
			terminate.sleep();
			//System.out.println("Just received terminate signal with " + mAdults + " madults and " + mChildren + " mchildren");
		}
	}

	static void AdultItinerary() {
		// thread wakes up
		awake.acquire();
		String location = "Oahu";
		oAdults += 1;
		awake.release();
		boat.acquire();
		// don't go until we can get the boat and also there's children on
		// Molokai
		while (!boatLocation.equals(location) || mChildren == 0 || passengerWaiting || oChildren >= 2) {
			oahu.sleep();
		}
		// go
		bg.AdultRowToMolokai();
		boatLocation = "Molokai";
		mAdults += 1;
		oAdults -= 1;
		molokai.wake();
		// let the master thread know we made it
		end.acquire();
		terminate.wake();
		end.release();
		boat.release();

	}

	static void ChildItinerary() {
		double randy = Math.random();
		// thread wakes up
		awake.acquire();
		String location = "Oahu";
		oChildren += 1;
		//System.out.println("Awakening is " + randy);
		awake.release();
		// children sail back and forth forever
		while (true) {
			//System.out.println(randy + " springing into action");
			//System.out.println(k + " " + oAdults + " " + oChildren + " " + mAdults + " " + mChildren);
			//System.out.println(passengerWaiting);
			// don't go until we can get the boat
			boat.acquire();
			//System.out.println(oChildren);
			while (!boatLocation.equals(location)) {
				islands.get(location).sleep();
			}
			if (k > 10 && location.equals("Oahu") && mChildren == 0) {
				k = 0;
				oahu.sleep();
				boat.release();
				continue;
			}
			if (k > 10 && location.equals("Molokai") && oChildren == 0 && oAdults == 0 && mChildren > 1) {
				molokai.sleep();
				boat.release();
				continue;
			}
			if (location.equals("Oahu")) {
				if (oChildren > 1 && !passengerWaiting) {
					k = 0;
					oahu.wakeAll();
					//ride the ship
					passengerWaiting = true;
					oahu.wakeAll();
					oahu.sleep();
					passengerWaiting = false;
					bg.ChildRideToMolokai();
					oChildren -= 1;
					mChildren += 1;
					location = "Molokai";
					end.acquire();
					terminate.wake();
					end.release();
					boat.release();
					continue;
				}
				if (passengerWaiting) {
					//captain the ship
					k = 0;
					bg.ChildRowToMolokai();
					oChildren -= 1;
					mChildren += 1;
					location = "Molokai";
					boatLocation = "Molokai";
					oahu.wakeAll();
					// let the master thread know we made it
					end.acquire();
					terminate.wake();
					end.release();
					molokai.sleep();
					boat.release();
					continue;
				}
				if (oChildren == 1) {
					//sail alone
					k += 1;
					bg.ChildRowToMolokai();
					oChildren -= 1;
					mChildren += 1;
					location = "Molokai";
					boatLocation = "Molokai";
					end.acquire();
					terminate.wake();
					end.release();
					boat.release();
					continue;
				}
			}
			if (location.equals("Molokai")) {
				bg.ChildRowToOahu();
				oChildren += 1;
				mChildren -= 1;
				location = "Oahu";
				boatLocation = "Oahu";
				oahu.wakeAll();
				if (oChildren != 1 || oAdults > 0) {

					oahu.sleep();
				}
				boat.release();
				//System.exit(1);
			}
		}
	}

	static void SampleItinerary() {
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

}
