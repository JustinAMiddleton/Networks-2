import java.util.ArrayList;

public class Frame {
	private int id;
	private long size;	//in bits
	private Node src,
				 dest;
	private NetworkElementInterface nextHop,
									prevHop;
	private boolean isUsed;
	private boolean finished;
	
	ArrayList<String> times = new ArrayList<String>();
	
	//Timing data!
	private long createTime,
			startTXTime = 0, 
			finishTXTime = 0, 
			deliveryAndACKTime = 0, 
			finishTime = 0;
	private int collisionsHere = 0,
			    collisionsAll = 0;
	
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
	
	//=====================================================
	
	public void create(NetworkElementInterface place) {
		times.add(place.getName());
		times.add(Clock.time() + "");
		isUsed = false;
		
		//this.createTime = Clock.time();
	}

	public void startTx() {
		times.add(Clock.time() + "");
		isUsed = true;
		//this.startTXTime = Clock.time();
	}

	public void finishTx() {
		times.add(Clock.time() + "");
		//this.finishTXTime = Clock.time();
	}

	public void collide() {
		this.collisionsHere++;
		this.collisionsAll++;
	}

	public void deliverAndACK() {
		times.add(Clock.time() + "");
		times.add(collisionsHere + "");
		
		collisionsHere = 0;
		//this.deliveryAndACKTime = Clock.time();
	}
	
	public void finish() {
		times.add(Clock.time() + "");
		//times.add(collisionsHere + "");
		times.add(collisionsAll + "");

		finished = true;
		//this.finishTime = Clock.time();
	}
	
	public boolean isFinished() {
		return finished;
	}
	
	public String toString() {
		String results = "";
		for (String s : times) {
			if (results.compareTo("") != 0) results += ",";
			results += s;
		}
		return results;
	}
}