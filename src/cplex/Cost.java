package cplex;

public class Cost {
	// read cost coefficients from file later on
	private static final double VALUE_OF_TIME = 0.5;
	private static final double DRIVER_IC_TIME = 0.2;
	private static final double DRIVER_IC_DISTANCE = 0.25;
	private static final double RIDER_IC_TIME = 0;
	private static final double RIDER_IC_DISTANCE = 0;
	
	private static final double FUEL_UNIT_COST = 0.13377;
	private static double[] PARKING_DAY_COST = new double[Data.numNodes];
	
	private static final double TAXI_BASE = 3.4;
	private static final double TAXI_DISTANCE_COST = 0.22;
	private static final double TAXI_DISTANCE_UNIT = 0.4;
	private static final double TAXI_TIME_COST = 0.22;
	private static final double TAXI_TIME_UNIT = 0.75;
	
	private static void setUpParkCost() {
		double[] temp = {3.666667, 3.666667, 3.666667, 8, 8, 8};
		PARKING_DAY_COST = temp;	
	}
	
	public static double[] generateSoloTimeCost(double[] odTime, int n) {
		double[] soloTC = new double[n];
		for (int i = 0; i < n; i++) {
			soloTC[i] = odTime[i] * VALUE_OF_TIME;
		}
		return soloTC;
	}
	
	public static double[] generateSoloDriverOutOfPocketCost(double[] odDrivers, int nDrivers, int[] driverDestinations) {
		double[] soloDriverF = new double[nDrivers];
		setUpParkCost();
		for (int i = 0; i < nDrivers; i++) {
			soloDriverF[i] = odDrivers[i] * FUEL_UNIT_COST + PARKING_DAY_COST[driverDestinations[i]-1];
		}
		return soloDriverF;
	}
	
	public static double[] generateSoloDriverTotalCost(double[] soloDriverTC, double[] soloDriverF, int nDrivers) {
		double[] soloDriverTotalCost = new double[nDrivers];
		for (int i = 0; i < nDrivers; i++) {
			soloDriverTotalCost[i] = soloDriverTC[i] + soloDriverF[i];
		}
		return soloDriverTotalCost;
	}
	
	public static double[] generateSoloRiderOutOfPocketCost(double[] odRiders, double[] tordr, int nRiders) {
		double[] soloRiderF = new double[nRiders];
		for (int i = 0; i < nRiders; i++) {
			soloRiderF[i] = TAXI_BASE + ((odRiders[i] - 1)/TAXI_DISTANCE_UNIT*TAXI_DISTANCE_COST) + (tordr[i]/TAXI_TIME_UNIT*TAXI_TIME_COST);
		}
		return soloRiderF;
	}
	
	public static double[] generateSoloRiderTotalCost(double[] soloRiderTC, double[] soloRiderF, int nRiders) {
		double[] soloRiderTotalCost = new double[nRiders];
		for (int i = 0; i < nRiders; i++) {
			soloRiderTotalCost[i] = soloRiderTC[i] + soloRiderF[i];
		}
		return soloRiderTotalCost;
	}
	
	public static double[][] generateShareDriverTimeCost(double[][] todor, double[] tordr, double[][] tdrdd, int nDrivers, int nRiders) {
		double[][] shareDriverTimeCost = new double[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				shareDriverTimeCost[i][j] = (todor[i][j] + tordr[j] + tdrdd[i][j]) * VALUE_OF_TIME;
			}
		}
		return shareDriverTimeCost;
	}
	
	public static double[][] generateShareDriverOutOfPocketCost(double[][] rideShareDistance, int nDrivers, int nRiders, int[] driverDestinations) {
		double[][] shareDriverOutOfPocketCost = new double[nDrivers][nRiders];
		setUpParkCost();
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				shareDriverOutOfPocketCost[i][j] = rideShareDistance[i][j] * FUEL_UNIT_COST + PARKING_DAY_COST[driverDestinations[i]-1];
			}
		}
		return shareDriverOutOfPocketCost;
	}
	
	public static double[][] generateShareDriverInconvenienceCost(double[][] rideShareDistance, double[] odDrivers, double[] odRiders, double[][] todor, 
			double[] tordr, double[][] tdrdd, double[] toddd, int nDrivers, int nRiders) {
		double[][] IC = new double[nDrivers][nRiders];
		double[][] detour = new double[nDrivers][nRiders];
		double[][] inconvenienceDistance = new double[nDrivers][nRiders];
		double[][] addedTime = new double[nDrivers][nRiders];
		
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j< nRiders; j++) {
				detour[i][j] = rideShareDistance[i][j] - odDrivers[i];
				inconvenienceDistance[i][j] = detour[i][j] + odRiders[j];
				addedTime[i][j] = todor[i][j] + tordr[j] + tdrdd[i][j] - toddd[i];
				IC[i][j] = inconvenienceDistance[i][j] * DRIVER_IC_DISTANCE + addedTime[i][j] * DRIVER_IC_TIME;
			}
 		}
		return IC;
	}
	
	public static double[][] generateShareDriverTotalCost(double[][] shareDriverTC, double[][] shareDriverF, double[][] shareDriverIC, int nDrivers, int nRiders) {
		double[][] totalCost = new double[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				totalCost[i][j] = shareDriverTC[i][j] + shareDriverF[i][j] + shareDriverIC[i][j];
			}
		}
		return totalCost;
	}
	
	public static double[][] generateMinRidesharingPayment(double[][] shareDriverTotalCost, double[] soloDriverTotalCost, int nDrivers, int nRiders) {
		double[][] minPayment = new double[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				minPayment[i][j] = shareDriverTotalCost[i][j] - soloDriverTotalCost[i];
			}
		}
		return minPayment;
	}
	
	public static double[][] generateMaxRidesharingPayment(double[] soloRiderF, int nDrivers, int nRiders) {
		double[][] maxPayment = new double[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				maxPayment[i][j] = soloRiderF[j];
			}
		}
		return maxPayment;
	}
	
	public static int[][] generateFeasiblePaymentMatches(double[][] minPayment, double[][] maxPayment, int nDrivers, int nRiders) {
		int[][] matrix = new int[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				if (minPayment[i][j] <= maxPayment[i][j])
					matrix[i][j] = 1;
				else
					matrix[i][j] = 0;
			}
		}
		return matrix;
	}
}
