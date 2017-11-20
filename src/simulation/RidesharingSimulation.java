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

	private static final String SIMULATION_PARTICIPANTS_FILE_NAME = "participants.csv";
	private static final String OPTIMISATION_PARTICIPANTS_FILE_NAME = "opt.csv";
	
	private int simPeriod;	// length of simulation in minutes
	private int totalDemand;	// number of participants, both riders and drivers
	private double totalRidesharingPayments = 0;	// sum of ridesharing payments received by all drivers
	private int numMatches = 0;		// number of participants matched
	private double successRate;	// PERFORMANCE MEASURE: proportion of trip announcements matched
	
	private ArrayList<Integer[]> fullList;
	private ArrayList<Integer> optTimeList;

	public RidesharingSimulation (int simPeriod, int totalDemand) {
		this.simPeriod = simPeriod;
		this.totalDemand = totalDemand;
	}
	
	public void run() {
		try {
			Information info = InputOutput.readBackground();
			generateParticipants(info);
			writeList(SIMULATION_PARTICIPANTS_FILE_NAME, fullList);
			generateOptimisationTimings();
			runSimulation();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void runSimulation() throws IOException {
		while (!optTimeList.isEmpty()) {
			OptimisationEvent oe = createOptimisationEvent(optTimeList.get(0));
			optTimeList.remove(0);
			totalRidesharingPayments += oe.getPayments();
			numMatches += oe.getNumMatches();
		}
	}
	
	private OptimisationEvent createOptimisationEvent(int optTime) throws IOException {
		OptimisationEvent oe = new OptimisationEvent(optTime);
		ArrayList<Integer[]> optParticipantList = getParticipants(optTime);
		String filename = Integer.toString(optTime) + OPTIMISATION_PARTICIPANTS_FILE_NAME;
		writeList(filename, optParticipantList);
		return oe;
	}
	
	private void generateParticipants(Information bg) {
		Random random = new Random(SEED);
		fullList = new ArrayList<Integer[]> ();
		for (int i = 0; i < totalDemand; i++) {
			Integer[] item = new Integer[NUM_INFO];
			item[SERIAL_NO_INDEX] = i + 1;
			item[ANNOUNCEMENT_TIME_INDEX] = random.nextInt(simPeriod);	// generates announcement time which must be before end of simulation period
			item[TYPE_INDEX] = 1 + random.nextInt(2);	// generates either 1 or 2
			item[ORIGIN_INDEX] = 1 + random.nextInt(bg.numNodes);
			int temp = 1 + random.nextInt(bg.numNodes);
			while (temp == item[ORIGIN_INDEX])
				temp = 1 + random.nextInt(bg.numNodes);
			item[DESTINATION_INDEX] = temp;
			item[DEPARTURE_TIME_INDEX] = item[ANNOUNCEMENT_TIME_INDEX] + random.randInt(10, 30);	// participant should leave between 10 to 30 min of announcement
			item[ARRIVAL_TIME_INDEX] = item[DEPARTURE_TIME_INDEX] + bg.times[item[ORIGIN_INDEX]-1][item[DESTINATION_INDEX]-1] + random.randInt(20, 40); // participants have a 20-40 min buffer
			fullList.add(item);
		}
	}
	
	private void writeList(String filename, ArrayList<Integer[]> list) throws IOException {
		int[][] printingList = convertList(list);
		InputOutput.writeList(printingList, list.size(), NUM_INFO, filename);
	}
	
	private void generateOptimisationTimings() {
		optTimeList = new ArrayList<Integer>();
		int timeCount = 0;
		while (timeCount < simPeriod) {
			timeCount = timeCount + OPTIMISATION_TIME_INTERVAL;
			optTimeList.add(timeCount);
		}
	}
	
	private ArrayList<Integer[]> getParticipants(int optTime) {
		ArrayList<Integer[]> participantList = new ArrayList<Integer[]>();
		for (int i = 0; i < fullList.size(); i++) {
			int annTime = fullList.get(i)[ANNOUNCEMENT_TIME_INDEX];
			if (annTime <= optTime) 
				participantList.add(fullList.get(i));
		}
		return participantList;
	}
	
	private int[][] convertList(ArrayList<Integer[]> list) {
		int[][] printingList = new int[list.size()][NUM_INFO];
		for (int i = 0; i < list.size(); i++) {
			for (int j = 0; j < NUM_INFO; j++) {
				printingList[i][j] = list.get(i)[j];
			}
		}
		return printingList;
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
