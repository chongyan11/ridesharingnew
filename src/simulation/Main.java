package simulation;

public class Main {

	private static final int SIM_PERIOD = 180;
	private static final int TOTAL_DEMAND = 200;
	
	public static void main(String[] args) {
		RidesharingSimulation simulation = new RidesharingSimulation(SIM_PERIOD, TOTAL_DEMAND);
		simulation.run();
	}
	
}
