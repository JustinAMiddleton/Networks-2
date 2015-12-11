import java.util.ArrayList;

public abstract class RoutingAlgorithm {
	public abstract void updateTable(Router router, ArrayList<Router> routers, ArrayList<Bus> busses);
}
