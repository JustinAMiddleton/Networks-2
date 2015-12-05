import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SimulationDriver {
	public static void main(String[] args) {
		//for (int i = 2; i <= 10; i += 2)
			simulate(10, 30);
	}

	private static void simulate(int N, long seconds) {
		Clock.reset();
		ProgressMonitor.reset();
		
		ArrayList<Node> nodes = makeNodes();
		ArrayList<Router> routers = makeRouters();
		Clock.setDuration(seconds);
		
		//TODO: Change if want more than one bus/link!
		ArrayList<Bus> busses = makeBussesAndLinks(nodes, routers);
		
		/**
		 * TODO: Figure out how often routers should send frames. As often as possible?
		 */
		do {
			try {
				//All events are recorded by the exact time they're completed, not by how long they 
				//should take. This reduces math done throughout a process.
				finishPropagations(busses);				//deliver frames/ACKs
				finishTransmissions(nodes);		//finish a transmission, start propagation
				detectCollisions(nodes, busses);		//detect any collisions on a bus
				
				if (Clock.isSlotTime()) {	
					generateFrames(nodes);		//calculate how many new frames arrive
					startTransmissions(nodes, busses);	//see which nodes start transmitting
				}		
				
				if (Clock.isUpdateTableTime()) {
					randomizeCosts(busses);
					updateTables(routers);
				}
						
				ProgressMonitor.flush();
				//if (Clock.isSecond()) writeStatsEachSecond(nodes);
			} catch (UnsupportedOperationException e) {
				System.out.println(e.getMessage());
				break;
			}
		} while (Clock.step());
		
		//writeStatsEachSecond(nodes);
		printData(nodes);
		deleteExtraFiles(nodes);
	}

	/**
	 * In the below three methods, we need to construct the network by hand.
	 * We can replace these methods with a single one, if we want.
	 * Whatever works.
	 */
	private static ArrayList<Node> makeNodes() {
		// TODO Auto-generated method stub
		return null;
	}
	private static ArrayList<Router> makeRouters() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static ArrayList<Bus> makeBussesAndLinks(ArrayList<Node> nodes, ArrayList<Router> routers) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static void generateFrames(ArrayList<Node> nodes) {
		for (Node node : nodes) {
			node.generateFrames();
		}
	}
		
	private static void startTransmissions(ArrayList<Node> nodes, ArrayList<Bus> busses) {
		for (NetworkElementInterface node : nodes) {
			node.sendFrameIfReady();
		}
		
		for (Bus bus : busses) bus.setStatus();					//set the status of the bus
	}
	
	private static void finishTransmissions(ArrayList<Node> nodes) {
		for (NetworkElementInterface node : nodes) {
			node.finishTransmission();
		}		
	}

	private static void detectCollisions(ArrayList<Node> nodes, ArrayList<Bus> busses) {
		for (Node node : nodes) {
			node.checkCollision();
		}
		
		for (Bus bus : busses) bus.setStatus();					//set the status of the bus
	}
	
	private static void finishPropagations(ArrayList<Bus> busses) {
		for (Bus bus : busses) {
			bus.checkIfFramesDone();
			bus.setStatus();					//set the status of the bus
		}
	}
	
	private static void randomizeCosts(ArrayList<Bus> busses) {
		for (Bus bus : busses) {
			if (bus instanceof Link) {
				((Link) bus).randomCost();
			}
		}
	}
	
	private static void updateTables(ArrayList<Router> routers) {
		for (Router router : routers) {
			router.updateTable();
		}
	}
	
	private static void printData(ArrayList<Node> nodes) {
		PrintWriter write = ProgressMonitor.getWriter("all-" + nodes.size() + ".CSV"),
					regData = ProgressMonitor.getWriter("regularData.txt");
		String collisions = "There were " + ProgressMonitor.getCollisions() + " collisions.";
		System.out.println(collisions);
		regData.println(collisions);
		for (Node node : nodes) {
			String printout = "Node " + node.getName() + " has " + node.getBuffer() + " waiting\n"
								+ "\tand has " + node.getCollisions() + " collisions\n"
								+ "\tand sent out " + (node.getCurrentID()-1) + " frames.";
			System.out.println(printout);
			regData.println(printout);
			
			try {
				File nodefile = new File(node.getName() + ".CSV");
				BufferedReader in = new BufferedReader(new FileReader(nodefile));
				String line = "";
				while (null != (line = in.readLine())) {
					write.println(line);
					write.flush();
				}
				write.flush();
				in.close();
			} catch (IOException e) {}
		}
		regData.println();
		
		regData.close();
		write.close();
	}

	private static void deleteExtraFiles(ArrayList<Node> nodes) {
		for (NetworkElementInterface node : nodes) {			
			File nodefile = new File(node.getName() + ".CSV");
			System.out.println(nodefile.getAbsolutePath());
			nodefile.delete();
		}
	}
}