package filereader;
import cplex.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import simulation.Params;
import simulation.Result;

public class InputOutput {
	private static final String INPUT_FILE_NAME = "test.csv";
	private static final String BG_FILE_NAME = "nodeinfo.csv";
	private static final String RESULT_FILE_NAME = "result.csv";
	private static final String ORIGIN_POPULATION_FILE_NAME = "residential-populations.csv";
	private static final String ORIGIN_ATTRACTIVENESS_FILE_NAME = "mrt-estates-origin.csv";
	private static final String DESTINATION_FACTOR_FILE_NAME = "industrial-weights.csv";
	private static final String DESTINATION_ATTRACTIVENESS_FILE_NAME = "mrt-industrial-destination.csv";
	private static final String DEMAND_MULTIPLIER_FILE_NAME = "demand-multiplier.csv";
	
	public static String getFileName(Integer i) {
		String testNum = i.toString();
		String fileName = testNum + INPUT_FILE_NAME;
		return fileName;
	}
	
	// Checks if file path is valid, and whether it can be read
	public static boolean checkPath(String fileName) throws NoSuchFileException{
		Path path = Paths.get(".", fileName);
		if(!Files.exists( path )) {
            throw new NoSuchFileException(path.toString());
        }
		return Files.isReadable(path);
	}
	
	// Reads file
	public static ArrayList<String> readFile(String fileName) throws FileNotFoundException, IOException{
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		ArrayList<String> trips = new ArrayList<String>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			trips.add(line);
		}
		reader.close();
		return trips;
	}
	
	// Read background info
	public static Information readBackground() throws FileNotFoundException, IOException {
		BufferedReader reader = new BufferedReader(new FileReader(BG_FILE_NAME));
		ArrayList<String> timeInfo = new ArrayList<String>();
		ArrayList<String> distanceInfo = new ArrayList<String>();
		// First line contains number of nodes
		String line = reader.readLine();
		int numNodes = Integer.parseInt(line);
		// Second sequence of lines contains time information between nodes
		for (int i = 0; i < numNodes; i++) {
			if ((line = reader.readLine()) == null) 
				break;
			else 
				timeInfo.add(line);
		}
		// Third sequence of lines contains distance information between nodes
		for (int i = 0; i < numNodes; i++) {
			if ((line = reader.readLine()) == null)
				break;
			else
				distanceInfo.add(line);
		}
		reader.close();
		int[][] times = new int[numNodes][numNodes];
		double[][] distances = new double[numNodes][numNodes];
		for (int i = 0; i < numNodes; i++) {
			String[] t = timeInfo.get(i).split(",", numNodes);
			String[] s = distanceInfo.get(i).split(",", numNodes);
			for (int j = 0; j < numNodes; j++) {
				// times are given in seconds
				times[i][j] = Integer.parseInt(t[j]) / 60;
				// distances are given in metres
				distances[i][j] = Double.parseDouble(s[j]) / 1000.0;
			}
		}
		
		Information in = new Information(numNodes, times, distances);
		return in;
	}
	
	public static void writeList(int[][] list, int length, int breadth, String filename) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(filename));
		for (int i = 0; i < length; i++) {
			String line = String.valueOf(list[i][0]);
			for (int j = 1; j < breadth; j++) {
				line = line.concat(", ");
				line = line.concat(String.valueOf(list[i][j]));
			}
			pw.println(line);
		}
		pw.close();
	}
	
	public static void appendResult(Result result, Params params) {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(RESULT_FILE_NAME, true));
			String line = new String();
			line = params.baseDemand + "," + params.simulationPeriod + "," + params.surge + "," + params.VOT + "," + params.surgeDemand + "," + params.surgeSupply + ","  
					+ result.successfulMatches + "," + result.totalDemand + "," + result.successRate + "," + result.totalRidesharingPayments + "," 
					+ result.totalDistanceSaved + "," + result.maxSystemMileage;
			pw.println(line);
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void clearResultFile() {
		try {
			PrintWriter pw = new PrintWriter(new FileWriter(RESULT_FILE_NAME));
			pw.print("");
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static double[] readOriginPopulations() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(ORIGIN_POPULATION_FILE_NAME));
			String line = new String();
			ArrayList<Integer> populations = new ArrayList<Integer>();
			while ((line = br.readLine()) != null) {
				String[] words = line.split(",");
				populations.add(Integer.parseInt(words[1]));
			}
			double[] originPopulations = new double[populations.size()];
			for (int i = 0; i < populations.size(); i++) {
				originPopulations[i] = (double) populations.get(i);
				// System.out.print(originPopulations[i] + " ");
			}
			br.close();
			return originPopulations;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new double[1];
	}
	
	public static double[][] readOriginAttractiveness() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(ORIGIN_ATTRACTIVENESS_FILE_NAME));
			String line = new String();
			ArrayList<ArrayList<Integer>> attractiveness = new ArrayList<ArrayList<Integer>>();
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				ArrayList<Integer> temp = new ArrayList<Integer> ();
				String[] words = line.split(",");
				for (int j = 1; j < words.length; j++) {
					temp.add(Integer.parseInt(words[j]));
				}
				attractiveness.add(temp);
			}
			double[][] OA = new double[attractiveness.size()][attractiveness.get(0).size()];
			for (int k = 0; k < attractiveness.size(); k++) {
				for (int j = 0; j < attractiveness.get(0).size(); j++) {
					OA[k][j] = attractiveness.get(k).get(j) / 1000.0;
					// System.out.print(OA[k][j] + " ");
				}
				// System.out.println();
			}
			return OA;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new double[1][1];
	}
	
	public static double[] readDestinationFactors() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(DESTINATION_FACTOR_FILE_NAME));
			String line = new String();
			ArrayList<Integer> factors = new ArrayList<Integer>();
			while ((line = br.readLine()) != null) {
				String[] words = line.split(",");
				factors.add(Integer.parseInt(words[1]));
			}
			double[] destinationFactors = new double[factors.size()];
			for (int i = 0; i < factors.size(); i++) {
				destinationFactors[i] = (double) factors.get(i);
				// System.out.print(destinationFactors[i] + " ");
			}
			br.close();
			return destinationFactors;
	} catch (IOException e) {
		e.printStackTrace();
	}
	return new double[1];
	}
	
	public static double[][] readDestinationAttractiveness() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(DESTINATION_ATTRACTIVENESS_FILE_NAME));
			String line = new String();
			ArrayList<ArrayList<Integer>> attractiveness = new ArrayList<ArrayList<Integer>>();
			line = br.readLine();
			while ((line = br.readLine()) != null) {
				ArrayList<Integer> temp = new ArrayList<Integer> ();
				String[] words = line.split(",");
				for (int j = 1; j < words.length; j++) {
					temp.add(Integer.parseInt(words[j]));
				}
				attractiveness.add(temp);
			}
			double[][] DA = new double[attractiveness.size()][attractiveness.get(0).size()];
			for (int k = 0; k < attractiveness.size(); k++) {
				for (int j = 0; j < attractiveness.get(0).size(); j++) {
					DA[k][j] = attractiveness.get(k).get(j) / 1000.0;
					// System.out.print(DA[k][j] + " ");
				}
				// System.out.println();
			}
			return DA;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new double[1][1];
	}
	
	public static double[] readDemandMultiplier() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(DEMAND_MULTIPLIER_FILE_NAME));
			String line = new String();
			ArrayList<Double> multipliers = new ArrayList<Double>();
			while((line = br.readLine()) != null) {
				multipliers.add(Double.parseDouble(line));
			}
			double[] array = new double[multipliers.size()];
			for (int i = 0; i < multipliers.size(); i++) {
				array[i] = multipliers.get(i);
			}
			return array;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new double[1];
	}
}
