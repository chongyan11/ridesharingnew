package simulation;

public class Params {

	public int initialDemand;
	public int seed;
	public int simulationPeriod;
	public double paymentSplit;
	
	public Params(int id, int s, int sp, double ps) {
		initialDemand = id;
		seed = s;
		simulationPeriod = sp;
		paymentSplit = ps;
	}
}
