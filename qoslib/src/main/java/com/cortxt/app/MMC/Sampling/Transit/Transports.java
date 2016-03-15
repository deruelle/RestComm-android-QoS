package com.cortxt.app.MMC.Sampling.Transit;

import com.google.gson.annotations.SerializedName;

public class Transports {

	@SerializedName("id") 
	private int transportId;
	
	//Ie: 201 - short name
	@SerializedName("sname") 
	private String sName;
	
	//Ie: Somerset-Bridlewood/Tuscany - long name
	@SerializedName("lname") 
	private String lName;
	
	private String url;
	
	//Ie: 1
	@SerializedName("city_id") 
	private int cityId;
	
	//Ie: 1e4eb05040e68d9312eb6ed016eaa4d8
	@SerializedName("agency_id") 
	private String agencyId;
	
	//Ie: 201-20327
	@SerializedName("route_id") 
	private String routeId;
	
	//Ie: 28267553-2014JU-1LRTSA-Saturday-77
	@SerializedName("trip_id") 
	private String tripId;
	
	//0-7 https://developers.google.com/transit/gtfs/reference 
	@SerializedName("route_type") 
	private int routeType;
	
	public String getUrl() {
		return url;
	}
	
	//Lat/longs of each stop along the route
	private GeomLineString geom;

	public int getCityId() {
		return cityId;
	}
	
	public String getAgencyId() {
		return agencyId;
	}
	
	public String getRouteId() {
		return routeId;
	}
	
	public GeomLineString getGeom() {
		return geom;
	}
	
	public void setGeom(GeomLineString geom) {
		this.geom = geom;
	}
	
	public int getTransportId() {
		return transportId;
	}
	
	public String getShortName() {
		return sName;
	}
	
	public String getLongName() {
		return lName;
	}
	
	public int getRouteType() {
		return routeType;
	}
	
	public String getTripId() {
		return tripId;
	}
	
	public void setStationId(int stationId) {
		this.transportId = stationId;
	}
	
	public void setShortName(String name) {
		this.sName = name;
	}
	
	public void setRouteType(int type) {
		this.routeType = type;
	}
	
	public void setTripId(String shapeId) {
		this.tripId = shapeId;
	}
}

