package com.cortxt.app.MMC.Reporters.LocalStorageReporter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.preference.PreferenceManager;

import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.ReportManager.EventKeys;
import com.cortxt.app.MMC.Reporters.ReportManager.SpeedTestKeys;
import com.cortxt.app.MMC.Reporters.ReportManager.StatsKeys;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventData;

public class LocalStorageReporter {
	private static final String TAG = LocalStorageReporter.class.getSimpleName();
	
	public static final int DATABASE_VERSION = 4;
	public static final String DATABASE_NAME = "mmcdb";
	
	public static final String PREFERENCE_KEY_DAYS_TO_KEEP_ENTRIES = "KEY_SETTINGS_DAYS_TO_KEEP_ENTRIES";
	private static final int MMC_DROPPED_NOTIFICATION = 1001;
	public static final String KEY_TIMESTAMP = "timeStamp";
	public static final String KEY_ID = "_id";
	
	public static class Events {
		public static final String TABLE_NAME = "events";
		public static final String KEY_ID = "_id";
		public static final String KEY_TYPE = "type";
		public static final String KEY_LATITUDE = "latitude";
		public static final String KEY_LONGITUDE = "longitude";
		public static final String KEY_MCC = "mcc";
		public static final String KEY_MNC = "mnc";
		public static final String KEY_TIER = "tier";
		public static final String KEY_CARRIER = "carrier";
		public static final String KEY_OPERATOR_ID = "operatorid";
		public static final String KEY_FROM_NETWORK_TYPE = "fromNetworkType";
		public static final String KEY_TO_NETWORK_TYPE = "toNetworkType";
	}
	
	public static class SpeedTest {
		//public static final String TABLE_NAME = "speedTests";
		public static final String KEY_ID = "_id";
		public static final String KEY_UPLOAD = "upload";
		public static final String KEY_DOWNLOAD = "download";
		public static final String KEY_LATENCY = "latency";
		public static final String KEY_TIER = "tier";
		public static final String KEY_OPERATOR_ID = "operatorid";
		
	}
	
	private Context mContext;
	protected DatabaseHelper mDatabaseHelper;
	protected SQLiteDatabase mDB;
	//protected PruneDBTimer mPruneDBTimer;
	
	public LocalStorageReporter(Context context) {
		mContext = context;
		mDatabaseHelper = new DatabaseHelper(context);
		if (mDatabaseHelper != null)
			mDB = mDatabaseHelper.getWritableDatabase();
		//mPruneDBTimer = new PruneDBTimer();
		//mPruneDBTimer.start();
	}
	
	
	/**
	 * Clean up any used resources and close database
	 */
	public void stop() {
		try{
		//mPruneDBTimer.stop();
		mDatabaseHelper.close();
		} catch (Exception e) {}
		
	}

	public void clearEvents () throws MMCException {
		mDB.delete(Events.TABLE_NAME, null, null);
	}

	
	public int storeEvent(final EventData event) {
		//new Thread() {
		//	@Override
		//	public void run() {
		//		saveEventToDB(event);
		//	}
		//}.start();
		return saveEventToDB(event);
	}
	
	/*
	public void storeSpeedTest(final EventData speedTestEvent) {
		new Thread() {
			@Override
			public void run() {
				//saveSpeedTestToDB(speedTestEvent);
				saveEventToDB(speedTestEvent);
			}
		}.start();
	}
	
	public void reportTroubleSpot(TroubleSpotData troubleSpot) {
		//TODO : implement reportTroubleSpot
	}
	*/
	
	/*
	 * Lookup a persisted event id from a timestamp and event type 
	 */
	public int getEventId (long timestamp, int eventType)
	{
		String selection = KEY_TIMESTAMP + "=? And " + Events.KEY_TYPE + "=?";
		String[] selectionArgs = new String[]{Long.toString(timestamp), Integer.toString(eventType)};
		Cursor cursor = null;
		
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, null, selection, selectionArgs, null, null, null);
		
			if(cursor.moveToFirst()) {
				int idIndex = cursor.getColumnIndex(Events.KEY_ID);
				int id = cursor.getInt(idIndex);
				return id;
			}
			else {
				return 0;
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getEventId", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return 0;
	}
	/*
	 * Lookup a persisted event id from a timestamp and event type 
	 */
	public void deleteEvent (int eventId)
	{
		String whereClause = Events.KEY_ID + "=?";
		String[] whereArgs = new String[]{Integer.toString(eventId)};
		try{
			mDB.delete(Events.TABLE_NAME, whereClause, whereArgs);
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "deleteEvent", "error", e);
		}
	}
	/**
	 * Get events that have happened.
	 * Should be called from worker thread, as it may block.
	 * @param eventTypes event types to retrieve. Passing null retrieve all event types.
	 * @return events that have happened. Keys are defined in {@link LocalStorageReporter.Events}
	 */
	public Cursor getRecentEvents(long timespan) {
		try{
		Cursor eventCursor = mDB.query(
				Events.TABLE_NAME,
				new String[]{ "timeStamp", Events.KEY_TYPE },
				TablesOld.TIMESTAMP_COLUMN_NAME + ">?",
				new String[]{ Long.toString(System.currentTimeMillis() - timespan) },
				null,null,"timeStamp DESC"
			);
			return eventCursor;
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getRecentEvents", "error", e);
			return null;
		}
	}
	/**
	 * Get events that have happened.
	 * Should be called from worker thread, as it may block.
	 * @param eventTypes event types to retrieve. Passing null retrieve all event types.
	 * @return events that have happened. Keys are defined in {@link LocalStorageReporter.Events}
	 */
	public List<HashMap<String, String>> getEvents(HashSet<Integer> eventTypes) {
		List<HashMap<String, String>> events = new ArrayList<HashMap<String, String>>();
		
		String selection = null;
		String[] selectionArgs = null;
		if(eventTypes != null) {
			selection = "type in (";
			for(int i=0; i<eventTypes.size()-1; i++) {
				selection += "?, ";
			}
			selection += "?)";
			
			selectionArgs = new String[eventTypes.size()];
			int i=0;
			for(Integer eventType : eventTypes) {
				selectionArgs[i] = Integer.toString(eventType);
				i++;
			}
		}
		Cursor cursor = null;
		try
		{
			cursor = mDB.query (Events.TABLE_NAME, null, selection, selectionArgs, null, null, "timeStamp DESC");
			
			while(cursor.moveToNext()) {
				HashMap<String, String> event = getEventFromCursor(cursor);
				events.add(event);
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getEvents", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return events;
	}
	
	/**
	 * Gets details of the event with the specified eventId
	 * Should be called from worker thread, as it may block.
	 * @param eventId
	 * @throws IllegalArgumentException if there is no event with the id <code>eventId</code>
	 */
	public HashMap<String, String> getEventDetails(int eventId) {
		String selection = Events.KEY_ID + "=?";
		String[] selectionArgs = new String[]{Integer.toString(eventId)};
		Cursor cursor = null;
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, null, selection, selectionArgs, null, null, null);
		
			if(cursor.moveToFirst()) {
				HashMap<String, String> event = getEventFromCursor(cursor);
				return event;
			}
			else {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getEventDetails", "eventId was not in database");
				return null;
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getEventDetails", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
	
	private HashMap<String, String> getEventFromCursor(Cursor cursor) {
		HashMap<String, String> event = new HashMap<String, String>();
		
		int eventType = cursor.getInt(cursor.getColumnIndex(Events.KEY_TYPE));
		event.put(EventKeys.TYPE, Integer.toString(eventType));
		event.put(EventKeys.ID, Integer.toString(cursor.getInt(cursor.getColumnIndex(Events.KEY_ID))));
		event.put(EventKeys.TIMESTAMP, Long.toString(cursor.getLong(cursor.getColumnIndex(KEY_TIMESTAMP))));
		event.put(EventKeys.RATING, Integer.toString(cursor.getInt(cursor.getColumnIndex(Events.KEY_TIER))));
		
		double latitude = cursor.getDouble(cursor.getColumnIndex(Events.KEY_LATITUDE));
		double longitude = cursor.getDouble(cursor.getColumnIndex(Events.KEY_LONGITUDE));
		
		if(latitude != 0.0 || longitude != 0.0) {
			event.put(EventKeys.LATITUDE, Double.toString(latitude));
			event.put(EventKeys.LONGITUDE, Double.toString(longitude));
		}

		if (eventType == EventType.MAN_SPEEDTEST.getIntValue() || eventType == EventType.LATENCY_TEST.getIntValue() || eventType == EventType.VIDEO_TEST.getIntValue() ||
				eventType == EventType.AUDIO_TEST.getIntValue() || eventType == EventType.YOUTUBE_TEST.getIntValue() || eventType == EventType.SMS_TEST.getIntValue() ||
				eventType == EventType.EVT_VQ_CALL.getIntValue() || eventType == EventType.WEBPAGE_TEST.getIntValue())
		{
			int downloadIndex = cursor.getColumnIndex(SpeedTest.KEY_DOWNLOAD);
			int uploadIndex = cursor.getColumnIndex(SpeedTest.KEY_UPLOAD);
			int latencyIndex = cursor.getColumnIndex(SpeedTest.KEY_LATENCY);
			if (downloadIndex >= 0) {
				event.put(SpeedTestKeys.DOWNLOAD_SPEED, Integer.toString(cursor.getInt(downloadIndex)));
				event.put(SpeedTestKeys.UPLOAD_SPEED, Integer.toString(cursor.getInt(uploadIndex)));
				event.put(SpeedTestKeys.LATENCY, Integer.toString(cursor.getInt(latencyIndex)));
			}
		}
		return event;
	}
	
	/**
	 * Gets a list of results of all speed tests performed between <code>startTime</code> and <code>endTime</code>.
	 * Should be called from worker thread, as it may block.
	 * @param startTime
	 * @param endTime
	 * @return List of Map of average latency, download speed, and upload speed. Keys are defined in {@link LocalStorageReporter.SpeedTest}.
	 */
	public List<HashMap<String, Long>> getSpeedTestResults(int eventtype, long startTime, long endTime) {
		String selection = KEY_TIMESTAMP  + ">?  AND " + KEY_TIMESTAMP + "<? AND " + Events.KEY_TYPE;
		if (eventtype == 24)
			selection += " in (24,51,54)";
		else
			selection += " = " + eventtype;
		String[] selectionArgs = new String[]{Long.toString(startTime), Long.toString(endTime)}; // , Integer.toString(EventType.MAN_SPEEDTEST.getIntValue()) +","+ Integer.toString(EventType.APP_MONITORING.getIntValue()) + ","+ Integer.toString(EventType.LATENCY_TEST.getIntValue())};
		
		List<HashMap<String, Long>> results = null;
		Cursor cursor = null;
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, null, selection, selectionArgs, null, null, "timeStamp DESC");
		
			results = new ArrayList<HashMap<String,Long>>(cursor.getCount());
			
			while(cursor.moveToNext()) {
				HashMap<String, Long> result = new HashMap<String, Long>();
				result.put(SpeedTestKeys.ID, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_ID)));
				result.put(SpeedTestKeys.TIMESTAMP, cursor.getLong(cursor.getColumnIndex(KEY_TIMESTAMP)));
				result.put(SpeedTestKeys.SPEEDTIER, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_TIER)));
				result.put(SpeedTestKeys.LATENCY, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_LATENCY)));
				result.put(SpeedTestKeys.DOWNLOAD_SPEED, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_DOWNLOAD)));
				result.put(SpeedTestKeys.UPLOAD_SPEED, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_UPLOAD)));
				result.put(EventKeys.TYPE, cursor.getLong(cursor.getColumnIndex(Events.KEY_TYPE)));
				
				results.add(result);
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getSpeedTestResults", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		
		return results;
	}
	
	public List<HashMap<String, Long>> getThroughputResults(long startTime, long endTime) {
		String selection = KEY_TIMESTAMP  + ">?  AND " + KEY_TIMESTAMP + "<? AND " + Events.KEY_TYPE + "=?";
		String[] selectionArgs = new String[]{Long.toString(startTime), Long.toString(endTime), Integer.toString(EventType.APP_MONITORING.getIntValue())};
		
		List<HashMap<String, Long>> results = null;
		Cursor cursor = null;
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, null, selection, selectionArgs, null, null, null);
		
			results = new ArrayList<HashMap<String,Long>>(cursor.getCount());
			
			while(cursor.moveToNext()) {
				HashMap<String, Long> result = new HashMap<String, Long>();
				result.put(SpeedTestKeys.ID, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_ID)));
				result.put(SpeedTestKeys.TIMESTAMP, cursor.getLong(cursor.getColumnIndex(KEY_TIMESTAMP)));
				result.put(SpeedTestKeys.SPEEDTIER, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_TIER)));
				result.put(SpeedTestKeys.LATENCY, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_LATENCY)));
				result.put(SpeedTestKeys.DOWNLOAD_SPEED, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_DOWNLOAD)));
				result.put(SpeedTestKeys.UPLOAD_SPEED, cursor.getLong(cursor.getColumnIndex(SpeedTest.KEY_UPLOAD)));
				
				results.add(result);
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getThroughputestResults", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		
		return results;
	}
	
	/**
	 * Gets average of results of speed tests performed between <code>startTime</code> and <code>endTime</code>.
	 * Should be called from worker thread, as it may block.
	 * @param startTime
	 * @param endTime
	 * @return Map of average latency, download speed, and upload speed. Keys are defined in {@link ReportManager.StatsKeys}
	 */
	public HashMap<String, Integer> getSpeedTestAverage(long startTime, long endTime, int speedTier) {
		String[] columns = new String[]{"AVG(" + SpeedTest.KEY_LATENCY + ")",
				"AVG(" + SpeedTest.KEY_DOWNLOAD + ")",
				"AVG(" + SpeedTest.KEY_UPLOAD + ")"};
		
		int speedTier2 = speedTier;
		if (speedTier == 3)
			speedTier2 = 4;
		else if (speedTier == 4)
			speedTier2 = 3;
		if (speedTier == 2)
			speedTier2 = 1;
		if (speedTier == 1)
			speedTier2 = 2;
		
		String selection = KEY_TIMESTAMP  + ">?  AND " + KEY_TIMESTAMP + "<? AND type = " + EventType.MAN_SPEEDTEST.getIntValue() + " AND (" + Events.KEY_TIER + "=? OR "+ Events.KEY_TIER + "=?) AND " + SpeedTest.KEY_DOWNLOAD + " >0 AND " + SpeedTest.KEY_UPLOAD + " >0";
		String[] selectionArgs = new String[]{Long.toString(startTime), Long.toString(endTime), Integer.toString(speedTier), Integer.toString(speedTier2)};
		
		Cursor cursor = null;
		try{
			cursor = mDB.query(Events.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
			
			if(cursor.moveToFirst()) {
				HashMap<String, Integer> result = new HashMap<String, Integer>();
				result.put(StatsKeys.LATENCY_AVERAGE, cursor.getInt(cursor.getColumnIndex(columns[0])));
				result.put(StatsKeys.DOWNLOAD_SPEED_AVERAGE, cursor.getInt(cursor.getColumnIndex(columns[1])));
				result.put(StatsKeys.UPLOAD_SPEED_AVERAGE, cursor.getInt(cursor.getColumnIndex(columns[2])));
				return result;
			}
			else {
				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "getSpeedTestAverage", "cursor was empty");
				throw new IllegalStateException("cursor was empty");
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getSpeedTestAverage", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
	
	/**
	 * Gets number of dropped, failed, and normally ended calls between <code>startTime</code> and <code>endTime</code>.
	 * Should be called from worker thread, as it may block.
	 * @param startTime
	 * @param endTime
	 * @return Map of number of dropped, failed, and normally ended calls. Keys are defined in {@link ReportManager.StatsKeys}
	 */
	public HashMap<String, Integer> getCallStats(long startTime, long endTime) {
		HashMap<String, Integer> result = new HashMap<String, Integer>();

		int normalCalls =  getNumberOfEvents(
				EventType.EVT_DISCONNECT.getIntValue(), startTime, endTime);
		normalCalls +=  getNumberOfEvents(
				EventType.EVT_UNANSWERED.getIntValue(), startTime, endTime);
		result.put(StatsKeys.NORMALLY_ENDED_CALLS, normalCalls);
		
		result.put(StatsKeys.DROPPED_CALLS, getNumberOfEvents(
				EventType.EVT_DROP.getIntValue(), startTime, endTime));
		
		result.put(StatsKeys.FAILED_CALLS, getNumberOfEvents(
				EventType.EVT_CALLFAIL.getIntValue(), startTime, endTime));
		
		return result;
	}
	
	public void updateEventDBField (int evtId, String field, String value)
	{
		ContentValues values = new ContentValues();
		values.put(field, value);
		String whereClause = Events.KEY_ID + "=?";
		String[] whereArgs = new String[]{Integer.toString(evtId)};
		try{
		mDB.update(Events.TABLE_NAME, values, whereClause, whereArgs);
		}catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "updateEventDBField", "error", e);
		}
		//context.getContentResolver().update(uri, values, null, null);
	}
	
	/**
	 * Saves event to events table in local database.
	 * Should be called from worker thread, as it can block.
	 * @param event
	 */
	protected int saveEventToDB(EventData event) {
		ContentValues cv = new ContentValues();
		
		cv.put(Events.KEY_TYPE, event.getEventType());
		cv.put(KEY_TIMESTAMP, event.getlTimestamp()*1000);
		
		cv.put(Events.KEY_LATITUDE, event.getFltEventLat());
		cv.put(Events.KEY_LONGITUDE, event.getFltEventLng());
		
		cv.put(Events.KEY_MCC, event.getMCC());
		cv.put(Events.KEY_MNC, event.getMNC());
		cv.put(Events.KEY_CARRIER, event.getCarrier());
		
		cv.put(Events.KEY_OPERATOR_ID, "");// event.getOperatorId());
		//cv.put(Events.KEY_TIER, event.getTier());
		
		if (event.getEventType() == EventType.MAN_SPEEDTEST.getIntValue() ||
				event.getEventType() == EventType.APP_MONITORING.getIntValue() ||
				event.getEventType() == EventType.LATENCY_TEST.getIntValue() ||
				event.getEventType() == EventType.VIDEO_TEST.getIntValue() ||
				event.getEventType() == EventType.AUDIO_TEST.getIntValue() ||
				event.getEventType() == EventType.WEBPAGE_TEST.getIntValue() ||
				event.getEventType() == EventType.YOUTUBE_TEST.getIntValue()
				)
		{
			if ((event.getFlags() & EventOld.SERVICE_WIMAX) > 0)
				cv.put(Events.KEY_TIER, 11);
			else if ((event.getFlags() & EventOld.SERVICE_WIFI) > 0)
				cv.put(Events.KEY_TIER, 10);
			else
				cv.put(Events.KEY_TIER, event.getTier());
			//SpeedTestEvent speedTestEvent = (SpeedTestEvent)event;
			cv.put(SpeedTest.KEY_LATENCY, event.getLatency());
			cv.put(SpeedTest.KEY_DOWNLOAD, event.getDownloadSpeed()*8);
			if (event.getEventType() == EventType.MAN_SPEEDTEST.getIntValue() ||
					event.getEventType() == EventType.APP_MONITORING.getIntValue())
				cv.put(SpeedTest.KEY_UPLOAD, event.getUploadSpeed()*8);
			else
				cv.put(SpeedTest.KEY_UPLOAD, event.getUploadSpeed());
			
		}
//		else if(event.getEventType() == EventType.APP_MONITORING.getIntValue()) {
//			int up =  event.getUploadSpeed(), down = event.getDownloadSpeed();
//			if(up > 0 && down > 0) {
//				cv.put(Events.KEY_TIER, 0);
//				cv.put(SpeedTest.KEY_LATENCY, 0);
//				cv.put(SpeedTest.KEY_DOWNLOAD, event.getDownloadSpeed()*8);
//				cv.put(SpeedTest.KEY_UPLOAD, event.getUploadSpeed()*8);
//			}
//		}
		else if (event.getEventType() == EventType.EVT_DROP.getIntValue() || event.getEventType() == EventType.EVT_CALLFAIL.getIntValue() ||
			event.getEventType() == EventType.EVT_DISCONNECT.getIntValue() || event.getEventType() == EventType.EVT_UNANSWERED.getIntValue())
			cv.put(Events.KEY_TIER, event.getEventIndex());
		int evtId = 0;
		try
		{
			evtId = (int)mDB.insert(Events.TABLE_NAME, null, cv);
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "saveEventToDB", "error", e);
		}
		/*
		String msg = "mmc detected ";
		String message = "";
    	int icon = 0;
    	String title = mContext.getString(R.string.app_label);
		
		if (event.getEventType() == EventType.EVT_CALLFAIL.getIntValue()) // || event.getType() == CallConnectedEvent.TYPE_CALL_CONNECTED)
		{
			message = mContext.getString(R.string.MMC_Notification_call_failed);
			icon = R.drawable.ic_stat_dropped;
		}
		else if (event.getEventType() == EventType.EVT_DROP.getIntValue())
		{
			message = mContext.getString(R.string.MMC_Notification_call_dropped);
			icon = R.drawable.ic_stat_dropped;
		}
		else 
			return evtId;
		
		if(event.getFltEventLng() != 0.0)
			message += ": " + mContext.getString(R.string.MMC_Notification_view_map);
		else
			message += ": " + mContext.getString(R.string.MMC_Notification_view_event);
		msg += message;
		NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(icon, message, System.currentTimeMillis());
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		Intent notificationIntent = new Intent(mContext, Dashboard.class);
		notificationIntent.putExtra("eventId", evtId);
		notificationIntent.setData((Uri.parse("foobar://"+SystemClock.elapsedRealtime())));
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, MMC_DROPPED_NOTIFICATION + evtId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		notification.setLatestEventInfo(mContext, title, message, pendingIntent);
		notificationManager.notify(MMC_DROPPED_NOTIFICATION, notification);
		long expiry = System.currentTimeMillis() +  60000  * 24 * 60;
		PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong(PreferenceKeys.Monitoring.NOTIFICATION_EXPIRY, expiry).commit();
		*/
		return evtId;
	}
	
	
	
	/**
	 * Saves speed test result in local database.
	 * Should be called from worker thread, as it can block.
	 * @param speedTestEvent
	 */
	/*
	protected void saveSpeedTestToDB(SpeedTestEvent speedTestEvent) {
		ContentValues cv = new ContentValues();
		
		cv.put(KEY_TIMESTAMP, speedTestEvent.getTimeStamp());
		cv.put(SpeedTest.KEY_LATENCY, speedTestEvent.getLatency());
		cv.put(SpeedTest.KEY_DOWNLOAD, speedTestEvent.getDownloadSpeed());
		cv.put(SpeedTest.KEY_UPLOAD, speedTestEvent.getUploadSpeed());
		cv.put(SpeedTest.KEY_TIER, speedTestEvent.getTier());
		
		mDB.insert(Events.TABLE_NAME, null, cv);
	}
	*/
	
	/**
	 * Get number of events of type <code>type</code> that occurred between <code>startTime</code> and <code>endTime</code>
	 * @param type
	 * @param startTime
	 * @param endTime
	 * @return number of events of type <code>type</code> that occurred between <code>startTime</code> and <code>endTime</code>
	 */
	protected int getNumberOfEvents(int type, long startTime, long endTime) {
		String[] columns = new String[]{"COUNT(*)"};
		
		String selection = KEY_TIMESTAMP  + ">?  AND " + KEY_TIMESTAMP + "<? AND " + Events.KEY_TYPE + "=?";
		String[] selectionArgs = new String[]{Long.toString(startTime), Long.toString(endTime), Integer.toString(type)};
		
		Cursor cursor = null;
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, columns, selection, selectionArgs, null, null, null);
		
			if(cursor.moveToFirst()) {
				int numberOfEvents = cursor.getInt(cursor.getColumnIndex(columns[0]));
				return numberOfEvents;
			}
			else {
				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "getCallStats", "cursor was empty");
				//throw new IllegalStateException("cursor was empty");
				return 0;
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getNumberOfEvents", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return 0;
	}
	
	
	public Location getLastKnownLocation ()
	{
		
		String selection = Events.KEY_LATITUDE + " IS NOT NULL AND " + Events.KEY_LATITUDE + " <> 0 ";
		String[] selectionArgs = null;
		Cursor cursor = null;
		try
		{
			cursor = mDB.query(Events.TABLE_NAME, null, selection, selectionArgs, null, null, "_id DESC", "1");
			if(cursor.moveToFirst()) {
				double latitude = cursor.getDouble(cursor.getColumnIndex(Events.KEY_LATITUDE));
				double longitude = cursor.getDouble(cursor.getColumnIndex(Events.KEY_LONGITUDE));
				Location location = new Location ("");
				location.setLatitude(latitude);	
				location.setLongitude(longitude);
				return location;
			}
			else {
				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "getLastKnownLocation", "cursor was empty");
				//throw new IllegalStateException("cursor was empty");
				return null;
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getLastKnownLocation", "error", e);
		}
		finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}
	
	/**
	 * Deletes entries in Events and SpeedTests tables that have a timeStamp less than expiryTime 
	 * @param expiryTime
	 */
	protected void removeOldEntries(final long expiryTime) {
		String whereClause = KEY_TIMESTAMP + "<?";
		String[] whereArgs = new String[]{Long.toString(expiryTime)};
		
		mDB.delete(Events.TABLE_NAME, whereClause, whereArgs);
		//mDB.delete(SpeedTest.TABLE_NAME, whereClause, whereArgs);
	}

	public void pruneDB ()
	{
		String daysToKeepEntriesPreference = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PREFERENCE_KEY_DAYS_TO_KEEP_ENTRIES, "30");
		int daysToKeepEntries = 30;
		try
		{
			daysToKeepEntries = Integer.parseInt(daysToKeepEntriesPreference);
			removeOldEntries(System.currentTimeMillis() - daysToKeepEntries * AlarmManager.INTERVAL_DAY);
		} catch (Exception e) {}  // probably an int parse error
	}
	
	private class DatabaseHelper extends SQLiteOpenHelper {
		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + Events.TABLE_NAME +
					" (" + KEY_ID + " integer primary key autoincrement, " +
					Events.KEY_TYPE + " integer not null," +
					KEY_TIMESTAMP + " integer not null," +
					Events.KEY_LATITUDE + " real, " +
					Events.KEY_LONGITUDE + " real," +
					Events.KEY_MCC + " integer," +
					Events.KEY_MNC + " integer," +
					Events.KEY_CARRIER + " text," +
					Events.KEY_OPERATOR_ID + " text," +
					Events.KEY_TIER + " integer," +
					SpeedTest.KEY_LATENCY + " integer," +
					SpeedTest.KEY_DOWNLOAD + " integer," +
					SpeedTest.KEY_UPLOAD + " integer," +
					Events.KEY_FROM_NETWORK_TYPE + " integer," +
					Events.KEY_TO_NETWORK_TYPE + " integer);");
			/*
			db.execSQL("CREATE TABLE " + SpeedTest.TABLE_NAME +
					" (" + KEY_ID + " integer primary key autoincrement, " +
					KEY_TIMESTAMP + " integer not null," +
					SpeedTest.KEY_LATENCY + " integer not null," +
					SpeedTest.KEY_DOWNLOAD + " integer not null," +
					SpeedTest.KEY_UPLOAD + " integer not null," + 
					SpeedTest.KEY_OPERATOR_ID + " integer not null," + 
					Events.KEY_LATITUDE + " real, " +
					Events.KEY_LONGITUDE + " real," +
					SpeedTest.KEY_TIER + " integer not null);");
					*/
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion == 3)
				remapEventTypes ( db);
			else
			{
				db.execSQL("DROP TABLE IF EXISTS " + Events.TABLE_NAME);
				//db.execSQL("DROP TABLE IF EXISTS " + SpeedTest.TABLE_NAME);
				onCreate(db);
			}
		}
		
		public void remapEventTypes (SQLiteDatabase db)
		{
			db.execSQL("DELETE FROM " + Events.TABLE_NAME + " WHERE type=5 or type=9");  // delete data change events
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=17 WHERE type=6");  // change service outages
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=16 WHERE type=7");  // change service outages
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=24 WHERE type=8");  // change speed test
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=6 WHERE type=1");  // change failed calls
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=1 WHERE type=4");  // change disconnect calls
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=4 WHERE type=2");  // change dropped calls
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=5 WHERE type=3");  // change connect calls
			db.execSQL("UPDATE " + Events.TABLE_NAME + " SET type=18 WHERE type=10");  // change update events
			
		}

	}
	
	/**
	 * A BroadcastReceiver that removes old entries from the database.
	 * @author nasrullah
	 *
	 */
	/*
	class PruneDBTimer extends BroadcastReceiver {

		private static final long FIRST_DELAY = 0;
		private static final long REPEAT_INTERVAL = AlarmManager.INTERVAL_DAY;
		private static final String ACTION_PRUNE_DB = "com.cortxt.app.MMC.intent.ACTION_PRUNE_DB";
		
		public void start() {
			try
			{
				IntentFilter intentFilter = new IntentFilter(ACTION_PRUNE_DB);
				mContext.registerReceiver(this, intentFilter);
				
				AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
				
				Intent intent = new Intent(ACTION_PRUNE_DB);
				PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
				alarmManager.setInexactRepeating(AlarmManager.RTC,
						System.currentTimeMillis() + FIRST_DELAY, REPEAT_INTERVAL, pendingIntent);
			}catch (Exception e)
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "PruneDBTimer", "start:Exception", e);	
			}
		}
		
		public void stop() {
			mContext.unregisterReceiver(this);
			
			AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(ACTION_PRUNE_DB);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
			alarmManager.cancel(pendingIntent);
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ACTION_PRUNE_DB)) {
				new Thread() {
					@Override
					public void run() {
						String daysToKeepEntriesPreference = PreferenceManager.getDefaultSharedPreferences(mContext).getString(PREFERENCE_KEY_DAYS_TO_KEEP_ENTRIES, "30");
						int daysToKeepEntries = 30;
						try
						{
							daysToKeepEntries = Integer.parseInt(daysToKeepEntriesPreference);
							removeOldEntries(System.currentTimeMillis() - daysToKeepEntries * AlarmManager.INTERVAL_DAY);
						} catch (Exception e) {}  // probably an int parse error
					}
				}.start();
			}
			
		}
		
	}
	*/
}
