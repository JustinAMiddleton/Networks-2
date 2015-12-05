import java.util.ArrayList;
import java.util.HashSet;

public class Router implements NetworkElementInterface {
	private String name;
	private RoutingAlgorithm routingAlgorithm;
	
	private HashSet<Bus> connections = new HashSet<Bus>();
	private ArrayList<Frame> frames = new ArrayList<Frame>();
	//TODO: SOME KIND OF ROUTING TABLE
	
	public Router(String name, RoutingAlgorithm ra) {
		this.name = name;
		this.routingAlgorithm = ra;
	}
	
	public void addConnection(Bus b) {  }
	
	public void updateTable() {
		// TODO		
	}

	@Override
	public void addBus(Bus b) {
		connections.add(b);
	}

	@Override
	public void generateFrames() {
		//NOTHING HAPPENS
	}

	@Override
	public void sendFrameIfReady() {
		// TODO Auto-generated method stub		
	}

	@Override
	public void checkCollision() {
		//NOTHING HAPPENS
	}

	@Override
	public void finishTransmission() {
		// TODO Auto-generated method stub		
	}

	@Override
	public void acceptFrame(Frame f) {
		// TODO Auto-generated method stub		
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBuffer() {
		return frames.size();
	}
	
}