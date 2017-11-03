/*
 *	Contains all data common between all instantiations of data. For now also used to instantiate test sets.  
 */

package cplex;
import java.util.*;

public class Data {
	public static int numAnnouncements; // Some function should define numAnnouncements
	
	// TO FIND A WAY TO READ THESE DISTANCES AND TIMES FROM EXCEL FILE 
	private static final int numInfo = 6;
	public static final int numNodes = 6;
	public static double[][] distances = {{0.0, 2.5, 2.1, 15.4, 16.4, 19.4}, 
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
	
	// Test set related
	public static ArrayList<TripAnnouncement> tripAnnouncements = new ArrayList<TripAnnouncement> ();
	
	public static void processData(ArrayList<String> rawData) {
		int[][] info = new int[rawData.size()][numInfo];
		for (int i = 0; i < rawData.size(); i++) {
			String[] line = rawData.get(i).split(", ");
			for (int j = 0; j < numInfo; j++) {
				info[i][j] = Integer.parseInt(line[j]);
			}
		}
		tripAnnouncements.clear();
		numAnnouncements = rawData.size();
		for (int i = 0; i < rawData.size(); i++) {
			TripAnnouncement trip = new TripAnnouncement(info[i][0], info[i][1], info[i][2], info[i][3], info[i][4], info[i][5]);
			tripAnnouncements.add(trip);
		}
	}
	
	
	/*
	 * Nodes:
	 * 1 - Jurong East MRT
	 * 2 - Bukit Batok MRT
	 * 3 - Chinese Garden MRT
	 * 4 - Clarke Quay MRT
	 * 5 - Somerset MRT
	 * 6 - Serangoon MRT
	 */
	
//	private static int[] idList = 			{1, 2, 3, 4, 5, 6, 7, 8, 9};
//	private static int[] typeList = 		{1, 2, 1, 2, 2, 1, 1, 1, 2};
//	private static int[] earlyTimeList = 	{5, 15, 20, 25, 25, 15, 15, 30, 0};
//	private static int[] lateTimeList = 	{65, 70, 80, 60, 50, 70, 65, 70, 80};
//	private static int[] originList = 		{1, 2, 3, 1, 2, 4, 5, 6, 5};
//	private static int[] destinationList = 	{4, 5, 6, 3, 4, 2, 2, 1, 6};
//	public static void setList() {
//		tripAnnouncements.clear();
//		for (int i = 0; i < numAnnouncements; i++) {
//			TripAnnouncement trip = new TripAnnouncement(idList[i], typeList[i], earlyTimeList[i], lateTimeList[i], originList[i], destinationList[i]);
//			tripAnnouncements.add(trip);
//		}
//	}
}
