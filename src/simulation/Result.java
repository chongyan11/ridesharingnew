package simulation;

public class Result {
	
	public int successfulMatches;
	public int totalDemand;
	public double successRate;
	public double totalRidesharingPayments;
	public double maxSystemMileage;
	public double totalDistanceSaved;
	
	public Result (int sm, int td, double sr, double trp, double msm, double tds) {
		successfulMatches = sm;
		totalDemand = td;
		successRate = sr;
		totalRidesharingPayments = trp;
		maxSystemMileage = msm;
		totalDistanceSaved = tds;
	}
}
