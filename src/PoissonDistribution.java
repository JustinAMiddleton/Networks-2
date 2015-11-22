import java.util.Random;

/**
 * @author Justin A. Middleton
 */
public class PoissonDistribution extends DistributionMethod {
	private final double LAMBDA;
	
	public PoissonDistribution(double lambda) {
		this.LAMBDA = lambda;
	}
	
	@Override
	/*Based on Knuth's approach.*/
	public int next() {
		Random rand = new Random();
		double L = Math.exp(-LAMBDA),
			   p = 1.;
		int k = 0;
		
		
		do {
			++k;
			p *= rand.nextDouble();
		} while (p > L);
		
		return k - 1;
	}
}