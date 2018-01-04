package simulation;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;

import cplex.Optimiser;

public class OptimisationEvent implements Closeable {
	private int eventTime;
	private double paymentSplit;
	private double ridesharingPayments;
	private double distanceSaved;
	private double totalDistance;
	private double[] ridesharePaymentsByNode;
	private double[] rideshareDistanceByNode;
	private boolean surge;
	private String fileName;
	private ArrayList<Integer> matchedParticipants;
	private ArrayList<Integer> soloParticipants;
		
	public OptimisationEvent(int eventTime, double paymentSplit, boolean surge) {
		this.eventTime = eventTime;
		this.paymentSplit = paymentSplit;
		this.surge = surge;
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
	
	public double[] getRidesharePaymentsByNode() {
		return ridesharePaymentsByNode;
	}
	
	public double[] getRideshareDistanceByNode() {
		return rideshareDistanceByNode;
	}
	
	public double getTotalDistance() {
		return totalDistance;
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public void runOptimisation() throws NoSuchFileException, IOException{
		Optimiser.run(fileName, paymentSplit, surge);
		matchedParticipants = Optimiser.getMatchedParticipants();
		soloParticipants = Optimiser.getSoloParticipants();
		ridesharingPayments = Optimiser.getRidesharingPayments();
		distanceSaved = Optimiser.getDistanceSaved();
		ridesharePaymentsByNode = Optimiser.getRidesharePaymentsByNode();
		rideshareDistanceByNode = Optimiser.getRideshareDistanceByNode();
		totalDistance = Optimiser.getTotalDistance();
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}
}
