
public interface NetworkElementInterface {

	void addBus(Bus b);
	
	void generateFrames();

	/**
	 * Checks to see if the node can transmit.
	 * If it can, then it sends one out.
	 */
	void sendFrameIfReady();
	
	void checkCollision();

	/**
	 * Check which frames are done transmitting, and if any are, put them fully on the bus
	 * for the propagation phase.
	 */
	void finishTransmission();

	void acceptFrame(Frame f);
	
	void acceptACK(Frame f);

	String getName();

	int getBuffer();

}