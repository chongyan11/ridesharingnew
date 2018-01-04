package simulation;
import java.io.IOException;

import filereader.*;

public class Main {

	private static final int SIM_PERIOD = 180;
	// private static final int TOTAL_DEMAND = 2000;
	private static final int BASE_DEMAND = 60;
	private static final double PAYMENT_SPLIT = 0.5;
	private static final int NUM_TRIALS = 5;
	private static final boolean SURGE = true;
	
	private static double VOT = 0.4;
	private static double VOT_interval = 0.1;
	
	private static double[] surgeDemandArray = {0.1, 0.15, 0.2, 0.25, 0.3};
	private static double[] surgeSupplyArray = {0.05, 0.1};
	
	public static void main(String[] args) {
		try {
			InputOutput.clearResultFile();
			runTrials();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runTrials() throws IOException {
		if (SURGE) {
			for (VOT = 0.4; VOT <= 0.6; VOT = VOT + VOT_interval) {
				for (int x = 0; x < surgeDemandArray.length; x++) {
					for (int y = 0; y < surgeSupplyArray.length; y++) {
						for (int i = 0; i < NUM_TRIALS; i++) {
							Params params = new Params(BASE_DEMAND, i, SIM_PERIOD, PAYMENT_SPLIT, SURGE, VOT, surgeDemandArray[x], surgeSupplyArray[y]);
							RidesharingSimulation simulation = new RidesharingSimulation(params);
							simulation.run();
							Result result = simulation.getResult();
							InputOutput.appendResult(result, params);
							simulation.close();
						}
					}
				}	
			}
		} else {
			for (VOT = 0.4; VOT <= 0.6; VOT = VOT + VOT_interval) {
				for (int i = 0; i < NUM_TRIALS; i++) {
					Params params = new Params(BASE_DEMAND, i, SIM_PERIOD, PAYMENT_SPLIT, SURGE, VOT);
					RidesharingSimulation simulation = new RidesharingSimulation(params);
					simulation.run();
					Result result = simulation.getResult();
					InputOutput.appendResult(result, params);
					simulation.close();
				}
			}
		}
	}
	
}
