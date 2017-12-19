package cplex;

import filereader.*;

import ilog.concert.*;
import ilog.cplex.*;
import java.util.*;
import java.io.IOException;
import java.math.*;
import java.nio.file.NoSuchFileException;

public class Optimiser {
	
	// for testing only
	// private static final int TEST_NUM = 2;
	
	private static ArrayList<Integer> matchedParticipantList = new ArrayList<Integer>();
	private static ArrayList<Integer> soloParticipantList = new ArrayList<Integer>();
	private static Double ridesharingPayments = 0.0;
	private static Double distanceSaved = 0.0;
	private static Double maxSysMileage = 0.0;
	private static int[][] matches;
	private static double[] paymentsByNode;
	private static double[] rideshareDistanceByNode;
	private static double split;
	
	public static void run(String fileName, double paymentSplit) throws NoSuchFileException, IOException {
		matchedParticipantList.clear();
		soloParticipantList.clear();
		ridesharingPayments = 0.0;
		distanceSaved = 0.0;
		maxSysMileage = 0.0;
		split = paymentSplit;
		Information info = InputOutput.readBackground();
		Data.numNodes = info.numNodes;
		Data.times = info.times;
		Data.distances = info.distances;
		ArrayList<String> rawData = readData(fileName);
		Data.processData(rawData);
		paymentsByNode = new double[Data.numNodes];
		rideshareDistanceByNode = new double[Data.numNodes];
		model();
	}
	
	// TODO: Edit this function to fit the new requirements
	private static ArrayList<String> readData(String fileName) throws NoSuchFileException, IOException{
		// String fileName = InputOutput.getFileName(TEST_NUM);
		ArrayList<String> rawData = new ArrayList<String>();
		if (InputOutput.checkPath(fileName))
			rawData = InputOutput.readFile(fileName);
		return rawData;
	}
	
	private static void model() {
		try {						
			// Setting Up All Required Information
			IloCplex cplex = new IloCplex();
			ArrayList<TripAnnouncement> driverAnnouncements = new ArrayList<TripAnnouncement>();
			ArrayList<TripAnnouncement> riderAnnouncements = new ArrayList<TripAnnouncement>();
			categorise(driverAnnouncements, riderAnnouncements);
			
			int nDrivers = driverAnnouncements.size();
			int nRiders = riderAnnouncements.size();
			
			assert (nDrivers + nRiders == Data.numAnnouncements);
			
			// Distance variables
			double oo[][] = new double[nDrivers][nRiders];
			double dd[][] = new double[nDrivers][nRiders];
			double odDrivers[] = new double [nDrivers];
			double odRiders[] = new double[nRiders];
			double rideshareDistance[][] = new double [nDrivers][nRiders];
			double distanceSavings[][] = new double[nDrivers][nRiders];
			double maxSystemMileage;
			
			// Time variables
			double todor[][] = new double[nDrivers][nRiders];
			double tordr[] = new double[nRiders];
			double tdrdd[][] = new double[nDrivers][nRiders];
			double toddd[] = new double[nDrivers];
			double driverEarliestDeparture[] = new double [nDrivers];
			double driverLatestArrival[] = new double [nDrivers];
			double riderEarliestDeparture[] = new double[nRiders];
			double riderLatestArrival[] = new double[nRiders];
			
			// Cost variables
			double soloDriverTC[] = new double[nDrivers];
			double soloDriverF[] = new double[nDrivers];
			double soloDriverTotalCost[] = new double[nDrivers];
			double soloRiderTC[] = new double[nRiders];
			double soloRiderF[] = new double[nRiders];
			double soloRiderTotalCost[] = new double[nRiders];
			double shareDriverTC[][] = new double[nDrivers][nRiders];
			double shareDriverF[][] = new double[nDrivers][nRiders];
			double shareDriverIC[][] = new double[nDrivers][nRiders];
			double shareDriverTotalCost[][] = new double[nDrivers][nRiders];
			double minPayment[][] = new double[nDrivers][nRiders];
			double maxPayment[][] = new double[nDrivers][nRiders];
			double costSavings[][] = new double[nDrivers][nRiders];
			// double shareRider[][] = new double[nDrivers][nRiders];	- same as solo rider due to assumptions
			int feasiblePaymentMatches[][] = new int[nDrivers][nRiders];
			
			// Origin and Destination sets
			int driverOrigins[] = new int[nDrivers];
			int driverDestinations[] = new int[nDrivers];
			int riderOrigins[] = new int[nRiders];
			int riderDestinations[] = new int[nRiders];
			
			generateDistances(driverAnnouncements, riderAnnouncements, oo, dd, odDrivers, odRiders, nDrivers, nRiders);
			generateRideShareDistance(rideshareDistance, oo, dd, odRiders, nDrivers, nRiders);
			distanceSavings = generateDistanceSavings(rideshareDistance, odDrivers, odRiders, nDrivers, nRiders);
			maxSystemMileage = generateMaxMileage(odDrivers, odRiders, nDrivers, nRiders);
			generateTravelTime(driverAnnouncements, riderAnnouncements, todor, tordr, tdrdd, toddd, nDrivers, nRiders);
			generateTimeMatrix(driverAnnouncements, Boolean.FALSE, driverEarliestDeparture);
			generateTimeMatrix(driverAnnouncements, Boolean.TRUE, driverLatestArrival);
			generateTimeMatrix(riderAnnouncements, Boolean.FALSE, riderEarliestDeparture);
			generateTimeMatrix(riderAnnouncements, Boolean.TRUE, riderLatestArrival);
			
			driverOrigins = generateLocationSet(driverAnnouncements, Boolean.TRUE);
			driverDestinations = generateLocationSet(driverAnnouncements, Boolean.FALSE);
			riderOrigins = generateLocationSet(riderAnnouncements, Boolean.TRUE);
			riderDestinations = generateLocationSet(riderAnnouncements, Boolean.FALSE);
			
			soloDriverTC = Cost.generateSoloTimeCost(toddd, nDrivers);
			soloDriverF = Cost.generateSoloDriverOutOfPocketCost(odDrivers, nDrivers, driverDestinations);
			soloDriverTotalCost = Cost.generateSoloDriverTotalCost(soloDriverTC, soloDriverF, nDrivers);
			soloRiderTC = Cost.generateSoloTimeCost(tordr, nRiders);
			soloRiderF = Cost.generateSoloRiderOutOfPocketCost(odRiders, tordr, nRiders);
			soloRiderTotalCost = Cost.generateSoloRiderTotalCost(soloRiderTC, soloRiderF, nRiders);
			shareDriverTC = Cost.generateShareDriverTimeCost(todor, tordr, tdrdd, nDrivers, nRiders);
			shareDriverF = Cost.generateShareDriverOutOfPocketCost(rideshareDistance, nDrivers, nRiders, driverDestinations);
			shareDriverIC = Cost.generateShareDriverInconvenienceCost(rideshareDistance, odDrivers, odRiders, todor, tordr, tdrdd, toddd, nDrivers, nRiders);
			shareDriverTotalCost = Cost.generateShareDriverTotalCost(shareDriverTC, shareDriverF, shareDriverIC, nDrivers, nRiders);
			minPayment = Cost.generateMinRidesharingPayment(shareDriverTotalCost, soloDriverTotalCost, nDrivers, nRiders);
			maxPayment = Cost.generateMaxRidesharingPayment(soloRiderF, nDrivers, nRiders);	// at this point, max ridesharing fare = taxi fare
			feasiblePaymentMatches = Cost.generateFeasiblePaymentMatches(minPayment, maxPayment, nDrivers, nRiders);
			costSavings = Cost.generateCostSavingsMatrix(soloDriverTotalCost, soloRiderTotalCost, shareDriverTotalCost, nDrivers, nRiders);
			
// *** Decision Variables ***
			
			// 1: rideshare array (i.e. value of 1 indicates driver i matched with rider j)
			IloIntVar[][] x = new IloIntVar[nDrivers][];	 
			for (int i = 0; i < nDrivers; i++) {
				// matching matrix - each row of x is an integer array of size equal to number of riders, limited from 0 to 1
				x[i] = cplex.intVarArray(nRiders, 0, 1);	
			}
			
			// 2: driver departure time
			IloNumVar[] driverDepartTime = cplex.numVarArray(nDrivers, 0, Double.MAX_VALUE);
			
			// 3: rider departure time
			IloNumVar[] riderDepartTime = cplex.numVarArray(nRiders, 0, Double.MAX_VALUE);
			
			// 4: ridesharing payment from rider to driver if x[i][j] == 1
			IloNumVar[][] p = new IloNumVar[nDrivers][];
			for (int i = 0; i < nDrivers; i++) {
				p[i] = cplex.numVarArray(nRiders, 0.0, Double.MAX_VALUE);
			}
			
// *** Setting Up Objective Function ***
			
			// Objective: Maximise distance saved due to ridesharing
			IloLinearNumExpr objective = cplex.linearNumExpr();
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
					objective.addTerm(distanceSavings[i][j], x[i][j]);
				}
			}
			cplex.addMaximize(objective);
			
// *** Setting Up Constraints ***
			
			/*
			 * Determine feasibility of each pairing: x[i][j] == 0 if matching is infeasible
			 * Checks for feasibility under 3 conditions:
			 * 1. Distance feasibility: Total rideshare distance must not exceed sum of individual travel distances
			 * 2. Time feasibility: Both driver and rider must not violate their ET and LT respectively
			 * 3. Cost feasibility: Driver must receive a worthwhile compensation while Rider pays less than solo travel
			 */
			
			int[][] feasibleMatching = new int[nDrivers][nRiders];
			
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
					int distanceInfeasibility = Block.calcDistFeasibility(i, j, rideshareDistance, odDrivers, odRiders);
					int timeInfeasibility = Block.calcTimeIncompatibility(i, j, todor, tordr, tdrdd, driverAnnouncements, riderAnnouncements);
					int costFeasibility = feasiblePaymentMatches[i][j];
					if (distanceInfeasibility == 0 && timeInfeasibility == 0 && costFeasibility == 1) {
						feasibleMatching[i][j] = 1;
					} else {
						feasibleMatching[i][j] = 0;
					}
				}
			}
			
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
					IloAnd matchConstraints = cplex.and();
					IloAnd nomatchConstraints = cplex.and();
					nomatchConstraints.add(cplex.eq(x[i][j], 0));
					nomatchConstraints.add(cplex.eq(p[i][j], 0.0));
					
					if (feasibleMatching[i][j] == 0) {
						cplex.add(nomatchConstraints);
					} 
					else {	
						IloOr feasibleConstraints = cplex.or();
						/*
						 * Add Blocking Constraints
						 * Blocking constraints ensure user equilibrium. Most preferred participant has to be matched in order to fulfil conditions
						 */
						ArrayList<Integer> driverBlock = Block.calcDriverBlock(maxPayment, nRiders, i, j);
						ArrayList<Integer> riderBlock = Block.calcRiderBlock(minPayment, nDrivers, i, j);
						IloLinearIntExpr sumBlocks = cplex.linearIntExpr();
						for (int k = 0; k < driverBlock.size(); k++) {
							sumBlocks.addTerm(1, x[i][driverBlock.get(k)]);
						}
						for (int l = 0; l < riderBlock.size(); l++) {
							sumBlocks.addTerm(1, x[riderBlock.get(l)][j]);
						}
						matchConstraints.add(cplex.ge(cplex.sum(sumBlocks, x[i][j]), 1, "Constraint 18"));
						
						/*
						 * Add Time Constraints
						 * Driver must leave after earliest departure, arrive at rider's origin after rider's earliest departure
						 * Must arrive at rider's destination before rider's latest arrival time
						 * Driver must arrive at driver's destination before driver's latest arrival time
						 */
						matchConstraints.add(cplex.ge(cplex.sum(driverDepartTime[i], todor[i][j]), riderEarliestDeparture[j], "Constraint 1"));
						matchConstraints.add(cplex.le(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), riderLatestArrival[j], "Constraint 2"));
						matchConstraints.add(cplex.le(cplex.sum(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), tdrdd[i][j]), driverLatestArrival[i], "Constraint 3"));
						matchConstraints.add(cplex.eq(riderDepartTime[j], cplex.sum(driverDepartTime[i], todor[i][j]), "Constraint 14"));
						
						/*
						 * Add Payment Constraints
						 * Ridesharing payments must be between minimum driver is willing to receive and maximum that rider is willing to pay
						 * 
						 * In this iteration, payment is set as the middle value between minimum and maximum payments
						 */
						matchConstraints.add(cplex.ge(p[i][j], minPayment[i][j], "Constraint 5"));
						matchConstraints.add(cplex.le(p[i][j], maxPayment[i][j], "Constraint 6"));
						matchConstraints.add(cplex.eq(p[i][j], (split * minPayment[i][j] + (1.0 - split) * maxPayment[i][j]), "Constraint 7"));
						
						/*
						 * Adding matchConstraints into cplex solver
						 */
						feasibleConstraints.add(matchConstraints);
						feasibleConstraints.add(nomatchConstraints);
						cplex.add(feasibleConstraints);
					}
				}
			}
			
			/*
			 * Add generic constraints.
			 * 1. Each rider can only have 1 driver from Origin to Destination
			 * 2. Each driver can only have 1 rider from Origin to Destination
			 */
			IloLinearIntExpr[] horizontalSum = new IloLinearIntExpr[nDrivers];
			for (int i = 0; i < nDrivers; i++) {
				horizontalSum[i] = cplex.linearIntExpr();
				for (int j = 0; j < nRiders; j++) {
					horizontalSum[i].addTerm(1, x[i][j]);
				}
				cplex.addLe(horizontalSum[i], 1.1);
			}
			
			IloLinearIntExpr[] verticalSum = new IloLinearIntExpr[nRiders];
			for (int j = 0; j < nRiders; j++) {
				verticalSum[j] = cplex.linearIntExpr();
				for (int i = 0; i < nDrivers; i++) {
					verticalSum[j].addTerm(1, x[i][j]);
				}
				cplex.addLe(verticalSum[j], 1.1);
			}	

// *** Solving and printing solutions ***
			if (cplex.solve()) {
				System.out.println("Maximum System Mileage(km): " + maxSystemMileage);
				maxSysMileage = maxSystemMileage;
				System.out.println("Total Distance Savings(km): " + cplex.getObjValue());
				distanceSaved = cplex.getObjValue();
				matches = new int[nDrivers][nRiders];
				for (int i = 0; i < nDrivers; i++) {
					for (int j = 0; j < nRiders; j++) {
						matches[i][j] = (int) cplex.getValue(x[i][j]);
						if (matches[i][j] == 1) {
							matchedParticipantList.add(driverAnnouncements.get(i).id);
							matchedParticipantList.add(riderAnnouncements.get(j).id);
							ridesharingPayments += cplex.getValue(p[i][j]);
							paymentsByNode[driverAnnouncements.get(i).origin - 1] += cplex.getValue(p[i][j]);
							// System.out.println(cplex.getValue(p[i][j]));
							rideshareDistanceByNode[driverAnnouncements.get(i).origin - 1] += rideshareDistance[i][j];
						} else {
							soloParticipantList.add(driverAnnouncements.get(i).id);
							soloParticipantList.add(riderAnnouncements.get(j).id);
						}
					}
				}
			} 
//			for (int i = 0; i < Data.numNodes; i++) {
//				System.out.print(paymentsByNode[i] + " ");
//				System.out.println(rideshareDistanceByNode[i] + " ");
//			}
			System.out.println();
			
			cplex.endModel();
		} catch (IloException e) {
			e.printStackTrace();
			System.out.println("infeasible");
		}
	}
	
	
// *** Functions called in the main model *** 
	public static void categorise(ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements) {
		for (int i = 0; i < Data.numAnnouncements; i++) {
			if (Data.tripAnnouncements.get(i).type == 1) {
				driverAnnouncements.add(Data.tripAnnouncements.get(i));
			} else if (Data.tripAnnouncements.get(i).type == 2) {
				riderAnnouncements.add(Data.tripAnnouncements.get(i));
			}
		}
		return;
	}
	
	
	private static void generateDistances(ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements,
			double[][] oo, double[][] dd, double[] odDrivers, double[] odRiders, int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				oo[i][j] = Data.distances[driverAnnouncements.get(i).origin-1][riderAnnouncements.get(j).origin-1];
				odRiders[j] = Data.distances[riderAnnouncements.get(j).origin-1][riderAnnouncements.get(j).destination-1];
				dd[i][j] = Data.distances[driverAnnouncements.get(i).destination-1][riderAnnouncements.get(j).destination-1];
			}
			odDrivers[i] = Data.distances[driverAnnouncements.get(i).origin-1][driverAnnouncements.get(i).destination-1];
		}
		return;
	}

	private static void generateRideShareDistance(double[][] c, double[][] oo, double[][] dd, double[] odRiders, int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				c[i][j] = oo[i][j] + dd[i][j] + odRiders[j];
				//System.out.print(c[i][j] + " ");
			}
			//System.out.println();
		}
		return;
	}
	
	private static void generateTravelTime(ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements, 
			double[][] todor, double[] tordr, double[][] tdrdd, double[] toddd, int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				todor[i][j] = Data.times[driverAnnouncements.get(i).origin-1][riderAnnouncements.get(j).origin-1];
				tordr[j] = Data.times[riderAnnouncements.get(j).origin-1][riderAnnouncements.get(j).destination-1];
				tdrdd[i][j] = Data.times[riderAnnouncements.get(j).destination-1][driverAnnouncements.get(i).destination-1];
			}
			toddd[i] = Data.times[driverAnnouncements.get(i).origin-1][driverAnnouncements.get(i).destination-1];
		}
	}
	
	private static void generateTimeMatrix(ArrayList<TripAnnouncement> list, Boolean isLatest, double[] timeList) {
		if (isLatest) {
			for (int i = 0; i < list.size(); i++) {
				timeList[i] = list.get(i).lateTime;
			}
		} else {
			for (int i = 0; i < list.size(); i++) {
				timeList[i] = list.get(i).earlyTime;
			}
		}
		return;
	}
	
	private static int[] generateLocationSet(ArrayList<TripAnnouncement> list, Boolean isOrigin) {
		int[] locationList = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			if (isOrigin)
				locationList[i] = list.get(i).origin;
			else
				locationList[i] = list.get(i).destination;
		}
		return locationList;
	}
	
	private static double[][] generateDistanceSavings(double[][] rideshareDistance, double[] odDrivers, double[] odRiders, int nDrivers, int nRiders) {
		double[][] distanceSavings = new double[nDrivers][nRiders];
		for (int i = 0; i < nDrivers; i++) {
			for (int j = 0; j < nRiders; j++) {
				distanceSavings[i][j] = odDrivers[i] + odRiders[j] - rideshareDistance[i][j];
				// System.out.print(distanceSavings[i][j] + " ");
			}
			// System.out.println();
		}
		return distanceSavings;
	}
	
	private static double generateMaxMileage(double[] odDrivers, double[] odRiders, int nDrivers, int nRiders) {
		double sum = 0.0;
		for (int i = 0; i < nDrivers; i++)
			sum += odDrivers[i];
		for (int j = 0; j < nRiders; j++)
			sum += odRiders[j];
		return sum;
	}
	
	public static ArrayList<Integer> getMatchedParticipants() {
		return matchedParticipantList;
	}
	
	public static ArrayList<Integer> getSoloParticipants() {
		return soloParticipantList;
	}
	
	public static Double getRidesharingPayments() {
		return ridesharingPayments;
	}
	
	public static Double getDistanceSaved() {
		return distanceSaved;
	}
	
	public static double[] getRideshareDistanceByNode() {
		return rideshareDistanceByNode;
	}
	
	public static double[] getRidesharePaymentsByNode() {
		return paymentsByNode;
	}
	
	public static double getTotalDistance() {
		return maxSysMileage;
	}
}