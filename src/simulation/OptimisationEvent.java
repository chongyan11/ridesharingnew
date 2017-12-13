package simulation;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import cplex.Pair;

import cplex.Optimiser;

public class OptimisationEvent {
	private int eventTime;
	private double ridesharingPayments;
	private double distanceSaved;
	private String fileName;
	private ArrayList<Integer> matchedParticipants;
	private ArrayList<Integer> soloParticipants;
		
	public OptimisationEvent(int eventTime) {
		this.eventTime = eventTime;
	}
	
	public double getPayments() {
		return ridesharingPayments;
	}
	
	public ArrayList<Integer> getMatchedParticipants() {
		return matchedParticipants;
	}
	
	public ArrayList<Integer> getSoloParticipants() {
		return soloParticipants;
	}
	
	public double getDistanceSaved() {
		return distanceSaved;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void runOptimisation() throws NoSuchFileException, IOException{
		Optimiser.run(fileName);
		matchedParticipants = Optimiser.getMatchedParticipants();
		soloParticipants = Optimiser.getSoloParticipants();
		ridesharingPayments = Optimiser.getRidesharingPayments();
		distanceSaved = Optimiser.getDistanceSaved();
	}
}
