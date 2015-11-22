import java.util.Random;

public class RandomBackoff {
	private static final int MAX_BACKOFF = 8;

	public int getBackoff() {
		return new Random().nextInt(MAX_BACKOFF) + 1;
	}
}