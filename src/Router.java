import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;

public class Router implements NetworkElementInterface {
	private String name;
	private RoutingAlgorithm routingAlgorithm;
	private final CSMACD csmacd 				= new CSMACD();					//How do nodes know if it's okay to transmit?	
	private final RandomBackoff random 			= new RandomBackoff();			//How does the node choose how long to wait?	
	private RoutingTableRow routingTable;
	
	private HashSet<Bus> connections = new HashSet<Bus>();
	private ArrayDeque<Frame> fromNodeBuffer = new ArrayDeque<Frame>(),
			             fromRouterBuffer = new ArrayDeque<Frame>();
	
	private Frame busFrame = null,		//will be null when nothing is being transmitted.
		          linkFrame = null;
	
	private long finishBusTX = -1l,
			     busCollisionCheck = -1l,
	             finishLinkTX = -1l;
	
	private String linkStatus = "", busStatus = "";
	public String getStatus() { return linkStatus + "/" + busStatus; }
			
	public Router(String name, RoutingAlgorithm ra) {
		this.name = name;
		this.routingAlgorithm = ra;
		this.routingTable = new RoutingTableRow("all", null, null);
	}
	
	public void addConnection(Bus b) {  }
	
	/**
	 * Update the table every 2 milliseconds.
	 * Because of the topology, every node is going to take the same link
	 * to the other side, so we have only one row for everything.
	 * 
	 * We also work with the assumption that each link has only two routers.
	 * @param busses 
	 * @param routers 
	 */
	public void updateTable(ArrayList<Router> routers, ArrayList<Bus> busses) {
		routingAlgorithm.updateTable(this, routers, busses);
		ProgressMonitor.write(routingTable.toString() + "\n");
	}

	@Override
	public void addBus(Bus b) {
		connections.add(b);
	}

	@Override
	public void generateFrames() {
		//NOTHING HAPPENS
	}

										//adjust for microseconds * # of bits / trans_speed
	private final long BUS_TRANS_TIME = (1000000l * 8000) / 100000000l,
					   LINK_TRANS_TIME = (1000000l * 8000) / 1000000000l;	//in b/s;	//in b/s
	private int currentCollisions = 0;
	private int collisionsAtRouter = 0;
	private int currentBackoff;

	@Override
	public void sendFrameIfReady() {
		//Send on links
		if (!fromNodeBuffer.isEmpty() && linkFrame == null) {
			linkFrame = fromNodeBuffer.remove();
			linkFrame.setNextHop(routingTable.nextHop);
			finishLinkTX = Clock.addStep(LINK_TRANS_TIME);
			
			routingTable.linkToTake.claim();
			ProgressMonitor.recordTransmissionStart(linkFrame, routingTable.linkToTake);
			linkStatus = "tx";
		}
		
		//Send on bus
		if (Clock.isSlotTime()) {
			currentBackoff = Math.max(currentBackoff-1, 0);
			if (!fromRouterBuffer.isEmpty() && busFrame == null && currentBackoff == 0) {
				Bus busToUse = getBus();
				busStatus = "";
				if (csmacd.canAccess(busToUse)) {
					busFrame = fromRouterBuffer.remove();
					
					if (busFrame.getNextHop() != busFrame.getDestination())
						busFrame.setNextHop(busFrame.getDestination());
					
					finishBusTX = Clock.addStep(BUS_TRANS_TIME);
					busCollisionCheck = Clock.addStep(busToUse.getPropTime(busFrame));	
					
					busToUse.claim();
					ProgressMonitor.recordTransmissionStart(busFrame, busToUse);
					busStatus = "tx";
				}
			}
		}
	}

	/**
	 * Since each router is connected to only one bus, then just return the first 
	 * non-link it finds in the connections list.
	 */
	private Bus getBus() {
		for (Bus bus : connections) 
			if (!(bus instanceof Link)) 
				return bus;
		return null;
	}

	@Override
	public void checkCollision() {
		if (Clock.equalsTime(busCollisionCheck)) {
			if (getBus().hasCollision()) {
				++currentCollisions;
				++collisionsAtRouter ;
				busFrame.collide();
				
				currentBackoff = this.random.getBackoff(currentCollisions);
				getBus().release();
				
				ProgressMonitor.recordCollision(this, busFrame, currentBackoff);
				resetTimesForCollision();
				busStatus = "col";
			} 
		}
	}

	private void resetTimesForCollision() {
		fromRouterBuffer.addFirst(busFrame);
		busFrame = null;
	    busCollisionCheck = -1l;
	    finishBusTX = -1l;
	}

	@Override
	public void finishTransmission() {
		if (Clock.equalsTime(finishLinkTX)) {
			routingTable.linkToTake.acceptFrame(linkFrame);
			ProgressMonitor.recordTransmissionFinish(linkFrame, routingTable.linkToTake);
			linkFrame = null;
			finishLinkTX = -1l;
			linkStatus = "";
		}
		
		if (Clock.equalsTime(finishBusTX)) {
			getBus().acceptFrame(busFrame);
			ProgressMonitor.recordTransmissionFinish(busFrame, getBus());
			busFrame = null;
			busCollisionCheck = -1l;
			finishBusTX = -1l;
			busStatus = "";
		}
	}

	//or ReceivedFromBus
	@Override
	public void acceptFrameFromNode(Frame f) {
		fromNodeBuffer.add(f);
	}
	
	//or ReceivedFromLink
	@Override
	public void acceptFrameFromRouter(Frame f) {
		fromRouterBuffer.add(f);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBuffer() {
		return 1;//TODO: frames.size();
	}
	
	public class RoutingTableRow { 
		public String destinationName;
		public Router nextHop;
		public Link linkToTake;
		
		public RoutingTableRow(String dest, Router next, Link link) {
			setValues(dest, next, link);
		}
		
		public void setValues(String dest, Router next, Link link) {
			this.destinationName = dest;
			this.nextHop = next;
			this.linkToTake = link;
		}
		
		public String toString() {
			return name + " sends packets along " + linkToTake.getName() + " to " + nextHop.getName();
		}
	}
	
	public void setRoutingTable(String dest, Router nextHop, Link linkToTake) {
		this.routingTable.setValues(dest, nextHop, linkToTake);
	}

	@Override
	public void acceptACK(Frame f) {
		//NOTHING
	}
	
	public HashSet<Bus> getLinks() {
		HashSet<Bus> links = new HashSet<Bus>(connections);
		links.remove(getBus());
		return links;
	}
}