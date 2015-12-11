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
		
		ArrayList<Bus> busses = makeBussesAndLinks(nodes, routers);
		
		do {
			try {
				if (Clock.isUpdateTableTime()) {
					randomizeCosts(busses);
					updateTables(routers, busses);
				}
				
				//All events are recorded by the exact time they're completed, not by how long they 
				//should take. This reduces math done throughout a process.
				finishPropagations(busses);				//deliver frames/ACKs
				finishTransmissions(nodes, routers);		//finish a transmission, start propagation
				detectCollisions(nodes, routers, busses);		//detect any collisions on a bus
				
				if (Clock.isSlotTime()) {	
					generateFrames(nodes);		//calculate how many new frames arrive
					startTransmissions(nodes, routers, busses);	//see which nodes start transmitting
				}		
						
				//ProgressMonitor.flush();
				//flush(nodes, routers, busses);
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

	private static void flush(ArrayList<Node> nodes, ArrayList<Router> routers, ArrayList<Bus> busses) {
		ArrayList<String> names = new ArrayList<String>(),
						  statuses = new ArrayList<String>();
		
		for (Node node : nodes) { names.add(node.getName()); statuses.add(node.status()); }
		for (Router router : routers) { names.add(router.getName()); statuses.add(router.getStatus()); }
		for (Bus bus : busses) { names.add(bus.getName()); statuses.add(bus.isBusy() ? "busy-" + bus.getNumTransmitting() : ""); }
		
		for (String name : names) System.out.print("\t" + name);
		System.out.println();
		for (String status : statuses) System.out.print("\t" + status);
		System.out.println();
		System.out.println();
	}

	/**
	 * In the below three methods, we need to construct the network by hand.
	 * We can replace these methods with a single one, if we want.
	 * Whatever works.
	 */
	private static ArrayList<Node> makeNodes() {
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (int i = 0; i < 4; ++i) {
			nodes.add(new Node((char) (i + 'A') + ""));
		}
		
		for (Node node: nodes) {
			node.setAllNodes(new ArrayList<Node>(nodes));
		}
		
		return nodes;
	}
	private static ArrayList<Router> makeRouters() {
		ArrayList<Router> routers = new ArrayList<Router>();
		for (int i = 0; i < 4; ++i) {
			routers.add(new Router("R" + i, new DijkstrasAlgorithm())); 
		}
		return routers;
	}
	
	private static ArrayList<Bus> makeBussesAndLinks(ArrayList<Node> nodes, ArrayList<Router> routers) {
		Bus BUS_1 = new Bus("BUS_1");
		BUS_1.addNode(nodes.get(0));
		BUS_1.addNode(nodes.get(1));
		BUS_1.addRouter(routers.get(0));
		BUS_1.addRouter(routers.get(1));
		
		Bus BUS_2 = new Bus("BUS_2");
		BUS_2.addNode(nodes.get(2));
		BUS_2.addNode(nodes.get(3));
		BUS_2.addRouter(routers.get(2));
		BUS_2.addRouter(routers.get(3));
		
		Link L03 = new Link("L0-3"),
		     L02 = new Link("L0-2"),
		     L13 = new Link("L1-3"),
		     L12 = new Link("L1-2");
		
		L03.addRouter(routers.get(0));		
		L03.addRouter(routers.get(3));	
		
		L02.addRouter(routers.get(0));	
		L02.addRouter(routers.get(2));	
		
		L13.addRouter(routers.get(1));	
		L13.addRouter(routers.get(3));
		
		L12.addRouter(routers.get(1));	
		L12.addRouter(routers.get(2));	
		
		ArrayList<Bus> allBusses = new ArrayList<Bus>();
		allBusses.add(BUS_1);
		allBusses.add(BUS_2);
		allBusses.add(L12);
		allBusses.add(L13);
		allBusses.add(L02);
		allBusses.add(L03);
		
		return allBusses;
	}
	
	private static void generateFrames(ArrayList<Node> nodes) {
		for (Node node : nodes) {
			node.generateFrames();
		}
	}
		
	private static void startTransmissions(ArrayList<Node> nodes, ArrayList<Router> routers, ArrayList<Bus> busses) {
		for (NetworkElementInterface node : nodes) {
			node.sendFrameIfReady();
		}
		
		for (NetworkElementInterface node : routers) {
			node.sendFrameIfReady();
		}
		
		for (Bus bus : busses) bus.setStatus();					//set the status of the bus
	}
	
	private static void finishTransmissions(ArrayList<Node> nodes, ArrayList<Router> routers) {
		for (NetworkElementInterface node : nodes) {
			node.finishTransmission();
		}		
		
		for (NetworkElementInterface node : routers) {
			node.finishTransmission();
		}		
	}

	private static void detectCollisions(ArrayList<Node> nodes, ArrayList<Router> routers, ArrayList<Bus> busses) {
		for (Node node : nodes) {
			node.checkCollision();
		}
		
		for (NetworkElementInterface node : routers) {
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
		System.out.println("\nRandomizing costs...");
		for (Bus bus : busses) {
			if (bus instanceof Link) {
				((Link) bus).randomCost();
			}
		}
	}
	
	private static void updateTables(ArrayList<Router> routers, ArrayList<Bus> busses) {
		for (Router router : routers) {
			router.updateTable(new ArrayList<Router>(routers), 
					new ArrayList<Bus>(busses));
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
								+ "\tand sent out " + (node.getCurrentID()-1) + " frames\n"
								+ "\tand made " + node.all + " frames overall.";
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
			nodefile.delete();
		}
	}
}