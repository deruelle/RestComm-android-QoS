package com.cortxt.app.MMC.Sampling.Transit;

import java.io.Serializable;
import java.util.Date;

import android.text.format.DateUtils;

public class TransitStats implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int samplesCollected;
	private double distance;
	private int speedTests;
	private float downloadSpeed;
	private float uploadSpeed;
	private double minutes;
	private long time;
	private int usage;
	private int stations;
	private int issues;
	
	public TransitStats() {
		samplesCollected = 0;
		speedTests = 0;
		downloadSpeed = 0;
		uploadSpeed = 0;
		distance = 0;
		time = 0;
		usage = 0;
		stations = 0;
		issues = 0;
	}
	
	public void incrementIssues() {
		this.issues += 1;
	}
	
	public String getIssues() {
		return String.valueOf(issues);
	}
	
	public void setStationCount(int size) {
		this.stations = size;
	}
	
	public void incrementStationCount() {
		this.stations = stations + 1;
	}
	
	public String getStationCount() {
		return String.valueOf(stations);
	}
	
	public void setUsage(int count) {
		//This method increments the previous route count
		this.usage = count;
	}
	
	public String getUsage() {
		return String.valueOf(usage);
	}
	
	public void setStartTime(long time) {
		this.time = time;
	}
	
	public void increaseTime(long newTime) {
		
		double difference = newTime - time;
		
//		minutes = (String) DateUtils.getRelativeTimeSpanString(time, newTime, 0);
//		difference /= 1000;
//		difference /= 1000;
		
		difference = Math.floor(difference / 1000);
		int secs_diff = (int) difference % 60;
		difference = Math.floor(difference / 60);
		int mins_diff = (int) difference % 60;
		difference = Math.floor(difference / 60);
//		double hours_diff = difference % 24;
//		difference = Math.floor(difference / 24);
		
		String temp = mins_diff + "." + secs_diff; 
		minutes += Double.valueOf(temp);
		this.time = newTime;
	}
	
	public String getMinutes() {
		return String.format("%.2f", minutes);
	}
	
	public void increaseSamplesCollected() {
		this.samplesCollected++;
	}
	
	public String getSamplesCollected() {
		return String.valueOf(samplesCollected); 
	}
	
	public void setTopUpload(float uploadSpeed) {
		if(uploadSpeed > this.uploadSpeed)
			this.uploadSpeed = uploadSpeed;
	}
	
	public String getTopUpload() {
		return String.valueOf(String.format("%.2f",uploadSpeed));
	}
	
	public void setTopDownload(float downloadSpeed) {
		if(downloadSpeed > this.downloadSpeed)
			this.downloadSpeed = downloadSpeed;
	}
	
	public String getTopDownload() {
		return String.valueOf(String.format("%.2f",downloadSpeed));
	}
	
	public void increaseSpeedTestsCount() {
		this.speedTests++;;
	}
	
	public String getSpeedTests() {
		return String.valueOf(speedTests);
	}
	
	public String getDistance() {
		if(distance > 1000) {
			double kilometers = distance/1000;
			return String.format("%.2f", kilometers) + "kms";
		}
		
		return String.format("%.2f", distance) + "meters";
	}
	
	public void increaseDistance(double increase) {
		this.distance += increase;
	}
}
