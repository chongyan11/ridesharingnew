package cplex;

public class TripAnnouncement {
	public int id;
	public int type;	// 1 - driver; 2 - rider
	public int earlyTime;	// earliest departure time
	public int lateTime;	// latest arrival time
	public ODPair od;
	
	// Various constructors
	public TripAnnouncement(int id, int type, int earlyTime, int lateTime, ODPair od) {
		this.id = id;
		this.type = type;
		this.earlyTime = earlyTime;
		this.lateTime = lateTime;
		this.od = od;
	}
	
	public TripAnnouncement(int id, int type, int earlyTime, int lateTime, Coordinate origin, Coordinate destination) {
		this.id = id;
		this.type = type;
		this.earlyTime = earlyTime;
		this.lateTime = lateTime;
		this.od = new ODPair(origin, destination);
	}
	
	public TripAnnouncement(int id, int type, int earlyTime, int lateTime, double x1, double y1, double x2, double y2) {
		this.id = id;
		this.type = type;
		this.earlyTime = earlyTime;
		this.lateTime = lateTime;
		Coordinate origin = new Coordinate(x1, y1);
		Coordinate destination = new Coordinate(x2, y2);
		this.od = new ODPair(origin, destination);
	}
}
