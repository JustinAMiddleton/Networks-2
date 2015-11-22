public class CSMACD extends AccessMethod {

	@Override
	public boolean canAccess(Bus bus) {
		return !bus.isBusy();	//If not occupied, access.
	}
	
}