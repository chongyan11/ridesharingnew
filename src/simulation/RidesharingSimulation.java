package simulation;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;

import cplex.*;
import filereader.InputOutput;

public class RidesharingSimulation{
	private static final int SEED = 1;
	private static final int NUM_INFO = 7;
	private static final int SERIAL_NO_INDEX = 0;
	private static final int ANNOUNCEMENT_TIME_INDEX = 6;
	private static final int TYPE_INDEX = 1;
	private static final int DEPARTURE_TIME_INDEX = 2;
	private static final int ARRIVAL_TIME_INDEX = 3;
	private static final int ORIGIN_INDEX = 4;
	private static final int DESTINATION_INDEX = 5;
	
	private static final int OPTIMISATION_TIME_INTERVAL = 5;

	private static final String SIMULATION_PARTICIPANTS_FILE_NAME = "participants.csv";
	private static final String OPTIMISATION_PARTICIPANTS_FILE_NAME = "opt.csv";
	
	private int simPeriod;	// length of simulation in minutes
	private int totalDemand;	// number of participants, both riders and drivers
	private double totalRidesharingPayments = 0;	// sum of ridesharing payments received by all drivers
	private int numMatches = 0;		// number of participants matched
	private double successRate;	// PERFORMANCE MEASURE: proportion of trip announcements matched
	private int currentTime = 0;
	private double distanceSaved = 0.0;
	
	private ArrayList<Integer[]> fullList;
	private ArrayList<Integer> optTimeList;

	public RidesharingSimulation (int simPeriod, int totalDemand) {
		this.simPeriod = simPeriod;
		this.totalDemand = totalDemand;
	}
	
	// main function called by Main
	public void run() {
		try {
			Information info = InputOutput.readBackground();
			generateParticipants(info);
			writeList(SIMULATION_PARTICIPANTS_FILE_NAME, fullList);	// full list of participants generated at the start will be printed
			generateOptimisationTimings();
			runSimulation();
			successRate = (double) numMatches / totalDemand;
			System.out.println("Number of successful matches: " + numMatches);
			System.out.println("Fraction of successful matches: " + successRate);
			System.out.println("Total ridesharing payments collected: " + totalRidesharingPayments);
			System.out.println("Total distance saved: " + distanceSaved);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// runs optimisation at fixed intervals, called by run()
	private void runSimulation() throws IOException, NoSuchFileException {
		while (!optTimeList.isEmpty()) {
			OptimisationEvent oe = createOptimisationEvent(optTimeList.get(0));
			oe.runOptimisation();
			currentTime = optTimeList.get(0);
			optTimeList.remove(0);
			totalRidesharingPayments += oe.getPayments();
			numMatches += oe.getMatchedParticipants().size();
			distanceSaved += oe.getDistanceSaved();
			updateList(oe);
		}
	}
	
	// creates OptimisationEvent object for each and every optimisation run, called by runSimulation();
	private OptimisationEvent createOptimisationEvent(int optTime) throws IOException {
		OptimisationEvent oe = new OptimisationEvent(optTime);
		ArrayList<Integer[]> optParticipantList = getParticipants(optTime);
		String filename = Integer.toString(optTime) + OPTIMISATION_PARTICIPANTS_FILE_NAME;
		writeList(filename, optParticipantList);
		oe.setFileName(filename);
		return oe;
	}
	
	// creates full list of participants prior to simulation, called by run();
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
	
	
	// prints list to file, called by run() [to print full participant list] and createOptimisationEvent() [to print optimisation run list (w/o announcement time]
	private void writeList(String filename, ArrayList<Integer[]> list) throws IOException {
		int[][] printingList = convertList(list);
		// Announcement times not printed to file
		InputOutput.writeList(printingList, list.size(), NUM_INFO-1, filename);
	}
	
	// generates all possible timings for optimisation runs, called by run()
	private void generateOptimisationTimings() {
		optTimeList = new ArrayList<Integer>();
		int timeCount = 0;
		while (timeCount < simPeriod) {
			timeCount = timeCount + OPTIMISATION_TIME_INTERVAL;
			optTimeList.add(timeCount);
		}
	}
	
	// generates participant list for each optimisation run, called by runSimulation();
	private ArrayList<Integer[]> getParticipants(int optTime) {
		ArrayList<Integer[]> participantList = new ArrayList<Integer[]>();
		for (int i = 0; i < fullList.size(); i++) {
			int annTime = fullList.get(i)[ANNOUNCEMENT_TIME_INDEX];
			if (annTime <= optTime) 
				participantList.add(fullList.get(i));
		}
		return participantList;
	}
	
	// converts ArrayList<Integer[]> into int[][], called by writeList()
	private int[][] convertList(ArrayList<Integer[]> list) {
		int[][] printingList = new int[list.size()][NUM_INFO-1];
		for (int i = 0; i < list.size(); i++) {
			// to suppress printing of announcement time
			for (int j = 0; j < NUM_INFO-1; j++) {
				printingList[i][j] = list.get(i)[j];
			}
		}
		return printingList;
	}
	
	private void updateList(OptimisationEvent oe) {
		ArrayList<Integer> solo = oe.getSoloParticipants();
		ArrayList<Integer> removalList = oe.getMatchedParticipants();
		
		// Unmatched participants which are scheduled to leave before the next optimisation run are removed from the list of participants
		for (int i = 0; i < solo.size(); i++) {
			int latestDeparture = 0;
			for (int k = 0; k < fullList.size(); k++) {
				if (fullList.get(k)[SERIAL_NO_INDEX].equals(solo.get(i))) {
					latestDeparture = fullList.get(k)[DEPARTURE_TIME_INDEX];
					break;
				}
			}
			if (!optTimeList.isEmpty()) {
				if (latestDeparture < optTimeList.get(0))
					removalList.add(solo.get(i));
			}
		}
		
		// Matched participants are removed from the list of participants
		for (int j = 0; j < removalList.size(); j++) {
			for (int i = fullList.size()-1; i >= 0; i--) {
				if (removalList.get(j).equals(fullList.get(i)[SERIAL_NO_INDEX])) {
					fullList.remove(i);
				}
			}
		}
	}

/* TODO:	
 * 5) Method to calculate total system mileage (cannot simply take the figures obtained from optimiser)
 */
}
