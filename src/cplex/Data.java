package cplex;
import java.util.*;

public class Data {
	public static int numAnnouncements = 4; // Some function should define numAnnouncements
	public static double speed = 1;
	public static ArrayList<TripAnnouncement> tripAnnouncements = new ArrayList<TripAnnouncement> ();
	private static int[] idList = {1, 2, 3, 4};
	private static int[] typeList = {1, 2, 1, 2};
	private static int[] earlyTimeList = {5, 15, 20, 25};
	private static int[] lateTimeList = {65, 70, 80, 60};
	private static double[] x1List = {15, 16, 18, 17};
	private static double[] y1List = {10, 11, 12, 10};
	private static double[] x2List = {30, 25, 29, 26};
	private static double[] y2List = {35, 27, 28, 27};
	
	public static void setList() {
		tripAnnouncements.clear();
		for (int i = 0; i < numAnnouncements; i++) {
			TripAnnouncement trip = new TripAnnouncement(idList[i], typeList[i], earlyTimeList[i], lateTimeList[i], x1List[i], y1List[i],
					x2List[i], y2List[i]);
			tripAnnouncements.add(trip);
		}
	}
}
