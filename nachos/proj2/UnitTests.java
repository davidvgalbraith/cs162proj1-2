package nachos.proj2;

import nachos.test.unittest.TestHarness;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * This class extends the TestHarness class for setting up and running JUnit
 * tests with Nachos. You must list all of the classes that contain tests you
 * want to run in a comma separated list associated with the @SuiteClasses
 * attribute below.
 * 
 * An ExampleTests class has been included for you. Take a look at the
 * definition of this class to see some examples of how to create tests and have
 * them run as part of this test suite.
 * 
 * @author Kevin Klues <klueska@cs.berkeley.edu>
 * 
 */

@RunWith(Suite.class)
@SuiteClasses({ 
	ExampleTests.class, 
})
public class UnitTests extends TestHarness {}
