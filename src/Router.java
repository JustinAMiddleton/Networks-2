import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;

public class Router implements NetworkElementInterface {
	private String name;
	private RoutingAlgorithm routingAlgorithm;
	private final CSMACD csmacd 				= new CSMACD();					//How do nodes know if it's okay to transmit?	
	private RoutingTableRow routingTable;
	
	private HashSet<Bus> connections = new HashSet<Bus>();
	private Queue<Frame> fromNodeBuffer = new ArrayDeque<Frame>(),
			             fromRouterBuffer = new ArrayDeque<Frame>();
	
	private Frame sendOnBus = null,		//will be null when nothing is being transmitted.
		          sendOnLink = null;
	
	private long finishBusTX = -1l,
			     busCollisionCheck = -1l,
	             finishLinkTX = -1l;
	
	
	//TODO: SOME KIND OF ROUTING TABLE
	
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
	 */
	public void updateTable() {
		int lowestCost = Integer.MAX_VALUE;
		Link lowestLink = null;
		
		for (Bus bus : connections) {
			if (bus instanceof Link) {
				Link link = ((Link) bus);
				int cost = link.getCost();
				
				if (lowestCost > cost) {
					lowestCost = cost;
					lowestLink = link;
				}
			}
		}
		
		Router dest = null;
		for (Router router : lowestLink.getRouters()) {
			if (router.getName().compareTo(this.name) != 0) {
				dest = router;
			}
		}
		
		routingTable.setValues("all", dest, lowestLink);
		System.out.println(routingTable.toString());
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
	@Override
	public void sendFrameIfReady() {
		if (!fromNodeBuffer.isEmpty() && sendOnLink == null) {
			sendOnLink = fromNodeBuffer.remove();
			finishLinkTX = Clock.addStep(LINK_TRANS_TIME);
		}
		
		if (!fromRouterBuffer.isEmpty() && Clock.isSlotTime() && sendOnBus == null) {
			Bus busToUse = getBus();
			if (csmacd.canAccess(busToUse)) {
				sendOnBus = fromRouterBuffer.remove();
				finishBusTX = Clock.addStep(BUS_TRANS_TIME);
				busCollisionCheck = Clock.addStep(busToUse.getPropTime(sendOnBus));	
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
		//TODO:
	}

	@Override
	public void finishTransmission() {
		if (Clock.equalsTime(finishLinkTX)) {
			//RoutingTableRow.
		}
		
		if (Clock.equalsTime(finishBusTX)) {
			
		}
	}

	@Override
	public void acceptFrame(Frame f) {
				
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBuffer() {
		return frames.size();
	}
	
	private class RoutingTableRow { 
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
}