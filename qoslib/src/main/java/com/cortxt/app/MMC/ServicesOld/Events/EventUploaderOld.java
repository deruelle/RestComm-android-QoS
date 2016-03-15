package com.cortxt.app.MMC.ServicesOld.Events;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.telephony.ServiceState;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.ReportManager.EventKeys;
import com.cortxt.app.MMC.Reporters.LocalStorageReporter.LocalStorageReporter.Events;
import com.cortxt.app.MMC.ServicesOld.Buffers.MMCSignalOld;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.DeviceInfoOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.app.MMC.UtilsOld.TrendStringGenerator;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventData;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventDataEnvelope;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventResponse;
import com.cortxt.com.mmcextension.datamonitor.database.DataMonitorDBReader;
import com.cortxt.com.mmcextension.datamonitor.database.DataMonitorDBWriter;
import com.google.gson.Gson;

/**
 * This class can be used to upload an event to the server. Everything in this
 * class happens in a separate thread.
 * @author Brad
 *
 */
public class EventUploaderOld implements Runnable{
	/**
	 * This is the maximum time (in milliseconds) after an event occurs that the location
	 * received would be considered valid for the event.
	 * This means than for an event that occurs at timestamp T (in milliseconds) would use as
	 * its location the first location received in the interval (T, T+{@value #EVENT_LOCATION_MAXIMUM_TIME_OFFSET}]
	 * (in milliseconds).
	 */
	public static final int EVENT_LOCATION_MAXIMUM_TIME_OFFSET = 180000;	//in milliseconds
	
	/**
	 * This is the maximum time (in milliseconds) before or after an event occurs that the base station 
	 * received would be considered valid for the event.
	 * This means than for an event that occurs at timestamp T (in milliseconds) would use as
	 * its base station the closest location received (temporally) in the interval 
	 * (T-{@value #EVENT_LOCATION_MAXIMUM_TIME_OFFSET}, T+{@value #EVENT_LOCATION_MAXIMUM_TIME_OFFSET}] (in milliseconds).
	 */
	public static final int EVENT_BASE_STATION_MAXIMUM_TIME_OFFSET = 180000;	//in milliseconds
	
	private static final String TAG = EventUploaderOld.class.getSimpleName();
	
	public EventOld event = null; // so that scheduler can check if event = null
	private EventOld complimentaryEvent = null;
	private List<EventCoupleOld> ongoingEventCouples = null;
	public MMCService owner = null;
	private EventResponse eventResponse = null;
	private boolean bReportLinux = false;
	
	private TrendStringGenerator trendStringGenerator = new TrendStringGenerator ();

	/**
	 * Constructor
	 */
	public EventUploaderOld(EventOld _event, EventOld _complimentaryEvent, MMCService context, boolean bLocal){
		this.ongoingEventCouples = context.getEventManager().getOngoingEventCouples();
		this.event = _event;
		this.complimentaryEvent = _complimentaryEvent; // hold reference to this event until event is sent
		this.owner = context;
		
		// decide whether to report event yet, set event = null to postpone
		
		if (bLocal)  // not being sent to server yet, don't postpone
			return;
		// IF TRYING TO SEND FIRST PART OF COUPLE, POSTPONE UNTIL 2ND PART OF COUPLE IS READY
		/*
		 * If the complimentary event is null or its uri is null, then this event is either a 
		 * singleton or it is the starting end of an event couple. In the later case,
		 * the event should not be uploaded to the server just yet.
		 */
		//iterate over the ongoing event couples if the complimentaryEvent is null or its uri is null
		if (_complimentaryEvent == null)
		{
			if (ongoingEventCouples != null && _event != null)
			{
				for (EventCoupleOld eventCouple : ongoingEventCouples){
					//check if this event is the "starting end" of an event couple
					//if so, then this is not the right time to be uploading this event
					if (eventCouple.getStartEventType() == _event.getEventType()){
						//this is not the right time to be uploading this event
						event = null;
						return;
					}
				}	
			}
		}
		// NO start-of-couple is going to get uploaded by itself
		// (should have been caught above, except can happen if end-of-couple is 'on stage' and was removed from ongoing
		if (_event != null && _event.getEventType().getGenre() == EventTypeGenreOld.START_OF_COUPLE)
		{
			event = null;
			return;
		}
		
		// Dont upload a Recorded Phone call until file is ready
		//if (_event != null && _event.getEventType().getGenre() == EventTypeGenreOld.END_OF_COUPLE && _complimentaryEvent != null && (_complimentaryEvent.getFlags() & EventOld.CALL_RECORDING) > 0 && _complimentaryEvent.getCause() == "")
		//	event = null;
	}
	
	@Override
	public void run () {	
		report (false, null); // report not local (to internet)
	}
	
	public void report (boolean local, Location location) {	
		//now create a list of eventData variables
		List<EventData> eventDataList = new ArrayList<EventData>();
		String strPhone = "";
		long duration = 0;
		
	    
		EventDataEnvelope eventDataEnvelope = null;
		if (event != null)
		{
			
			//if (!local)
			//	event.setFlag (EventOld.SERVER_SENDING, true);
			//if (event.getEventType() != EventType.EVT_DISCONNECT) // don't send actual event on the end of a normal call 
			
			EventData eventData = generateEventDataFromEvent(event, local);
			if (eventData == null)
				return;
			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "reporting event type=" + event.getEventType() + ",lat:" + eventData.getFltEventLat() + ",local=" + local);
			if ( eventData.getFltEventLat() == 0 && location != null)
			{
				eventData.setFltEventLat((float)location.getLatitude());
				eventData.setFltEventLng((float)location.getLongitude());
				eventData.setiUncertainty((int)location.getAccuracy());
			}
			eventData.setCallID(event.getLocalID());
			EventData eventData2 = null;
			eventDataList.add(eventData);
			
			if(event.getEventType() == EventType.APP_MONITORING && event.getDownloadSpeed() > 0 && event.getUploadSpeed() > 0) {
				//App throughput was getting stored twice
				boolean stored = PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.THROUGHPUT_STORED, false);
				if(!stored) {
					owner.getReportManager().storeEvent(eventData);
					PreferenceManager.getDefaultSharedPreferences(owner).edit().putBoolean(PreferenceKeys.Miscellaneous.THROUGHPUT_STORED, true).commit();
				}
				else
					PreferenceManager.getDefaultSharedPreferences(owner).edit().putBoolean(PreferenceKeys.Miscellaneous.THROUGHPUT_STORED, false).commit();;
			}
			if (event.getEventType() == EventType.MAN_PLOTTING) {
				eventData.setLookupid1(event.getBuildingID());
				eventData.setRunningApps(event.getAppData());  // contains user's polyline
			}
			
			// Event is 'reported' locally before GPS sampling is complete
			// to make it show up on the map as soon as it gets a first fix
			//if (local == true && ((event.getEventType() != EventType.MAN_SPEEDTEST && event.getEventType() != EventType.LATENCY_TEST && event.getEventType() != EventType.APP_MONITORING) || event.latency != 0))
			if (local == true && (event.getEventType().waitsForSpeed() == false || event.latency != 0))
				//	(local == false && event.getEventType() == EventType.MAN_SPEEDTEST))  // but for speedtest, wait until complete
			{
				if (event.getLocalID() > 0 && eventData.getFltEventLng() != 0)
				{
					owner.getReportManager().updateEventField (event.getLocalID(), Events.KEY_LATITUDE, String.valueOf(eventData.getFltEventLat()));
					owner.getReportManager().updateEventField (event.getLocalID(), Events.KEY_LONGITUDE, String.valueOf(eventData.getFltEventLng()));
				}
				else if (event.getLocalID() == 0 )
				{
					int evtID = owner.getReportManager().storeEvent(eventData); // .reportEvent (eventData, event, local, location);
					event.setLocalID (evtID);
					eventData.setCallID(evtID);
				}
				if (complimentaryEvent != null)
				{
					if (complimentaryEvent.getLocalID() > 0 && location != null)
					{
						owner.getReportManager().updateEventField (complimentaryEvent.getLocalID(), Events.KEY_LATITUDE, String.valueOf(location.getLatitude()));
						owner.getReportManager().updateEventField (complimentaryEvent.getLocalID(), Events.KEY_LONGITUDE, String.valueOf(location.getLongitude()));
					}
					else if (complimentaryEvent.getLocalID() == 0)
					{
						int evtID = owner.getReportManager().storeEvent(eventData); //(eventData2, complimentaryEvent, local, location);
						complimentaryEvent.setLocalID (evtID);
					}
				}
			}
			if (local)
				return;
			
			//if the complimentary event is not null, then this event must be 
			//the "starting end" of an event couple. If so, then this event should
			//be uploaded alongside the main event
			if (complimentaryEvent != null && local == false)
			{
				eventData2 = generateEventDataFromEvent(complimentaryEvent, local);
				if (eventData2 != null)
				{
					//complimentaryEvent.setFlag (EventOld.SERVER_SENDING, true);
					eventData2.setCallID(complimentaryEvent.getLocalID());
					eventDataList.add(eventData2);
				}
			}
			/*
			if (event.getEventType() == EventType.COV_4G_YES && complimentaryEvent != null && (complimentaryEvent.getFlags() & Event.PHONE_INUSE) > 0)
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "cancelled LTE outage because it occurred because of phone call");
				event.setFlag(Event.SERVER_CANCELLED, true);
				event.setDuration(0);
				if (complimentaryEvent != null)
				{
					complimentaryEvent.setFlag(Event.SERVER_CANCELLED, true);
					complimentaryEvent.setDuration(0);
				}
			}
			*/
			//now create the eventDataEnvelope to wrap the list of eventData variables
			//along with other required variables
			String phoneNumFirst4 = "";
			if (strPhone != null && strPhone.length() > 4)
				phoneNumFirst4 = strPhone.substring(0,4);
			
			
			eventDataEnvelope = generateEventDataEnvFromEventList(eventDataList, phoneNumFirst4);
			// when event is filled in, travel and fillin would like to see it before sending
			if (owner.isServiceRunning() && owner.getTravelDetector() != null)
				owner.getTravelDetector().eventCompleted (event);
		}
		else  // null event create dummy event envelope to trigger sending the event queue (without adding a new event)
			eventDataEnvelope = new EventDataEnvelope ();
		
		boolean bSent = false,bFromQueue =false; 
		loadEventsQueue();  // only loads if queue hasn't loaded yet (ensure loaded)
		ConcurrentLinkedQueue<EventDataEnvelope> eventQueue = owner.getEventManager().getEventQueue();
		
		// Send this event and any other events that were in the queue, as long as it didn't fail to send
 		while (eventDataEnvelope != null)
		{
			bSent = uploadEventEnvelope (eventDataEnvelope, bFromQueue);  // as long as event was sent to server, it sent (even if server had an error)
			if (!bSent)
			{	
				//if (!bFromQueue)
				{
					eventQueue.add (eventDataEnvelope);
					while (eventQueue.size() > 200)
						eventQueue.poll();
				}
				break;
			}
			else
			{
				eventDataEnvelope = eventQueue.poll();
				bFromQueue = true;
			}
		} 
		// persist the queue every 3 hrs in case something happens
 		if (event != null && (event.isCheckin)) // || event.getEventType() == EventType.EVT_SHUTDOWN ))
 			saveEvents(eventQueue);
	}
	
	/*
	 * Uploads the EVentEnvelope as a JSON packet using wsManager
	 * and then updates that event in the database with the result of sending
	 * return bSent = true if it reaches the server
	 */
	private boolean uploadEventEnvelope (EventDataEnvelope eventDataEnvelope, boolean queued)
	{
		//now upload the eventDataEnvelope to the server
		eventResponse = null;
		int eventTypeId = 0;
		int eventId = 0;
		EventType eventType = null;
		EventData eventData = null;
		int eventFlags = 0;
		boolean bForceSend = false;

		// if this is just a dummy event, pretend it was sent so it goes on to the next queued event
		if (eventDataEnvelope.getlStartTime() == 0l)
			return true;
		Iterator<EventData> iterator = null;
		try
		{
			iterator = eventDataEnvelope.getoEventData().iterator();
			// Last chance to update fields on an event before its about to be sent to server
			// read the event from the disk-based SQLite events database to see if the event type changed due to dropped call confirmation
			for (;iterator.hasNext();)
			{
				eventData = iterator.next ();
				eventTypeId = eventData.getEventType();
				eventType = EventType.get(eventTypeId);
				eventId = (int)eventData.getCallID();  // local SQLite database id of the event
				eventFlags = eventData.getFlags();
				if (eventId > 0 && (eventTypeId == 4 || eventTypeId == 6 || eventTypeId == 1 || eventTypeId == 7))
				{
					HashMap<String, String> event = ReportManager.getInstance(owner).getEventDetails(eventId);
					if (event != null && event.containsKey(EventKeys.RATING))
					{
						int newType = Integer.parseInt(event.get(EventKeys.TYPE));
						int rating = Integer.parseInt(event.get(EventKeys.RATING));
						eventData.setEventType(newType);
						eventData.setEventIndex(rating);
					}
				}
//				if (eventType == EventType.EVT_CONNECT && (eventFlags & EventOld.CALL_RECORDING) > 0)
//				{
//					bForceSend = true;
//					uploadRecording = eventData.getCause();
//					samplerate = eventData.getSampleInterval();
//					// Change the Call eventType to a VQ Call eventType
//					eventData.setEventType(EventType.EVT_VQ_CALL.getIntValue());
//				}
//				else 
				if (eventType == EventType.MAN_SPEEDTEST || eventType == EventType.EVT_STARTUP || eventType == EventType.EVT_SHUTDOWN)
					bForceSend = true;
				// Force send TroubleTweets
				else if (eventType.getIntValue() >= 30 && eventType.getIntValue() <= 39)
					bForceSend = true;
                else if (eventType == EventType.EVT_VQ_CALL || eventType == EventType.VIDEO_TEST || eventType == EventType.AUDIO_TEST  || eventType == EventType.WEBPAGE_TEST)
                    bForceSend = true;
			}
			
//			iterator = eventDataEnvelope.getoEventData().iterator();
//			if (eventDataEnvelope.getoEventData() != null && iterator.hasNext())
//			{
//				eventData = iterator.next();
//				eventTypeId = eventData.getEventType();
//				eventType = EventType.get(eventTypeId);
//				eventFlags = eventData.getFlags();
//				eventId = (int)eventData.getCallID();  // local SQLite database id of the event
//				eventUri = Uri.parse("content://" + TablesOld.AUTHORITY + "/" + TablesEnumOld.EVENTS.RelativeUri + "/" + eventId);
//			}
//			else
//				return true;
			
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "uploadEventEnvelope", "exception reading the event type", e);
			return true;
		}
		
		boolean bSent = false;
		boolean allowRoaming = PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.DATA_ROAMING, false);
		boolean defaultWifi = false;
		String noConfirm = (owner.getResources().getString(R.string.WIFI_DEFAULT));
    	if (noConfirm.equals("1"))
    		defaultWifi = true;  // don't even allow confirmation buttons on drilled down event
    	
		boolean sendOnWifi = PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.SEND_ON_WIFI, defaultWifi);
		
		
 		int resultflag = EventOld.SERVER_ERROR;
 		if (queued && (eventType == EventType.TRAVEL_CHECK || eventType == EventType.EVT_STARTUP))
 		{
 			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "not send queued event of type=" + eventType + " id=" + eventId);
 			return true;
 		}
		if (!owner.isOnline() || owner.getPhoneStateListener().isCallConnected()) // if no connectivity, send event be sent later?
		{
			resultflag = EventOld.SERVER_NODATA;
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "offline, unable to send event type=" + eventType + " id=" + eventId);
		}
		else if (!owner.isServiceRunning() && eventType != EventType.EVT_SHUTDOWN)
		{
			resultflag = EventOld.SERVER_NODATA;
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "service was stopped, queueing event " + eventType + " id=" + eventId);
		
		}
		else if (!allowRoaming && owner.getPhoneStateListener().isRoaming() && !owner.isNetworkWifi() && !bForceSend)
		{
			resultflag = EventOld.SERVER_NODATA;
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "roaming no-wifi, unable to send event type=" + eventType + " id=" + eventId);
		}
		else if (sendOnWifi && !owner.isNetworkWifi() && !bForceSend)
		{
			resultflag = EventOld.SERVER_NODATA;
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "queuing no-wifi, unable to send event type=" + eventType + " id=" + eventId);
		}
		else
		{
			try {
//				if ((eventFlags & EventOld.SERVER_CANCELLED) > 0)
//				{
//					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "event cancelled =" + eventType + " id=" + eventId);
//					bSent = true; // event was cancelled, but mark as sent so it doesnt go back in queue
//				}
//				else
				{
					int allowSpeedTest = 0;
					if (!owner.isNetworkWifi() && !queued)
						allowSpeedTest = owner.getUsageLimits().allowSpeedTest(100000);
					eventDataEnvelope.setAllowSpeedTest (allowSpeedTest);
					//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "uploading staged event type=" + eventType + " id=" + eventId);
					eventResponse = owner.getReportManager().submitEvent(eventDataEnvelope);
					resultflag = EventOld.SERVER_SENT;
					bSent = true;
                    MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "uploaded staged event type=" + eventType + " id=" + eventId);
				}
			} catch (ClientProtocolException cpe){
				//resultflag = EventOld.SERVER_FAILED;
				bSent = true; // event was cancelled, but mark as sent so it doesnt go back in queue
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "uploadEventEnvelope", "cpe exception uploading event type=" + eventType + " id=" + eventId, cpe);
			} catch (IOException ioe){
				bSent = false;
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "uploadEventEnvelope", "io exception uploading event type=" + eventType + " id=" + eventId, ioe);
			} catch (Exception e){
				bSent = true;
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "uploadEventEnvelope", "exception uploading event type=" + eventType + " id=" + eventId, e);
			}
			// also try reporting the event to Linux
			/*
			if (bReportLinux)
			{
				try {
					if ((eventFlags & EventOld.SERVER_CANCELLED) > 0)
					{
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "event cancelled =" + eventType + " id=" + eventId);
						bSent = true; // event was cancelled, but mark as sent so it doesnt go back in queue
					}
					else
					{
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "uploading staged event type=" + eventType + " id=" + eventId);
						boolean res = owner.reportEvent (eventData, event, false, null);
						resultflag = EventOld.SERVER_SENT;
						bSent = true;
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "run", "uploaded staged event type=" + eventType + " id=" + eventId);
					}
				} catch (Exception e){
					bSent = false;
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "run", "error uploading event type=" + eventType + " id=" + eventId, e);
				}
			}
			*/
		}
		try
		{
			eventDataEnvelope.getoEventData();
			eventData.setFlags (eventData.getFlags() | resultflag);
			
			// Disabled block because it updates the in-memory database to say that event is sent
			// no-point in doing that because its temporary and doesnt get read back
			
			// handle event response
			if (eventResponse != null && !eventResponse.isNull())
			{
                eventResponse.handleEventResponse (owner, false);

//				if (uploadRecording != null)
//				{
//					long[] eventids = eventResponse.getEventIds();
//					long eventid = 0;
//					if (eventids != null)
//						eventid = eventids[eventids.length-1];
//					owner.getVQManager().uploadRecordedFile (uploadRecording, eventid, samplerate);
//				}
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "run", "error updating sent event", e);
		}
		return bSent;
	}
	
	
	/**
	 * Loads event requests from storage, and adds it to the queue 
	 */
	protected void loadEventsQueue(){
		
		ConcurrentLinkedQueue<EventDataEnvelope> eventQueue = owner.getEventManager().getEventQueue();
		if (eventQueue == null)
		{
			eventQueue = new ConcurrentLinkedQueue<EventDataEnvelope>();
			owner.getEventManager().setEventQueue(eventQueue);
		} else
			return;
	
		Gson gson = new Gson();
        SharedPreferences secureSettings = MMCService.getSecurePreferences(owner);
		if (secureSettings.contains(PreferenceKeys.Miscellaneous.EVENTS_QUEUE)){
			try {
				String strQueue = secureSettings.getString(PreferenceKeys.Miscellaneous.EVENTS_QUEUE, "");
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "loadQueue", strQueue); 
				if (strQueue.length() < 100)
					return;
				JSONArray jsonqueue = new JSONArray(strQueue);
				for(int i = 0; i<jsonqueue.length(); i++){
					JSONObject jsonRequest = jsonqueue.getJSONObject(i); 
					//if(jsonRequest.getString("type").equals(EventDataEnvelope.TAG)) 
					{
						EventDataEnvelope request = gson.fromJson(jsonRequest.toString(), EventDataEnvelope.class);
						//EventDataEnvelope request = new EventDataEnvelope(jsonRequest);
						eventQueue.add(request);
					}
				}
				// remove the oldest events until queue is below 1000
				while (eventQueue.size() > 300)
					eventQueue.poll();
			} catch (JSONException e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "loadEventsQueue", "JSONException loading events from storage", e);
			}
			catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "loadEventsQueue", "Exception loading events from storage", e);
			}
		}
		
	}
	
	/**
	 * Persists the queue of events to the phone's preferences
	 */
	protected void saveEvents(ConcurrentLinkedQueue<EventDataEnvelope> eventQueue){
		//JSONArray jsonQueue= new JSONArray();
		if (eventQueue == null)
			return;
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		Gson gson = new Gson();
		// remove the oldest events until queue is below 400
		while (eventQueue.size() > 300)
			eventQueue.poll();
		
		for(EventDataEnvelope eventEnv: eventQueue){
			try {
				String strJSON = gson.toJson(eventEnv);
				sb.append(strJSON);
				sb.append(",");
		        //JSONObject evtJSON =  new JSONObject(strJSON);
				//jsonQueue.put(evtJSON);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "persistQueue", "failed to persist event request", e);
			}
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");

		SharedPreferences preferenceSettings = MMCService.getSecurePreferences(owner);
		String stringQueue = sb.toString();//  jsonQueue.toString();

		
		//MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "saveEvents", stringQueue);

		preferenceSettings.edit().putString(PreferenceKeys.Miscellaneous.EVENTS_QUEUE, stringQueue).commit();
	}
	/**
	 * Uses the provided event instance to generate a {@link EventData} instance.
	 * @param event
	 * @return
	 */

	private EventData generateEventDataFromEvent(EventOld _event, boolean local){
		Cursor cellDataCursor = null;
		Cursor locationDataCursor = null;
		Cursor signalDataCursor = null;
		boolean bTroubleTweet = false;
		if (_event.getEventType().getIntValue() >= EventType.TT_DROP.getIntValue() && _event.getEventType().getIntValue() <= EventType.TT_NO_SVC.getIntValue())
			bTroubleTweet = true;
		
		try 
		{
			if (!bTroubleTweet) {	
				String accuracyWhere = "";
				if(_event.getEventType() == EventType.MAN_PLOTTING) {
					accuracyWhere = " and accuracy = -1 or accuracy = -2";
				} 
				else if( _event.getEventType() == EventType.MAN_TRANSIT){
					accuracyWhere = " and accuracy = -3 or accuracy = -4";
				}	
				else {
					accuracyWhere = " and accuracy <> -1";
				}
				cellDataCursor = getCellsAssociatedWithEvent(_event);
				locationDataCursor = getLocationsAssociatedWithEvent(_event, accuracyWhere );
				signalDataCursor = getSignalStrengthsAssociatedWithEvent(_event);
			}
			 
			int MCCMNC[] = owner.getMCCMNC();
			int mcc = 0;
			int mnc = 0;
			if (MCCMNC != null && MCCMNC.length > 1)
			{
				mcc = MCCMNC[0];
				mnc = MCCMNC[1];
			}
	
			int localId = 0;
			if (_event.getUri() != null)
				localId = Integer.parseInt(_event.getUri().getLastPathSegment());
			
			//if(owner.getLastMMCSignal() != null) {
			//	Integer signal = owner.getLastMMCSignal().getDbmValue(owner.getNetworkType(), owner.getPhoneType());
				//Marking the rssi unknown with -256
			//	rssi = signal != null ? (int)signal : -256;
			//}
		
			TrendStringGenerator.CoverageSamplesSend covSamples = null;
			String neighbors = "";
			String connections = "";
			String trend2 = "";
			String Stats = "";
			String APs = "";
			String Apps = "";
			String ThroughputStats = "";
			String latency = "fail";
			int lookupid1 = 0;
			int lookupid2 = 0;
			String transitAccel = "";
			
			boolean check = false;
			
			if (local == false && !bTroubleTweet)
			{
				try{
					covSamples = trendStringGenerator.generateSamples(	//trend string (primary)
					cellDataCursor, locationDataCursor, signalDataCursor, owner,_event);
				}
				catch (Exception e)
				{
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception occured trying to generate trend string ", e);
				}
			
				if (locationDataCursor == null || locationDataCursor.getCount() == 0 )
				{
					// failed to get GPS location, send a little GPS diagnosis
					boolean gpsEnabled = owner.getGpsManager().isLocationEnabled();
					if (!gpsEnabled)
						trend2 = "GPS=disabled,";
					else
						trend2 = "GPS=enabled,";
				}
				if (_event.getEventType() == EventType.COV_UPDATE)
				{
					//latency test
//					try {
//						String host = "http://d1l72qawknwf5q.cloudfront.net";
//						latency = owner.runLatencyTest(host);							
//					} catch(Exception e) {
//						e.printStackTrace();
//					}
					// Send datamonitor stats on 3Hr update events, unless disabled
					//if (_event.isCheckin == true)
					{
						int intervalDM = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.MANAGE_DATAMONITOR, 0);
						if (intervalDM > 0) // if enabled
						{
							int sleepHandoffs = owner.getUsageLimits().handleCheckin(true);
							trend2 += "IdleHandoffs=" + sleepHandoffs + ",";	
							SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(owner);
							int allowDM = preferenceSettings.getInt(PreferenceKeys.Miscellaneous.MANAGE_DATAMONITOR, 0);
							if(allowDM > 0) {
								DataMonitorDBReader dbReader = new DataMonitorDBReader();
								DataMonitorDBWriter dbWriter = new DataMonitorDBWriter();	
								
								try{
									
									//get Stats, APs, and Apps strings
									Stats = "[" + dbReader.getStatsString(owner.getApplicationContext()) + "]";					
									APs = "5,type,start,dur,id,sig," + owner.getAccessPointHistory().toString();	
								} catch (Exception e) {
									MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception in getStatsString: ", e);
								}
								try{
									owner.getAccessPointHistory().clearAccessPointsHistory(); 
								} catch (Exception e) {
									MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception in clearAccessPointsHistory: ", e);
								}
								try{
									Apps = owner.getDataMonitorStats().getRunningAppsString();
									//cleanup
									dbWriter.deleteAppsFromDB(owner.getApplicationContext());							
									check = dbWriter.delete15MinStatsBucket(owner.getApplicationContext());
								} catch (Exception e) {
									MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception in getRunningAppsString: ", e);
								}
								
							}
						}
					} 
				}
				if (_event.getEventType() == EventType.MAN_TRANSIT) {
//					try{				
//						transitAccel = "4,time,x,y,z," + owner.getAccelHistory().toString(_event.getEventTimestamp(), _event.getStageTimestamp());	
//					} catch (Exception e) {
//						MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception in transit accel toString: ", e);
//					}
					
					lookupid1 = _event.getLookupid1();
					lookupid2 = _event.getLookupid2();
					Apps = _event.getAppData();
					if(lookupid1 == 0 || lookupid2 == 0)
						return null;
				}
                if (_event.getEventType() == EventType.EVT_VQ_CALL)
                {
                    Apps = _event.getAppData();
                }
                if (_event.getJSONResult() != null)
                {
                    Stats = _event.getJSONResult();
                }
				//if (_event.getEventType() == EventType.VIDEO_TEST || _event.getEventType() == EventType.AUDIO_TEST)
				//	stationTo = _event.getLookupid2();

                lookupid2 = _event.getLookupid2();
                lookupid1 = _event.getLookupid1();
					
				//if (MMCLogger.isDebuggable())
				//	trend2 += listSignalFields ();
				long startTimestamp = _event.getEventTimestamp() - _event.getEventType().getPreEventStageTime();	//get the starting timestamp of the trend string
				
				neighbors = owner.getCellHistory().getNeighborHistory(startTimestamp, _event.getEventTimestamp());
				connections = owner.getConnectionHistory().getHistory(startTimestamp, _event.getEventTimestamp(), _event.getStageTimestamp()+5000, _event);
				String servicemode = owner.getConnectionHistory().getServiceModeHistory(startTimestamp, _event.getEventTimestamp(), _event.getStageTimestamp() + 5000, _event.getEventType());
				if (servicemode != null && servicemode.length() > 10)
					trend2 = servicemode;
			}

			//iUserID = serviceSettings.getInt(PreferenceKeys.User.USER_ID, -1);
			String apiKey = MMCService.getApiKey(owner);
			
			int iUserID = MMCService.getUserID(owner);
			if (iUserID == 0)
				iUserID = -1;
			Location location = _event.getLocation();
			
			if (location == null) {
				location = new Location("");
				if(_event.getEventType() == EventType.MAN_TRANSIT) {
					String loc = PreferenceManager.getDefaultSharedPreferences(owner).getString("transitEvent", null);
					if(loc != null) {
						String[] locs = loc.split(",");
						int lat = Integer.valueOf(locs[0]);
						int lon = Integer.valueOf(locs[1]);
						location = new Location("");
						location.setLatitude(lat/1000000.0);
						location.setLongitude(lon/1000000.0);
						PreferenceManager.getDefaultSharedPreferences(owner).edit().putString(
				    			"transitEvent", null).commit();
					}
				}
			}
			
//			MMCCellLocationOld cellLoc = null;
//			cellLoc.getCellLocation();
//			
//			
			int bsHigh = 0, bsLow = 0, bsMid = 0, bsCode = 0;
			
			if (_event.getCell() != null) {
				bsHigh = _event.getCell().getBSHigh();
				bsLow = _event.getCell().getBSLow();
				bsMid = _event.getCell().getBSMid();
				bsCode = _event.getCell().getBSCode();
			}
					
//			if (_event.getCell() != null && owner.getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
//			{
//				GsmCellLocation gsmCellLocation = (GsmCellLocation) _event.getCell().getCellLocation();
//				int cid = gsmCellLocation.getCid();
//				bsHigh = gsmCellLocation.getLac();//event.getCell.getBSHigh  //make getCelllocation private, call with ie getBSHIgh
//				bsMid = cid >> 16;
//				bsLow = cid & 0xFFFF;
//			}
//			else if (_event.getCell() != null && owner.getTelephonyManager().getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA)
//			{
//				CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) _event.getCell().getCellLocation();
//				bsLow = cdmaCellLocation.getBaseStationId();
//				bsMid = cdmaCellLocation.getNetworkId();
//				bsHigh = cdmaCellLocation.getSystemId();
//			}
////			
			Integer signalDB = -255;
			if (_event.getSignal() != null)
				signalDB = _event.getSignal().getDbmValue(owner.getPhoneStateListener().getNetworkType(), owner.getPhoneStateListener().getPhoneType());
			// unknown signal? server sees that as -255
			if (signalDB == null || signalDB == 0)
				signalDB = -255; 
			
			EventData eventData = new EventData(
				0,	// 1 would allow the server to request an auto speed test
				_event.connectTime,	//ConnectTime
//				-1,	//QOSRating is deprecated
//				0,	//wifi id1 is deprecated
//				0,	//wifi id2 is deprecated
//				0,	//wifi id3 is deprecated
//				0,	//wifi sig1 in deprecated
//				0,	//wifi sig2 in deprecated
//				0,	//wifi sig3 in deprecated
				"",	//phone number
				owner.getPhoneStateListener().getNetworkOperatorName(),
				String.format("Android %s,%s,%s", DeviceInfoOld.getManufacturer(), DeviceInfoOld.getPhoneModel(), DeviceInfoOld.getDevice()), 
				_event.getDuration(),	// the duration between the event and last complementary event
				covSamples,	// trend string 
				trend2, //trend string (secondary)
				neighbors,
				connections,
				_event.getEventTimestamp() / 1000,
				location.getTime() / 1000,	//timestamp of the gps fix used for this event (will be filled later)
				0L, //lStartTime (will be filled later)
				(float)location.getLongitude(),	//fltEventLng is the unscaled latitude
				(float)location.getLatitude(), //fltEventLat is the unscaled longitude
				_event.getEventType().getIntValue(),
				0L,	//we don't know the server side event id yet
				0L, //This is generated by the server so we don't worry about it just yet
				(int)_event.getEventIndex(),  //TODO funky event index
				0,	//sample interval is deprecated
				(int)location.getAltitude(),	//altitude (will be filled later)
				(int)location.getAccuracy(),	//uncertainty (will be filled later)
				(int)location.getSpeed(),	//speed (will be filled later)
				(int)location.getBearing(),	//heading (will be filled later)
				_event.getSatellites(),	//number of satellites
				signalDB,  //signal strengh (will be filled later)
				bsLow,	// cellId at the time of the event
				bsMid,	//nid
				bsCode,	//previous cell (will be filled later) 
				getAppVersionCode(),
				_event.getBattery(),	// battery level
				0, // ec/i0 may be filled in later
				_event.getFlags(),	// flags
				localId,	//CallId is internal to the server
				iUserID,
				mcc,
				mnc,
				bsHigh,	//LAC 
				_event.getCause(),	// cause of dropped call
				apiKey,
				MMCDevice.getIPAddress (),			
				Stats, 
				APs, 
				Apps,
				latency,
				lookupid1,
				lookupid2
			);
			
			eventData.setAppName (_event.getAppName());
			if (_event.getEventType().waitsForSpeed()) {
				eventData.setLatency (_event.latency);
				eventData.setDownloadSpeed (_event.downloadSpeed);
				eventData.setUploadSpeed (_event.uploadSpeed);
				eventData.setDownloadSize (_event.downloadSize);
				eventData.setUploadSize (_event.uploadSize);
				eventData.setTier (_event.getTier());	
				if (_event.getEventType() == EventType.LATENCY_TEST && _event.latency < 0)
					eventData.setDownloadSpeed(-_event.latency);
				
			}
			
			if (_event.getEventType() == EventType.APP_MONITORING && !local) {	
//				ThroughputStats = PreferenceManager.getDefaultSharedPreferences(owner).getString(PreferenceKeys.Miscellaneous.MONITOR_THROUGHPUT, "");				
//				if(!ThroughputStats.equals("")) {
//					eventData.setThroughputStats(ThroughputStats);	
//					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "generateEventDataFromEvent", "ThroughputStats: " + ThroughputStats);
//					PreferenceManager.getDefaultSharedPreferences(owner).edit().putString(PreferenceKeys.Miscellaneous.MONITOR_THROUGHPUT, "").commit();
//				}
				if(_event.getAppData() != null) {
					eventData.setRunningApps(_event.getAppData());	
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "generateEventDataFromEvent", "AppData: " + _event.getAppData());
				}
				//else
				//	return null;
			}

//			if (_event.getEventType() == EventType.MAN_PLOTTING) {
//								
//			}
			if (_event.getEventID () > 0)
				eventData.setEventId (_event.getEventID());
			eventData.setWifi(_event.getWifiInfo(), _event.getWifiConfig());

			return eventData;
		} catch (Exception ex){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception occured trying to generate event data ", ex);
		}
		finally
		{
			//now close the cursors
			if (cellDataCursor != null)
				cellDataCursor.close();
			if (locationDataCursor != null)
				locationDataCursor.close();
			if (signalDataCursor != null)
				signalDataCursor.close();
		}
		return null;
	}
	/*
	 * Query the database to fill in the Base station closest matching the start time of the event
	 */
	/*
	private void setEventDataCellParams(EventData outEventData, Cursor cellDataLocationCursor, EventOld _event){
		if (cellDataLocationCursor == null)
			return;
		cellDataLocationCursor.moveToLast();
		int cursorIndex;
		
		//if there is no location data available from this cursor, then return
		if (cellDataLocationCursor.getCount() == 0){
			cellDataLocationCursor.close();
			return;
		}
		
		// access location timestamps
		cursorIndex = cellDataLocationCursor.getColumnIndex(TablesOld.Locations.TIMESTAMP);
		
		// find the last known Base station before the event began
		while(!cellDataLocationCursor.isBeforeFirst() && cellDataLocationCursor.getLong(cursorIndex) > _event.getEventTimestamp())
			cellDataLocationCursor.moveToPrevious();
		if (cellDataLocationCursor.isBeforeFirst())
			cellDataLocationCursor.moveToFirst();
		if (owner.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA){
			//set the cellId using the bid
			int BIDIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.CDMAVersion.BID);
			if(BIDIndex != -1)
				outEventData.setiCellId(cellDataLocationCursor.getInt(BIDIndex));
			
			//set the network id
			int NIDIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.CDMAVersion.NID);
			if (NIDIndex != -1)
				outEventData.setNID(cellDataLocationCursor.getInt(NIDIndex));
			
			//set the system id
			int SIDIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.CDMAVersion.SID);
			if (SIDIndex != -1)
				outEventData.setLAC(cellDataLocationCursor.getInt(SIDIndex));
			
			if (!cellDataLocationCursor.isFirst() && cursorIndex != -1){
				cellDataLocationCursor.moveToPrevious();
				while (!cellDataLocationCursor.isBeforeFirst())
				{
					long prevSID = (long)cellDataLocationCursor.getInt(SIDIndex);
					int bid = cellDataLocationCursor.getInt(BIDIndex);
					int nid = cellDataLocationCursor.getInt(NIDIndex);
					bid = bid & 0xFFFF;
					prevSID = (long)(prevSID << 16) + (long)bid;
					if (bid > 0)
					{	
						outEventData.setPrevCell((int)prevSID);
						outEventData.setPrevNID (nid);
						break;
					}
					cellDataLocationCursor.moveToPrevious();
				}	
			}
		} else if (owner.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM){
			//set the cellId using
			cursorIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.GSMVersion.CELL_ID);
			
			if(cursorIndex != -1)
			{
				int cellID = cellDataLocationCursor.getInt(cursorIndex);
				int bsic = cellID >> 16;
				cellID = cellID & 0xFFFF;
				outEventData.setiCellId(cellID);
				outEventData.setNID(bsic); // server will know the NID is actually a BSIC in UMTS
			}
			
			//the network Id is kept 0 for gsm phones
			
			//set the lac
			cursorIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.GSMVersion.LAC);
			int lacIndex = cursorIndex;
			if (cursorIndex != -1)
				outEventData.setLAC(cellDataLocationCursor.getInt(cursorIndex));
			
			cursorIndex = cellDataLocationCursor.getColumnIndex(TablesOld.BaseStations.GSMVersion.CELL_ID);
			
			if (!cellDataLocationCursor.isFirst() && cursorIndex != -1){
				cellDataLocationCursor.moveToPrevious();
				while (!cellDataLocationCursor.isBeforeFirst())
				{
					long prevlac = (long)cellDataLocationCursor.getInt(lacIndex);
					int cellID = cellDataLocationCursor.getInt(cursorIndex);
					int bsic = cellID >> 16;
					cellID = cellID & 0xFFFF;
					if (cellID > 0)
					{	
						prevlac = (long)(prevlac << 16) + (long)cellID;
						outEventData.setPrevCell((int)prevlac);
						outEventData.setPrevNID (bsic);
						break;
					}
					cellDataLocationCursor.moveToPrevious();
				}
			}
		}
	}
	*/
	/*
	 * Query the database to fill in the Location closest matching the start time of the event
	 */
	/*
	private void setEventDataLocationParams(EventData outEventData, Cursor locationDataCursor, EventOld _event){
		if (locationDataCursor == null)
			return;
		locationDataCursor.moveToFirst();
		int cursorIndex;
		
		//if there is no location data available from this cursor, then return
		if (locationDataCursor.getCount() == 0){
			locationDataCursor.close();
			return;
		}
		
		// access location timestamps
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.TIMESTAMP);
		// accuracy index
		int accuracyIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.ACCURACY);
		// find the first known Location after the event began, with sufficient accuracy as well
		while(!locationDataCursor.isAfterLast() && (locationDataCursor.getLong(cursorIndex) < _event.getEventTimestamp() + 1000 || 
							locationDataCursor.getFloat(accuracyIndex) > GpsManagerOld.LOCATION_UPDATE_MIN_EVENT_ACCURACY)) 
			locationDataCursor.moveToNext();
		
		// No GPS results? try same thing again but this look for <200m accuracy
		if (locationDataCursor.isAfterLast())
		{
			locationDataCursor.moveToFirst();
			while(!locationDataCursor.isAfterLast() && (locationDataCursor.getLong(cursorIndex) < _event.getEventTimestamp() + 1000 || 
					locationDataCursor.getFloat(accuracyIndex) > 200))
				locationDataCursor.moveToNext();
		}
		// No GPS results? try same thing again but this time allow any accuracy
		if (locationDataCursor.isAfterLast())
		{
			locationDataCursor.moveToFirst();
			while(!locationDataCursor.isAfterLast() && locationDataCursor.getLong(cursorIndex) < _event.getEventTimestamp() + 1000)
				locationDataCursor.moveToNext();
		}
		// Still no GPS results? 
		if (locationDataCursor.isAfterLast())
		{
			locationDataCursor.moveToLast();
			return;
		}
		
		//set gps timestamp of location
		if (cursorIndex != -1)
			outEventData.setlGpsTimestamp(locationDataCursor.getLong(cursorIndex) / 1000);
		
		//set accuracy of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.ACCURACY);
		if (cursorIndex != -1)
			outEventData.setiUncertainty((int) locationDataCursor.getFloat(cursorIndex));
		
		//set altitude of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.ALTITUDE);
		if (cursorIndex != -1)
			outEventData.setiAltitude((int) locationDataCursor.getFloat(cursorIndex));
		
		//set bearing of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.BEARING);
		if (cursorIndex != -1)
			outEventData.setiHeading((int) locationDataCursor.getFloat(cursorIndex));
		
		//set latitude of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.LATITUDE);
		if (cursorIndex != -1) {
			//outEventData.setiEventLat((int) (locationDataCursor.getFloat(cursorIndex)));
			outEventData.setFltEventLat(locationDataCursor.getFloat(cursorIndex));
		}
		
		//set longitude of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.LONGITUDE);
		if (cursorIndex != -1) {
			//outEventData.setiEventLng((int) (locationDataCursor.getFloat(cursorIndex)));
			outEventData.setFltEventLng(locationDataCursor.getFloat(cursorIndex));
		}
		
		//set speed of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.SPEED);
		if (cursorIndex != -1)
			outEventData.setiSpeed((int) locationDataCursor.getFloat(cursorIndex));
		
		//get event id of location
		cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.EVENT_ID);
		long evtid = locationDataCursor.getLong(cursorIndex);
		if (evtid != outEventData.getCallID())
		{
			String provider = "GPS";
			//set longitude of location
			cursorIndex = locationDataCursor.getColumnIndex(TablesOld.Locations.PROVIDER);
			if (cursorIndex != -1) {
				provider = locationDataCursor.getString(cursorIndex);
			}
			
			ContentValues values = ContentValuesGeneratorOld.generateFromEventLocation(outEventData.getFltEventLat(), outEventData.getFltEventLng(), outEventData.getlGpsTimestamp()*1000, 
								outEventData.getiUncertainty(), outEventData.getiAltitude(), outEventData.getiSpeed(), outEventData.getiHeading(), provider, outEventData.getCallID());
			owner.getContentResolver().insert(TablesEnumOld.LOCATIONS.getContentUri(), values); 
			
		}
		
	}
	*/
	/*
	 * Query the database to fill in the Signal strength closest matching the start time of the event
	 */
	/*
	private void setEventDataSignalParams(EventData outEventData, Cursor signalDataCursor, EventOld _event){
		if (signalDataCursor == null)
			return;
		signalDataCursor.moveToFirst();
		int cursorIndex;
		//if there is no signal data available from this cursor, then return
		if (signalDataCursor.getCount() == 0){
			signalDataCursor.close();
			return;
		}
		
		// access location timestamps
		int timeIndex = signalDataCursor.getColumnIndex(TablesOld.SignalStrengths.TIMESTAMP);
		// find the last known signal before the event began
		while(!signalDataCursor.isAfterLast() && signalDataCursor.getLong(timeIndex) < _event.getEventTimestamp() - 10 )
			signalDataCursor.moveToNext();
		// in case there were no signal samples prior to event, let it use the 1st sample 
		// This can happen if the signal hasn't changed for a while before the event 
		// and then it uses the signal at the time the event was staged, which is the same as the last known signal
		if (signalDataCursor.isAfterLast()) 
			signalDataCursor.moveToLast();
		if (!signalDataCursor.isAfterLast() && signalDataCursor.getLong(timeIndex) > _event.getEventTimestamp() + 3000)
			signalDataCursor.moveToPrevious();
		
		//set accuracy of location
		cursorIndex = signalDataCursor.getColumnIndex(TablesOld.SignalStrengths.SIGNAL);
		int eci0Index = signalDataCursor.getColumnIndex(TablesOld.SignalStrengths.ECI0);
		if (cursorIndex != -1)
		{
			int iSignal = 0, eci0 = 0;
			// look at the signals at or just after the event, and use the last known
			do
			{
				if (!signalDataCursor.isAfterLast() && signalDataCursor.getLong(timeIndex) <= _event.getEventTimestamp() + 3000)
				{
				    iSignal = signalDataCursor.getInt(cursorIndex);
					if (iSignal == 0 || iSignal == -1)
						iSignal = -255;
					outEventData.setiSignal( iSignal);
					eci0 = signalDataCursor.getInt(eci0Index);
					outEventData.setRssi( eci0);
					signalDataCursor.moveToNext();
				}
				else
					break;
			} while (!signalDataCursor.isAfterLast());
		}
		
	}
	*/
	/**
	 * Uses the provided event data list to generate a {@link EventDataEnvelope} instance.
	 * @param eventDataList
	 * @return
	 */
	private EventDataEnvelope generateEventDataEnvFromEventList(List<EventData> eventDataList, String phoneNumber) {
		try
		{
			//SharedPreferences serviceSetting = PreferenceManager.getDefaultSharedPreferences(owner);
			int userID = MMCService.getUserID(owner);
			if (userID <= 0 && !owner.isServiceRunning())
				userID = owner.getLastUserID (); // have the userid if the user logged out and service is closing
			String apikey = MMCService.getApiKey(owner);
			int MCCMNC[] = owner.getMCCMNC();
			int mcc = 0;
			int mnc = 0;
			if (MCCMNC != null && MCCMNC.length > 1)
			{
				mcc = MCCMNC[0];
				mnc = MCCMNC[1];
			}
			MMCDevice mmcdev = owner.getDevice();
            
			EventDataEnvelope eventDataEnvelope = new EventDataEnvelope(
				0, 				//qos rating. This is deprecated
				0, 				//deprecated
				getAppVersionCode(), 
				0, 				//TODO 1 if the call was dropped, 0 otherwise
				0, 				//TODO 1 if dataspeed test is allowed, 0 otherwise
				userID,
				owner.getPhoneStateListener().getNetworkOperatorName(),
				String.format(Locale.US,"Android %s,%s,%s", DeviceInfoOld.getManufacturer(), DeviceInfoOld.getPhoneModel(), DeviceInfoOld.getDevice()), 
				eventDataList.get(0).getlTimestamp(), 
				0,				//deprecated 
				0,				//deprecated 
				phoneNumber, 		
				DeviceInfoOld.battery, 			
				String.format(Locale.US,"Android %d", DeviceInfoOld.getAndroidVersion()),
				owner.getPhoneStateListener().getPhoneType(), owner.getPhoneStateListener().getNetworkType(),owner.getPhoneStateListener().isRoaming(),
				MMCDevice.getIPAddress (), mcc, mnc, 
				mmcdev.getIMSI(),
				apikey,
				eventDataList
			);
			return eventDataEnvelope;
		}
	    catch (Exception ex){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "generateEventDataFromEvent", "exception occured trying to generate trend string: " + ex.getMessage());
		}
		return null;
	}
	
	
	private int getAppVersionCode(){
		try {
			return owner.getPackageManager().getPackageInfo(owner.getPackageName(), 0).versionCode;
		} catch(NameNotFoundException e){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getAppVersionCode", "Could not find app version" + e.getMessage());
		}
		return -1;
	} 
	
	/**
	 * This method gets a cursor that contains all the location objects that are associated with the
	 * given event.
	 * @return
	 */
	private Cursor getLocationsAssociatedWithEvent(EventOld _event, String accuracy){

		long startTime = _event.getEventTimestamp(); 
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getLocationsAssociatedWithEvent", "startTime="+startTime);
		Cursor cursor = owner.getDBProvider().query(
				UriMatchOld.LOCATIONS.getContentUri(),
				null,
				TablesOld.Locations.TIMESTAMP  + ">=?" + accuracy,
				new String[]{ String.valueOf(startTime)},
				TablesOld.Locations.TIMESTAMP + " ASC"
			);
		cursor.moveToFirst();
		long gpsTime = 0;
		if (!cursor.isBeforeFirst())
		{
			gpsTime = cursor.getLong(1);
			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getLocationsAssociatedWithEvent", "time="+gpsTime);
			
		}
		return cursor;
	}
	
	/**
	 * This method gets a cursor that contains all the baseStation rows that are associated with the given
	 * event.
	 * @return
	 */
	private Cursor getCellsAssociatedWithEvent(EventOld _event){
		Uri baseStationTable = 	(UriMatchOld.BASE_STATIONS.getContentUri()				
			/*owner.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA 
			? UriMatchOld.BASE_STATIONS_CDMA.getContentUri()
			: UriMatchOld.BASE_STATIONS_GSM.getContentUri()*/
		);
	
		// If we go back to 15 minutes before the event for cellid changes, then it will walk forward to find the latest change before the event
		long startTime = _event.getEventTimestamp() - _event.getEventType().getPreEventStageTime() - 900000; 
		
		Cursor cursor = owner.getDBProvider().query(
				baseStationTable,
				null,
				TablesOld.BaseStations.TIMESTAMP  + ">?",
				new String[]{ String.valueOf(startTime)},
				TablesOld.BaseStations.TIMESTAMP + " ASC"
			);
		return cursor;
	}
	
	/**
	 * This method gets a cursor that contains all the signal strength rows that are associated with the given
	 * event. It gets signal strength well before event, but only the last one before 30 seconds before event will be the start
	 * @return
	 */
	private Cursor getSignalStrengthsAssociatedWithEvent(EventOld _event){

		// Single signal strength table now
		Uri signalStrengthTable = UriMatchOld.SIGNAL_STRENGTHS.getContentUri();
		String[] projection = new String[]{BaseColumns._ID, TablesOld.TIMESTAMP_COLUMN_NAME, TablesOld.SignalStrengths.SIGNAL, TablesOld.SignalStrengths.ECI0, TablesOld.SignalStrengths.SNR, TablesOld.SignalStrengths.BER, TablesOld.SignalStrengths.RSCP, TablesOld.SignalStrengths.SIGNAL2G, TablesOld.SignalStrengths.LTE_SIGNAL, TablesOld.SignalStrengths.LTE_RSRP, TablesOld.SignalStrengths.LTE_RSRQ, TablesOld.SignalStrengths.LTE_SNR, TablesOld.SignalStrengths.LTE_CQI, TablesOld.SignalStrengths.SIGNALBARS, TablesOld.SignalStrengths.ECN0, TablesOld.SignalStrengths.WIFISIGNAL, TablesOld.SignalStrengths.COVERAGE };

		long startTime = _event.getEventTimestamp() - _event.getEventType().getPreEventStageTime() - 300000; 
		
		Cursor cursor = owner.getDBProvider().query(
				signalStrengthTable,
				projection,
				TablesOld.SignalStrengths.TIMESTAMP  + ">?",
				new String[]{ String.valueOf(startTime)},
				TablesOld.SignalStrengths.TIMESTAMP + " ASC"
			);
		
		return cursor;
	}
	
	public EventResponse getEventResponse(){
		return this.eventResponse;
	}
	
	private String listSignalFields ()
	{
		int i;
		MMCSignalOld mmcsignal = owner.getPhoneStateListener().getLastMMCSignal();
		String strSignals = "";
		if (mmcsignal != null && !mmcsignal.isUnknown())
		{
			Object signalStrength = mmcsignal.getSignalStrength();

			Field[] fields = null;
			try {
				fields = signalStrength.getClass().getDeclaredFields();
				
				for (i=0; i<fields.length; i++)
				{
					fields[i].setAccessible(true);
					//if (!fields[i].getName().equals("CREATOR") && !fields[i].getName().equals("LOG_TAG") &&
					//		fields[i].getName().indexOf("INVALID") == -1 && fields[i].getName().indexOf("STRENGTH") == -1)
					if (fields[i].getName().toLowerCase().substring(0,1).equals(fields[i].getName().substring(0,1)))
					{
						try
						{
						strSignals += fields[i].getName() + "=";
						if (fields[i].get(signalStrength) != null)
							strSignals += fields[i].get(signalStrength).toString() + ",";
						else
							strSignals += "null";
						}
						catch (Exception e)
						{
							MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "exception", e);
						}
					}
				}
			} catch (SecurityException e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "SecurityException", e);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "exception", e);
			}
		}
		ServiceState serviceState = event.getServiceState();
		if (serviceState != null)
		{
			Field[] fields = null;
			try {
				fields = serviceState.getClass().getDeclaredFields();
				for (i=0; i<fields.length; i++)
				{
					fields[i].setAccessible(true);
					if (fields[i].getName().toLowerCase().substring(0,1).equals(fields[i].getName().substring(0,1)))
					{
						try
						{
						strSignals += fields[i].getName() + "=";
						if (fields[i].get(serviceState) != null)
							strSignals += fields[i].get(serviceState).toString() + ",";
						else
							strSignals += "null";
						}
						catch (Exception e)
						{
							MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "exception", e);
						}
					}
				}
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", strSignals);
				
			} catch (SecurityException e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "SecurityException", e);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", "exception", e);
			}
		}
		return strSignals;
	}
}
