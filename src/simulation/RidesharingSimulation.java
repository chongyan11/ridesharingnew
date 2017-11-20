package simulation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import cplex.*;
import filereader.InputOutput;

public class RidesharingSimulation{
	private static final int SEED = 1;
	private static final int NUM_INFO = 7;
	private static final int SERIAL_NO_INDEX = 0;
	private static final int ANNOUNCEMENT_TIME_INDEX = 1;
	private static final int TYPE_INDEX = 2;
	private static final int DEPARTURE_TIME_INDEX = 3;
	private static final int ARRIVAL_TIME_INDEX = 4;
	private static final int ORIGIN_INDEX = 5;
	private static final int DESTINATION_INDEX = 6;
	private static final int OPTIMISATION_TIME_INTERVAL = 10;
	
	private int simPeriod;	// length of simulation in minutes
	private int totalDemand;	// number of participants, both riders and drivers
	private double totalRidesharingPayments = 0;	// sum of ridesharing payments received by all drivers
	private int numMatches = 0;		// number of participants matched
	private double successRate;	// PERFORMANCE MEASURE: proportion of trip announcements matched
	
	private int[][] list;
	private ArrayList<Integer> optList;

	public RidesharingSimulation (int simPeriod, int totalDemand) {
		this.simPeriod = simPeriod;
		this.totalDemand = totalDemand;
	}
	
	public void run() {
		try {
			Information info = InputOutput.readBackground();
			generateParticipants(info);
			writeList();
			generateOptimisationTimings();
			runSimulation();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void runSimulation() {
		while (!optList.isEmpty()) {
			OptimisationEvent oe = createOptimisationEvent(optList.get(0));
			optList.remove(0);
			totalRidesharingPayments += oe.getPayments();
			numMatches += oe.getNumMatches();
		}
	}
	
	private OptimisationEvent createOptimisationEvent(int optTime) {
		OptimisationEvent oe = new OptimisationEvent(optTime);
		return oe;
	}
	
	private void generateParticipants(Information bg) {
		Random random = new Random(SEED);
		list = new int[totalDemand][NUM_INFO];
		for (int i = 0; i < totalDemand; i++) {
			list[i][SERIAL_NO_INDEX] = i + 1;
			list[i][ANNOUNCEMENT_TIME_INDEX] = random.nextInt(simPeriod);	// generates announcement time which must be before end of simulation period
			list[i][TYPE_INDEX] = 1 + random.nextInt(2);	// generates either 1 or 2
			list[i][ORIGIN_INDEX] = 1 + random.nextInt(bg.numNodes);
			int temp = 1 + random.nextInt(bg.numNodes);
			while (temp == list[i][ORIGIN_INDEX])
				temp = 1 + random.nextInt(bg.numNodes);
			list[i][DESTINATION_INDEX] = temp;
			list[i][DEPARTURE_TIME_INDEX] = list[i][ANNOUNCEMENT_TIME_INDEX] + random.randInt(10, 30);	// participant should leave between 10 to 30 min of announcement
			list[i][ARRIVAL_TIME_INDEX] = list[i][DEPARTURE_TIME_INDEX] + bg.times[list[i][ORIGIN_INDEX]-1][list[i][DESTINATION_INDEX]-1] + random.randInt(20, 40); // participants have a 20-40 min buffer
		}
	}
	
	private void writeList() throws IOException {
		InputOutput.writeList(list, totalDemand, NUM_INFO);
	}
	
	public void generateOptimisationTimings() {
		ArrayList<Integer> optList = new ArrayList<Integer> ();
		int timeCount = 0;
		while (timeCount < simPeriod) {
			timeCount = timeCount + OPTIMISATION_TIME_INTERVAL;
			optList.add(timeCount);
		}
	}
/* 	
 * TODO: 
 * 1) Schedule a series of Optimisation Events
 * 2) Variables to keep track of number of participants matched and unmatched
 * 3) Random generation of new participants
 * 4) Method to update the list of announcements when (1) participants are matched (2) announcement expires
 * 5) Method to calculate total system mileage (cannot simply take the figures obtained from optimiser)
 */
}
