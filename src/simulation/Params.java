package simulation;

public class Params {

	public int baseDemand;
	public int seed;
	public int simulationPeriod;
	public double paymentSplit;
	public boolean surge;
	public double surgeDemand = 0.0;
	public double surgeSupply = 0.0;
	public double VOT;
	
	public Params(int id, int s, int sp, double ps, boolean surge, double vot) {
		baseDemand = id;
		seed = s;
		simulationPeriod = sp;
		paymentSplit = ps;
		this.surge = surge; 
		VOT = vot;
	}
	
	public Params(int id, int s, int sp, double ps, boolean surge, double vot, double sd, double ss) {
		baseDemand = id;
		seed = s;
		simulationPeriod = sp;
		paymentSplit = ps;
		this.surge = surge; 
		VOT = vot;
		surgeDemand = sd;
		surgeSupply = ss;
	}
	
}
