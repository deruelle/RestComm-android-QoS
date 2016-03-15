package com.cortxt.app.MMC.Sampling.UnusedClasses;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.PowerManager;
import android.widget.Toast;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Sampling.Transit.SerializableGeoPoint;
import com.cortxt.app.MMC.Sampling.Transit.TransitInfo;
import com.cortxt.com.mmcextension.datamonitor.database.DatabaseHandler;

public class TransitDatabaseReadWrite {
	
//	private SQLiteDatabase sqlDB;
//	private Context context;
//	
//	public TransitDatabaseReadWrite(Context context) {
//		this.context = context;
//	}
//	
//	//Area comes from the GeoCoder or user manually entering it
//	public String getAreaId(String areaName) {
//		String areaId = null;
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "name = '" + areaName + "'";
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS,
//					null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					areaId = cursor.getString(cursor.getColumnIndex("area_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		//will return null if the area was not found
//		return areaId;
//	}
//	
//	public String[][] getRoutesFromDB(String areaId) { 
//		
//		String[][] stations = null;
//				
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES,
//					null, whereClause, null, null, null, null);
//			//2D array of route IDs and names, used to populate start and stop ListViews
//			stations = new String[cursor.getCount()][1];
//			int index = 0;
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						int routeId = cursor.getInt(cursor.getColumnIndex("route_id")); 
//						String routeName = cursor.getString(cursor.getColumnIndex("name"));
//						stations[index][0] = String.valueOf(routeId);
//						stations[index][1] = routeName;
//						index++;
//					} while (cursor.moveToNext());
//				cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return stations;
//	}
//	
//	public ArrayList<TransitInfo> getRouteStopsFromDB(String routeId) { 
//		
//		ArrayList<TransitInfo> transitInfo = new ArrayList<TransitInfo>();
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//	
//		String whereClause = "route_id = '" + routeId + "'";
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
//					null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				//Going in descending order so > finds the closest next route time
//				//ie: current time = 8:35am
//				//stops: 830am 840am 850am -- will pick 840am (closest train that hasn't left already)
//				if (cursor.moveToFirst()) {
//					do {
//						String name = cursor.getString(cursor.getColumnIndex("name")); 
//						long time = cursor.getLong(cursor.getColumnIndex("start_time"));
//						int latitude = cursor.getInt(cursor.getColumnIndex("latitude"));
//						int longitude = cursor.getInt(cursor.getColumnIndex("longitude"));
//						String type = null;
//						try {
//							type = cursor.getString(cursor.getColumnIndex("type"));
//						} catch (Exception e) {
////							e.printStackTrace();
//						}
//						TransitInfo info = new TransitInfo(name, routeId, new SerializableGeoPoint(latitude, longitude), time, type);
//						transitInfo.add(info);
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return transitInfo;
//	}
//	
//	public String getRouteByTimeAndLocation(String name, int latitude, int longitude) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		long currentTime = System.currentTimeMillis()/1000;
//		String routeId = null;
//		
//		String whereClause = "route_name = '" + name + "'";
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
//			null, whereClause, null, null, null, "start_time ASC");
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						routeId = cursor.getString(cursor.getColumnIndex("route_id"));
//						int savedLatitude = cursor.getInt(cursor.getColumnIndex("latitude"));
//						int savedLongtitude = cursor.getInt(cursor.getColumnIndex("longitude"));
//						long savedTime = cursor.getLong(cursor.getColumnIndex("start_time"));
//						
//						//Is latitude within 100 meters of this stop
//						if((latitude+1000) >= savedLatitude && ((latitude-1000) <= savedLatitude)) {
//							//Is longitude?
//							if((longitude+1000) >= savedLongtitude && ((longitude-1000) <= savedLongtitude)) {
//								//Is the starting time of this stop after current time
//								//Stops are saved early to late times, so first stop > current time is likeliest the stop that will be taken
//								if(currentTime <= savedTime) {
//									//Stop looking, return current route id
//									break;
//								}
//							}
//						}
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return routeId;
//	}
//	
//	public String getRouteByTimeAndLocation2(String name, int latitude, int longitude) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		long currentTime = System.currentTimeMillis()/1000;
//		String routeId = null;
//		
//		int maxLat = 9999999;
//		int maxLong = 9999999;
//		
//		String whereClause = "route_name = '" + name + "'";
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
//			null, whereClause, null, null, null, "start_time ASC");
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String savedRouteId = cursor.getString(cursor.getColumnIndex("route_id"));
//						int savedLatitude = cursor.getInt(cursor.getColumnIndex("latitude"));
//						int savedLongtitude = cursor.getInt(cursor.getColumnIndex("longitude"));
//						long savedTime = cursor.getLong(cursor.getColumnIndex("start_time"));
//						
//						int tempLat = Math.abs(latitude - savedLatitude);
//						int tempLong = Math.abs(longitude - savedLongtitude);
//						if(tempLat < maxLat && tempLong < maxLong) {
//							maxLat = tempLat;
//							maxLong = tempLong;
//							
//							Date current = new Date(currentTime);
//							SimpleDateFormat f1 = new SimpleDateFormat("HH:mm:ss");
//							f1.setTimeZone(TimeZone.getTimeZone("GMT"));
//							String s1 = f1.format(current);
////							
//							Date saved = new Date(savedTime);
//							SimpleDateFormat f2 = new SimpleDateFormat("HH:mm:ss");
//							f2.setTimeZone(TimeZone.getTimeZone("GMT"));
//							String s2 = f2.format(saved);
//							
//							DateFormat df = new SimpleDateFormat("HH:mm:ss");
//							Date date1 = df.parse(s1);  
//							Date date2 = df.parse(s2);
//							
//							if(date1.compareTo(date2) < 0) {
//								routeId = savedRouteId;
//							}
//						}
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return routeId;
//	}
//	
//	public String testRouteIds(String ngame) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		String routeId = null;
//		
////		String whereClause = "route_name = '" + name + "'";
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
//			null, null, null, null, null, "start_time ASC");
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						routeId = cursor.getString(cursor.getColumnIndex("route_id"));
//						String areaId = cursor.getString(cursor.getColumnIndex("area_id"));
//						String stopId = cursor.getString(cursor.getColumnIndex("stop_id"));
//						String name = cursor.getString(cursor.getColumnIndex("name"));
//						System.out.println("routeId = " + routeId + ", areaId = " + areaId + ", stopId = " + stopId + ", name = " + name);
//						
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return routeId;
//	}
//	
//	public String getRouteByTimeAndName(String name) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		long currentTime = System.currentTimeMillis()/1000;
//		String routeId = null;
//		
//		String whereClause = "route_name = '" + name + "'"  + " and start_time" + ">=?" + currentTime + "'" ;
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
//					null, whereClause, new String[]{ String.valueOf(currentTime)}, null, null, "start_time ASC");
//			if (cursor != null) {
//				//Going in descending order so > finds the closest next route time
//				//ie: current time = 8:35am
//				//stops: 830am 840am 850am -- will pick 840am (closest train that hasn't left already)
//				if (cursor.moveToFirst()) {
//					routeId = cursor.getString(cursor.getColumnIndex("route_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return routeId;
//	}
//	
//	public void saveAreaToDB(String name, String id) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("name", name);
//		values.put("area_id", id);
//		values.put("timestamp", System.currentTimeMillis()/1000);
//		values.put("download", String.valueOf(false));
//		
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_AREAS, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void setDownload(String areaId, boolean checked) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "area_id = '" + areaId + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("download", String.valueOf(checked));
//
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_AREAS, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public boolean getDownload(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + areaId;
//		boolean complete = false;
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					complete = Boolean.valueOf(cursor.getString(cursor.getColumnIndex("download")));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return complete;
//	}
//	
//	public void updateAreas(String name, String id) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "area_id = '" + id + "' and name = '" + name + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("name", name);
//		values.put("area_id", id);
//		values.put("timestamp", System.currentTimeMillis()/1000);
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_AREAS, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//
//	public void saveCityToDB(String name, String cityId, String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("city_name", name);
//		values.put("city_id", cityId);
//		values.put("area_id", areaId);
//		
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_CITIES, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void updateCities(String name, String cityId, String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "city_id = '" + cityId + "' and city_name = '" + name + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("city_name", name);
//		values.put("city_id", cityId);
//		values.put("area_id", areaId);
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_CITIES, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void saveRoutesToDB(String newName, String routeId, String areaId, String newType, int newSpeed, int count, String shapeId) {
//
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("name", newName);
//		values.put("route_id", routeId);
//		values.put("area_id", areaId);
//		values.put("type", newType);
//		values.put("speed", newSpeed);
//		values.put("count", count);
//		values.put("shape_id", shapeId);
//		
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void updateRoutes(String newName, String routeId, String areaId, String newType, int newSpeed, int count, String shapeId) {
//
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "route_id = '" + routeId + "' and name = '" + newName + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("name", newName);
//		values.put("route_id", routeId);
//		values.put("area_id", areaId);
//		values.put("type", newType);
//		values.put("speed", newSpeed);
//		values.put("count", count);
//		values.put("shape_id", shapeId);
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_ROUTES, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void saveRouteStopsToDB(String newName, String depart, String arrive, String stopId, String areaId,
//			int newLatitude, int newLongitude, String routeName, String routeId) {
//
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("name", newName);
//		values.put("stop_id", stopId);
//		values.put("route_id", routeId);
//		values.put("area_id", areaId);
//		values.put("route_name", routeName);
//		values.put("stop_duration", setStopDuration(depart, arrive));
//		values.put("start_time", convertDateToUnixTime(depart));
////		values.put("start_time", depart);
//		values.put("latitude", newLatitude);
//		values.put("longitude", newLongitude);
//		
////		System.out.println("SAVE routeId = " + routeId + ", areaId = " + areaId + ", stopId = " + stopId + ", name = " + newName);
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public List<String> getListOfNextAvailableStopsForRoute(String routeName) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "route_name = '" + routeName + "' and start_time >? '" + System.currentTimeMillis()/1000 + "'";
//		List<String> stopNames = new ArrayList<String>();
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						stopNames.add(cursor.getString(cursor.getColumnIndex("stop_name")));
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return stopNames;
//	}
//	
//	public long convertDateToUnixTime(String date) {
//		Date newDate = null;
//		try {
//			newDate = new SimpleDateFormat("HH:mm:ss").parse(date);
//		} catch(Exception e) {
//			return 0;
//		}
//		return newDate.getTime();
//	}
//	
//	/*** This calculates any delays at the station ***/
//	public long setStopDuration(String departTime, String arriveTime) {
//		//Arrival time: arrives at current station
//		//Departure time: leaves current station
//		
//		if(departTime == null && arriveTime == null) {
//			return 0;
//		}
//		
//		Date depart = null;
//		Date arrive = null;
//		long difference = 0;
//		try {
//			depart = new SimpleDateFormat("HH:mm:ss").parse(departTime);
//			arrive = new SimpleDateFormat("HH:mm:ss").parse(arriveTime);
//			difference =  depart.getTime() - arrive.getTime();
//		} catch (ParseException e) {
//			e.printStackTrace();
//		}
//		
////			float inMinutes = difference / (60 * 1000);
////		    float inHours = inMinutes / 60;
//		return difference; //in unix time - milliseconds
//	}
//
//	public int getRouteCount(String routeId) {
//		if(routeId == null)
//			return 0;
//		int count = 0;
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "route_id = '" + routeId + "'";
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES,
//					null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					//If route does not exist count will remain 0
//					count = cursor.getInt(cursor.getColumnIndex("count"));
//				}
//				cursor.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return count;
//	}
//	
//	public void increaseRouteCount(String routeId, int count) {
//		if(routeId == null)
//			return; 
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("count", ++count);
//		
//		String whereClause = "route_id = '" + routeId + "'";
//		
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_ROUTES, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public boolean isAreaCurrent(String area_id) {
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + area_id + "'";
//		boolean current = false;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS,
//					null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					Long timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
//					Long currentTime = System.currentTimeMillis()/1000;
//					Long oneMonth = (long) (60*60*24*30*1); //ss * mm * HH * dd * MM * 3
//					if((currentTime - timestamp) < oneMonth) 
//						current = true; //data is not more than 3 months old
//				cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return current;
//	}
//	
//	public boolean doesAreaAlreadyHaveData(String area_id) {
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		boolean data = false;
//		String whereClause = "area_id = '" + area_id + "'";
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES,
//					null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					data = true; //there are routes saved for area
//				cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return data;
//	}
//	
//	public void saveItinerary(String routeId, String arrivalId, String departId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		ContentValues values = new ContentValues();
//		values.put("arrival_id", arrivalId);
//		values.put("depart_id", departId);
//		values.put("route_id", routeId);
//		values.put("itinerary_id", departId + "," + arrivalId);
//
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void updateItinerary(String routeId, String arrivalId, String departId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String itineraryId = departId + "," + arrivalId;
//		String whereClause = "itinerary_id = '" + itineraryId + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("arrival_id", arrivalId);
//		values.put("depart_id", departId);
//		values.put("route_id", routeId);
//		values.put("itinerary_id", itineraryId);
//
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void setAreaUpToDate(String cityId, boolean upToDate) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "city_id = '" + cityId + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("uptodate", upToDate);
//
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_CITIES, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public String[][] getSavedItineraryIds() {
//		
//		String[][] itineraries = null; //[itinerary_id][route_id]
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//	
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, null, null, null, null, null, null);
//			if (cursor != null) {
//				itineraries = new String[cursor.getCount()][2];
//				int index = 0;
//				if (cursor.moveToFirst()) {
//					do {
//						String itineraryId = cursor.getString(cursor.getColumnIndex("itinerary_id"));
//						String routeId = cursor.getString(cursor.getColumnIndex("route_id"));
//						itineraries[index][0] = itineraryId;
//						itineraries[index][1] = routeId;
//						index++;
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		
//		return itineraries;
//	}
//	
//	public String getAreaName(String areaId) {
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String name = null;
//		String whereClause = "area_id = '" + areaId;
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause, null, null, null, null);
//			if (cursor != null) {
//				int index = 0;
//				if (cursor.moveToFirst()) {
//					name = cursor.getString(cursor.getColumnIndex("name"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		
//		return name;
//	}
//	
//	public String[] getAllAreas() {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String[] areas = null;
//	
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, null, null, null, null, null);
//			if (cursor != null) {
//				areas = new String[cursor.getCount()];
//				int index = 0;
//				if (cursor.moveToFirst()) {
//					do {
//						String areaName = cursor.getString(cursor.getColumnIndex("name"));
//						areas[index] = areaName;
//						index++;
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		
//		return areas;
//	}
//	
//	public boolean doesAreaExist(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		boolean exists = true;
//		String whereClause =  "area_id = '" + areaId;
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					exists = false;
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		
//		return exists;
//	}
//	
//	public String[] getAllAreasIds() {
//		
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String[] areas = null;
//	
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, null, null, null, null, null);
//			if (cursor != null) {
//				areas = new String[cursor.getCount()];
//				int index = 0;
//				if (cursor.moveToFirst()) {
//					do {
//						String areaId = cursor.getString(cursor.getColumnIndex("area_id"));
//						areas[index] = areaId;
//						index++;
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		
//		return areas;
//	}
//
//	public String getStopId(String name, String routeId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "route_id = '" + routeId + "' and name ='" + name + "'";
//		String stopId = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					stopId = cursor.getString(cursor.getColumnIndex("stop_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return stopId;
//	}
//	
//	public String getStopName(String stopId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "stop_id = '" + stopId + "'";
//		String name = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					name = cursor.getString(cursor.getColumnIndex("name"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return name;
//	}
//	
//	public long getAreaTimestamp(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		long time = 0;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					time = (int) cursor.getInt(cursor.getColumnIndex("timestamp"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return time;
//	}
//	
//	public String getRouteName(String routeId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "route_id = '" + routeId + "'";
//		String name = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					name = cursor.getString(cursor.getColumnIndex("name"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return name;
//	}
//
//	public String getCityId(String cityName) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "city_name = '" + cityName + "'";
//		String id = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					id = cursor.getString(cursor.getColumnIndex("city_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return id;
//	}
//	
//	public String getAreaIdFromCity(String cityName, String cityId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "city_name = '" + cityName + "' and city_name = '" + cityName + "'";
//		String id = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					id = cursor.getString(cursor.getColumnIndex("area_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return id;
//	}
//	
//	public List<CityInfo> returnAllCitiesAndIds(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		if(areaId == null)
//			whereClause = null;
//		List<CityInfo> cities = new ArrayList<CityInfo>();
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String name = cursor.getString(cursor.getColumnIndex("city_name"));
//						String id = cursor.getString(cursor.getColumnIndex("city_id"));
//						CityInfo cityInfo = new CityInfo(name, id);
//						cities.add(cityInfo);
//					} while (cursor.moveToNext());
//					
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return cities;
//	}
//
//	public String[] returnAllCities() {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		String[] cities = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, null,
//					null, null, null, null);
//			cities = new String[cursor.getCount()];
//			int count = 0;
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String name = cursor.getString(cursor.getColumnIndex("city_name"));
//						cities[count] = name;
//						count++;
//					} while (cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return cities;
//	}
//	
//	public List<CityInfo> returnAllRoutesAndIds(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		if(areaId == null)
//			whereClause = null;
//		List<CityInfo> cities = new ArrayList<CityInfo>();
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String name = cursor.getString(cursor.getColumnIndex("name"));
//						String id = cursor.getString(cursor.getColumnIndex("route_id"));
//						CityInfo cityInfo = new CityInfo(name, id);
//						cities.add(cityInfo);
//					} while (cursor.moveToNext());
//					
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return cities;
//	}
//
//	public String findAreaId(String areaName) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String id = null;
//		String whereClause = "area_name = '" + areaName + "'";
//
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					id = cursor.getString(cursor.getColumnIndex("area_id"));
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
//			//If the doing download behind the scenes don't show toast
//			if (powerManager.isScreenOn())
//				Toast.makeText(context, context.getString(R.string.transitsampling_area_not_exist) + ": " + areaName, Toast.LENGTH_SHORT).show();
//		} finally {
//			sqlDB.close();
//		}
//		return id;
//	}
//	
//	public void saveShape(int latitude, int longitude, int sequence, String shapeId, String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//
//		ContentValues values = new ContentValues();
//		values.put("latitude", latitude);
//		values.put("longitude", longitude);
//		values.put("point_sequence", sequence);
//		values.put("shape_id", shapeId);
//		values.put("area_id", areaId);
//
//		try {
//			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_STOPS, null, values);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public void updateShape(int latitude, int longitude, int sequence, String shapeId, String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		try {
//			sqlDB = dbHandler.getWritableDatabase();
//		} catch (SQLException sqle) {
//			sqle.printStackTrace();
//		}
//		
//		String whereClause = "shape_id = '" + shapeId + "'";
//		
//		ContentValues values = new ContentValues();
//		values.put("latitude", latitude);
//		values.put("longitude", longitude);
//		values.put("point_sequence", sequence);
//		values.put("shape_id", shapeId);
//		values.put("area_id", areaId);
//
//		try {
//			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_STOPS, values, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close(); 
//		}
//	}
//	
//	public ArrayList<TransitShape> getShape(String shapeId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		String whereClause = "shape_id = '" + shapeId + "'";
//		ArrayList<TransitShape> shapes = new ArrayList<TransitShape>();
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STOPS, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						int latitude = cursor.getInt(cursor.getColumnIndex("latitude"));
//						int longitude = cursor.getInt(cursor.getColumnIndex("longitude"));
//						int sequence = cursor.getInt(cursor.getColumnIndex("point_sequence"));
//						TransitShape shape = new TransitShape(latitude, longitude, sequence, shapeId);
//						shapes.add(shape);
//					}
//					while(cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return shapes;
//	}
//	
//	public ArrayList<TransitShape> testShapesExist() {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		ArrayList<TransitShape> shapes = new ArrayList<TransitShape>();
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STOPS, null, null,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						int latitude = cursor.getInt(cursor.getColumnIndex("latitude"));
//						int longitude = cursor.getInt(cursor.getColumnIndex("longitude"));
//						int sequence = cursor.getInt(cursor.getColumnIndex("point_sequence"));
//						String shapeId = cursor.getString(cursor.getColumnIndex("shape_id"));
//						System.out.println("shape id = " + shapeId);
//						TransitShape shape = new TransitShape(latitude, longitude, sequence, shapeId);
//						shapes.add(shape);
//					}
//					while(cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return shapes;
//	}
//	
//	public void testShapesExistInRoutes() {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, null,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String shapeId = cursor.getString(cursor.getColumnIndex("shape_id"));
//						System.out.println("shape id in routes table = " + shapeId);
//					}
//					while(cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public String getShapeId(String routeId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		
//		String whereClause = "route_id = '" + routeId + "'";
//		String shapeId = null;
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					shapeId = cursor.getString(cursor.getColumnIndex("shape_id"));
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return shapeId;
//	}
//	
//	public void cleanStops(String areaId) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_STATIONS, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void cleanRoutes(String areaId) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_ROUTES, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//
//	public void cleanShapes(String areaId) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_STOPS, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void cleanAreas() { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_AREAS, null, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void cleanArea(String areaId) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_AREAS, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void cleanCities(String areaId) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		String whereClause = "area_id = '" + areaId + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_CITIES, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void deleteItinerary(String id) { 
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getWritableDatabase();
//		String whereClause = "itinerary_id = '" + id + "'";
//		try {
//			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, whereClause, null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
//	
//	public void testRoutes(String areaId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "area_id = " + areaId;
//		
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ROUTES, null, whereClause,
//					null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						String name = cursor.getString(cursor.getColumnIndex("name"));
//						System.out.println("area id " + areaId + ", route name = " + name);
//					}
//					while(cursor.moveToNext());
//					cursor.close();
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//	}
}
