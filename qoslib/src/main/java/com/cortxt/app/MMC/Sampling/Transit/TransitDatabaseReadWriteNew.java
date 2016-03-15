package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.PowerManager;
import android.widget.Toast;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Sampling.UnusedClasses.TransitShape;
import com.cortxt.com.mmcextension.datamonitor.database.DatabaseHandler;
import com.google.android.maps.GeoPoint;

public class TransitDatabaseReadWriteNew {

	private SQLiteDatabase sqlDB;
	private Context context;
	
	public TransitDatabaseReadWriteNew(Context context) {
		this.context = context;
	}
	
	/*** Save functions ***/
	
	public void saveArea(String name, int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("name", name);
		values.put("area_id", areaId);
		values.put("download_flag", 1);
		values.put("timestamp", (System.currentTimeMillis()/1000));

		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_AREAS, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void saveCity(String cityName, int cityId, int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("city_name", cityName);
		values.put("city_id", cityId);
		values.put("area_id", areaId);

		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_CITIES, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void saveTransport(String shortName, String longName, int transportId, String agencyId, int areaId, int type, int count) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("short_name", shortName);
		values.put("long_name", longName);
		values.put("agency_id", agencyId);
		values.put("transport_id", transportId);
		values.put("area_id", areaId);
		values.put("type", type);
		values.put("count", count);
		
		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void saveStation(int stationId,String name, int areaId, int latitude, int longitude, double distance, 
			int duration, int sequence, int transportId) {

		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("transport_id", transportId);
		values.put("station_id", stationId);
		values.put("stop_sequence", sequence);
		values.put("stop_id", 0);// stopId);
		values.put("name", name);
		values.put("area_id", areaId);
		values.put("latitude", latitude);
		values.put("longitude", longitude);
		values.put("distance", String.valueOf(distance));
		values.put("duration", duration);
		
		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void savePolyline(int latitude, int longitude, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
//		values.put("area_id", areaId);
		values.put("latitude", latitude);
		values.put("longitude", longitude);
		values.put("transport_id", transportId);
		values.put("has_stations", "false");

		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_POLYLINE, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	private boolean hasItenerary (int departId, int arrivalId)
	{
		boolean exists = false;
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "arrival_id = '" + arrivalId + "' and depart_id = '" + departId + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ITINERARIES,
					null, whereClause, null, null, null, null);
			if (cursor != null && cursor.moveToFirst())
				exists = true;
					
			cursor.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return exists;
	}
	public void saveItinerary(int transportId, int departId, int arrivalId) {
		if (hasItenerary(departId, arrivalId) == true)
			return;
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("arrival_id", arrivalId);
		values.put("depart_id", departId);
		values.put("transport_id", transportId);
		values.put("itinerary_id", departId + "," + arrivalId);

		try {
			sqlDB.insert(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void saveIntersect(int stationId, GeoPoint intersect, int transportId) {
		if(intersect == null)
			return;
		
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		
		ContentValues values = new ContentValues();
		values.put("intersect_latitude", intersect.getLatitudeE6());
		values.put("intersect_longitude", intersect.getLongitudeE6());

		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_STATIONS, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void saveStationLine(int stationId, List<GeoPoint> stationLine, int transportId) {
		String line = "";
		double distance = 0;
		GeoPoint point0 = null;
		
		for(int i = 0; i < stationLine.size(); i++) {
			GeoPoint point = stationLine.get(i);
			if (point0 != null)
				distance += TransitSamplingMapMath.distanceTo (point, point0);
			point0 = point;
			line += point.getLatitudeE6() + ":" + point.getLongitudeE6();
			if(i != stationLine.size() -1)
				line += ",";
		}
		
		if(line.equals(""))
			return;
		
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		int i = 0;
		if (stationId == 51160)
		{
			i = 51160;
		}
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";
		
		ContentValues values = new ContentValues();
		values.put("station_line", line);
		values.put("distance", String.valueOf(distance));

		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_STATIONS, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	/*** Retrieval functions ***/
	
	public int getAreaId(String areaName) {
		int areaId = 0;
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "name = '" + areaName + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS,
					null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					areaId = cursor.getInt(cursor.getColumnIndex("area_id"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return areaId;
	}
	
//	public String getStopId(String name, int stationId, int transportId) {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = "station_id = '" + stationId + "' and name = '" + name + "'" + " and transport_id = '" + transportId + "'";
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
	
	public int getAreaIdFromAreaName(String areaName) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int id = 0;
		String whereClause = "name = '" + areaName + "'";

		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					id = cursor.getInt(cursor.getColumnIndex("area_id"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
			//If the doing download behind the scenes don't show toast
			if (powerManager.isScreenOn())
				Toast.makeText(context, context.getString(R.string.transitsampling_area_not_exist) + ": " + areaName, Toast.LENGTH_SHORT).show();
		} finally {
			sqlDB.close();
		}
		return id;
	}
	
	public String[] getCities() {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		
		String[] cities = null;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, null,
					null, null, null, null);
			cities = new String[cursor.getCount()];
			int count = 0;
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						String name = cursor.getString(cursor.getColumnIndex("city_name"));
						cities[count] = name;
						count++;
					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return cities;
	}
	
/*	public List<CityInfo> getCitiesAndIds(int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "area_id = '" + areaId + "'";
		if(areaId == -1 || areaId == 0)
			whereClause = null;
		List<CityInfo> cities = new ArrayList<CityInfo>();
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						String name = cursor.getString(cursor.getColumnIndex("city_name"));
						int id = cursor.getInt(cursor.getColumnIndex("city_id"));
						CityInfo cityInfo = new CityInfo(name, id);
						cities.add(cityInfo);
					} while (cursor.moveToNext());
					
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return cities;
	} */
	
/*	public List<CityInfo> getTransportsAndIds(int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "area_id = '" + areaId + "'";
		if(areaId == -1 || areaId == 0)
			whereClause = null;
		List<CityInfo> cities = new ArrayList<CityInfo>();
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					do {
						String name = cursor.getString(cursor.getColumnIndex("long_name"));
						int id = cursor.getInt(cursor.getColumnIndex("transport_id"));
						CityInfo cityInfo = new CityInfo(name, id);
						cities.add(cityInfo);
					} while (cursor.moveToNext());
					
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return cities;
	} */
	
	public int getTransportCount(int transportId) {
		int count = 0;
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "transport_id = '" + transportId + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT,
					null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					//If transport does not exist count will remain 0
					count = cursor.getInt(cursor.getColumnIndex("count"));
				}
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return count;
	}
	
	public void increaseTransportCount(int transportId, int count) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("count", ++count);
		
		String whereClause = "transport_id = '" + transportId + "'";
		
		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public String getShapeId(int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		
		String whereClause = "transport_id = '" + transportId + "'";
		String shapeId = null;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					shapeId = cursor.getString(cursor.getColumnIndex("shape_id"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return shapeId;
	}
	
	public int[][] getShape(int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		
		String whereClause = "transport_id = '" + transportId + "'";
		int[][] shapes = null;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_POLYLINE, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				shapes = new int[cursor.getCount()][2];
				int i = 0;
				if (cursor.moveToFirst()) {
					do {
						int latitude = cursor.getInt(cursor.getColumnIndex("latitude"));
						int longitude = cursor.getInt(cursor.getColumnIndex("longitude"));
						shapes[i][0] = latitude;
						shapes[i][1] = longitude;
						i++;
					}
					while(cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return shapes;
	}
	
	public String[][] getSavedItineraryIds() {
		String[][] itineraries = null; //[itinerary_id][transport_id]
		
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
	
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, null, null, null, null, null, null);
			if (cursor != null) {
				itineraries = new String[cursor.getCount()][2];
				int index = 0;
				if (cursor.moveToFirst()) {
					do {
						String itineraryId = cursor.getString(cursor.getColumnIndex("itinerary_id"));
						int transportId = cursor.getInt(cursor.getColumnIndex("transport_id"));
						itineraries[index][0] = itineraryId;
						itineraries[index][1] = String.valueOf(transportId);
						index++;
					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		return itineraries;
	}
	
	public List<GeoPoint> getStationLine(int stationId, int transportId) {
		List<GeoPoint> line = new ArrayList<GeoPoint>();
		String lineAsString = null;
		
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "station_id = '" + stationId + "'"  + " and transport_id = '" + transportId + "'";;
	
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					lineAsString = cursor.getString(cursor.getColumnIndex("station_line"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		try {
			if(lineAsString != null) {
				String[] lineAsArray = lineAsString.split(",");
				for(int i = 0; i < lineAsArray.length; i++) {
					String[] point = lineAsArray[i].split(":");
					int lat = Integer.valueOf(point[0]);
					int lon = Integer.valueOf(point[1]);
					GeoPoint geoPoint = new GeoPoint(lat, lon);
					line.add(geoPoint);
				}
			}
		} catch(Exception e) {
			return null;
		}
		
		if(line.size() == 0)
			return null;
		else
			return line;
	}
	
	public String getTransportName(int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "transport_id = '" + transportId + "'";
		String name = null;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					name = cursor.getString(cursor.getColumnIndex("long_name"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return name;
	}
	
	public GeoPoint getStationLocation(int stationId, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		int lat = 0;
		int lon = 0;
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					lat = cursor.getInt(cursor.getColumnIndex("latitude"));
					lon = cursor.getInt(cursor.getColumnIndex("longitude"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		if(lat != 0 && lon != 0)
			return new GeoPoint(lat, lon);
		return null;
	}
	
	public GeoPoint getStationIntersect(int stationId, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
//		String whereClause = ", = '" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		String whereClause = "station_id ='" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		int lat = 0;
		int lon = 0;
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					lat = cursor.getInt(cursor.getColumnIndex("intersect_latitude"));
					lon = cursor.getInt(cursor.getColumnIndex("intersect_longitude"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		if(lat != 0 && lon != 0)
			return new GeoPoint(lat, lon);
		return null;
	}
	
	//public String getStopName(String stopId) {
	public String getStopName(String stationId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "station_id = '" + stationId + "'";
		String name = null;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					name = cursor.getString(cursor.getColumnIndex("name"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return name;
	}
	
	public ArrayList<TransitInfo> getTransportStopsFromDB(int transportId) { 
		
		ArrayList<TransitInfo> transitInfo = new ArrayList<TransitInfo>();
		
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		
	
		String whereClause = "transport_id = '" + transportId + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS,
					null, whereClause, null, null, null, "stop_sequence");
			if (cursor != null) {
				//Going in descending order so > finds the closest next transport time
				//ie: current time = 8:35am
				//stops: 830am 840am 850am -- will pick 840am (closest train that hasn't left already)
				if (cursor.moveToFirst()) {
					do {
						String name = cursor.getString(cursor.getColumnIndex("name")); 
						int latitude = cursor.getInt(cursor.getColumnIndex("latitude"));
						int longitude = cursor.getInt(cursor.getColumnIndex("longitude"));
						int stationId = cursor.getInt(cursor.getColumnIndex("station_id"));

						TransitInfo info = new TransitInfo(name, stationId, new SerializableGeoPoint(latitude, longitude));
						transitInfo.add(info);
					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return transitInfo;
	}
	
	public int getAreaIdFromCity(String cityName, int cityId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "city_name = '" + cityName + "' and city_name = '" + cityName + "'";
		int id = 0;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					id = cursor.getInt(cursor.getColumnIndex("area_id"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return id;
	}
	
	public int getCityId(String cityName) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "city_name = '" + cityName + "'";
		int id = 0;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					id = cursor.getInt(cursor.getColumnIndex("city_id"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return id;
	}
	
	public boolean getHasStations(int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "transport_id = '" + transportId + "'";
		boolean hasStations = false;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_POLYLINE, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					hasStations = Boolean.valueOf(cursor.getString(cursor.getColumnIndex("has_stations")));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return hasStations;
	}
	
	public String[] getAllAreas() {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String[] areas = null;
	
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, null, null, null, null, null);
			if (cursor != null) {
				areas = new String[cursor.getCount()];
				int index = 0;
				if (cursor.moveToFirst()) {
					do {
						String areaName = cursor.getString(cursor.getColumnIndex("name"));
						areas[index] = areaName;
						index++;
					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		return areas;
	}
	
	public int getTransportIdFromAreaIdAndTransportName(int areaId, String transportName) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int transportId = 0;
		String whereClause = "area_id = '" + areaId + "' and long_name = '" + transportName + "'";
	
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
//					do {
					transportId = cursor.getInt(cursor.getColumnIndex("transport_id"));
//					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		return transportId;
	}
	
	public int getTransportIdFromName(String transportName) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int transportId = 0;
		String whereClause = "long_name = '" + transportName + "'";
	
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
//					do {
					transportId = cursor.getInt(cursor.getColumnIndex("transport_id"));
//					} while (cursor.moveToNext());
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		return transportId;
	}
	
/*	public boolean getDownloadFlag(int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "area_id = '" + areaId + "'";
		int complete = 1;
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					complete = cursor.getInt(cursor.getColumnIndex("download_flag"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		if(complete == 0)
			return false;
		//New areas will return need download, as well as incomplete areas
		return true;
	} */
	
	public boolean getDownloadFlag(int transportid) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "transport_id = '" + transportid + "'";
		int complete = 1;
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_POLYLINE, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					complete = cursor.getInt(cursor.getColumnIndex("download_flag"));
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		
		if(complete == 0)
			return false;
		//New areas will return need download, as well as incomplete areas
		return true;
	}
	
	public int getDuration(int stationId, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int duration = 0;
	
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					duration = cursor.getInt(cursor.getColumnIndex("duration"));
				}
			cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return duration;
	}
	
	public double getDistance(int stationId, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		double duration = 0;
	
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					duration = Double.valueOf(cursor.getString(cursor.getColumnIndex("distance")));
				}
			cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return duration;
	}
	
	public int getStopSequence(int stationId, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int stopSequence = 0;
	
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + transportId + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					stopSequence = cursor.getInt(cursor.getColumnIndex("stop_sequence"));
				}
			cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return stopSequence;
	}
	
	public int getStationId(String name, int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		int stationId = 0;
	
		String whereClause = "transport_id = '" + transportId + "' and name = '" + name + "'";
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					stationId = cursor.getInt(cursor.getColumnIndex("station_id"));
				}
			cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return stationId;
	}
	
	/*** Delete functions ***/
	
	public void deleteItinerary(String id) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		String whereClause = "itinerary_id = '" + id + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_ITINERARIES, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void deleteStops(int areaId) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		
		String whereClause = "area_id = '" + areaId + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_STATIONS, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void deleteTransports(int areaId) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		
		String whereClause = "area_id = '" + areaId + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}

	public void deleteShapes(int areaId) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		
		String whereClause = "area_id = '" + areaId + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_POLYLINE, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void deleteAreas() { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_AREAS, null, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void deleteArea(int areaId) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		String whereClause = "area_id = '" + areaId + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_AREAS, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void deleteCities(int areaId) { 
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getWritableDatabase();
		String whereClause = "area_id = '" + areaId + "'";
		try {
			sqlDB.delete(DatabaseHandler.TABLE_TRANSIT_CITIES, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	/*** misc functions ***/
	
	public boolean doesAreaExist(int areaId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		boolean exists = true;
		String whereClause =  "area_id = '" + areaId + "'";
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					exists = false;
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	
		return exists;
	}
	
	public boolean doesCityExist(int cityId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		boolean exists = true;
		String whereClause =  "city_id = '" + cityId + "'";
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_CITIES, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					exists = false;
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	
		return exists;
	}
	
	public boolean doesTransportExist(int transportId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		boolean exists = true;
		String whereClause =  "transport_id = '" + transportId + "'";
		
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT, null, whereClause,
					null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					exists = false;
					cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	
		return exists;
	}
	
	public boolean doesAreaAlreadyHaveData(int area_id) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		boolean data = false;
		String whereClause = "area_id = '" + area_id + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_TRANSPORT,
					null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					data = true; //there are transports saved for area
				cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return data;
	}
	
	public boolean isAreaCurrent(int area_id) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		String whereClause = "area_id = '" + area_id + "'";
		boolean current = false;
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_AREAS,
					null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					Long timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
					Long currentTime = System.currentTimeMillis()/1000;
					Long oneMonth = (long) (60*60*24*30*1); //ss * mm * HH * dd * MM * 3
					if((currentTime - timestamp) < oneMonth) 
						current = true; //data is not more than 3 months old
				cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return current;
	}
	
	public boolean doesStationAlreadyExist(int stationId, String lineId) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		sqlDB = dbHandler.getReadableDatabase();
		boolean data = false;
		String whereClause = "station_id = '" + stationId + "'" + " and transport_id = '" + lineId + "'";
		try {
			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, whereClause, null, null, null, null);
			if (cursor != null) {
				if (cursor.moveToFirst()) {
					data = true;
				cursor.close();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
		return data;
	}
	
//	public int testDuration() {
//		DatabaseHandler dbHandler = new DatabaseHandler(context);
//		sqlDB = dbHandler.getReadableDatabase();
//		int duration = 0;
//	
//		try {
//			Cursor cursor = sqlDB.query(DatabaseHandler.TABLE_TRANSIT_STATIONS, null, null, null, null, null, null);
//			if (cursor != null) {
//				if (cursor.moveToFirst()) {
//					do {
//						duration = cursor.getInt(cursor.getColumnIndex("duration"));
//						//String name = cursor.getString(cursor.getColumnIndex("name"));
//						//String stopid = cursor.getString(cursor.getColumnIndex("stop_id"));
//						//String stationId = cursor.getString(cursor.getColumnIndex("station_id"));
//						//System.out.println(name + ", " + duration + ", " + stopid + ", stationId: " + stationId);
//					} while (cursor.moveToNext());
//				}
//			cursor.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			sqlDB.close();
//		}
//		return duration;
//	}
//	
	/*** Update functions ***/
	
/*	public void updateDownloadFlag(int areaId, boolean download) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		String whereClause = "area_id = '" + areaId + "'";
		
		int flag = 0;
		if(download) {
			flag = 1;
		}
		
		ContentValues values = new ContentValues();
		values.put("download_flag", String.valueOf(flag));

		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_AREAS, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	} */
	
	public void updateDownloadFlag(int transportId, boolean download) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		String whereClause = "transport_id = '" + transportId + "'";
		
		int flag = 0;
		if(download) {
			flag = 1;
		}
		
		ContentValues values = new ContentValues();
		values.put("download_flag", String.valueOf(flag));

		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_POLYLINE, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void updateHasStations(int transportId, boolean hasStations) {
		DatabaseHandler dbHandler = new DatabaseHandler(context);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
		ContentValues values = new ContentValues();
		values.put("has_stations", String.valueOf(hasStations));
		
		String whereClause = "transport_id = '" + transportId + "'";
		
		try {
			sqlDB.update(DatabaseHandler.TABLE_TRANSIT_POLYLINE, values, whereClause, null);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
}
