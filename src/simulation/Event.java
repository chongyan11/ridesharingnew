package simulation;

public abstract class Event {
	protected abstract void actions();
	private static double time = 0;
	private double eventTime;
	private Event pred, suc;
	
	public final void schedule(double evTime) {
		if (evTime < time)
			throw new RuntimeException ("attempt to schedule event in the past");
		cancel();
		eventTime = evTime;
		Event ev = SQS.pred;
		while (ev.eventTime > eventTime)
			ev = ev.pred;
		pred = ev;
		suc = ev.suc;
		ev.suc = suc.pred = this;
	}
	
	public final void cancel() {
		if (suc != null) {
			suc.pred = pred;
			pred.suc = suc;
			suc = pred = null;
		}
	}

	public final static double time() { return time; }

	public final static void runSimulation(double period) {
		while (SQS.suc != SQS) {
			Event ev = SQS.suc;
			time = ev.eventTime;
			if (time > period)
			break;
			ev.cancel();
			ev.actions();
		}
		stopSimulation();
	}
	
	public final static void stopSimulation() {
		while (SQS.suc != SQS)
			SQS.suc.cancel();
		time = 0;
	}
	
	private final static Event SQS = new Event() {
		{ SQS.pred = SQS.suc = this; }	// edited
		protected void actions() {}
	};
}