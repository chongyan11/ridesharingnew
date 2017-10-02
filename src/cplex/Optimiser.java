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
			double tordr[][] = new double[nDrivers][nRiders];
			double tdrdd[][] = new double[nDrivers][nRiders];
			double driverEarliestDeparture[] = new double [nDrivers];
			double driverLatestArrival[] = new double [nDrivers];
			double riderEarliestDeparture[] = new double[nRiders];
			double riderLatestArrival[] = new double[nRiders];
			
			generateDistances(driverAnnouncements, riderAnnouncements, oo, dd, odDrivers, odRiders);
			generateRideShareDistance(c, oo, dd, odRiders, nDrivers, nRiders);
			generateTravelTime(oo, dd, odRiders, todor, tordr, tdrdd, nDrivers, nRiders);
			generateTimeMatrix(driverAnnouncements, Boolean.FALSE, driverEarliestDeparture);
			generateTimeMatrix(driverAnnouncements, Boolean.TRUE, driverLatestArrival);
			generateTimeMatrix(riderAnnouncements, Boolean.FALSE, riderEarliestDeparture);
			generateTimeMatrix(riderAnnouncements, Boolean.TRUE, riderLatestArrival);
			
			// *** Decision Variables ***
			IloIntVar[][] x = new IloIntVar[nDrivers][];	// method to instantiate 2D array
			for (int i = 0; i < nDrivers; i++) {
				x[i] = cplex.intVarArray(nRiders, 0, 1);	// matching matrix - each row of x is an integer array of size equal to number of riders, limited from 0 to 1
			}
			IloIntVar[] y = cplex.intVarArray(nDrivers, 0, 1);	// value of 1 indicates driver i is driving alone
			IloIntVar[] w = cplex.intVarArray(nRiders, 0, 1);		// value of 1 indicates rider j is traveling alone	
			IloNumVar[] driverDepartTime = cplex.numVarArray(nDrivers, 0, Double.MAX_VALUE);	// scheduled departure times for drivers
			
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
			
			// Time Constraints: 4 sets of constraints to follow, at each step of the ride-sharing
			for (int i = 0; i < nDrivers; i ++) {
				for (int j = 0; j < nRiders; j++) {
					cplex.addGe(cplex.sum(driverDepartTime[i], todor[i][j]), riderEarliestDeparture[j], "Constraint Time 2");	// Rider leaves after ET(j)
					cplex.addLe(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[i][j]), riderLatestArrival[j], "Constraint Time 3");	// Rider arrives before LT(j)
					cplex.addLe(cplex.sum(cplex.sum(cplex.sum(driverDepartTime[i], todor[i][j]), tordr[i][j]), tdrdd[i][j]), driverLatestArrival[i], "Constraint Time 4");	// Driver arrives before LT(i)
				}
				cplex.addGe(driverDepartTime[i], driverEarliestDeparture[i], "Constraint Time 1");	// Driver leaves after ET(i)
			}
			
			// Driver, Rider Constraints
			for (int i = 0; i < nDrivers; i++) {
				cplex.addEq(horizontalSum[i], 1, "Constraint Total Riders per Driver");	// Each driver only has 1 driver at the most, or else go alone
			}
			
			for (int j = 0; j < nRiders; j++) {
				cplex.addEq(verticalSum[j], 1, "Constraint Total Drivers per Rider");	// Each rider only has 1 driver at the most, or else go alone
			}

			// Printing Solutions
			if (cplex.solve()) {
				System.out.println(cplex.getObjValue());
				for (int i = 0; i < nDrivers; i++) {
					for (int j = 0; j < nRiders; j++) {
						System.out.print(cplex.getValue(x[i][j]) + " ");
					}
					System.out.println();
				}
				for (int i = 0; i < nDrivers; i++) {
					System.out.println("Driver " + i + " leaves at " + cplex.getValue(driverDepartTime[i]));
				}
			}
			
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
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
			double[][] oo, double[][] dd, double[] odDrivers, double[] odRiders) {
		for (int i = 0; i < driverAnnouncements.size(); i++) {
			for (int j = 0; j < riderAnnouncements.size(); j++) {
				oo[i][j] = Math.sqrt(Math.pow((driverAnnouncements.get(i).od.origin.x - riderAnnouncements.get(j).od.origin.x), 2) + 
						Math.pow((driverAnnouncements.get(i).od.origin.y - riderAnnouncements.get(j).od.origin.y), 2));
				dd[i][j] = Math.sqrt(Math.pow((driverAnnouncements.get(i).od.destination.x - riderAnnouncements.get(j).od.destination.x), 2) + 
						Math.pow((driverAnnouncements.get(i).od.destination.y - riderAnnouncements.get(j).od.destination.y), 2));
				odRiders[j] = Math.sqrt(Math.pow((riderAnnouncements.get(j).od.origin.x - riderAnnouncements.get(j).od.destination.x), 2) + 
						Math.pow((riderAnnouncements.get(j).od.origin.y - riderAnnouncements.get(j).od.destination.y), 2));
			}
			odDrivers[i] = Math.sqrt(Math.pow((driverAnnouncements.get(i).od.origin.x - driverAnnouncements.get(i).od.destination.x), 2) + 
					Math.pow((driverAnnouncements.get(i).od.origin.y - driverAnnouncements.get(i).od.destination.y), 2));
		}
		return;
	}
	
	public static void generateRideShareDistance(double[][] c, double[][] oo, double[][] dd, double[] odRiders, int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				c[i][j] = oo[i][j] + dd[i][j] + odRiders[j];
			}
		}
		return;
	}
	
	public static void generateTravelTime(double[][] oo, double[][] dd, double[]odRiders, double[][] todor, double[][] tordr, double[][] tdrdd, 
			int numDrivers, int numRiders) {
		for (int i = 0; i < numDrivers; i++) {
			for (int j = 0; j < numRiders; j++) {
				todor[i][j] = oo[i][j]/Data.speed;
				tordr[i][j] = odRiders[j]/Data.speed;
				tordr[i][j] = dd[i][j]/Data.speed;
			}
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
