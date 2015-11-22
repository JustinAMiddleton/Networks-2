import java.util.Random;

public class RandomBackoff extends BackoffMethod {
	private static final int MAX_BACKOFF = 8;

	@Override
	public int getBackoff() {
		return new Random().nextInt(MAX_BACKOFF) + 1;
	}
}