package simulation;
import java.math.*;

import filereader.InputOutput;

public class Demand {

	private static final int DRIVER_TYPE = 1;
	private static final int RIDER_TYPE = 2;
	private static final double K_DEMAND = -0.2879;
	private static final double K_SUPPLY = 1.8868;
	private static final double ALT_DEMAND_RATE = 0.8433333;
	private static final double ALT_SUPPLY_RATE = 0.45;
	
	private static final double HIGH_A = 3.0;
	private static final double MOD_A = 2.0;
	private static final double LOW_A = 1.0;
	
	private static final double HIGH_D = 4.0;
	private static final double MOD_D = 3.0;
	private static final double LOW_D = 2.0;
	
	private static double[] originWeights;
	private static double[] originPopulation;
	private static double[][] originAttractiveness;
	
	private static double[] destinationWeights;
	private static double[] destinationFactors;
	private static double[][] destinationAttractiveness;
	
	private static double oldPaymentRate = 0.75; // initial value at t = 0
	
	private static double meanPaymentRates(double[] paymentRate) {
		double sum = 0.0;
		int count = 0;
		for (int i = 0; i < paymentRate.length; i++) {
			if (paymentRate[i] > 0.0) {
				sum += paymentRate[i];
				count++;
			}
		}
		double mean = sum / count;
		return mean;
	}
	
	public static int updateDemand(int type, double[] paymentRate, int oldDemand, double multiplier) {
		int demand = 0;
		double oldRideshareUtil;
		double altUtil;
		double newRideshareUtil;
		double fractionDemand;
		
		double meanPaymentRate = meanPaymentRates(paymentRate);
		if (type == DRIVER_TYPE) {
			oldRideshareUtil = Math.exp(K_SUPPLY * oldPaymentRate);
			altUtil = Math.exp(K_SUPPLY * ALT_SUPPLY_RATE);
			newRideshareUtil = Math.exp(K_SUPPLY * meanPaymentRate);
			fractionDemand = (newRideshareUtil / (newRideshareUtil + altUtil)) / (oldRideshareUtil / (oldRideshareUtil + altUtil));
			// System.out.println(fractionDemand);
			demand = (int) Math.rint(fractionDemand * oldDemand * multiplier);
		} else if (type == RIDER_TYPE) {
			oldRideshareUtil = Math.exp(K_DEMAND * oldPaymentRate);
			altUtil = Math.exp(K_DEMAND * ALT_DEMAND_RATE);
			newRideshareUtil = Math.exp(K_DEMAND * meanPaymentRate);
			fractionDemand = (newRideshareUtil / (newRideshareUtil + altUtil)) / (oldRideshareUtil / (oldRideshareUtil + altUtil));
			// System.out.println(fractionDemand);
			demand = (int) Math.rint(fractionDemand * oldDemand * multiplier);
			// Because driver demand is sorted first then rider demand
			oldPaymentRate = meanPaymentRate;
		}
		return demand;
	}
	
	// Improved demand generation method (not completely random). Refer to Operational Strategies for electrical vehicle sharing systems p23
	// originWeights is the desired output - a p.d.f. for generation of origin nodes later on
	public static void readDistributions() {
		System.out.println("Origin Weights: ");
		originPopulation = InputOutput.readOriginPopulations();
		originAttractiveness = InputOutput.readOriginAttractiveness();
		originWeights = new double[originAttractiveness.length];
		double sumPopulation = 0.0;
		double sumWeights = 0.0;
		for (int i = 0; i < originPopulation.length; i++)
			sumPopulation += originPopulation[i];
		for (int i = 0; i < originWeights.length; i++) {
			double weight = 0.0;
			for (int j = 0; j < originPopulation.length; j++) {
				double inc = 0.0;
				if (originAttractiveness[i][j] < LOW_D) {
					inc = HIGH_A * originPopulation[j] / sumPopulation;
				} else if (originAttractiveness[i][j] < MOD_D) {
					inc = MOD_A * originPopulation[j] / sumPopulation;
				} else if (originAttractiveness[i][j] < HIGH_D) {
					inc = LOW_A * originPopulation[j] / sumPopulation;
				}
				weight += inc;
			}
			originWeights[i] = weight;
			// System.out.print(originWeights[i] + " ");
			sumWeights += weight;
		}
		// System.out.println();
		// System.out.println(sumWeights);
		
		// To implement for destination as well
		// System.out.println("Destination Weights: ");
		destinationFactors = InputOutput.readDestinationFactors();
		destinationAttractiveness = InputOutput.readDestinationAttractiveness();
		destinationWeights = new double[destinationAttractiveness.length];
		
		sumPopulation = 0.0;
		sumWeights = 0.0;
		for (int i = 0; i < destinationFactors.length; i++)
			sumPopulation += destinationFactors[i];
		for (int i = 0; i < destinationWeights.length; i++) {
			double weight = 0.0;
			for (int j = 0; j < destinationFactors.length; j++) {
				double inc = 0.0;
				if (destinationAttractiveness[i][j] < LOW_D) {
					inc = HIGH_A * destinationFactors[j] / sumPopulation;
				} else if (destinationAttractiveness[i][j] < MOD_D) {
					inc = MOD_A * destinationFactors[j] / sumPopulation;
				} else if (destinationAttractiveness[i][j] < HIGH_D) {
					inc = LOW_A * destinationFactors[j] / sumPopulation;
				}
				weight += inc;
			}
			destinationWeights[i] = weight;
			// System.out.print(destinationWeights[i] + " ");
			sumWeights += weight;
		}
		// System.out.println();
		// System.out.println(sumWeights);
	}
	
	public static double[] getOriginWeights() {
		return originWeights;
	}
	
	public static double[] getDestinationWeights() {
		return destinationWeights;
	}
}
