import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Bus {
	protected String name;					//Name of the bus.
	protected Set<Node> nodes;				//All the nodes it's connected to.
	protected Set<Router> routers;			//All the routers it's connected to.
	protected Map<Frame, Long> propFrames_finishTime;	//All the frames currently propagating (after the node finishes transmission)
											//TODO: Does this have to be a map?
	protected final long PROP_SPEED = 	200000000, 					 //in m/s
					   	 PROP_TIME_x2 = 1000000l  *2*2000/PROP_SPEED; // microseconds, 2000 is distance and 2 is for both there and back
	
	protected int numTransmitting;	//how many nodes are trying to transmit? more than one means collision			
	protected boolean busy,			//is this bus claimed by a node?
					collision;		//did frames collide here?
	
	public Bus(String name) {
		this.name = name;
		this.nodes = new HashSet<Node>();
		this.routers = new HashSet<Router>();
		this.propFrames_finishTime = new HashMap<Frame, Long>();
		this.busy = false;
		this.collision = false;
	}
	
	public void addNode(Node n) {
		this.nodes.add(n);
		n.addBus(this);
	}
	
	public void addNode(Collection<Node> nodes) {
		for (Node node : nodes) 
			addNode(node);
	}
	
	public void addRouter(Router r) {
		this.routers.add(r);
		r.addBus(this);
	}
	
	//////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Puts a frame on the bus.
	 * @param frame		The frame that a node just finished transmitting.
	 * @return			Double the propagation time, which is when the propagation of 
	 * 					both frame and ACK will be done.
	 */
	public long putOnBus(Frame frame) {
		long timeRemaining = PROP_TIME_x2; 
		long finish = Clock.addStep(timeRemaining);
		this.propFrames_finishTime.put(frame, finish);
		
		return timeRemaining;
	}
	
	/**
	 * Checks to see if any frames have finished their expected propagation time.
	 */
	public void checkIfFramesDone() {
		ArrayList<Frame> toRemove = new ArrayList<Frame>();	//frames to remove from map if done propagating
		
		for (Frame frame : propFrames_finishTime.keySet()) {			
			if (Clock.equalsTime(propFrames_finishTime.get(frame))) {
				deliver(frame);		
				toRemove.add(frame);
			}
		}
		
		for (Frame frame : toRemove)
			propFrames_finishTime.remove(frame);
	}
	
	/**
	 * Deliver the frame to its destination, deliver the ACK to the source.
	 * @param frame
	 */
	protected void deliver(Frame frame) {
		NetworkElementInterface destination = frame.getNextHop(),
								src = frame.getSource();
		destination.acceptFrame(frame);
		src.acceptACK(frame);
		numTransmitting--; 
		ProgressMonitor.recordDelivery(frame);
	}
	
	public boolean isBusy() { return this.busy; }	
	public boolean hasCollision() {	return this.collision; }
	public void claim() { ++this.numTransmitting; }	
	public void release() {	--this.numTransmitting; }
	
	/**
	 * Run once every step to check to see if the status of the bus (busy, idle) has changed.
	 * This is NOT done when the bus is claimed because the status should only update once 
	 * every node has gotten a chance to claim it. Since this simulation is completely 
	 * sequential (with no concurrency), I have to set the status after all send.
	 */
	public void setStatus() { 
		this.busy = this.numTransmitting > 0;		
		if (!this.busy)
			this.collision = false;
		else if (this.numTransmitting > 1) {
			if (!this.collision) ProgressMonitor.addCollision();	//Add a collision only the first time it transitions.
			this.collision = true;
		}
	}
	
	//////////////////////////////////////////////////////////////////////////
	
	/**
	 * Gets the distance a frame has to travel, between source and destination.
	 * @param frame
	 * @return
	 */
	public long getDistance(Frame frame) {
		return getDistance(frame.getSource(), frame.getDestination());
	}
	
	public long getDistance(NetworkElementInterface src, NetworkElementInterface dest) {
		if (!nodes.contains(src) || !nodes.contains(dest))
			return Long.MAX_VALUE; //highest value in case there are no paths.
		else
			return 2000;	//All nodes are currently 2000 meters from each other.
	}
	
	public String getName() { return this.name; }	
	public Iterable<Node> getNodes() { return this.nodes; }	
	public Iterable<Router> getRouters() { return this.routers; }
	public long getPropTime(Frame frame) { return PROP_TIME_x2; } //1000000 * 2*getDistance(frame) / PROP_SPEED; }
}