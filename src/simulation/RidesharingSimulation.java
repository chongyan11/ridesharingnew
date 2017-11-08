package simulation;
import cplex.*;

public class RidesharingSimulation extends Simulation{
	private double simPeriod;	// length of simulation in minutes
	private int totalDemand;	// number of participants, both riders and drivers
	private double totalRidesharingPayments;	// sum of ridesharing payments received by all drivers
	private int numMatches;		// number of participants matched
	private double successRate = numMatches / totalDemand;	// PERFORMANCE MEASURE: proportion of trip announcements matched

/* 	
 * TODO: 
 * 1) Schedule a series of Optimisation Events
 * 2) Variables to keep track of number of participants matched and unmatched
 * 3) Random generation of new participants
 * 4) Method to update the list of announcements when (1) participants are matched (2) announcement expires
 * 5) Method to calculate total system mileage (cannot simply take the figures obtained from optimiser)
 */
}
