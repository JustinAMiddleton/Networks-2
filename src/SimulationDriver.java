import java.util.ArrayList;
import java.util.Set;

public class SimulationDriver {
	public static void main(String[] args) {
		simulate(2, 60);
	}

	private static void simulate(int N, long seconds) {
		ArrayList<Node> nodes = makeNodes(N);
		Clock.setDuration(seconds);
		
		//TODO: Change if want more than one bus/link!
		Bus BUS_I = new Bus("BUS_I");
		BUS_I.addNode(nodes);
		
		do {
			try {
				//All events are recorded by the exact time they're completed, not by how long they 
				//should take. This reduces math done throughout a process.
				moveFrames(BUS_I);				//deliver frames/ACKs
				finishTransmissions(nodes);		//finish a transmission, start propagation
				detectCollisions(nodes, BUS_I);		//detect any collisions on a bus
				
				if (Clock.isSlotTime()) {	
					generateFrames(nodes);		//calculate how many new frames arrive
					startTransmissions(nodes, BUS_I);	//see which nodes start transmitting
				}				
						
				ProgressMonitor.flush();
			} catch (UnsupportedOperationException e) {
				System.out.println(e.getMessage());
				break;
			}
		} while (Clock.step());
		
		printData(nodes);
	}

	private static ArrayList<Node> makeNodes(int N) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (int i = 0; i < N; ++i) {
			String name = (char) (i + 'A') + "";	//Increments through A, B, C...
			nodes.add(new Node(name, new PoissonDistribution(.5), new CSMACD(), new RandomBackoff()));
		}
		return nodes;
	}
	
	private static void generateFrames(ArrayList<Node> nodes) {
		for (Node node : nodes) {
			node.generateFrames();
		}
	}
		
	private static void startTransmissions(ArrayList<Node> nodes, Bus BUS_I) {
		for (Node node : nodes) {
			node.sendFrameIfReady();
		}
		BUS_I.setStatus();					//set the status of the bus
	}
	
	private static void finishTransmissions(ArrayList<Node> nodes) {
		for (Node node : nodes) {
			node.finishTransmission(); //TODO: MAKE THIS BETTER
		}		
	}

	private static void detectCollisions(ArrayList<Node> nodes, Bus BUS_I) {
		for (Node node : nodes) {
			node.checkCollision();
		}
		BUS_I.setStatus();					//set the status of the bus
	}
	
	private static void moveFrames(Bus BUS_I) {
		BUS_I.checkIfFramesDone();
		BUS_I.setStatus();					//set the status of the bus
	}
	
	private static void printData(ArrayList<Node> nodes) {
		System.out.println("There were " + ProgressMonitor.getCollisions() + " collisions.");
		for (Node node : nodes) {
			System.out.println("Node " + node.getName() + " has " + node.getBuffer() + " waiting");
			System.out.println("\tand has " + node.getCollisions() + " collisions");
			System.out.println("\tand sent out " + (node.getCurrentID()-1) + " frames.");
		}
	}
}