import java.util.PriorityQueue;

/**
 * The clock keeps track of microseconds elapsed, representing the current
 * time as a long value. Every node and bus keeps track of events (e.g.
 * end of transmission, check for collision) as one of these long value,
 * and that's how everything knows how to take the next step.
 * @author Justin
 *
 */
public class Clock {
	private static long time = 0,
							  //delta = BigDecimal.ZERO,
							  duration = 0,
							  nextSlot = 0;
	private static boolean isSlot = true;
	private static PriorityQueue<Long> nextSteps = new PriorityQueue<Long>();
	private final static long defaultStep = 50;		//in seconds
	
	public static void reset() {
		time = 0;
		duration = 0;
		nextSlot = 0;
		isSlot = true;
		nextSteps = new PriorityQueue<Long>();
	}
	
	/**
	 * Accepts a duration in seconds, converts and stores it in microseconds.
	 * @param d		Duration of simulation in seconds.
	 */
	public static void setDuration(long d) {
		duration = 1000000*d;
	}
	
	/**
	 * Set another event in the clock so the simulations stops at that time.
	 * @param next		The number of microseconds from now the new event should happen.
	 * @return			The objective clock time at which the next event will occur.
	 */
	public static long addStep(long next) {
		long nextTime = time + next;
		if (!nextSteps.contains(nextTime))
			nextSteps.add(nextTime);
		return nextTime;
	}
	
	/**
	 * Move the clock forward to the next event.
	 * @return	True if we're still in simulation time, false if we've passed the input duration
	 */
	public static boolean step() {
		if (isSlotTime()) {
			nextSlot = addStep(defaultStep);
		}

		time = nextSteps.remove();		//TODO: Add check to make sure this never throws an error?		
		isSlot = time == nextSlot;		
		return time < duration; //isNotDone
	}
	
	public static long time() {
		return time;
	}
	
	public static void printTime() {
		System.out.printf("Time: %.5f\n", (double) time / 1000000);
	}
	
	/**
	 * Compares an input time in microseconds to the current time and returns true if they're equal.
	 * Since nodes and busses use clock times to record when they should be done transmitting a node
	 * or whatever, I use this so that other classes can tell if its time yet to do their action.
	 * @param otherTime		The comparison time.
	 * @return	true if the input time is now, false otherwise.
	 */
	public static boolean equalsTime(Long otherTime) {
		return time == otherTime; 
	}
	
	public static boolean isSecond() {
		return time > 0 && time % 1000000 == 0;
	}
	
	/**
	 * Is it a time at which new frames can be sent out?
	 * @return
	 */
	public static boolean isSlotTime() {
		return isSlot;
	}
}