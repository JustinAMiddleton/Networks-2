import java.util.Random;

public class RandomBackoff {
	private static final int MAX_BACKOFF = 8;

	public int getBackoff(int currentCollisions) {
		int max_back = currentCollisions > 3 ? MAX_BACKOFF : (int) Math.pow(2, currentCollisions);
		return new Random().nextInt(max_back) + 1;
	}
}