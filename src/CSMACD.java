public class CSMACD {
	public boolean canAccess(Bus bus) {
		return !bus.isBusy();	//If not occupied, access.
	}
	
}