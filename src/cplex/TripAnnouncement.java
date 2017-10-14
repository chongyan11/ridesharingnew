package cplex;

public class TripAnnouncement {
	public int id;
	public int type;	// 1 - driver; 2 - rider
	public int earlyTime;	// earliest departure time
	public int lateTime;	// latest arrival time
	public int origin;	// node which represents origin
	public int destination;	// node which represents destination
	
	// Various constructors
	public TripAnnouncement(int id, int type, int earlyTime, int lateTime, int origin, int destination) {
		this.id = id;
		this.type = type;
		this.earlyTime = earlyTime;
		this.lateTime = lateTime;
		this.origin = origin;
		this.destination = destination;
	}
	
}
