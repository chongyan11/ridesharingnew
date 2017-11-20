package simulation;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import cplex.Pair;

import cplex.Optimiser;

public class OptimisationEvent {
	private int eventTime;
	private double ridesharingPayments;
	private String fileName;
	private ArrayList<Integer> matchedParticipants;
	private ArrayList<Pair> soloParticipants;
		
	public OptimisationEvent(int eventTime) {
		this.eventTime = eventTime;
	}
	
	public double getPayments() {
		return ridesharingPayments;
	}
	
	public ArrayList<Integer> getMatchedParticipants() {
		return matchedParticipants;
	}
	
	public ArrayList<Pair> getSoloParticipants() {
		return soloParticipants;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void runOptimisation() throws NoSuchFileException, IOException{
		Optimiser model = new Optimiser();
		model.run(fileName);
		matchedParticipants = model.getMatchedParticipants();
		soloParticipants = model.getSoloParticipants();
		ridesharingPayments = model.getRidesharingPayments();
	}
}
