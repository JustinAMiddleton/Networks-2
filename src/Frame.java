public class Frame {
	private int id;
	private long size;	//in bits
	private Node src,
				 dest;
	private boolean isUsed;
	
	public Frame() {
		this.isUsed = false;
	}
	
	public void setValues(int id, Node src, Node dest, long size) {
		this.id = id;
		this.src = src;
		this.dest = dest;
		this.size = size;
		this.isUsed = true;
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
	
	public long getSize() {
		return this.size;
	}
	
	public void reset() {
		this.isUsed = false;
	}
	
	public boolean isAlreadyInitialized() {
		return this.isUsed;
	}
}