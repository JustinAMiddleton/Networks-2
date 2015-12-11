import java.util.ArrayList;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Node implements NetworkElementInterface {
	private final String NAME;
	private final PoissonDistribution poisson 	= new PoissonDistribution(.5);	//How often do frames arrive?
	private final CSMACD csmacd 				= new CSMACD();					//How do nodes know if it's okay to transmit?	
	private final RandomBackoff random 			= new RandomBackoff();			//How does the node choose how long to wait?	
	private ArrayList<Bus> busses				= new ArrayList<Bus>();			//What busses are connected to this node?
	private int currentBackoff 					= 0,							//How many slots does this node have to wait before transmitting?
				currentID 						= 1,							//What frame # is next?
				buffer							= 0,							//How many nodes are waiting in the queue	
				currentCollisions				= 0,							//How many times has this node detected a collision for the current frame?
				collisionsAtNode				= 0;							//How many times has this node detected a collision overall?
	//DATA FOR CURRENT FRAME SENT OUT
	private Bus usingBus						= null;	
	private long frameFinish					= -1l,
				 frameCollisionCheck			= -1l;	
	//MISC DATA					
	private int frame_num 						= 200000;						//But for a window size greater than 1, it might need it?
	private ArrayList<Frame> frames				= new ArrayList<Frame>(frame_num);	//All frames.
	private ArrayList<Node> allNodes;
	private PrintWriter writer;													//Writer for this node's stats file.
	
	private String status = "";
	public String status() { return status + (status.compareTo("col") == 0 ? "-" + currentBackoff : ""); }
	
	/**
	 * Constructor; now takes in only name and assumes distribution, access, backoff will be same.
	 * @param name
	 */
	public Node(String name) {
		this.NAME = name;
		
		for (int i = 0; i <= frame_num; ++i) 
			frames.add(new Frame(i));
		//this.writer = ProgressMonitor.getWriter(this.NAME + ".csv");
	}
	
	/* (non-Javadoc)
	 * @see NetworkInterface#addBus(Bus)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see NetworkInterface#sendFrameIfReady()
	 */
	@Override
	public void sendFrameIfReady() {
		currentBackoff = Math.max(currentBackoff-1, 0);
		if (currentBackoff > 0 
				|| buffer == 0 || usingBus != null)	
			return;	

		status = "";
		Node dest = getRandomDestination(); //TODO: With only one, it doesn't matter if I record the destination.
		Bus path = findBestPath(dest);		//may throw exception if no path at all	
		if (csmacd.canAccess(path)) { 		//Checks to see if the path can be accessed.
			sendFrame(dest, path);
			status = "(tx)";
		}
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
		Frame frame = frames.get(currentID); //- 1 is to offset: Frame 1 is at position 0, etc.
		if (!frame.isAlreadyInitialized()) {
			frame.setValues(this, dest, FRAME_SIZE);
			frame.startTx();
			--buffer;			//One less frame on the queue.
			
			if (getAccessibleNodes().contains(dest))
				frame.setNextHop(dest);
			else 		
				frame.setNextHop(getRandomRouter(path));
		}
		
		usingBus = path;
		frameFinish = Clock.addStep(TRANS_TIME);
		frameCollisionCheck = Clock.addStep(path.getPropTime(frame));	
		path.claim();											//One more node transmitting to this path.
		
		ProgressMonitor.recordTransmissionStart(frame, path);		
	}

	private Router getRandomRouter(Bus path) {
		Router randomRouter = null;
		Iterable<Router> routers = path.getRouters();
		int index = new Random().nextInt(2),
			i = 0;
		
		for (Router router : routers) {
			if (i == index) {
				randomRouter = router;
				break;
			} else
				++i;
		}
		return randomRouter;
	}
	
	/* (non-Javadoc)
	 * @see NetworkInterface#finishTransmission()
	 */
	@Override
	public void finishTransmission() {
		if (Clock.equalsTime(frameFinish)) {	
			Frame frame = frames.get(currentID);
			usingBus.acceptFrame(frame);			
			frame.finishTx();
			ProgressMonitor.recordTransmissionFinish(frame, usingBus);
			status = "";
		}
	}

	/**
	 * Find the best path to get to the destination node.
	 * @param dest		The node to get to.
	 * @return			The best path to get to that node.
	 * @throws UnsupportedOperationException
	 */
	private Bus findBestPath(NetworkElementInterface dest) throws UnsupportedOperationException {
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
		if (Clock.equalsTime(frameCollisionCheck)) {
			if (usingBus.hasCollision()) {
				++currentCollisions;
				++collisionsAtNode;
				++buffer;
				frames.get(currentID).collide();
				
				currentBackoff = this.random.getBackoff(currentCollisions);
				usingBus.release();
				resetTimes();
				
				ProgressMonitor.recordCollision(this, frames.get(currentID), currentBackoff);
				status = "col";
			} else status = "tx";
		}
	}
	
	/* (non-Javadoc)
	 * @see NetworkInterface#acceptFrame(Frame)
	 */
	@Override
	public void acceptFrameFromNode(Frame f) {
		f.deliverAndACK();
	}
	
	/**
	 * When the ACK returns, remove the frame from any storage.
	 */
	public void acceptACK(Frame frame) {
		++currentID;
		resetTimes();
		currentCollisions = 0;
		frame.deliverAndACK();
	}

	private void resetTimes() {
		usingBus = null;
		frameFinish = -1l;
		frameCollisionCheck = -1l;
	}
	
	/**
	 * Pick any accessible random Node for a destination.
	 * @return the Node destination.
	 * @throws UnsupportedOperationException	if no destination is available.
	 */
	public Node getRandomDestination() throws UnsupportedOperationException {
		Random rand = new Random();
		int index = rand.nextInt(allNodes.size());
		return allNodes.get(index);
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
		return currentBackoff;
	}
	
	/* (non-Javadoc)
	 * @see NetworkInterface#getName()
	 */
	@Override
	public String getName() {
		return this.NAME;
	}
	
	/* (non-Javadoc)
	 * @see NetworkInterface#getBuffer()
	 */
	@Override
	public int getBuffer() {
		return this.buffer;
	}
	
	public int getCurrentID() {
		return this.currentID;
	}
	
	public int getCollisions() {
		return collisionsAtNode;
	}	
	
	public void setAllNodes(ArrayList<Node> nodes) {
		if (nodes.contains(this))
			nodes.remove(this);
		
		allNodes = nodes;
	}

	@Override
	public void acceptFrameFromRouter(Frame f) {
		//NOTHING
	}
}