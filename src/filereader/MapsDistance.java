package filereader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class MapsDistance {
	private static final String FILENAME = "locationlist.csv";
	private static final String OUTPUT = "locations.csv";
	private static final int NUM_LOCATIONS = 118;
	
	public static void main(String[] args) {
		try {
			run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void run() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(FILENAME));
		PrintWriter pw = new PrintWriter(new FileWriter(OUTPUT));
		String line;
		String[] words;
		
		pw.println("Node,Location");
		
		for (int i = 0; i < NUM_LOCATIONS; i++) {
			line = br.readLine();
			words = line.split(" ");
			String toWrite = new String();
			toWrite = toWrite.concat((i+1) + ",");
			for (int j = 0; j < words.length; j++) {
				toWrite = toWrite.concat(words[j]);
				toWrite = toWrite.concat("+");
			}
			toWrite = toWrite.concat("MRT");
			System.out.println(toWrite);
			pw.println(toWrite);
		}
		
		br.close();
		pw.close();
	}
}
