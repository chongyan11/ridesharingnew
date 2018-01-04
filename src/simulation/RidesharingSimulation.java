package simulation;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;

import cplex.*;
import filereader.InputOutput;

public class RidesharingSimulation implements Closeable{
	private static final int NUM_INFO = 7;
	private static final int SERIAL_NO_INDEX = 0;
	private static final int ANNOUNCEMENT_TIME_INDEX = 6;
	private static final int TYPE_INDEX = 1;
	private static final int DEPARTURE_TIME_INDEX = 2;
	private static final int ARRIVAL_TIME_INDEX = 3;
	private static final int ORIGIN_INDEX = 4;
	private static final int DESTINATION_INDEX = 5;
	
	private static final int DRIVER_TYPE = 1;
	private static final int RIDER_TYPE = 2;
	
	private static final int OPTIMISATION_TIME_INTERVAL = 10;

	private static final String SIMULATION_PARTICIPANTS_FILE_NAME = "participants.csv";
	private static final String OPTIMISATION_PARTICIPANTS_FILE_NAME = "opt.csv";
	
	private int simPeriod;	// length of simulation in minutes
	private double totalRidesharingPayments = 0;	// sum of ridesharing payments received by all drivers
	private int numMatches = 0;		// number of participants matched
	private double successRate;	// PERFORMANCE MEASURE: proportion of trip announcements matched
	private int currentTime = 0;
	private double distanceSaved = 0.0;
	private int demandCount = 0;
	private double maxTotalDistance = 0.0;
	private int seed;
	private double paymentSplit;
	private boolean surge;
	private double[] destinationWeights;
	private double[] originWeights;
	private double[] demandMultiplier;
	private double VOT;
	private double surgeDemandFactor;
	private double surgeSupplyFactor;
	
	private int baseDemand;
	private int driverDemand;
	private int riderDemand;
	
	private ArrayList<Integer[]> fullList = new ArrayList<Integer[]> ();
	private ArrayList<Integer> optTimeList;
	private ArrayList<ArrayList<Integer[]>> archive = new ArrayList<ArrayList<Integer[]>>();
	
	private Result result;

	public RidesharingSimulation (int simPeriod, int initialDemand) {
		this.simPeriod = simPeriod;
		this.baseDemand = initialDemand;
	}
	
	public RidesharingSimulation(Params params) {
		this.simPeriod = params.simulationPeriod;
		this.baseDemand = params.baseDemand;
		this.seed = params.seed;
		this.paymentSplit = params.paymentSplit;
		this.surge = params.surge;
		this.VOT = params.VOT;
		this.surgeDemandFactor = params.surgeDemand;
		this.surgeSupplyFactor = params.surgeSupply;
	}
	
	// main function called by Main
	public void run() {
		try {
			Cost.setVOT(VOT);
			if (surge) {
				Cost.setSurge(surgeDemandFactor, surgeSupplyFactor);
			}
			// change size of demand multiplier array if simulation duration changes
			demandMultiplier = InputOutput.readDemandMultiplier();
			Information info = InputOutput.readBackground();
			setupInitialDemand(info.numNodes);
			writeList(SIMULATION_PARTICIPANTS_FILE_NAME, fullList);	// full list of participants will be printed
			generateOptimisationTimings();
			runSimulation(info);
			successRate = (double) numMatches / demandCount;
			result = new Result(numMatches, demandCount, successRate, totalRidesharingPayments, maxTotalDistance, distanceSaved);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// runs optimisation at fixed intervals, called by run()
	private void runSimulation(Information info) throws IOException, NoSuchFileException {
		int count = 1;
		while (!optTimeList.isEmpty()) {
			generateNewParticipants(optTimeList.get(0), info);
			OptimisationEvent oe = createOptimisationEvent(optTimeList.get(0));
			oe.runOptimisation();
			currentTime = optTimeList.get(0);
			optTimeList.remove(0);
			totalRidesharingPayments += oe.getPayments();
			numMatches += oe.getMatchedParticipants().size();
			distanceSaved += oe.getDistanceSaved();
			maxTotalDistance += oe.getTotalDistance();
			double[] rideshareDistanceByNode = oe.getRideshareDistanceByNode();
			double[] ridesharePaymentsByNode = oe.getRidesharePaymentsByNode();
			updateList(oe);
			if (currentTime > OPTIMISATION_TIME_INTERVAL)
				updateDemand(rideshareDistanceByNode, ridesharePaymentsByNode, info.numNodes, demandMultiplier[count]);
			oe.close();
			count++;
		}
	}
	
	// creates OptimisationEvent object for each and every optimisation run, called by runSimulation();
	private OptimisationEvent createOptimisationEvent(int optTime) throws IOException {
		OptimisationEvent oe = new OptimisationEvent(optTime, paymentSplit, surge);
		ArrayList<Integer[]> optParticipantList = getParticipants(optTime);
		String filename = Integer.toString(optTime) + OPTIMISATION_PARTICIPANTS_FILE_NAME;
		writeList(filename, optParticipantList);
		oe.setFileName(filename);
		return oe;
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
	
	private void generateNewParticipants(int time, Information bg) {
		Random random = new Random(time + seed);
		ArrayList<Integer[]> list = new ArrayList<Integer[]>();
		for (int j = 0; j < driverDemand; j++)
			list.add(generateParticipant(DRIVER_TYPE, time, random, bg));
		// System.out.println();
		for (int j = 0; j < riderDemand; j++) 
			list.add(generateParticipant(RIDER_TYPE, time, random, bg));
		archive.add(list);
	}
	
	private Integer[] generateParticipant(int type, int time, Random random, Information bg) {
		demandCount++;
		Integer[] item = new Integer[NUM_INFO];
		item[SERIAL_NO_INDEX] = demandCount;
		item[TYPE_INDEX] = type;
		item[ORIGIN_INDEX] = randomGenerateLocation(originWeights, random); 
		int temp = randomGenerateLocation(destinationWeights, random); // 1 + random.nextInt(bg.numNodes);
		while (temp == item[ORIGIN_INDEX])
			temp = randomGenerateLocation(destinationWeights, random); // 1 + random.nextInt(bg.numNodes);
		item[DESTINATION_INDEX] = temp;
		item[ANNOUNCEMENT_TIME_INDEX] = time + 1;
		item[DEPARTURE_TIME_INDEX] = item[ANNOUNCEMENT_TIME_INDEX] + random.randInt(10, 30);	// participant should leave between 10 to 30 min of announcement
		item[ARRIVAL_TIME_INDEX] = item[DEPARTURE_TIME_INDEX] + bg.times[item[ORIGIN_INDEX]-1][item[DESTINATION_INDEX]-1] + random.randInt(20, 40); // participants have a 20-40 min buffer
		fullList.add(item);
		return item;
	}
	
	private void setupInitialDemand(int numNodes) {
		
		// To scrap after implementing arrayed demand
		riderDemand = (int) (0.55 * baseDemand * demandMultiplier[0]);
		driverDemand = (int) (0.45 * baseDemand * demandMultiplier[0]);
		
		Demand.readDistributions();
		originWeights = Demand.getOriginWeights();
		destinationWeights = Demand.getDestinationWeights();
	}
	
	private void updateDemand(double[] distances, double[] payments, int numNodes, double multiplier) {
		double[] paymentRate = new double[numNodes];
		for (int i = 0; i < numNodes; i++) {
			if (distances[i] > 0)
				paymentRate[i] = payments[i] / distances[i];
			else
				paymentRate[i] = 0.0;
		}
		
		// reference point is based on baseDemand instead of most recent demand, reason being the multiplier already takes into account changes according to time
		driverDemand = (int) Demand.updateDemand(DRIVER_TYPE, paymentRate, driverDemand, multiplier);
		riderDemand = (int) Demand.updateDemand(RIDER_TYPE, paymentRate, riderDemand, multiplier);
		
		// Stopgap measure
		if (driverDemand > 2 * riderDemand) {
			driverDemand = 2 * riderDemand;
		} else if (riderDemand > 2 * driverDemand) {
			riderDemand = 2 * driverDemand;
		}
		System.out.println(driverDemand + " " + riderDemand);
	}
	
	public Result getResult() {
		return result;
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
	
	public int randomGenerateLocation(double[] weights, Random random) {
		// Setting up 
		int location = 0;
		double sumWeights = 0.0;
		for (int i = 0; i < weights.length; i++)
			sumWeights += weights[i];
		double[] dartboard = new double[weights.length + 1];
		dartboard[0] = 0.0;
		for (int i = 1; i < dartboard.length; i++) 
			dartboard[i] = weights[i-1] / sumWeights + dartboard[i-1];
		// Generating location
		double rand = random.nextDouble();
		for (int i = 0; i < (dartboard.length - 1); i++)
			if (dartboard[i] < rand && rand <= dartboard[i+1])
				location = i+1;
		// System.out.print(location + " ");
		return location;
	}

/* TODO:	
 * 5) Method to calculate total system mileage (cannot simply take the figures obtained from optimiser)
 */
}


// creates full list of participants prior to simulation, called by run();
// private void generateParticipants(Information bg) {
//	Random random = new Random(SEED);
//	fullList = new ArrayList<Integer[]> ();
//	for (int i = 0; i < totalDemand; i++) {
//		Integer[] item = new Integer[NUM_INFO];
//		item[SERIAL_NO_INDEX] = i + 1;
//		item[ANNOUNCEMENT_TIME_INDEX] = random.nextInt(simPeriod);	// generates announcement time which must be before end of simulation period
//		item[TYPE_INDEX] = 1 + random.nextInt(2);	// generates either 1 or 2
//		item[ORIGIN_INDEX] = 1 + random.nextInt(bg.numNodes);
//		int temp = 1 + random.nextInt(bg.numNodes);
//		while (temp == item[ORIGIN_INDEX])
//			temp = 1 + random.nextInt(bg.numNodes);
//		item[DESTINATION_INDEX] = temp;
//		item[DEPARTURE_TIME_INDEX] = item[ANNOUNCEMENT_TIME_INDEX] + random.randInt(10, 30);	// participant should leave between 10 to 30 min of announcement
//		item[ARRIVAL_TIME_INDEX] = item[DEPARTURE_TIME_INDEX] + bg.times[item[ORIGIN_INDEX]-1][item[DESTINATION_INDEX]-1] + random.randInt(20, 40); // participants have a 20-40 min buffer
//		fullList.add(item);
//	}
//}

