package com.cortxt.app.MMC.Sampling.UnusedClasses;

import java.lang.reflect.Type;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class CityDeserializer {//implements JsonDeserializer<CityAPI> {

//	/**** This class is not being used anymore ****/
//	
//	private TransitDatabaseReadWrite transitDB;
//	private Context context;
//	
//	public CityDeserializer(Context context) {
//		this.context = context;
//	}
//	
//	@Override
//	public CityAPI deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//		JsonObject jsonObject = json.getAsJsonObject();
//		transitDB = new	TransitDatabaseReadWrite(this.context);
//	    boolean success = jsonObject.get("success").getAsBoolean();
//	  
//	    CityAPI city = new CityAPI();
//	    city.setSuccess(success);
//	    
//		if(success == false)
//			return city;
//		
////	    int total = jsonObject.get("total").getAsInt();
////	    int count = jsonObject.get("count").getAsInt();
//
//	    JsonObject valuesObject = jsonObject.get("values").getAsJsonObject();
//	    String areaId = valuesObject.get("area_id").getAsString();
////	    String areaName = valuesObject.get("area_name").getAsString();
//
////	    CityAPI city = new CityAPI(success, total, count);
////	    city.newValue(areaId, areaName);
////	    city.newRouteList();
////	    city.newShape();
//	    
//	    JsonArray routesArray = valuesObject.get("routes_array").getAsJsonArray();
//	    for (int i = 0; i < routesArray.size(); i++) {
//	    	JsonObject routeJson = routesArray.get(i).getAsJsonObject();
//			String routeId = routeJson.get("route_id").getAsString();
//			String shapeId = routeJson.get("shape_id").getAsString();
//			JsonElement speedElement = routeJson.get("speed"); 
//			String speed = "0";
//			if(speedElement.equals("null"))
//				speed = speedElement.getAsString();
//			String name = routeJson.get("name").getAsString();
//			JsonElement typeElement = routeJson.get("type"); 
//			String type = null;
//			if(typeElement.equals("null"))
//				type = typeElement.getAsString();
//
//			//If this route does not already exist in DB, getRouteCount() will assign 0
//			int usageCount = transitDB.getRouteCount(routeId);
//			
//			transitDB.saveRoutesToDB(name, routeId, String.valueOf(areaId), type, Integer.valueOf(speed), usageCount, shapeId);
////			city.addRoute(routeId, name, type, speed, shapeId);
//			
//			JsonArray stopsJsonArray = routeJson.get("stops_array").getAsJsonArray();
//			for(int k = 0; k < stopsJsonArray.size(); k++) {
//				JsonObject stopsJson = stopsJsonArray.get(k).getAsJsonObject();
//				String stopId = stopsJson.get("stop_id").getAsString();
//				String arrivalTime = stopsJson.get("arrival_time").getAsString();
//				String departTime = stopsJson.get("departure_time").getAsString();
//				String stopName = stopsJson.get("stop_name").getAsString();
//				String _stopLat = stopsJson.get("stop_lat").getAsString();
//				String _stopLong = stopsJson.get("stop_long").getAsString();
//				
//				int stopLat = (int) (Float.valueOf(_stopLat)*1000000);
//				int stopLong = (int) (Float.valueOf(_stopLong)*1000000);
//				
//				transitDB.saveRouteStopsToDB(stopName, departTime, arrivalTime, stopId, String.valueOf(areaId), stopLat, stopLong, name, routeId);
////				city.addStop(stopName, arrivalTime, departTime, stopId, stopLat, stopLong);
//			}
//			
//	    }
//
//		JsonObject shapesJson = valuesObject.get("shapes").getAsJsonObject();
//		Set<Entry<String, JsonElement>> jsonEntrySet = shapesJson.entrySet();
//		
//		for (Entry<java.lang.String, JsonElement> entry : jsonEntrySet) {
//			String key = entry.getKey();
//			
//			JsonObject shape = shapesJson.get(key).getAsJsonObject();
//			String shapeId = shape.get("shape_id").getAsString();
//			JsonArray latitudes = shape.get("lat_arr").getAsJsonArray();
//			JsonArray longitudes = shape.get("lon_arr").getAsJsonArray();
//			JsonArray sequences = shape.get("pt_seq_arr").getAsJsonArray();				
//			
//			int size = latitudes.size();
//			int[] latArr = new int[size];
//			int[] longArr = new int[size];
//			int[] ptSeqArr = new int[size];
//			
//			for(int n = 0; n < size; n++) {
//				int latitude = (int) (latitudes.get(n).getAsDouble() * 1000000);
//				latArr[n] = latitude;
//				int longitude = (int) (longitudes.get(n).getAsDouble() * 1000000);
//				longArr[n] = longitude;
//				int sequence = sequences.get(n).getAsInt();
//				ptSeqArr[n] = sequence;
//				transitDB.saveShape(latitude, longitude, sequence, shapeId, String.valueOf(areaId));
//			}
////			city.addShape(shapeId, latArr, longArr, ptSeqArr);
//		}
//
//	    return city;
//	}
}
