import java.util.Random;
public class Link extends Bus {
	private int cost; 
	private static Random rand = new Random();
	
	public Link(String name) {
		super(name);
	}	
	
	public int getCost() { return cost; }
	public void randomCost() { 
		this.cost = rand.nextInt(10) + 1; 
		System.out.println("Link " + this.name + " cost = " + cost);
	}
}