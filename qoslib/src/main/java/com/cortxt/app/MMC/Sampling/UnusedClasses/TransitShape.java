package com.cortxt.app.MMC.Sampling.UnusedClasses;

public class TransitShape {
	
	private int latitude;
	private int longitude;
	private int sequence;
	private String id;
	
	public TransitShape(int latitude, int longitude, int sequence, String id) {
		setLatitude(latitude);
		setLongitude(longitude);
		setSequence(sequence);
		setId(id);
	}

	public void setLatitude(int latitude) {
		this.latitude = latitude;
	}
	
	public int getLatitude() {
		return latitude;
	}
	
	public void setLongitude(int longitude) {
		this.longitude = longitude;
	}
	
	public int getLongitude() {
		return longitude;
	}
	
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	
	public int getSequence() {
		return sequence;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
}
