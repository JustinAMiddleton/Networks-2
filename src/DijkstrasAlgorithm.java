import java.util.ArrayList;
import java.util.HashMap;

public class DijkstrasAlgorithm extends RoutingAlgorithm {
	@Override
	public void updateTable(Router source, ArrayList<Router> routers, ArrayList<Bus> busses) {
		HashMap<Router, Integer> routerCosts = initDistances(source, routers);
		HashMap<Router, Router> prevSteps = initSteps(source, routers);
		HashMap<Router, Link> linkToReachRouter = new HashMap<Router, Link>();
		
		boolean routingTableSet = false;
		
		String algorithmPrintOut = "";
		algorithmPrintOut += "From " + source.getName() + ":\n";
		while (!routers.isEmpty()) {
			Router closest = getClosest(routers, routerCosts);
			routers.remove(closest);
			
			for (Bus link : closest.getLinks()) {
				Router neighbor = getNeighbor(closest, link);
				int distance = ((Link) link).getCost() + routerCosts.get(closest);
				if (distance < routerCosts.get(neighbor)) {
					routerCosts.put(neighbor, distance);
					prevSteps.put(neighbor, closest);
					linkToReachRouter.put(neighbor, (Link) link);
				}
			}
			
			//TODO: Because of the topology, quickest router will always be second in current configuration.
			if (closest != source && !routingTableSet) {
				source.setRoutingTable("all", closest, linkToReachRouter.get(closest));
				routingTableSet = true;
			}
			
			algorithmPrintOut += closest.getName() + ";\tCost: " + routerCosts.get(closest) 
								+ ";\tNext Hop: " + (prevSteps.get(closest) != null ? prevSteps.get(closest).getName() : "-")
								+ ";\tLink Used: " + (linkToReachRouter.containsKey(closest) ? linkToReachRouter.get(closest).getName() : "-") + "\n";
		}
		
		ProgressMonitor.write(algorithmPrintOut);
	}

	private Router getNeighbor(Router closest, Bus link) {
		for (Router r : link.getRouters())
			if (r != closest)	//Two routers to a link
				return r;
		return null;
	}

	private Router getClosest(ArrayList<Router> routers, HashMap<Router, Integer> routerCosts) {
		int lowest = Integer.MAX_VALUE;
		Router closest = null;
		for (Router router : routers) {
			int cost = routerCosts.get(router);
			if (cost < lowest) {
				lowest = cost;
				closest = router;
			}
		}
		return closest;
	}

	private HashMap<Router, Router> initSteps(Router router, ArrayList<Router> routers) {
		HashMap<Router, Router> prevSteps = new HashMap<Router, Router>();
		for (Router r : routers) {
			prevSteps.put(r, null);
		}
		return prevSteps;
	}

	private HashMap<Router, Integer> initDistances(Router router, ArrayList<Router> routers) {
		HashMap<Router, Integer> routerCosts = new HashMap<Router, Integer>();
		for (Router r : routers) {
			routerCosts.put(r, Integer.MAX_VALUE);
		}
		routerCosts.put(router, 0);
		return routerCosts;
	}
}