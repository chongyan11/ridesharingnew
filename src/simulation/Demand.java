package simulation;
import java.math.*;

public class Demand {

	private static final int DRIVER_TYPE = 1;
	private static final int RIDER_TYPE = 2;
	private static final double K_DEMAND = -0.64896;
	private static final double K_SUPPLY = -0.71691;
	private static final double ALT_DEMAND_RATE = 0.8433333;
	private static final double ALT_SUPPLY_RATE = 0.45;
	
	private static double oldPaymentRate = 0.7; // initial value at t = 0
	
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
	
	public static int updateDemand(int type, double[] paymentRate, int oldDemand) {
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
			demand = (int) Math.rint(fractionDemand * oldDemand);
		} else if (type == RIDER_TYPE) {
			oldRideshareUtil = Math.exp(K_DEMAND * oldPaymentRate);
			altUtil = Math.exp(K_DEMAND * ALT_DEMAND_RATE);
			newRideshareUtil = Math.exp(K_DEMAND * meanPaymentRate);
			fractionDemand = (newRideshareUtil / (newRideshareUtil + altUtil)) / (oldRideshareUtil / (oldRideshareUtil + altUtil));
			// System.out.println(fractionDemand);
			demand = (int) Math.rint(fractionDemand * oldDemand);
			// Because driver demand is sorted first then rider demand
			oldPaymentRate = meanPaymentRate;
		}
		return demand;
	}
}
