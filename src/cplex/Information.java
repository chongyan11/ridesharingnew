package cplex;

public class Information {
	public int numNodes;
	public int[][] times;
	public double[][] distances;
	
	public Information(int numNodes, int[][] times, double[][] distances) {
		this.numNodes = numNodes;
		this.times = times;
		this.distances = distances;
	}
}
