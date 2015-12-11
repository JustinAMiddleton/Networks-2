public class Frame {
	private int id;
	private long size;	//in bits
	private Node src,
				 dest;
	private NetworkElementInterface nextHop,
									prevHop;
	private boolean isUsed;
	
	//Timing data!
	private long startTXTime = 0, 
			finishTXTime = 0, 
			deliveryAndACKTime = 0, 
			finishTime = 0;
	private int collisions = 0;
	
	public Frame(int id) {
		this.id = id;
		this.isUsed = false;
	}
	
	public void setValues(Node src, Node dest, long size) {
		this.src = src;
		this.dest = dest;
		this.size = size;
		this.isUsed = true;
		
		nextHop = null;
		prevHop = src;
	}
	
	public int getID() {
		return this.id;
	}
	
	public String getName() {
		return this.id + src.getName() + "-" + dest.getName();
	}
	
	public Node getSource() {
		return this.src;
	}
	
	public Node getDestination() {
		return this.dest;
	}
	
	public void setNextHop(NetworkElementInterface nextHop) {
		if (this.nextHop != null)
			this.prevHop = this.nextHop;
		this.nextHop = nextHop;
	}
	
	public NetworkElementInterface getNextHop() {
		return this.nextHop;
	}
	
	public NetworkElementInterface getPrevHop() {
		return this.prevHop;
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
	
	public void finish() {
		this.finishTime = Clock.time();
	}
	
	public String toString() {
		return id+","+startTXTime+","+finishTXTime+","+collisions+","+deliveryAndACKTime;
	}
}