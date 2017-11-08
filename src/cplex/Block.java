package cplex;

import java.util.ArrayList;

public class Block {

	// Distance based blocking, since benefits are purely based on cost savings
	
	// Returns a list of riders which are preferred by driver i to the rider j
	public static ArrayList<Integer> calcDriverBlock(int driver, int rider, double[][] todor, double[] tordr, double[][] tdrdd, double[] toddd, int nRiders) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		double thisPairDistanceSavings = toddd[driver] - todor[driver][rider] - tdrdd[driver][rider];
		for (int i = 0; i < nRiders; i++) {
			if (i != rider) {
				double newPairDistanceSavings = toddd[driver] - todor[driver][i] - tdrdd[driver][i];
				if (newPairDistanceSavings >= thisPairDistanceSavings)
					list.add(i);
			}
		}
		return list;
	}
	
	public static ArrayList<Integer> calcRiderBlock(int driver, int rider, double[][] todor, double[] tordr, double[][] tdrdd, double[] toddd, int nDrivers) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		double thisPairDistanceSavings = toddd[driver] - todor[driver][rider] - tdrdd[driver][rider];
		for (int i = 0; i < nDrivers; i++) {
			if (i != driver) {
				double newPairDistanceSavings = toddd[i] - todor[i][rider] - tdrdd[i][rider];
				if (newPairDistanceSavings >= thisPairDistanceSavings)
					list.add(i);
			}
		}
		return list;
	}
	
	// Returns 1 if time incompatible, 0 is compatible
	public static int calcTimeIncompatibility(int driver, int rider, double[][] todor, double[] tordr, double[][] tdrdd, 
			ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements) {
		int ETDriver = driverAnnouncements.get(driver).earlyTime;
		int LTDriver = driverAnnouncements.get(driver).lateTime;
		int ETRider = riderAnnouncements.get(rider).earlyTime;
		int LTRider = riderAnnouncements.get(rider).lateTime;
		boolean isCompatible = false;
		
		if (ETDriver + todor[driver][rider] <= LTRider - tordr[rider]) 
			if (ETRider + tordr[rider] <= LTDriver - tdrdd[driver][rider])
				isCompatible = true;
		
		if (isCompatible)
			return 0;
		else 
			return 1;
	}
	
	// Returns 1 if distance savings is negative for a best matching pair scenario
	public static int calcDistFeasibility(int driver, int rider, double[][] c, double[] odDrivers, double[] odRiders) {
		double distanceSavings = odDrivers[driver] + odRiders[rider] - c[driver][rider];
//		System.out.println("Driver " + (driver+1) + ", Rider " + (rider+1) + " = " + distanceSavings);
		if (distanceSavings <= -0.01)
			return 1;
		else 
			return 0;
	}
}

/*
 * 	public static double[][] distances = {{0.0, 2.5, 2.1, 15.4, 16.4, 19.4}, 
	                                      {2.5, 0.0, 2.6, 17.1, 18.1 ,18.8},
	                                      {2.1, 2.6, 0.0, 17.6, 18.2, 20.8},
	                                      {15.4, 17.1, 17.6, 0.0, 1.7, 9.6},
	                                      {16.4, 18.1, 18.2, 1.7, 0.0, 9.3},
	                                      {19.4, 18.8, 20.8, 9.6, 9.3, 0.0}};
	
	public static int[][] times =  {{0, 6, 4, 20, 21, 21},
									{6, 0, 5, 21, 23, 22},
									{4, 5, 0, 20, 22, 22},
									{20, 21, 20, 0, 6, 17},
									{21, 23, 22, 6, 0, 14},
									{21, 22, 22, 17, 14, 0}};
	
	public static ArrayList<TripAnnouncement> tripAnnouncements = new ArrayList<TripAnnouncement> ();
	private static int[] idList = 			{1, 2, 3, 4, 5, 6, 7, 8, 9};
	private static int[] typeList = 		{1, 2, 1, 2, 2, 1, 1, 1, 2};
	private static int[] earlyTimeList = 	{5, 15, 20, 25, 25, 15, 15, 30, 0};
	private static int[] lateTimeList = 	{65, 70, 80, 60, 50, 70, 65, 70, 80};
	private static int[] originList = 		{1, 2, 3, 1, 2, 4, 5, 6, 5};
	private static int[] destinationList = 	{4, 5, 6, 3, 4, 2, 2, 1, 6};
 */