import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SimulationDriver {
	public static void main(String[] args) {
		for (int i = 2; i <= 10; i += 2)
			simulate(i, 60);
	}

	private static void simulate(int N, long seconds) {
		Clock.reset();
		ProgressMonitor.reset();
		
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
				if (Clock.isSecond()) writeStatsEachSecond(nodes);
			} catch (UnsupportedOperationException e) {
				System.out.println(e.getMessage());
				break;
			}
		} while (Clock.step());
		
		writeStatsEachSecond(nodes);
		printData(nodes);
		deleteExtraFiles(nodes);
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
	
	private static void writeStatsEachSecond(ArrayList<Node> nodes) {
		for (Node node : nodes) {
			PrintWriter write = ProgressMonitor.getWriter(node.getName() + ".CSV");
			String lineToWrite = "";
			if (node.getLastFinishedStats() != null)
				lineToWrite += "," + node.getLastFinishedStats().toString();
			else lineToWrite += "-,-,-,-,-,-,-";
			write.println(lineToWrite);
			write.flush();
			write.close();
		}
	}
	
	private static void printData(ArrayList<Node> nodes) {
		PrintWriter write = ProgressMonitor.getWriter("all-" + nodes.size() + ".CSV"),
					regData = ProgressMonitor.getWriter("regularData.txt");
		System.out.println("There were " + ProgressMonitor.getCollisions() + " collisions.");
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
		for (Node node : nodes) {			
			File nodefile = new File(node.getName() + ".CSV");
			System.out.println(nodefile.getAbsolutePath());
			nodefile.delete();
		}
	}
}