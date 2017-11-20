package simulation;

public class OptimisationEvent {
	private int eventTime;
	private int numMatches;
	private double rideSharingPayments;
	private int[][] list;
	
	public OptimisationEvent(int eventTime, int[][] list) {
		this.eventTime = eventTime;
		this.list = list;
	}
	
	public OptimisationEvent(int eventTime) {
		this.eventTime = eventTime;
	}
	
	public int getNumMatches() {
		return numMatches;
	}
	
	public double getPayments() {
		return rideSharingPayments;
	}
}
