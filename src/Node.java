import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Node {
	private final String NAME;
	private final PoissonDistribution poisson 	= new PoissonDistribution(.5);	//How often do frames arrive?
	private final CSMACD csmacd 				= new CSMACD();					//How do nodes know if it's okay to transmit?	
	private final RandomBackoff random 			= new RandomBackoff();			//How does the node choose how long to wait?
	
	private ArrayList<Bus> busses				= new ArrayList<Bus>();			//What busses are connected to this node?
	private int currentBackoff 					= 0,							//How many slots does this node have to wait before transmitting?
				currentID 						= 1,							//What node # is next?
				buffer							= 0;							//How many nodes are waiting in the queue
	
	private int currentCollisions				= 0,							//How many times has this node detected a collision for the current frame?
				collisionsAtNode				= 0;							//How many times has this node detected a collision overall?
	
	private Map<Frame, Bus> frameBus			= new HashMap<Frame, Bus>();	//TODO: MAPS
	private Map<Frame, Long> frameFinish		= new HashMap<Frame, Long>();	//		ARE
	private Map<Frame, Long> frameCollisionCheck= new HashMap<Frame, Long>();	//		OVERKILL if I can only transmit one frame once.
							
	private int frame_num 						= 200000;						//But for a window size greater than 1, it might need it?
	private ArrayList<Frame> frames				= new ArrayList<Frame>(frame_num);	//All frames.
	private PrintWriter writer;						//Writer for this node's stats file.
	
	public Node(String name) {
		this.NAME = name;
		
		for (int i = 1; i <= frame_num; ++i) 
			frames.add(new Frame(i));
		//this.writer = ProgressMonitor.getWriter(this.NAME + ".csv");
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
		buffer += arrived;										  
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
	private final long FRAME_SIZE = 8000;			//How many bits is each frame?
	private final long TRANS_SPEED = 	100000000;	//bits per second
	private final long TRANS_TIME = (1000000 * FRAME_SIZE) / TRANS_SPEED;	//in b/s
	private void sendFrame(Node dest, Bus path) {	
		Frame frame = frames.get(currentID - 1); //- 1 is to offset: Frame 1 is at position 0, etc.
		++currentID;
		if (!frame.isAlreadyInitialized()) {
			frame.setValues(this, dest, FRAME_SIZE);
			frame.startTx();
			--buffer;			//One less frame on the queue.
		}
		
		long finishTime = Clock.addStep(TRANS_TIME),
		     collisionCheckTime = Clock.addStep(path.getPropTime(frame));
		frameBus.put(frame, path);
		frameFinish.put(frame, finishTime);						//For these two, finish and collisionCheck
		frameCollisionCheck.put(frame, collisionCheckTime);		//are the times at which to check.		
		path.claim();											//One more node transmitting to this path.
		
		ProgressMonitor.recordTransmissionStart(this, frame, path);		
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
				
				frame.finishTx();
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
					++currentCollisions;
					++collisionsAtNode;
					--currentID;
					frame.collide();
					
					currentBackoff = getBackoff();
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
	public void acceptACK(Frame frame) {
		frameBus.remove(frame);
		frameCollisionCheck.remove(frame);
		currentCollisions = 0;
		frame.deliverAndACK();
		System.out.println("\t\t" + frame.toString());
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
		return this.random.getBackoff(currentCollisions);
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
}