package cplex;

import filereader.*;

import ilog.concert.*;
import ilog.cplex.*;
import java.util.*;
import java.io.IOException;
import java.math.*;
import java.nio.file.NoSuchFileException;

public class Optimiser {
	private static final int TEST_NUM = 2;
	public static void main(String[] args) {
		ArrayList<String> rawData = readData();
		Data.processData(rawData);
		model();
	}
	
	public static ArrayList<String> readData() {
		try {
			String fileName = ReadInput.getFileName(TEST_NUM);
			ArrayList<String> rawData = new ArrayList<String>();
			if (ReadInput.checkPath(fileName))
				rawData = ReadInput.readFile(fileName);
			return rawData;
		} catch (NoSuchFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void model() {
		try {						
			// Setting Up All Required Information
			IloCplex cplex = new IloCplex();
			ArrayList<TripAnnouncement> driverAnnouncements = new ArrayList<TripAnnouncement>();
			ArrayList<TripAnnouncement> riderAnnouncements = new ArrayList<TripAnnouncement>();
			// Data.setList();
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
			// double shareRider[][] = new double[nDrivers][nRiders];	- same as solo rider due to assumptions
			int feasiblePaymentMatches[][] = new int[nDrivers][nRiders];
			
			// Origin and Destination sets
			int driverOrigins[] = new int[nDrivers];
			int driverDestinations[] = new int[nDrivers];
			int riderOrigins[] = new int[nRiders];
			int riderDestinations[] = new int[nRiders];
			
// ** TODO: Create assert function to check numNodes = dimensions of the distance matrix
			generateDistances(driverAnnouncements, riderAnnouncements, oo, dd, odDrivers, odRiders, nDrivers, nRiders);
			generateRideShareDistance(rideshareDistance, oo, dd, odRiders, nDrivers, nRiders);
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
			
// *** Decision Variables ***
			IloIntVar[][] x = new IloIntVar[nDrivers][];	// DVAR: rideshare array (i.e. value of 1 indicates driver i matched with rider j) 
			// method to instantiate 2D array
			for (int i = 0; i < nDrivers; i++) {
				x[i] = cplex.intVarArray(nRiders, 0, 1);	// matching matrix - each row of x is an integer array of size equal to number of riders, limited from 0 to 1
			}
			IloIntVar[] y = cplex.intVarArray(nDrivers, 0, 1);	// DVAR: solo driver array (i.e. value of 1 indicates driver i is driving alone)
			IloIntVar[] w = cplex.intVarArray(nRiders, 0, 1);		// DVAR: solo rider array (i.e. value of 1 indicates rider j is traveling alone)	
			IloNumVar[] driverDepartTime = cplex.numVarArray(nDrivers, 0, Double.MAX_VALUE);	// DVAR: scheduled departure times for drivers
			IloNumVar[] riderDepartTime = cplex.numVarArray(nRiders, 0, Double.MAX_VALUE);	// DVAR: scheduled departure time for riders
			
			IloNumVar[][] p = new IloNumVar[nDrivers][];	// DVAR: amount of ridesharing payment rider j pays to driver i if matched (i.e. x[i][j] == 1)
			for (int i = 0; i < nDrivers; i++) {
				p[i] = cplex.numVarArray(nRiders, 0.0, Double.MAX_VALUE);
			}
			
			IloLinearIntExpr[] horizontalSum = new IloLinearIntExpr[nDrivers];
			IloLinearIntExpr[] verticalSum = new IloLinearIntExpr[nRiders];
			
			// Horizontal sum is to ensure that each driver only has one match (i.e. Sum to 1)
			for (int i = 0; i < nDrivers; i++) {
				horizontalSum[i] = cplex.linearIntExpr();
				for (int j = 0; j < nRiders; j++) {
					horizontalSum[i].addTerm(1, x[i][j]);
				}
				horizontalSum[i].addTerm(1, y[i]);
			}
			
			// Vertical sum is to ensure that each rider only has one match (i.e. Sum to 1)
			for (int j = 0; j < nRiders; j++) {
				verticalSum[j] = cplex.linearIntExpr();
				for (int i = 0; i < nDrivers; i++) {
					verticalSum[j].addTerm(1, x[i][j]);
				}
				verticalSum[j].addTerm(1, w[j]);
			}
			
// *** Setting Up Objective Function ***
			IloLinearNumExpr objective = cplex.linearNumExpr();
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
					objective.addTerm(rideshareDistance[i][j], x[i][j]);
				}
				objective.addTerm(odDrivers[i], y[i]);
			}
			for (int j = 0; j < nRiders; j++) {
				objective.addTerm(odRiders[j], w[j]);
			}
			cplex.addMinimize(objective);
			
// *** Setting Up Constraints ***
			
			/*
			 * Time Constraints for matched participants
			 * To ensure convexity, while only restricting this constraint to matched participants, IloOr object was invoked.
			 * Generally, if the time constraints relating to driver departure, rider departure, rider arrival, driver arrival
			 * cannot be met, then x[i][j] will be constrained to 0 (i.e. no match).
			 * 
			 * Ridesharing Payment Constraints for matched participants	(REMOVE TO CONSIDER CASE WITHOUT PRICING STRATEGIES)
			 * Ridesharing is only feasible if the p[i][j] > minPayment[i][j] && p[i][j] < maxPayment[i][j]. In other words, the proposed ridesharing
			 * payment must benefit both the driver and the rider
			 */
			for (int i = 0; i < nDrivers; i ++) {
				for (int j = 0; j < nRiders; j++) {
					IloAnd subConstraints = cplex.and();
					IloAnd timeConstraints = cplex.and();
					IloAnd paymentConstraints = cplex.and();
					IloOr constraints = cplex.or();
					
					timeConstraints.add(cplex.ge(cplex.sum(driverDepartTime[i], todor[i][j]), riderEarliestDeparture[j]));
					timeConstraints.add(cplex.le(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), riderLatestArrival[j]));
					timeConstraints.add(cplex.le(cplex.sum(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), tdrdd[i][j]), driverLatestArrival[i]));
					timeConstraints.add(cplex.ge(driverDepartTime[i], driverEarliestDeparture[i]));
					subConstraints.add(timeConstraints);
					
					paymentConstraints.add(cplex.ge(p[i][j], minPayment[i][j]));
					paymentConstraints.add(cplex.le(p[i][j], maxPayment[i][j]));
					paymentConstraints.add(cplex.eq(p[i][j], (0.5 * (minPayment[i][j] + maxPayment[i][j]))));
					subConstraints.add(paymentConstraints);
					
					constraints.add(subConstraints);
					constraints.add(cplex.and(cplex.eq(x[i][j], 0), cplex.eq(p[i][j], 0.0)));
					
					cplex.add(constraints);
				}
			}
			
			// Time Constraints for solo drivers (general constraint which applies for paired drivers as well)
			for (int i = 0; i < nDrivers; i++) {
				cplex.addGe(driverDepartTime[i], driverEarliestDeparture[i]); // Driver leaves after ET(i)
				cplex.addLe(cplex.sum(driverDepartTime[i], toddd[i]), driverLatestArrival[i]);	// Driver arrives before LT(i)
			}
			
			// Time Constraints for solo riders (general constraint which applies for paired riders as well)
			for (int j = 0; j < nRiders; j++) {
				cplex.addGe(riderDepartTime[j], riderEarliestDeparture[j]);	// Rider leaves after ET(j)
				cplex.addLe(cplex.sum(riderDepartTime[j], tordr[j]), riderLatestArrival[j]);	// Rider arrives before LT(j)
			}
						
			// Create equivalent numbers between riderDepartTime and driverDepartTime + time spent getting from driver origin to rider origin
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
					// GENERAL IDEA: IF x[i][j] == 1, THEN riderDepartTime[j] == driverDepartTime[i] + todor[i][j]
					IloConstraint matchedTime = cplex.eq(riderDepartTime[j], cplex.sum(driverDepartTime[i], todor[i][j]));
					IloOr matchedRiderTime = cplex.or();
					matchedRiderTime.add(matchedTime);
					matchedRiderTime.add(cplex.eq(x[i][j], 0));
					
					cplex.add(matchedRiderTime);
				}
			}
			
			// Constraints for the number of drivers and riders
			for (int i = 0; i < nDrivers; i++) {
				cplex.addEq(horizontalSum[i], 1, "Constraint Total Riders per Driver");	// Each driver only has 1 driver at the most, or else go alone
			}
			for (int j = 0; j < nRiders; j++) {
				cplex.addEq(verticalSum[j], 1, "Constraint Total Drivers per Rider");	// Each rider only has 1 driver at the most, or else go alone
			}
			
			/*
			 * Blocking Constraints	(REMOVE TO OBTAIN RESULT THAT DOES NOT CONSIDER STABLE USER EQUILIBRIUM)
			 * Prevents blocking from occurring by ensuring that for all drivers and riders, they are matched with their most preferred partner (by cost)
			 * Algorithm works by considering the type of cases that makes it possible to have a scenario where terms sum to 0 - only where
			 * the driver-rider pair most preferred to each other do not get matched by the system.
			 * Sum of total blocks and x[i][j] must sum to at least 1 for time feasible and distance feasible solutions
			 */
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) {
//					ArrayList<Integer> driverBlock = Block.calcDriverBlock(i, j, todor, tordr, tdrdd, toddd, nRiders);
//					ArrayList<Integer> riderBlock = Block.calcRiderBlock(i, j, todor, tordr, tdrdd, toddd, nDrivers);
					ArrayList<Integer> driverBlock = Block.calcDriverBlock(maxPayment, nRiders, i, j);
					ArrayList<Integer> riderBlock = Block.calcRiderBlock(minPayment, nDrivers, i, j);
					IloLinearIntExpr sumBlocks = cplex.linearIntExpr();
					int distanceInfeasibility = 0;
					int timeIncompatibility = 0;
					
					for (int k = 0; k < driverBlock.size(); k++) 
						sumBlocks.addTerm(1, x[i][driverBlock.get(k)]);
					for (int l = 0; l < riderBlock.size(); l++)
						sumBlocks.addTerm(1, x[riderBlock.get(l)][j]);
					
					// Equals 1 if incompatible - for distance, incompatible if negative distance savings; for time, incompatible if timings do not match
					distanceInfeasibility = Block.calcDistFeasibility(i, j, rideshareDistance, odDrivers, odRiders);
					timeIncompatibility = Block.calcTimeIncompatibility(i, j, todor, tordr, tdrdd, driverAnnouncements, riderAnnouncements);
					
					// Adds blocking constraints for all distance and time compatible blocks
					if (distanceInfeasibility == 0 && timeIncompatibility == 0)
						if (feasiblePaymentMatches[i][j] == 1)
							cplex.addGe(cplex.sum(sumBlocks, x[i][j]), 1);
				}
			}
			

// *** Solving and printing solutions ***
			if (cplex.solve()) {
				System.out.println("Total Vehicle KM: " + cplex.getObjValue());
				System.out.println();
				for (int i = 0; i < nDrivers; i++) 
					for (int j = 0; j < nRiders; j++) 
						if ((int) cplex.getValue(x[i][j]) == 1)
							System.out.println("Driver " + (i+1) + " matched with Rider " + (j+1));
				System.out.println();
				for (int i = 0; i < nDrivers; i++)
					if ((int) cplex.getValue(y[i]) == 1)
						System.out.println("Driver " + (i+1) + " leaves alone");
				System.out.println();
				for (int j = 0; j < nRiders; j++)
					if((int) cplex.getValue(w[j]) == 1)
						System.out.println("Rider " + (j+1) + " leaves alone");
				System.out.println();;
				for (int i = 0; i < nDrivers; i++) 
					System.out.println("Driver " + (i+1) + " leaves at " + cplex.getValue(driverDepartTime[i]));
				System.out.println();
				for (int j = 0; j < nRiders; j++) 
					System.out.println("Riders " + (j+1) + " leaves at " + cplex.getValue(riderDepartTime[j]));
				System.out.println();
				for (int i = 0; i < nDrivers; i++) {
					for (int j = 0; j < nRiders; j++) {
						System.out.print(cplex.getValue(p[i][j]) + " ");
					}
					System.out.println();
				}
			}
			
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
	
	
	public static void generateDistances(ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements,
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
	
	public static void generateRideShareDistance(double[][] c, double[][] oo, double[][] dd, double[] odRiders, int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				c[i][j] = oo[i][j] + dd[i][j] + odRiders[j];
				//System.out.print(c[i][j] + " ");
			}
			//System.out.println();
		}
		return;
	}
	
	public static void generateTravelTime(ArrayList<TripAnnouncement> driverAnnouncements, ArrayList<TripAnnouncement> riderAnnouncements, 
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
	
	public static void generateTimeMatrix(ArrayList<TripAnnouncement> list, Boolean isLatest, double[] timeList) {
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
	
	public static int[] generateLocationSet(ArrayList<TripAnnouncement> list, Boolean isOrigin) {
		int[] locationList = new int[list.size()];
		for (int i = 0; i < list.size(); i++) {
			if (isOrigin)
				locationList[i] = list.get(i).origin;
			else
				locationList[i] = list.get(i).destination;
		}
		return locationList;
	}
}