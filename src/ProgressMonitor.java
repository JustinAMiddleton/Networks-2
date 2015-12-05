import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ProgressMonitor {
	private static ArrayList<String> messages = new ArrayList<String>();
	private static int collisionCount = 0;
	
	public static void reset() {
		messages = new ArrayList<String>();
		collisionCount = 0;
	}
	
	/**
	 * Record a message to write out to the console.
	 * @param s	The message to write, eventually.
	 */
	public static void write(String s) {
		messages.add(s);
	}
	
	/**
	 * Print out all stored messages.
	 */
	public static void flush() {
		String fullMessage = "";
		for (int i = 0; i < messages.size(); ++i) {
			String message = messages.get(i);
			fullMessage += "\t" + message;
			if (i < messages.size() - 1)	
				fullMessage += "\n";
		}
		
		messages = new ArrayList<String>();
		
		if (fullMessage.compareTo("") != 0) {
			Clock.printTime();					//print time into console
			System.out.println(fullMessage);
		}
	}
	
	public static void recordTransmissionStart(NetworkElementInterface src, Frame sent, Bus path) {
		write("Node " + src.getName() + " starts transmitting Frame " + sent.getID() + " on Bus " + path.getName());
	}
	
	public static void recordFinishTransmission(NetworkElementInterface src, Frame frame) {
		write("Node " + src.getName() + " finished transmitting Frame " + frame.getID());
	}
	
	public static void recordDelivery(Frame frame) {
		write("Frame successfully propogated and ACKed from Node " + frame.getSource().getName()
				 + " to Node " + frame.getDestination().getName());
	}

	public static void recordCollision(NetworkElementInterface src, int backoff) {
		write("\tNode " + src.getName() + " detects collision, will wait " + backoff + " slots.");
	}
	
	public static void addCollision() {
		++collisionCount;
	}

	public static int getCollisions() {
		return collisionCount;
	}
	
	public static PrintWriter getWriter(String file) {		
		try {
			return new PrintWriter(new BufferedWriter(new FileWriter(file, true))); 
		} catch (IOException e1) {
			System.err.println("ERROR: getWriter: Writer cannot be created.");
			return null;
		}
	}
}