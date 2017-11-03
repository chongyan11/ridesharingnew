package filereader;
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

public class ReadInput {
	private static final String INPUT_FILE_NAME = "test.csv";
	
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
}
