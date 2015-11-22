import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Node {
	private final String NAME;
	private final PoissonDistribution poisson;	//How often do frames arrive?
	private final CSMACD csmacd;				//How do nodes know if it's okay to transmit?	
	private final RandomBackoff random;			//How does the node choose how long to wait?
		
	private final long FRAME_SIZE = 8000;			//How many bits is each frame?
	private final long TRANS_SPEED = 	100000000;	//bits per second
	private final long TRANS_TIME = (1000000 * FRAME_SIZE) / TRANS_SPEED;	//in b/s
	
	private ArrayList<Bus> busses;					//What busses are connected to this node?
	private int currentBackoff,						//How many slots does this node have to wait before transmitting?
				currentID,							//What node # is next?
				buffer,								//How many nodes are waiting in the queue
				collisionsAtNode;					//How many times has this node detected a collision?
	
	private Map<Frame, Bus> frameBus;				//TODO: MAPS
	private Map<Frame, Long> frameFinish;			//		ARE
	private Map<Frame, Long> frameCollisionCheck;	//		OVERKILL if I can only transmit one frame once.
													//But for a window size greater than 1, it might need it?
	private Frame sent;								//The frame that this node sends. Single frame (instead of creating a new one)
													//	to save time in garbage collection.
	private PrintWriter writer;						//Writer for this node's stats file.
	
	public Node(String name, PoissonDistribution d, CSMACD a, RandomBackoff b) {
		this.NAME = name;
		this.poisson = d;
		this.csmacd = a;
		this.random = b;
		
		this.busses = new ArrayList<Bus>();
		this.buffer = 0;
		this.currentBackoff = 0;
		this.currentID = 1;
		this.collisionsAtNode = 0;
		
		this.frameBus = new HashMap<Frame, Bus>();
		this.frameFinish = new HashMap<Frame, Long>();
		this.frameCollisionCheck = new HashMap<Frame, Long>();
		this.sent = new Frame(); 
		this.writer = ProgressMonitor.getWriter(this.NAME + "_new_new.csv");
	}
	
	public void addBus(Bus b) {
		this.busses.add(b);
	}
	
	/**
	 * Receive frames from higher layers.
	 * Creates a stats element for every 1000 nodes.
	 */
	public void generateFrames() {
		int arrived = poisson.next();
		int remainder = (currentID - 1 + buffer) % 1000 + arrived; //currentID will be 1 above how many have been made
		buffer += arrived;										   //if remainder is above 1000, that means we need a new stats node.
		if (remainder >= 1000) {
			int id = (currentID - 1 + buffer) / 1000 * 1000; //rounding to the nearest thousand
			addStats(id);
		}
	}
	
	/**
	 * Checks to see if the node can transmit.
	 * If it can, then it sends one out.
	 */
	public void sendFrameIfReady() {
		currentBackoff = Math.max(currentBackoff-1, 0);
		if (currentBackoff > 0 || buffer == 0 || !frameBus.isEmpty())	
			return;	

		Node dest = getRandomDestination(); //TODO: With only one, it doesn't matter if I record the destination.
		Bus path = findBestPath(dest);		//may throw exception if no path at all	
		if (csmacd.canAccess(path)) 		//Checks to see if the path can be accessed.
			sendFrame(dest, path);
	}

	/**
	 * "Creates" a new frame and stores it, then records the time at which the frame will finish
	 * transmitting and when we should check for collisions (2 times the propogation time)
	 * @param dest
	 * @param path
	 */
	private void sendFrame(Node dest, Bus path) {
		if (buffer == 0)
			throw new UnsupportedOperationException("sendFrame: No frames to be sent.");
		
		sent.setValues(currentID++, this, dest, FRAME_SIZE);
		long finish = Clock.addStep(TRANS_TIME),
		     collisionCheck = Clock.addStep(path.getPropTime(sent));
		frameBus.put(sent, path);
		frameFinish.put(sent, finish);						//For these two, finish and collisionCheck
		frameCollisionCheck.put(sent, collisionCheck);		//are the times at which to check.
		--buffer;			//One less frame on the queue.
		path.claim();		//One more node transmitting to this path.
		
		ProgressMonitor.recordTransmissionStart(this, sent, path);		
	}
	
	/**
	 * Check which frames are done transmitting, and if any are, put them fully on the bus
	 * for the propagation phase.
	 */
	public void finishTransmission() {
		for (Frame frame : frameBus.keySet()) {		//For all frames currently in the bus...
			if (!frameFinish.containsKey(frame)) 	//...check to see if it's still transmitting...
				continue;			
			
			if (Clock.equalsTime(frameFinish.get(frame))) {	//...and if it is, see if it's time for that to end.
				Bus path = frameBus.get(frame);
				path.putOnBus(frame);
				frameFinish.remove(frame);
				ProgressMonitor.recordFinishTransmission(this, frame);
			}
		}
	}

	/**
	 * Find the best path to get to the destination node.
	 * @param dest		The node to get to.
	 * @return			The best path to get to that node.
	 * @throws UnsupportedOperationException
	 */
	private Bus findBestPath(Node dest) throws UnsupportedOperationException {
		return busses.iterator().next(); //shortcut because I know there'll be only one
		
		/**
		 * Commented out, for when we have more than two nodes.
		 */
//		Bus path = null;
//		
//		//TODO: What if I want to find the shortest path?
//		for (Bus bus : busses) {
//			double distance = bus.getDistance(this, dest);
//			if (distance < Double.POSITIVE_INFINITY) {
//				path = bus;
//				break;
//			}
//		}
//		
//		if (path == null)
//			throw new UnsupportedOperationException("sendFrames: No viable path to destination");
//		
//		return path;
	}
	
	/**
	 * For all nodes currently in transmission, check to see if there has been a collision on the Bus.
	 */
	public void checkCollision() {
		ArrayList<Frame> toRemove = new ArrayList<Frame>();	
		
		for (Frame frame : frameCollisionCheck.keySet()) {
			if (Clock.equalsTime(frameCollisionCheck.get(frame))) {
				Bus path = frameBus.get(frame);
				if (path.hasCollision()) {
					currentBackoff = getBackoff();
					--currentID;
					++buffer;	//TODO: Will have to change for more than 2 nodes (since just getting rid of 
					++collisionsAtNode;			// the frame doesn't maintain destination).
					path.release();
					
					toRemove.add(frame);
					ProgressMonitor.recordCollision(this, currentBackoff);
				} 
			}
		}
		
		for (Frame frame : toRemove) {
			frameBus.remove(frame);
			frameCollisionCheck.remove(frame);
		}
	}
	
	public void acceptFrame(Frame f) {
		//TODO: Nothing happens right now.
	}
	
	/**
	 * When the ACK returns, remove the frame from any storage.
	 */
	public void acceptACK(Frame f) {
		frameBus.remove(f);
		frameCollisionCheck.remove(f);
	}
	
	/**
	 * Pick any accessible random Node for a destination.
	 * @return the Node destination.
	 * @throws UnsupportedOperationException	if no destination is available.
	 */
	public Node getRandomDestination() throws UnsupportedOperationException {
		//Random rand = new Random();
		ArrayList<Node> destinations = getAccessibleNodes();		
		if (destinations.size() == 0)
			throw new UnsupportedOperationException("getRandomDestination: No viable destinations.");
		int index = 0;//rand.nextInt(destinations.size());
		return destinations.get(index);
	}
	
	/**
	 * get all nodes that are accessible from this.
	 * @return
	 */
	private ArrayList<Node> getAccessibleNodes() {
		ArrayList<Node> accessibleNodes = new ArrayList<Node>();
		for (Bus bus : this.busses) {
			for (Node node : bus.getNodes()) {
				if (!accessibleNodes.contains(node) && !node.equals(this)) {
					accessibleNodes.add(node);
				}
			}				
		}
		return accessibleNodes;
	}
	
	/**
	 * Use the BACKOFF method to get a backoff for this.
	 * @return
	 */
	private int getBackoff() {
		return this.random.getBackoff();
	}
	
	public String getName() {
		return this.NAME;
	}
	
	public int getBuffer() {
		return this.buffer;
	}
	
	public int getCurrentID() {
		return this.currentID;
	}
	
	public int getCollisions() {
		return collisionsAtNode;
	}
	//////////////////////////////
	
	/**
	 * A class to collect stats. Called from the progress monitor.
	 */
	public class Stats {
		private long creation, start, finish, delivery;
		private int collisions;		
		public Stats() { creation = Clock.time(); start = finish = delivery = collisions = 0; }
		public void start(long l) { if (start == 0)  start = l; }
		public void finish(long l) { finish = l; }
		public void deliver(long l) { delivery = l; }
		public void collide() { ++collisions; }
		public String toString() { return Clock.time()+","+creation+","+start+","
									+collisions+","+finish+","+delivery+","
									+collisionsAtNode+","+buffer; }
	}
	private Map<Integer, Stats> stats = new HashMap<Integer, Stats>();	//Holds stats for every 1000 nodes.
	public void addStats(int id) { stats.put(id, new Stats()); } 
	public Stats getStats(int id) { if (stats.containsKey(id)) return stats.get(id); else return null; }
	public void removeStats(int id) { if (stats.containsKey(id)) stats.remove(id); }
	public PrintWriter getWriter() { return writer; }
	
}