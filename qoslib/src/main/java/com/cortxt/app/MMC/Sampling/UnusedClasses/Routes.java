package com.cortxt.app.MMC.Sampling.UnusedClasses;

import java.util.ArrayList;
import java.util.List;

import com.cortxt.app.MMC.Sampling.UnusedClasses.Stop;
import com.google.gson.annotations.SerializedName;

public class Routes {
	@SerializedName("route_id") 
	private String routeId;
	
	private String name;
	
	private String type;
	
	private String speed;
	
	@SerializedName("shape_id") 
	private String shapeId;
	
	@SerializedName("stops_array")
	private List<Stop> stops = new ArrayList<Stop>();

	public List<Stop> getStops() {
		return this.stops;
	}
	
	public void setStops(List<Stop> stops) {
		this.stops = stops;
	}
	
	public String getRouteId() {
		return routeId;
	}
	
	public String getRouteName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	public String getSpeed() {
		return speed;
	}
	
	public String getShapeId() {
		return shapeId;
	}
	
	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}
	
	public void setRouteName(String name) {
		this.name = name;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void setSpeed(String speed) {
		if(speed.equals("null") || speed == null)
			this.speed = "0";
		else
			this.speed = speed;
	}
	
	public void setShapeId(String shapeId) {
		this.shapeId = shapeId;
	}
}
