/*
 *	Contains all data common between all instantiations of data. For now also used to instantiate test sets.  
 */

package cplex;
import java.util.*;

public class Data {
	public static int numAnnouncements; // Some function should define numAnnouncements
	public static final int numInfo = 6;	// number of attributes within each trip announcement
	public static int numNodes;
	public static double[][] distances;
	public static int[][] times;
	
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

