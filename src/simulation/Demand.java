package simulation;

public class Demand {

	private static final double PED = -0.22;
	private static final double PES = 0.28;
	private static final int DRIVER_TYPE = 1;
	private static final int RIDER_TYPE = 2;
	
	public static int[] updateDemand(int type, double[] paymentRate, int[] oldDemand, int numNodes) {
		int[] newDemand = new int[numNodes];
		double equilibriumPaymentRate;
		double fractionChange;
		equilibriumPaymentRate = sumPaymentRates(paymentRate) / numNodes;
		if (type == DRIVER_TYPE) {
			for (int i = 0; i < numNodes; i++) {
				System.out.println(paymentRate[i] + " " + oldDemand[i]);
				fractionChange = 10 * PED * (paymentRate[i] - equilibriumPaymentRate);
				// System.out.println(fractionChange + " ");
				newDemand[i] = (int) (oldDemand[i] * (1.0 + fractionChange));
			}
		} else if (type == RIDER_TYPE) {
			for (int i = 0; i < numNodes; i++) {
				fractionChange = 10 * PES * (paymentRate[i] - equilibriumPaymentRate);
				System.out.println(paymentRate[i] + " " + oldDemand[i]);
				newDemand[i] = (int) (oldDemand[i] * (1.0 + fractionChange));
				// System.out.println(fractionChange + " ");
			}
		}
		return newDemand;
	}
	
	private static double sumPaymentRates(double[] paymentRate) {
		double sum = 0.0;
		for (int i = 0; i < paymentRate.length; i++) {
			sum += paymentRate[i];
		}
		return sum;
	}
}
