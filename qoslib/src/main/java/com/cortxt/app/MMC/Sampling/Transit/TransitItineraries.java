package com.cortxt.app.MMC.Sampling.Transit;

public class TransitItineraries {
	private String line;
	private Integer depart;
	private Integer arrive;
	private String departName, arriveName;
	
	public TransitItineraries (String line, Integer depart, Integer arrive, String departName, String arriveName) {
		this.line = line;
		this.depart = depart;
		this.arrive = arrive;
		this.departName = departName;
		this.arriveName = arriveName;
	}
	
	public void setLine(String line) {
		this.line = line;
	}
	
	public String getLine() {
		return line;
	}
	
	public void setDepart(Integer depart) {
		this.depart = depart;
	}
	
	public Integer getDepart() {
		return depart;
	}
	
	public String getDepartName() {
		return departName;
	}
	
	public void setArrive(Integer arrive) {
		this.arrive = arrive;
	}
	
	public Integer getArrive() {
		return arrive;
	}
	public String getArriveName() {
		return arriveName;
	}
}
