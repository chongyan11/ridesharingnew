package cplex;

import ilog.concert.*;
import ilog.cplex.*;
import java.util.*;
import java.math.*;

public class Optimiser {
	
	public static void main(String[] args) {
		model();
	}
	
	public static void model() {
		try {						
			// Setting Up All Required Information
			IloCplex cplex = new IloCplex();
			ArrayList<TripAnnouncement> driverAnnouncements = new ArrayList<TripAnnouncement>();
			ArrayList<TripAnnouncement> riderAnnouncements = new ArrayList<TripAnnouncement>();
			Data.setList();
			categorise(driverAnnouncements, riderAnnouncements);
			
			int nDrivers = driverAnnouncements.size();
			int nRiders = riderAnnouncements.size();
			
			double oo[][] = new double[nDrivers][nRiders];
			double dd[][] = new double[nDrivers][nRiders];
			double odDrivers[] = new double [nDrivers];
			double odRiders[] = new double[nRiders];
			double c[][] = new double [nDrivers][nRiders];
			double todor[][] = new double[nDrivers][nRiders];
			double tordr[] = new double[nRiders];
			double tdrdd[][] = new double[nDrivers][nRiders];
			double toddd[] = new double[nDrivers];
			double driverEarliestDeparture[] = new double [nDrivers];
			double driverLatestArrival[] = new double [nDrivers];
			double riderEarliestDeparture[] = new double[nRiders];
			double riderLatestArrival[] = new double[nRiders];
			
// ** Create assert function to check numNodes = dimensions of the distance matrix
			
			generateDistances(driverAnnouncements, riderAnnouncements, oo, dd, odDrivers, odRiders, nDrivers, nRiders);
			generateRideShareDistance(c, oo, dd, odRiders, nDrivers, nRiders);
			generateTravelTime(driverAnnouncements, riderAnnouncements, todor, tordr, tdrdd, toddd, nDrivers, nRiders);
			generateTimeMatrix(driverAnnouncements, Boolean.FALSE, driverEarliestDeparture);
			generateTimeMatrix(driverAnnouncements, Boolean.TRUE, driverLatestArrival);
			generateTimeMatrix(riderAnnouncements, Boolean.FALSE, riderEarliestDeparture);
			generateTimeMatrix(riderAnnouncements, Boolean.TRUE, riderLatestArrival);
			
			for (int i = 0; i < nDrivers; i++) {
				for (int j = 0; j < nRiders; j++) 
					System.out.print(todor[i][j] + " ");
				System.out.println();
			}
				
			
// *** Decision Variables ***
			IloIntVar[][] x = new IloIntVar[nDrivers][];	// method to instantiate 2D array
			for (int i = 0; i < nDrivers; i++) {
				x[i] = cplex.intVarArray(nRiders, 0, 1);	// matching matrix - each row of x is an integer array of size equal to number of riders, limited from 0 to 1
			}
			IloIntVar[] y = cplex.intVarArray(nDrivers, 0, 1);	// value of 1 indicates driver i is driving alone
			IloIntVar[] w = cplex.intVarArray(nRiders, 0, 1);		// value of 1 indicates rider j is traveling alone	
			IloNumVar[] driverDepartTime = cplex.numVarArray(nDrivers, 0, Double.MAX_VALUE);	// scheduled departure times for drivers
			IloNumVar[] riderDepartTime = cplex.numVarArray(nRiders, 0, Double.MAX_VALUE);	// scheduled departure time for riders
			
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
					objective.addTerm(c[i][j], x[i][j]);
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
			 */
			for (int i = 0; i < nDrivers; i ++) {
				for (int j = 0; j < nRiders; j++) {
					IloAnd subTimeConstraints = cplex.and();
					IloOr timeConstraints = cplex.or();
					
					subTimeConstraints.add(cplex.ge(cplex.sum(driverDepartTime[i], todor[i][j]), riderEarliestDeparture[j]));
					subTimeConstraints.add(cplex.le(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), riderLatestArrival[j]));
					subTimeConstraints.add(cplex.le(cplex.sum(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[j]), tdrdd[i][j]), driverLatestArrival[i]));
					subTimeConstraints.add(cplex.ge(driverDepartTime[i], driverEarliestDeparture[i]));
					
					timeConstraints.add(subTimeConstraints);
					timeConstraints.add(cplex.eq(x[i][j], 0));
					
					cplex.add(timeConstraints);
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

			// Printing Solutions
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
				
			}
			
		} catch (IloException e) {
			e.printStackTrace();
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
				System.out.print(c[i][j] + " ");
			}
			System.out.println();
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
}