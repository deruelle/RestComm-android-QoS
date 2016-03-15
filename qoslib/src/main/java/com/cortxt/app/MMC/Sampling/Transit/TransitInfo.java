package com.cortxt.app.MMC.Sampling.Transit;

import java.io.Serializable;

import com.google.android.maps.GeoPoint;

public class TransitInfo implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private SerializableGeoPoint geoPoint;
	private String name;
	private int stationId;
	private boolean notMovingIssue;
	private float averageSpeed;
	private long startTime;
	
	public TransitInfo(String newName, int newId, SerializableGeoPoint newGeoPoint) {
		setGeoPoint(newGeoPoint);
		setStationId(newId);
		setName(newName);
		setNotMovingIssue(false);
		setStartTime(0);
	}
	
	public void setGeoPoint(SerializableGeoPoint newGeoPoint) {
		this.geoPoint = newGeoPoint;
	}
	
	public SerializableGeoPoint getGeoPoint() {
		return geoPoint;
	}
	
	public void setName(String newName) {
		this.name = newName;
	}
	
	public String getName() {
		return name;
	}
	
	public void setStationId(int stationId) {
		this.stationId = stationId;
	}
	
	public int getStationId() {
		return stationId;
	}
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getStartTime() {
		return startTime;
	}
	
//	public void setStopTime(/*String departTime, String arriveTime*/ long difference) {
//		//Departure time: leaves current station
//		//Arrival time: arrives at the next station
//		
////		if(departTime == null && arriveTime == null) {
////			this.stopTime = 0;
////			return;
////		}
////		
////		Date depart = null;
////		Date arrive = null;
////		long difference = 0;
////		try {
////			depart = new SimpleDateFormat("HH:mm:ss").parse(departTime);
////			arrive = new SimpleDateFormat("HH:mm:ss").parse(arriveTime);
////			difference = arrive.getTime() - depart.getTime();
////		} catch (ParseException e) {
////			e.printStackTrace();
////		}
//		
////		float inMinutes = difference / (60 * 1000);
////	    float inHours = inMinutes / 60;
//		this.stopTime = difference;
//	}
//	
	public void setNotMovingIssue(boolean issue) {
		this.notMovingIssue = issue;
	}
	
	public boolean getMovingIssue() {
		return notMovingIssue;
	}
	
	public void setSpeed(float newSpeed) {
		this.averageSpeed = newSpeed;
	}
	
	public float getSpeed() {
		return averageSpeed;
	}
}
