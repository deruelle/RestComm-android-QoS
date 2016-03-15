package com.cortxt.app.MMC.ContentProviderOld;


import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.telephony.TelephonyManager;

import com.cortxt.app.MMC.ServicesOld.Location.GpsManagerOld;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.DeviceInfoOld;

/**
 * This is the content provider that will be used by everything in the project
 * that has to do CRUD operations to the SQLite database.
 * @author Abhin
 *
 */
public class ProviderOld {
	/*
	 * ============================================================
	 * Start private variables
	 */

	private static final String TAG = ProviderOld.class.getSimpleName(); 

	private static final String DATABASE_NAME = null; // "mmc.db";
	private static final int DATABASE_VERSION = 9;

	private int phoneType = TelephonyManager.PHONE_TYPE_NONE;
	private DatabaseHelper databaseHelper; 
	private static final UriMatcher uriMatcher;

	/*
	 * End private variables
	 * ============================================================
	 * Start overriden methods
	 */

	public ProviderOld(Context context) {
		phoneType = DeviceInfoOld.getPhoneType(context);
		databaseHelper = new DatabaseHelper(context);
	}
	
	public String getType(Uri uri) {
		UriMatchOld match = UriMatchOld.get(uriMatcher.match(uri));
		switch (match){
		//first check if the match is for a single row
//		case BASE_STATION_CDMA_ID:
//		case BASE_STATION_GSM_ID:
//		case EVENT_COUPLE_ID:
//		case EVENT_ID:
//		case LOCATION_ID:
//		case SIGNAL_STRENGTH_ID:
//			return match.Table.ContentItemType;
//
//			//now check for the multiple row case
//		case BASE_STATIONS_CDMA:
//		case BASE_STATIONS_GSM:
//		case SIGNAL_STRENGTHS:
//		case EVENT_COUPLES:
//		case EVENTS:
//		case LOCATIONS:
//			return match.Table.ContentType;
		
			//first check if the match is for a single row
		case BASE_STATION_ID:
		//case EVENT_COUPLE_ID:
		//case EVENT_ID:
		case LOCATION_ID:
		case SIGNAL_STRENGTH_ID:
			return match.Table.ContentItemType;

			//now check for the multiple row case
		case BASE_STATIONS:
		case SIGNAL_STRENGTHS:
		//case EVENT_COUPLES:
		//case EVENTS:
		case LOCATIONS:
			return match.Table.ContentType; 

			//and for defaulting, throw an exception
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * This method is automatically called when an "insert" is attempted. 
	 * This method first confirms that uri is valid.
	 * Then this method checks whether the ContentValues variables has
	 * all the required values for the insert.
	 * Then this method does the actual insert and if it fails, then it 
	 * throws a SQLException.
	 * @throws SQLException Thrown if the insert fails
	 */
	public void insert(Uri uri, ContentValues initialValues) {
		//validate the uri
		int uriMatchCode = uriMatcher.match(uri);
		if (uriMatchCode == UriMatcher.NO_MATCH){
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null){
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}


		// Make sure that the fields are all set
		UriMatchOld uriMatch = UriMatchOld.get(uriMatchCode);

		try {
			SQLiteDatabase db = databaseHelper.getWritableDatabase();
			long rowId = db.insert(uriMatch.Table.Name, null, values);
			if (rowId > 0){
	//			Uri rowUri = ContentUris.withAppendedId(uriMatch.getContentUri(), rowId);
	//			getContext().getContentResolver().notifyChange(rowUri, null);
	//			return rowUri;
				return;
			}
		}
		catch (Exception e){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "Exception inserting row into ", uri.toString(), e);
		}

		MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "Failed to insert row into ", uri.toString() + " value: " + values.toString());
		//throw new SQLException("Failed to insert row into " + uri);
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		UriMatchOld match = UriMatchOld.get(uriMatcher.match(uri));

		qb.setProjectionMap(null);	//this should let all the columns through TODO change this later if necessary
		qb.setTables(match.Table.Name);
//		switch (match){
//		case EVENT_ID:	
//			qb.appendWhere(TablesOld.Events._ID + "=" + uri.getPathSegments().get(1));
//			break;
//		case LOCATION_ID:
//			qb.appendWhere(TablesOld.Locations._ID + "=" + uri.getPathSegments().get(1));
//			break;
//		case EVENT_COUPLE_ID:
//			qb.appendWhere(TablesOld.EventCouples._ID + "=" + uri.getPathSegments().get(1));
//			break;
//		case SIGNAL_STRENGTH_ID:
//			qb.appendWhere(TablesOld.SignalStrengths._ID + "=" + uri.getPathSegments().get(1));
//			break;
////		case BASE_STATION_CDMA_ID:  // Fixed, this used to say SignalStrengths instead of BaseStations
////			qb.appendWhere(TablesOld.BaseStations.CDMAVersion._ID + "=" + uri.getPathSegments().get(2));
////			break;
////		case BASE_STATION_GSM_ID:
////			qb.appendWhere(TablesOld.BaseStations.GSMVersion._ID + "=" + uri.getPathSegments().get(2));
////			break;
//		case BASE_STATION_ID:
//			qb.appendWhere(TablesOld.BaseStations._ID + "=" + uri.getPathSegments().get(1));
//			break;
//		}

		//TODO implement the fallback default order later

		// Get the database and run the query
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		//Tell the cursor what uri to watch so it knows when its source data changes
//		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

//	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
//		SQLiteDatabase db = databaseHelper.getWritableDatabase();
//		UriMatchOld match = UriMatchOld.get(uriMatcher.match(uri));
//		int count = 0;
////		switch (match){
//////		case EVENT_COUPLE_ID:
////		case SIGNAL_STRENGTH_ID:
//////		case BASE_STATION_GSM_ID:
//////		case BASE_STATION_CDMA_ID:
////		case BASE_STATION_ID:
////		case LOCATION_ID:
//////		case EVENT_ID:
//////			count = updateTableRow(match, uri, db, values, selection, selectionArgs);
////			break;
//////		case EVENT_COUPLES:
////		case SIGNAL_STRENGTHS:
//////		case BASE_STATIONS_GSM:
//////		case BASE_STATIONS_CDMA:
////		case BASE_STATIONS:
////		case LOCATIONS:
//////		case EVENTS:
////			count = db.update(match.Table.Name, values, selection, selectionArgs);
////			break;
////		default:
////			throw new IllegalArgumentException("failed update: Unknown URI " + uri); 
////		}
//		count = db.update(match.Table.Name, values, selection, selectionArgs);
//		//getContext().getContentResolver().notifyChange(uri, null);
//		return count;
//	}
//
	public int delete (Uri uri, String selection, String[] selectionArgs) {
		
		//validate the uri
		int uriMatchCode = uriMatcher.match(uri);
		if (uriMatchCode == UriMatcher.NO_MATCH){
			//throw new IllegalArgumentException("Unknown URI " + uri);
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "Unknown URI", "uri");
			return -1;
		}

		// Make sure that the fields are all set
		UriMatchOld uriMatch = UriMatchOld.get(uriMatchCode);

		try{
			SQLiteDatabase db = databaseHelper.getWritableDatabase();
			int count = db.delete(uriMatch.Table.Name, selection, selectionArgs);
			return count;
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "Exception deleting ",  uri.toString(), e);
		}
		return -1;
	}
	/**
	 * This functions returns the sum of  durations of
	 * all couple events of a given type except EVT_START
	 * @param eventType The event Type
	 * @returns The elapsed time in miliseconds of all events
	 */
//	public float getTotalElapsedTimeForEvent(int eventType) {
//		long elapsedTime = 0;
//		SQLiteDatabase db = databaseHelper.getWritableDatabase();
//
//		//Sql to find all event couples for a the given type
//		String sqlEvents = "select a.startEvent, a.stopEvent from eventCouples as a,events as b" +
//				" where a.startEvent = b._id and a.stopEvent IS NOT NULL" +
//				" and b.eventType = ?";
//
//		//prepare sql for event duration, first parameter is stopEvent second should be startEvent
//		String sqlDuration = "select b.timeStamp - a. timeStamp from " +
//				"(select timeStamp from events where _id = ?) as a, " +
//				"(select timeStamp from events where _id = ?) as b";
//
//		//Create a compile statement that can be reused for all events
//		SQLiteStatement stmtDuration = db.compileStatement(sqlDuration);
//
//		//Query for all events couples of a given type
//		Cursor cur = db.rawQuery(sqlEvents, new String[] {Integer.toString(eventType)});
//		
//		if(cur != null) {
//			try
//			{
//				//Iterate through all event couples
//				for(cur.moveToFirst();!cur.isAfterLast();cur.moveToNext()) {
//					int stopEventId = cur.getInt(cur.getColumnIndex(TablesOld.EventCouples.STOP_EVENT));
//					int startEventId = cur.getInt(cur.getColumnIndex(TablesOld.EventCouples.START_EVENT));
//					stmtDuration.clearBindings();
//					stmtDuration.bindString(1, Integer.toString(startEventId));
//					stmtDuration.bindString(2, Integer.toString(stopEventId));
//	
//					//Execute sql for event duration
//					try {
//						long dur = stmtDuration.simpleQueryForLong();
//						elapsedTime += dur;
//					} catch (SQLiteDoneException e) {
//						//No value was return by the query
//						Log.d(TAG, e.toString());
//					}
//	
//				}
//			}
//			catch (Exception e)
//			{
//				Log.d(TAG, e.toString());
//			}
//			finally
//			{
//				stmtDuration.close();
//				cur.close();
//			}
//		}
//		return elapsedTime;
//	}

	/**
	 * This functions calculates the total monitoring time
	 * of the app until the current time
	 * @returns The total monitoring time in miliseconds
	 */
//	public float getTotalMonitoringTime() {
//		float totalTime = 0;
//
//		//Find the duration of all START,STOP couple events;
//		float partialTime = this.getTotalElapsedTimeForEvent(EventType.EVT_STARTUP.getIntValue());
//
//		// Find the elapsed time from the last START event until now
//		SQLiteDatabase db = databaseHelper.getWritableDatabase();
//		String[] column = new String[] {"MAX(" + TablesOld.Events.TIMESTAMP + ")"};
//		String where = TablesOld.Events.EVENT_TYPE + " = ?";
//		String[] whereArgs = new String[] {Integer.toString(EventType.EVT_STARTUP.getIntValue())};
//
//		Cursor lastStartup = db.query(
//				TablesEnumOld.EVENTS.Name,
//				column,
//				where,
//				whereArgs,
//				null,
//				null,
//				null);
//
//		if(lastStartup != null) {
//			//Calculate the delta until now
//			lastStartup.moveToFirst();
//			long lastTimestamp = lastStartup.getLong(0);
//			long currElapsedTime = System.currentTimeMillis() - lastTimestamp;
//			totalTime = currElapsedTime + partialTime;
//			lastStartup.close();
//		}
//
//		return totalTime;
//	}
		
//	public long getLastCellSighting (MMCCellLocationOld cellLoc, MMCCellLocationOld lastCell)
//	{
//		if (lastCell == cellLoc)
//			return System.currentTimeMillis();
//		
//		String table = "baseStations";
//		String sql = "select timestamp from " + table + " where bsHigh = " + cellLoc.getBSHigh() + " and bsMid = " + cellLoc.getBSMid() + 
//			" and bsLow = " + cellLoc.getBSLow() + " order by timestamp desc limit 1";
////		String sql = getSQL(1, cellLoc, "baseStations"); //, table = "baseStations";
////		if (phoneType == TelephonyManager.PHONE_TYPE_GSM)
////		{
////			sql = getSQL(1, cellLoc, table);
//////			GsmCellLocation gsmCellLocation = (GsmCellLocation) cellLoc.getCellLocation();
//////			//table = table + "/gsm";
//////			//Sql to find last matching base station in last 20 minutes
//////			sql = "select timestamp from " + table +
//////					//" where lac = " + gsmCellLocation.getLac() + " and cellId = " + gsmCellLocation.getCid() + 
//////					" where cellId = " + gsmCellLocation.getCid() + 
//////					" order by timestamp desc limit 1";
////		}
////		else if (phoneType == TelephonyManager.PHONE_TYPE_CDMA)
////		{
////			sql = getSQL(2, cellLoc, table);
//////			CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) cellLoc.getCellLocation();
//////			//table = table + "/cdma";
//////			//Sql to find last atching base staion in last 20 minutes
//////			sql = "select timestamp from " + table + 
//////					" where bsHigh = " + cdmaCellLocation.getSystemId() + " and bsMid = " + cdmaCellLocation.getNetworkId() + " and bsLow = " + cdmaCellLocation.getBaseStationId() + 
//////					" order by timestamp desc limit 1";
////		
////		}
//		
//		// Get the database and run the query
//		SQLiteDatabase db = databaseHelper.getReadableDatabase();
//		Cursor cursor = null;
//		try
//		{
//			cursor = db.rawQuery(sql, null);
//		}
//		catch (Exception e) {
//			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getLastCellSighting", "Exception", e);
//			System.currentTimeMillis();
//		}
//		long lastseen = 0;
//		if (cursor != null)
//		{
//			try 
//			{
//				if (cursor.moveToFirst())
//				{
//					lastseen = cursor.getLong(0);
//					// we have when base station was last seen
//					// now find out when it was seen until, if it changed
//					sql = "select timestamp from " + table + 
//							" where timestamp > " + lastseen + " order by timestamp asc limit 1";
//					Cursor cursor2 = db.rawQuery(sql, null);
//					if (cursor != null && cursor2.moveToFirst())
//						lastseen = cursor2.getLong(0);
//					else // if no base station change occurred since, then it must still be same base station
//						lastseen = System.currentTimeMillis();	
//					cursor2.close();
//				} 
//			}catch (Exception e) {
//				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getLastCellSighting", "Exception", e);
//			}
//			finally
//			{
//				cursor.close ();
//			}
//		}
//		return lastseen;	
//	}
	/*
	 * get distance travelled in last number seconds
	 * 
	 */
	public double getDistanceTravelled(int seconds) {
		double lat = 0.0, lng = 0.0, lat0 = 0.0, lng0 = 0.0, lat1 = 0.0, lng1 = 0.0;
		Long tStart = System.currentTimeMillis() - seconds*1000;
		String startTime = tStart.toString(); //  df.format (new Date(tStart));
		
		//Sql to find all event couples for a the given type
		String sqlLocations = "select latitude, longitude from locations" +
				" where accuracy <= " +  String.valueOf(GpsManagerOld.LOCATION_UPDATE_MIN_TREND_ACCURACY) +
				" and timestamp > " + startTime + " and timestamp < " + System.currentTimeMillis() + " order by timestamp asc";
		// Get the database and run the query
		SQLiteDatabase db = databaseHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery(sqlLocations, null);
		if (cursor != null)
		{
			try 
			{
				int latitudeIndex = cursor.getColumnIndexOrThrow(TablesOld.Locations.LATITUDE);
				int longitudeIndex = cursor.getColumnIndexOrThrow(TablesOld.Locations.LONGITUDE);
				
				if (cursor.moveToFirst())
				{
					// oldest location in n seconds
					lat1 = cursor.getDouble(latitudeIndex);
					lng1 = cursor.getDouble(longitudeIndex);
				
					cursor.moveToLast();
					// newest location in n seconds
					lat0 = cursor.getDouble(latitudeIndex);
					lng0 = cursor.getDouble(longitudeIndex);
	
					double earthRadius = 6371000.0;
					lat = (lat1 - lat0);
					lng = (lng1 - lng0);
					// distance vector in meters
					double dX = lng * (Math.PI * earthRadius*Math.cos(lat1*Math.PI/180.0)) / 180.0;
					double dY = lat * (Math.PI * earthRadius) / 180.0;
					double dis = Math.sqrt(dX*dX+dY*dY);
					// MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getDistanceTravelled", "distance travelled in " + seconds + ": " + dis + " lat0=" + lat0 + ",lat1=" + lat1 + ",lng0=" + lng0 + ",lng1=" + lng1);
					return dis;
				}
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getDistanceTravelled", "Exception", e);
			}
			finally
			{
				cursor.close ();
			}
		}
		return 0.0;
	}

	/**
	 * Deletes records from the temporary db (signals, locations, cells) that are 4 hours old
	 *
	 * @param numDays
	 */
	public void pruneDB() {
		SQLiteDatabase db = databaseHelper.getReadableDatabase();

		try {
			int numRows = -1;
			//delete event couples
			//db.execSQL(sqlDelCouples);

			//delete event tables
			long timeLimit = System.currentTimeMillis() - 4L*3600L*1000L;
			 //numRows = db.delete(TablesEnumOld.EVENTS.Name, TablesOld.Events._ID + " IN (" + sqlDelEvnt +")", null);
			 
			 //numRows = db.delete(TablesEnumOld.SPEED_TEST_RESULTS.Name, TablesOld.SpeedTestResults.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});
				
			 numRows = db.delete(TablesEnumOld.LOCATIONS.Name, TablesOld.Locations.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});
			
			 //numRows = db.delete(TablesEnumOld.SIGNAL_STRENGTHS.Name, TablesOld.SignalStrengths.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});

			 // Also prune signals from more than 6 hours ago
			 // We keep event matched signals so they can be used to populate event signal details for old events
			 numRows = db.delete(TablesEnumOld.SIGNAL_STRENGTHS.Name, TablesOld.SignalStrengths.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});

//			if(phoneType == TelephonyManager.PHONE_TYPE_GSM) {
			 numRows = db.delete(TablesEnumOld.BASE_STATIONS.Name,  TablesOld.BaseStations.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});
//			}else if(phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
//				db.delete(TablesEnumOld.BASE_STATIONS_GENERIC.Name, TablesOld.BaseStations.TIMESTAMP + "< ?", new String[]{String.valueOf(timeLimit)});
//			}

			}catch(SQLiteException e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "pruneDB", "Prunning the db failed", e);
		}

	}
	
	/*
	 * End overriden methods
	 * ====================================================================
	 * Start public methods
	 */


//	private void checkEventCoupleValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private void checkEventValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private void checkSignalStrengthCDMAValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private void checkLocationValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}

//	private void checkBaseStationCDMAValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private void checkBaseStationGSMValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
	
//	private void checkBaseStationCellValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}
//
//	private void checkSignalStrengthGSMValues(ContentValues values) {
//		// TODO Auto-generated method stub
//
//	}


	/*
	 * End private helper methods
	 * ====================================================================
	 * Start private helper classes / objects
	 */

//	private int updateTableRow(UriMatchOld match, Uri uri, SQLiteDatabase db, ContentValues values, String selection, String[] selectionArgs){
//		String rowId = uri.getLastPathSegment();
//		int count = db.update(
//				match.Table.Name,
//				values, 
//				TextUtils.isEmpty(selection)
//				? String.format("%s=%s", BaseColumns._ID, rowId)
//						: String.format("%s=%s AND (%s)", BaseColumns._ID, rowId, selection), 
//						TextUtils.isEmpty(selection) ? new String[]{} : selectionArgs);
//		return count;
//	}
//	
//	private int deleteTableRow(UriMatchOld match, Uri uri, SQLiteDatabase db, String selection, String[] selectionArgs){
//		String rowId = uri.getLastPathSegment();
//		int count = db.delete(
//				match.Table.Name,
//				TextUtils.isEmpty(selection)
//				? String.format(Locale.US,"%s=%s", BaseColumns._ID, rowId)
//						: String.format(Locale.US,"%s=%s AND (%s)", BaseColumns._ID, rowId, selection), 
//						TextUtils.isEmpty(selection) ? new String[]{} : selectionArgs);
//		return count;
//	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context){
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			
			//create the signal strength table
			db.execSQL(String.format(
					"CREATE TABLE %s " +
							"(%s INTEGER PRIMARY KEY, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER, " +
							"%s INTEGER);",

							TablesEnumOld.SIGNAL_STRENGTHS.Name,
							TablesOld.SignalStrengths._ID,
							TablesOld.SignalStrengths.TIMESTAMP,
							TablesOld.SignalStrengths.SIGNAL,
							TablesOld.SignalStrengths.ECI0,
							TablesOld.SignalStrengths.SNR,
							TablesOld.SignalStrengths.BER,
							TablesOld.SignalStrengths.RSCP,
							TablesOld.SignalStrengths.SIGNAL2G,
							TablesOld.SignalStrengths.LTE_SIGNAL,
							TablesOld.SignalStrengths.LTE_RSRP,
							TablesOld.SignalStrengths.LTE_RSRQ,
							TablesOld.SignalStrengths.LTE_SNR,
							TablesOld.SignalStrengths.LTE_CQI,
							TablesOld.SignalStrengths.SIGNALBARS,
							TablesOld.SignalStrengths.ECN0,
							TablesOld.SignalStrengths.WIFISIGNAL,
							TablesOld.SignalStrengths.COVERAGE,
							TablesOld.SignalStrengths.EVENT_ID
					));
			// create the signal strengths table depending on the mode of the phone
//			if (phoneType == TelephonyManager.PHONE_TYPE_CDMA || phoneType == TelephonyManager.PHONE_TYPE_GSM
//					 || phoneType == TelephonyManager.NETWORK_TYPE_LTE){
//				
				//create the base station table
				String strSQL = String.format(
						"CREATE TABLE %s " +
								"(%s INTEGER PRIMARY KEY, " +
								"%s INTEGER, " +
//								"%s INTEGER, " +
								"%s STRING, " +
								"%s INTEGER, " +
								"%s INTEGER, " +
								"%s INTEGER, " +
								"%s INTEGER, " +
								"%s INTEGER, " +
								"%s INTEGER, " +
								"%s INTEGER);",
//
//								TablesEnumOld.BASE_STATIONS_CDMA.Name,
//								TablesOld.BaseStations.CDMAVersion._ID,
//								TablesOld.BaseStations.TIMESTAMP,
//								TablesOld.BaseStations.CDMAVersion.BID,
//								TablesOld.BaseStations.CDMAVersion.LATITUDE,
//								TablesOld.BaseStations.CDMAVersion.LONGITUDE,
//								TablesOld.BaseStations.CDMAVersion.NID,
//								TablesOld.BaseStations.CDMAVersion.SID,
//								TablesOld.BaseStations.EVENT_ID
								
								TablesEnumOld.BASE_STATIONS.Name,
								TablesOld.BaseStations._ID,
								TablesOld.BaseStations.TIMESTAMP,	
//								TablesOld.BaseStations.GenericVersion.LATITUDE,
//								TablesOld.BaseStations.GenericVersion.LONGITUDE,
								TablesOld.BaseStations.NET_TYPE,
								TablesOld.BaseStations.BS_LOW,
								TablesOld.BaseStations.BS_MID,
								TablesOld.BaseStations.BS_HIGH,
								TablesOld.BaseStations.BS_CODE,
								TablesOld.BaseStations.BS_CHAN,
								TablesOld.BaseStations.BS_BAND,
								TablesOld.BaseStations.EVENT_ID
//						
						);
				db.execSQL(strSQL);

//			} else if (phoneType == TelephonyManager.PHONE_TYPE_GSM){
//				//create the base station table
//				db.execSQL(String.format(
//						"CREATE TABLE %s " +
//								"(%s INTEGER PRIMARY KEY, " +
//								"%s INTEGER, " +
//								"%s INTEGER, " +
//								"%s INTEGER, " +
//								"%s INTEGER, " +
//								"%s INTEGER);",
//
////								TablesEnumOld.BASE_STATIONS_GSM.Name,
////								TablesOld.BaseStations.GSMVersion._ID,
////								TablesOld.BaseStations.TIMESTAMP,
////								TablesOld.BaseStations.GSMVersion.CELL_ID,
////								TablesOld.BaseStations.GSMVersion.LAC,
////								TablesOld.BaseStations.GSMVersion.PSC,
////								TablesOld.BaseStations.EVENT_ID
//					
//						));
//			} else {
//				//the phone is either running SIP or it doesn't have a radio
//				MMCLogger.logToFile(MMCLogger.Level.WARNING, TAG, "DatabaseHelper.onCreate", "The phone is either running SIP or it doesn't have a radio; Unable to create signalStrengths and BaseStations tables");
//			}

			//create the locations table
			db.execSQL(String.format(
					"CREATE TABLE %s " +
							"(%s INTEGER PRIMARY KEY, " +
							"%s INTEGER, " +
							"%s REAL, " +
							"%s REAL, " +
							"%s REAL, " +
							"%s REAL, " +
							"%s REAL, " +
							"%s REAL, " +
							"%s INTEGER, " +
							"%s TEXT, " +
							"%s INTEGER);",

							TablesEnumOld.LOCATIONS.Name,
							TablesOld.Locations._ID,
							TablesOld.Locations.TIMESTAMP,
							TablesOld.Locations.ALTITUDE,
							TablesOld.Locations.ACCURACY,
							TablesOld.Locations.BEARING,
							TablesOld.Locations.LATITUDE,
							TablesOld.Locations.LONGITUDE,
							TablesOld.Locations.SPEED,
							TablesOld.Locations.SATELLITES,
							TablesOld.Locations.PROVIDER,
							TablesOld.Locations.EVENT_ID
					));

			//create the events table
//			db.execSQL(String.format(
//					"CREATE TABLE %s " +
//							"(%s INTEGER PRIMARY KEY, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s NUMERIC, " +
//							"%s INTEGER, " +
//							"%s INTEGER);",
//
//							TablesEnumOld.EVENTS.Name,
//							TablesOld.Events._ID,
//							TablesOld.Events.SERVER_SIDE_EVENT_ID,
//							TablesOld.Events.TIMESTAMP,
//							TablesOld.Events.EVENT_TYPE,
//							TablesOld.Events.DURATION,
//							TablesOld.Events.IS_UPLOADED,
//							TablesOld.Events.FLAGS,
//							TablesOld.Events.CAUSE
//					));
//
//			//create the eventCouples table
//			db.execSQL(String.format(
//					"CREATE TABLE %s " +
//							"(%s INTEGER PRIMARY KEY, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s NUMERIC);",
//
//							TablesEnumOld.EVENT_COUPLES.Name,
//							TablesOld.EventCouples._ID,
//							TablesOld.EventCouples.START_EVENT,
//							TablesOld.EventCouples.STOP_EVENT,
//							TablesOld.EventCouples.IS_COMPLETE
//					));

			//create the speed test results table
//			db.execSQL(String.format(
//					"CREATE TABLE %s " +
//							"(%s INTEGER PRIMARY KEY, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER, " +
//							"%s INTEGER); ",
//
//							TablesEnumOld.SPEED_TEST_RESULTS.Name,
//							TablesOld.SpeedTestResults._ID,
//							TablesOld.SpeedTestResults.DOWNLOAD_SPEED,
//							TablesOld.SpeedTestResults.UPLOAD_SPEED,
//							TablesOld.SpeedTestResults.DOWNLOAD_LATENCY,
//							TablesOld.SpeedTestResults.UPLOAD_LATENCY,
//							TablesOld.SpeedTestResults.NETWORK_TYPE,
//							TablesOld.SpeedTestResults.TIMESTAMP
//					));
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			/*
			 * Till the alpha is released, we would just use "drop table" as the upgrade
			 * procedure. After the alpha of course, proper upgrade procedures would have to be used.
			 */
			if (newVersion > oldVersion){
				//drop all the tables if they exist
				for (TablesEnumOld table : TablesEnumOld.values())
					db.execSQL("DROP TABLE IF EXISTS " + table.Name);

				//now call onCreate
				onCreate(db);
			}
		}

	}

	/*
	 * End private helper classes / objects
	 * =======================================================================
	 * Start static area
	 */

	static {
		//Initialise the uri matcher
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		for (UriMatchOld match : UriMatchOld.values()){
			uriMatcher.addURI(TablesOld.AUTHORITY, match.Path, match.Code);
		}
	}

	/*
	 * End static area
	 * =======================================================================
	 */

}
