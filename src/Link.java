import java.util.Random;
public class Link extends Bus {
	private int cost; 
	private static Random rand = new Random();
	
	public Link(String name) {
		super(name);
	}	
	
	@Override
	public void deliver(Frame frame) {
		NetworkElementInterface destination = frame.getNextHop(),
				src = frame.getPrevHop();
		destination.acceptFrameFromRouter(frame);
		src.acceptACK(frame);
		numTransmitting--; 
		ProgressMonitor.recordDelivery(frame);
	}
	
	public int getCost() { return cost; }
	public void randomCost() { 
		this.cost = rand.nextInt(10) + 1; 
		ProgressMonitor.write("Link " + this.name + " cost = " + cost);
	}
}