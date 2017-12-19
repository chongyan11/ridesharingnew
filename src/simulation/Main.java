package simulation;
import java.io.IOException;

import filereader.*;

public class Main {

	private static final int SIM_PERIOD = 180;
	// private static final int TOTAL_DEMAND = 2000;
	private static final int INITIAL_DEMAND = 60;
	private static double PAYMENT_SPLIT = 0.05;
	private static final int NUM_TRIALS = 10;
	private static final double PAYMENT_SPLIT_INTERVAL = 0.05;
	
	public static void main(String[] args) {
		try {
			InputOutput.clearResultFile();
			runTrials();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void runTrials() throws IOException {
		while (PAYMENT_SPLIT < 1.0) { 
			for (int i = 0; i < NUM_TRIALS; i++) {
				Params params = new Params(INITIAL_DEMAND, i, SIM_PERIOD, PAYMENT_SPLIT);
				RidesharingSimulation simulation = new RidesharingSimulation(params);
				simulation.run();
				Result result = simulation.getResult();
				InputOutput.appendResult(result, params);
				simulation.close();
			}
			PAYMENT_SPLIT += PAYMENT_SPLIT_INTERVAL;
		}
	}
	
}
