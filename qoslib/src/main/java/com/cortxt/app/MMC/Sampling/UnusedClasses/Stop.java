package com.cortxt.app.MMC.Sampling.UnusedClasses;

import com.google.gson.annotations.SerializedName;

public class Stop {
	@SerializedName("stop_name") 
	private String stopName;
	@SerializedName("arrival_time") 
	private String arrivalTime;
	@SerializedName("departure_time") 
	private String departureTime;
	@SerializedName("stop_id") 
	private String stopId;
	@SerializedName("stop_lat") 
	private double stopLat;
	@SerializedName("stop_long") 
	private double stopLong;
	
	public String getStopName() {
		return stopName;
	}
	
	public String getArrivalTime() {
		return arrivalTime;
	}
	
	public String getDepartureTime() {
		return departureTime;
	}
	
	public String getStopId() {
		return stopId;
	}
	
	public double getStopLat() {
		return stopLat;
	}
	
	public double getStopLong() {
		return stopLong;
	}
	
	public void setStopName(String stopName) {
		this.stopName = stopName;
	}
	
	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	
	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}
	
	public void setStopId(String stopId) {
		this.stopId = stopId;
	}
	
	public void setStopLat(double stopLat) {
		this.stopLat = stopLat;
	}
	
	public void setStopLong(double stopLong) {
		this.stopLong = stopLong;
	}
}
