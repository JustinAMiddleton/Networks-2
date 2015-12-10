public class Frame {
	private int id;
	private long size;	//in bits
	private Node src,
				 dest;
	private NetworkElementInterface nextHop;
	private boolean isUsed;
	
	//Timing data!
	private long startTXTime, finishTXTime, deliveryAndACKTime;
	private int collisions;
	
	public Frame(int id) {
		this.id = id;
		this.isUsed = false;
	}
	
	public void setValues(Node src, Node dest, long size) {
		this.src = src;
		this.dest = dest;
		this.size = size;
		this.isUsed = true;
		
		this.startTXTime = 0;
		this.finishTXTime = 0;
		this.deliveryAndACKTime = 0;
		this.collisions = 0;
	}
	
	public int getID() {
		return this.id;
	}
	
	public String getName() {
		return this.id + "-" + dest.getName();
	}
	
	public Node getSource() {
		return this.src;
	}
	
	public Node getDestination() {
		return this.dest;
	}
	
	public void setNextHop(NetworkElementInterface nextHop) {
		this.nextHop = nextHop;
	}
	
	public NetworkElementInterface getNextHop() {
		return this.nextHop;
	}
	
	public long getSize() {
		return this.size;
	}
	
	public void reset() {
		this.isUsed = false;
	}
	
	public boolean isAlreadyInitialized() {
		return this.isUsed;
	}

	public void startTx() {
		this.startTXTime = Clock.time();
	}

	public void finishTx() {
		this.finishTXTime = Clock.time();
	}

	public void collide() {
		this.collisions++;
	}

	public void deliverAndACK() {
		this.deliveryAndACKTime = Clock.time();
	}
	
	public String toString() {
		return id+","+startTXTime+","+finishTXTime+","+collisions+","+deliveryAndACKTime;
	}
}