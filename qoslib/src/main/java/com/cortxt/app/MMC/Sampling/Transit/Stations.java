package com.cortxt.app.MMC.Sampling.Transit;

import com.google.gson.annotations.SerializedName;

public class Stations {

	private int id;
	
	@SerializedName("stop_name")
	private String stopName;
	
	@SerializedName("stop_id")
	private String stopId;
	
	@SerializedName("stop_sequence")
	private int stopSequence;
	
	private GeomPoint geom;
	
	private double distance;
	
	private Duration duration;
	
	public int getId() {
		return id;
	}
	
	public String getStopName() {
		return stopName;
	}
	
	public String getStopId() {
		return stopId;
	}
	
	public int getStopSequence() {
		return stopSequence;
	}
	
	public GeomPoint getGeomPoint() {
		return geom;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public Duration getDuration() {
		return duration;
	}
}
