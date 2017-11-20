package filereader;
import cplex.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class InputOutput {
	private static final String INPUT_FILE_NAME = "test.csv";
	private static final String BG_FILE_NAME = "nodes.csv";
	private static final String SIMULATION_PARTICIPANTS_FILE_NAME = "participants.csv";
	
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
			String[] t = timeInfo.get(i).split(", ", numNodes);
			String[] s = distanceInfo.get(i).split(", ", numNodes);
			for (int j = 0; j < numNodes; j++) {
				times[i][j] = Integer.parseInt(t[j]);
				distances[i][j] = Double.parseDouble(s[j]);
			}
		}
		Information in = new Information(numNodes, times, distances);
		return in;
	}
	
	public static void writeList(int[][] list, int length, int breadth) throws IOException {
		PrintWriter pw = new PrintWriter(new FileWriter(SIMULATION_PARTICIPANTS_FILE_NAME));
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
}
