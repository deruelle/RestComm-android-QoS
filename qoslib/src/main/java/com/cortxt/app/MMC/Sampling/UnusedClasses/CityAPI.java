package com.cortxt.app.MMC.Sampling.UnusedClasses;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class CityAPI {
	
    private boolean success;
//    @SuppressWarnings("unused")
//	private int total;
//    private int count;
//    private Values values;
//    
    public CityAPI() { }
//    
//    public CityAPI(boolean success, int total, int count) {
//		this.success = success;
//		this.total = total;
//		this.count = count;
//	}
//    
    public boolean getSuccess() {
    	return success;
    }
//    
//    public int getCount() {
//    	return count;
//    }
//    
//    public Values getValues() {
//    	return values;
//    }
//    
//    public void setTotal(int total) {
//    	this.total = total;
//    }
//    
    public void setSuccess(boolean success) {
    	this.success = success;
    }
//    
//    public void setCount(int count) {
//    	this.count = count;
//    }
//    
//    public void getValues(Values values) {
//    	this.values = values;
//    }
//    
//    //Should be called only once, to initialize
//    public void newValue(String areaId, String areaName) {
//    	values = new Values(areaId, areaName);
//    }
//    
//    //Should be called only once, to initialize
//    public void newShape() {
//    	List<Shape> shapes = new ArrayList<Shape>();
//    	values.newShapes(shapes);
//    }
//    
//    public void addShape(String shapeId, int[] latArr, int[] longArr, int[] ptSeqArr) {
//    	values.getShapesArray().add(new Shape(shapeId, latArr, longArr, ptSeqArr));
//    }
//    
//    //Should be called only once, to initialize
//    public void newRouteList() {
//    	List<Route> routes = new ArrayList<Route>();
//    	values.newRoutes(routes);
//    }
//    
//    public void addStop(String stopName, String arrivalTime, String departureTime, String stopId, int stopLat, int stopLong) {
//    	int size = values.getRoutesArray().size();
//    	if(size < 1)
//    		return;
//    	Route route = values.getRoutesArray().get(size - 1);
//    	route.addStop(stopName, arrivalTime, departureTime, stopId, stopLat, stopLong);
//	}
//    
//    public void addRoute(String routeId, String name, String type, String speed, String shapeId) {
//    	values.addRoute(routeId, name, type, speed, shapeId);
//	}
//    
//    public String routeToString(int index) {
//    	return values.getRouteToString(index);
//    } //TODO add for shape
//    
//    public class Values {
//    	@SerializedName("area_id") 
//    	private String areaId;
//    	private List<Route> routes;
//    	private List<Shape> shapes;
//    	
//    	@SerializedName("area_name") 
//    	private String areaName;
//    	
//    	public Values(String areaId, String areaName) {
//    		this.areaId = areaId;
//    		this.areaName = areaName;
//    	}
//    	
//    	public String getRouteToString(int index) { //TODO
//    		Route route = routes.get(index);
//    		String info = route.getRouteId();
//    		
//    		
//    		return info;
//    	}
//    	
//    	public void newRoutes(List<Route> routes) {
//    		this.routes = routes;
//    	}
//    	
//    	public List<Route> getRoutesArray() {
//    		return routes;
//    	}
//    	
//    	public List<Shape> getShapesArray() {
//    		return shapes;
//    	}
//    	
//    	public void newShapes(List<Shape> shapes) {
//    		this.shapes = shapes;
//    	}
//    	
//    	public String getAreaName() {
//    		return areaName;
//    	}
//    	
//    	public String getAreaId() {
//    		return areaId;
//    	}
//    	
//    	public void addRoute(String routeId, String name, String type, String speed, String shapeId) {
//    		routes.add(new Route(routeId, name, type, speed, shapeId));
//    	}
//    }
//    
//    private class Shape {
//    	@SerializedName("shape_id") 
//    	private String shapeId;
//    	
//    	@SerializedName("lat_arr") 
//    	private int[] latArr;
//    	
//    	@SerializedName("long_arr") 
//    	private int[] longArr;
//    	
//    	@SerializedName("pt_seq_arr") 
//    	private int[] ptSeqArr;
//    	
//    	public Shape(String shapeId, int[] latArr, int[] longArr, int[] ptSeqArr) {
//    		this.shapeId = shapeId;
//    		this.latArr = latArr;
//    		this.longArr = longArr;
//    		this.ptSeqArr = ptSeqArr;
//    	}
//    	
//    	public String getShapeId() {
//    		return shapeId;
//    	}
//    	
//    	public int[] getLatArr() {
//    		return latArr;
//    	}
//    	
//    	public int[] getLonArr() {
//    		return longArr;
//    	}
//    	
//    	public int[] getPtSeqArr() {
//    		return ptSeqArr;
//    	}
//    }
//    
//    private class Route {
//    	@SerializedName("route_id") 
//    	private String routeId;
//    
//    	private String name;
//    	
//    	private String type;
//    	
//    	private String speed;
//    	
//    	@SerializedName("shape_id") 
//    	private String shapeId;
//    	
//    	@SerializedName("stops_array") 
//    	private List<Stop> stopsArray;
//    	
//    	public Route() { }
//    	
//    	public Route(String routeId, String name, String type, String speed, String shapeId) {
//    		this.routeId = routeId;
//    		this.name = name;
//    		this.type = type;
//    		this.speed = speed;
//    		this.shapeId = shapeId;;
//    		stopsArray = new ArrayList<Stop>();
//    	}
//    	
//    	public String getRouteId() {
//    		return routeId;
//    	}
//    	
//    	public String getRouteName() {
//    		return name;
//    	}
//    	
//    	public String getType() {
//    		return type;
//    	}
//    	
//    	public String getSpeed() {
//    		return speed;
//    	}
//    	
//    	public String getShapeId() {
//    		return shapeId;
//    	}
//    	
//    	public List<Stop> getStops() {
//    		return stopsArray;
//    	}
//    	
//    	public void addStop(String stopName, String arrivalTime, String departureTime, String stopId, int stopLat, int stopLong) {
//    		stopsArray.add(new Stop(stopName, arrivalTime, departureTime, stopId, stopLat, stopLong));
//    	}
//    }
//    
//    private class Stop {
//    	@SerializedName("stop_name") 
//    	private String stopName;
//    	@SerializedName("arrival_time") 
//    	private String arrivalTime;
//    	@SerializedName("departure_time") 
//    	private String departureTime;
//    	@SerializedName("stop_id") 
//    	private String stopId;
//    	@SerializedName("stop_lat") 
//    	private int stopLat;
//    	@SerializedName("stop_long") 
//    	private int stopLong;
//    
//    	public Stop() { }
//    	
//    	public Stop(String stopName, String arrivalTime, String departureTime, String stopId, int stopLat, int stopLong) {
//        	this.stopName = stopName;
//        	this.arrivalTime = arrivalTime;
//        	this.departureTime = departureTime;
//        	this.stopId = stopId;
//        	this.stopLat = stopLat;
//        	this.stopLong = stopLong;
//        }
//    	
//    	public String getStopName() {
//    		return stopName;
//    	}
//    	
//    	public String getArrivalTime() {
//    		return arrivalTime;
//    	}
//    	
//    	public String getDepartureTime() {
//    		return departureTime;
//    	}
//    	
//    	public String getStopId() {
//    		return stopId;
//    	}
//    	
//    	public int getStopLat() {
//    		return stopLat;
//    	}
//    	
//    	public int getStopLong() {
//    		return stopLong;
//    	}
//    }
}