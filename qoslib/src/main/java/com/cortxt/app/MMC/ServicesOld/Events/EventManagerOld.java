package com.cortxt.app.MMC.ServicesOld.Events;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.telephony.CellLocation;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import com.cortxt.app.MMC.ContentProviderOld.ContentValuesGeneratorOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesEnumOld;
import com.cortxt.com.mmcextension.EventTriggers.SpeedTestTrigger;
import com.cortxt.com.mmcextension.EventTriggers.VideoTestTrigger;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Reporters.LocalStorageReporter.LocalStorageReporter;
import com.cortxt.app.MMC.ServicesOld.Buffers.MMCCellLocationOld;
import com.cortxt.app.MMC.ServicesOld.Buffers.MMCSignalOld;
import com.cortxt.app.MMC.ServicesOld.Location.GpsListenerOld;
import com.cortxt.app.MMC.ServicesOld.Location.GpsManagerOld;
import com.cortxt.app.MMC.Utils.MMCCommand;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.DeviceInfoOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventDataEnvelope;
import com.cortxt.com.mmcextension.UsageLimits;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This is the class responsible for the event related logic that has to be executed
 * on the app. Some of its main features are :-
 * <ul>
 * 	<li>Storing the events (in both the SQLite tables and the internal caches).</li>
 * 	<li>Responding to queries about the various events quickly.</li>
 * 	<li>Associating other pieces of data (like the locations, the signal strengths and the
 * base stations) with the corresponding events.</li>
 * </ul>
 * 
 * Note: In this version of the event manager, we are not going to calculate the durations
 * of the events. Instead we are just going to declare the durations zero. This is because the
 * new server would not require calculation of these durations and we have to move to the new
 * server eventually anyways.
 * @author Abhin
 *
 */
public class EventManagerOld {
	public static final String TAG = EventManagerOld.class.getSimpleName();
	/**
	 * When an event occurs (whether it belongs to an event couple or not), it is able to accept
	 * data (like signal strength information) for a definite period of time after its start.
	 * This period is called the lime light period of the event.
	 */
	public static final int EVENT_STAGING_PERIOD = 30000;	//in milliseconds
	
	/**
	 * This is a list of ongoing events
	 */
	private List<EventCoupleOld> ongoingEventCouples = new ArrayList<EventCoupleOld>();
	
	/**
	 * This variable stores the currently staged event ("staged" would be referred to as a phase of an event). During
	 * this phase, the event is able to associate additional data with itself.
	 * 
	 * Note:- There can only be one event at a time that can be considered "staged". That event is 
	 * whichever event this variable stores.
	 * @see EventManagerOld#EVENT_STAGING_PERIOD
	 */
	private EventOld stagedEvent = null;
	
	/**
	 * This variable stores the complimentary event of the event that is currently stored. In case the staged event 
	 * is the "starting end" of the eventCouple or the staged event is a singleton, this variable would be null.
	 */
	private EventOld stagedComplimentaryEvent = null;
	
	/**
	 * This is the timer than "kicks events off of the stage" when their time is up.
	 */
	private Timer stageTimer = new Timer();
	
	/**
	 * This is the timer that uploads the event after the designated delay.
	 */
	private Timer uploadTimer = new Timer();
	
	/**
	 * This is an instance of the StageTimerTask class that is being stored in this
	 * variable to make the process of resetting the timer easier.
	 */
	private StageTimerTask stageTimerTask;
	
	/**
	 * The eventCache holds all the eventTypes that are currently active
	 */
	private EnumSet<EventType> eventCache = EnumSet.noneOf(EventType.class);
	// Queue events if unable to send to server
	private static ConcurrentLinkedQueue<EventDataEnvelope> eventQueue = null;

	private MMCService context;
	protected long mLastServiceStateChangeTimeStamp =0, mLast3GStateChangeTimeStamp = 0, mLastScreenOffDuration = 0;
	private SpeedTestTrigger mSpeedTestTrigger;
	private VideoTestTrigger mVideoTestTrigger;
	private EventOld trackingEvent = null;

	public EventManagerOld(MMCService context){
		this.context = context;
		mLastServiceStateChangeTimeStamp = System.currentTimeMillis();
		mLast3GStateChangeTimeStamp = System.currentTimeMillis();
		mSpeedTestTrigger = new SpeedTestTrigger(context.getExtensionManager(), context.handler);
		mVideoTestTrigger = new VideoTestTrigger(context.getExtensionManager(), context.handler);
	}
	
	/**
	 * Cancels timers used by this class, as they do not get cancelled when the object is garbage collected.
	 */
	public void stop ()
	{
		stageTimer.cancel();
		uploadTimer.cancel();
		mSpeedTestTrigger.stop();
		mVideoTestTrigger.stop();
	}
	
	/**
	 * This method starts an event couple and updates the cache. The steps it takes are :-
	 * <ol>
	 * 	<li>Creates an entry in the Event table with the appropriate eventType and 0 duration.</li>
	 * 	<li>Creates an entry in the EventCouple Table with the stopEvent field null.</li>
	 * 	<li>Using the uris it gets from the above 2 inserts, it creates the event couple and adds it to {@link #ongoingEventCouples}</li>
	 * 	<li>Now it "flags" the event in the {@link #eventCache} for quick access.</li>
	 * </ol>
	 * <b>Note: This method assumes that the event couple is not already running.</b>
	 * <b>Note: This method does not stage the event. That part has to be done separately.</b>
	 * @param startEventType The event type of the starting event.
	 * @param stopEventType The event type of the ending event.
	 * @return eventCouple The event couple that was just created
	 */
	public EventCoupleOld beginEventCouple(EventType startEventType, EventType stopEventType)
	{
		return beginEventCouple(startEventType, stopEventType, System.currentTimeMillis());
	}
	public EventCoupleOld beginEventCouple(EventType startEventType, EventType stopEventType, long time){	
		//create an entry in the events table
		//ContentValues values = ContentValuesGeneratorOld.generateEventValues(startEventType, time);
		//Uri startEventUri = context.getContentResolver().insert(TablesEnumOld.EVENTS.getContentUri(), values);
		
		//create an entry in the event couples table
		//values = ContentValuesGeneratorOld.generateEventCoupleValues(startEventUri);
		//Uri eventCoupleUri = context.getContentResolver().insert(TablesEnumOld.EVENT_COUPLES.getContentUri(), values);
		
		//create the event couple and add it to the ongoingEvents list
		EventCoupleOld eventCouple = new EventCoupleOld(startEventType, stopEventType, null, null);//eventCoupleUri, startEventUri);
		ongoingEventCouples.add(eventCouple);
		
		//flag the event in the event cache
		eventCache.add(startEventType);
		signalSnapshot (eventCouple.getStartEvent());
//		String evtid = eventCouple.getStartEvent().getUri().getLastPathSegment();
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "beginEventCouple", "added event to database: type=" + eventCouple.getStartEvent().getEventType().getEventName() + " id=" + evtid);
		
		return eventCouple;
	}
	
	/**
	 * This method stops an event pair and updates the cache.
	 * The startEventType and stopEventType define the type of event pair, such as Lost/Regained Service
	 * this method ensures that the start of the event couple is already running
	 * @param startEventType
	 * @param stopEventType
	 * @return
	 */
	public EventCoupleOld endEventCouple(EventType startEventType, EventType stopEventType){

		// move the event out of the ongoing event couples list
		EventCoupleOld completedEventCouple = getEventCouple(startEventType, stopEventType);
		if (completedEventCouple == null){
			return null;	//if the event doesn't exist in the list, then forget it
		}
		completedEventCouple.triggerStopEvent(0);

		// remove the event from the internal event cache and the ongoing event list
		eventCache.remove(startEventType);	//since the startEventTypes of the events in the list are going to be unique
											//all the time, the flag can safely be removed from the eventCache.
		ongoingEventCouples.remove(completedEventCouple);
		eventCache.add(stopEventType);  // the 2nd event of couple is still ongoing using the gps
		signalSnapshot (completedEventCouple.getStopEvent());

		return completedEventCouple;
	}

	public void stopTracking() {
		unstageAndUploadEvent(trackingEvent, null);
		if (trackingEvent != null && trackingEvent.gpsListener != null)
			context.getGpsManager().unregisterListener(trackingEvent.gpsListener);
		context.getTrackingManager().cancelScheduledEvents();
	}

	/**
	 * This method :-
	 * <ol>
	 * 	<li>Notifies the {@link EventManagerOld} to register the event.</li>
	 * 	<li>Dispatches this information to the UI in case it wants to draw a marker for it on the chart.</li>
	 * 	<li>Starts the GPS.</li>
	 * </ol>
	 * @param eventType
	 */
	public EventOld triggerSingletonEvent(EventType eventType)
	{
		final EventOld singletonEvent  = registerSingletonEvent(eventType);

		if (eventType == EventType.MAN_TRACKING)
			trackingEvent = singletonEvent;
		runEvent(singletonEvent, null, false);
		return singletonEvent;
	}
	/**
	 * This method starts a standalone event, and event that is not a part of a pair
	 * @param eventType
	 * @return
	 */
	public EventOld registerSingletonEvent(EventType eventType){
		//now move the event into the "staged" phase
		EventOld event = null;
		event = new EventOld(eventType, null); //endEventUri);	
		
		//and now flag the event in the event cache for quick access
		eventCache.add(eventType);	//if the event is already flagged in the eventCache, then nothing will happen
		signalSnapshot(event);

		return event;
	}

	/**
	 * Start a paired event. Specifies the start and stop events to define the paired event (EventCouple)
	 */

	public EventOld startPhoneEvent(EventType startEventType, EventType stopEventType)
	{
		if (startEventType == EventType.COV_4G_NO)
			context.getIntentDispatcher().updateLTEIdentity (null);


		return startPhoneEvent(startEventType, stopEventType, System.currentTimeMillis());
	}
	public EventOld startPhoneEvent(EventType startEventType, EventType stopEventType, long time)
	{
		// cancel any phone related event when in dormant mode
		if (getUsageLimits().getDormantMode() >= 1)
			return null;
		if (getUsageLimits().getUsageProfile() <= UsageLimits.MINIMAL)
		{
			// Lets not return these data outages during minimal mode.
			if (startEventType == EventType.COV_3G_NO || startEventType == EventType.COV_4G_NO || startEventType == EventType.COV_DATA_NO)
				return null;
		}
		if (!context.getPhoneStateListener().validSignal &&
				(startEventType == EventType.COV_4G_NO || startEventType == EventType.COV_3G_NO || startEventType == EventType.COV_DATA_NO || startEventType == EventType.COV_4G_NO || startEventType == EventType.COV_VOD_NO))
			// phone must see at least one valid signal before allowing these events
			return null;

		if (this.isEventRunning(startEventType)){
			if (startEventType == EventType.SIP_CONNECT)
			{
				EventCoupleOld targetEventCouple = getEventCouple(startEventType, stopEventType);
				if (targetEventCouple != null)
					this.unstageEvent(targetEventCouple.getStartEvent());
			}
			else
				//if the event is somehow already running, then it cannot be made to run again
				//this method will now simply return
				return null;  // This could be very bad if it somehow misses a Disconnect and its never allowed to trigger a connect/disconnect event again
		}
		EventCoupleOld eventCouple = this.beginEventCouple(startEventType, stopEventType, time);
		runEvent(eventCouple.getStartEvent(), eventCouple.getStopEvent(), false);
		return eventCouple.getStartEvent();
	}

	/**
	 * This method :-
	 * <ol>
	 * 	<li>Notifies the {@link EventManagerOld} to stop the event.</li>
	 * 	<li>Dispatches this information to the UI in case it wants to draw a marker for it on the chart.</li>
	 * 	<li>Starts the GPS.</li>
	 * </ol>
	 */
	public EventOld stopPhoneEvent(EventType startEventType, EventType stopEventType)
	{
		// cancel any phone related event when in dormant mode
		if (getUsageLimits().getDormantMode() >= 1)
			return null;

		if (!this.isEventRunning(startEventType)){
			//if the start event of this couple is somehow not running, then the stop event can't run
			//this method will now simply return
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "stopPhoneEvent", "missing start of pair " + startEventType.name());
			return null;
		}
		EventCoupleOld eventCouple = this.endEventCouple(startEventType, stopEventType);
		long duration = 0;
		if (eventCouple != null && eventCouple.getStopEvent() != null && eventCouple.getStartEvent() != null)
		{
			duration = eventCouple.getStopEvent().getEventTimestamp() - eventCouple.getStartEvent().getEventTimestamp();
			if (eventCouple.getStartEventType() == EventType.EVT_CONNECT || eventCouple.getStartEventType() == EventType.SIP_CONNECT)
			{
				eventCouple.getStopEvent().setDuration (duration);
				eventCouple.getStartEvent().setDuration (duration);
			}
		}
		else if (eventCouple == null)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "stopPhoneEvent", "eventCouple=null");
			return null;
		}
		boolean bUnstage = false;
		int cutoff = 30;  // 30 second cutoff on most paired events
		if (stopEventType == EventType.COV_VOD_YES)
			cutoff = 15; // shorter cutoff on voice outage events, which means we want to try for the full gps fix on a 15 second outage
		if (getUsageLimits().getUsageProfile() == 0)
			cutoff = cutoff * 4 / 3; // minimum profile will require the outage or event pair to last longer to allow a full gps timeout
		else if (getUsageLimits().getUsageProfile() >= 2)
			cutoff = 0; // maximum profile will allow a full gps timeout on every paired event no matter how short

		if (duration < cutoff*1000 && !context.getTravelDetector().isTravelling() && (stopEventType == EventType.COV_3G_YES
				|| stopEventType == EventType.COV_4G_YES || stopEventType == EventType.COV_DATA_YES
				|| stopEventType == EventType.COV_VOD_YES || stopEventType == EventType.EVT_DISCONNECT || stopEventType == EventType.SIP_DISCONNECT))
			bUnstage = true;

		// When a call ends, unstage the connect call
		if ((stopEventType == EventType.EVT_DROP || stopEventType == EventType.EVT_DISCONNECT || stopEventType == EventType.EVT_CALLFAIL || stopEventType == EventType.EVT_UNANSWERED ||
				stopEventType == EventType.SIP_DROP || stopEventType == EventType.SIP_DISCONNECT || stopEventType == EventType.SIP_CALLFAIL || stopEventType == EventType.SIP_UNANSWERED))
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "stopPhoneEvent", "bUnstage = true ");
		}

		runEvent(eventCouple.getStopEvent(), eventCouple.getStartEvent(), bUnstage);
		return eventCouple.getStopEvent();

	}

	public void runEvent (final EventOld event, final EventOld compEvent, final boolean bUnstage)
	{
		final EventType eventType = event.getEventType();

		final boolean bRunEvent;
		// decide whether to let event run the GPS and set mmc active
		if (event.getEventType().getPostEventStageTime() > 0  //  && !getUsageLimits ().exceededGps (event.getEventType(), true)
				&& (!bUnstage || event.getEventType() == EventType.EVT_DROP || event.getEventType() == EventType.EVT_DISCONNECT || event.getEventType() == EventType.SIP_DROP || event.getEventType() == EventType.SIP_DISCONNECT))  // Only invoke Gps if EventType needs GPS (not for normal disconnect)
		{
			bRunEvent = true;

			if (event.getEventType() != EventType.COV_UPDATE && event.getEventType() != EventType.TRAVEL_CHECK && event.getEventType() != EventType.LATENCY_TEST &&
					event.getEventType() != EventType.WIFI_CONNECT && event.getEventType() != EventType.WIFI_DISCONNECT && event.getEventType() != EventType.LATENCY_TEST)
				context.keepAwake(true, false);  // use a screen-on wake lock for most events
			else
				context.keepAwake(true, true);  // but use a partial wake lock for update and travel events so it doesnt look like screen turns on mysteriously every 3 hours
		}
		else
			bRunEvent = false;
		context.handler.post(new Runnable() {
			// @Override
			public void run() {
				try {
					if (context.isServiceRunning() == false) {
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "runEvent", "service not running ");
						return;
					}
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "runEvent", "starting event type " + eventType.getEventName());

					context.getIntentDispatcher().markEvent(eventType);
					event.setAppName(context.getDataMonitorStats().getForegroundAppName());

					if (bRunEvent)  // Only invoke Gps if EventType needs GPS (not for normal disconnect)
					{
						String reason = null;
						if (eventType == EventType.EVT_FILLIN) {
							reason = null; // "mapping new coverage area";
						} else if (eventType == EventType.COV_3G_NO || eventType == EventType.COV_3G_YES ||
								eventType == EventType.COV_4G_NO || eventType == EventType.COV_4G_YES ||
								eventType == EventType.COV_DATA_NO || eventType == EventType.COV_DATA_YES ||
								eventType == EventType.COV_VOD_NO || eventType == EventType.COV_VOD_YES ||
								eventType == EventType.EVT_DROP || eventType == EventType.EVT_CALLFAIL || eventType == EventType.MAN_SPEEDTEST ||
								eventType == EventType.SIP_DROP || eventType == EventType.SIP_CALLFAIL ||
								eventType == EventType.SMS_TEST || eventType == EventType.APP_MONITORING || eventType == EventType.VIDEO_TEST || eventType == EventType.YOUTUBE_TEST
								|| eventType == EventType.AUDIO_TEST || eventType == EventType.WEBPAGE_TEST)
							reason = eventType.getEventString(context);
						else if (eventType == EventType.MAN_TRACKING)
							reason = EventType.MAN_TRACKING.getEventString(context);// getString(R.string.Gene"recording";
						else if (eventType == EventType.COV_UPDATE || eventType == EventType.TRAVEL_CHECK || eventType == EventType.LATENCY_TEST || eventType == EventType.WIFI_CONNECT || eventType == EventType.WIFI_DISCONNECT)
							reason = "background";
						else
							reason = null;

						int allowPopup = PreferenceManager.getDefaultSharedPreferences(context).getInt(PreferenceKeys.Miscellaneous.ALLOW_DROP_POPUP, 2);
						if (allowPopup == 1 && !getUsageLimits().getUseRadioLog())
							allowPopup = 0;
						if (allowPopup == 0 && (eventType == EventType.EVT_DROP || eventType == EventType.EVT_CALLFAIL || eventType == EventType.SIP_DROP || eventType == EventType.SIP_CALLFAIL))
							reason = context.getString(R.string.GenericText_Call_logged);

						//else if (eventType == EventType.COV_UPDATE)
						//	reason = "";
						//else
						//	reason = "checking coverage";
						context.startRadioLog(true, reason, eventType);
						if (event.getEventType() == EventType.MAN_PLOTTING || event.getEventType() == EventType.MAN_TRANSIT)
							return;
						if (event.gpsListener == null) {
							event.gpsListener = new GpsListenerForEvent(event, compEvent);
						}
						context.getGpsManager().registerListener(event.gpsListener);
						context.getNetLocationManager().registerListener(new LocationListenerForEvent(event));  // contingency coarse location listener that will try to write first coarse location to same location database

					} else if (event.getEventType().getGenre() == EventTypeGenreOld.END_OF_COUPLE) {
						//Location lastLocation = null;
						//if (event.gpsListener != null)
						//	lastLocation = event.gpsListener.getLastLocation ();
						context.getGpsManager().unregisterListener(compEvent.gpsListener);
						temporarilyStageEvent(event, compEvent, context.getLastLocation());
					} else
						temporarilyStageEvent(event, compEvent, null);
					if (event.getEventType() == EventType.EVT_DROP || event.getEventType() == EventType.EVT_DISCONNECT || event.getEventType() == EventType.SIP_DROP || event.getEventType() == EventType.SIP_DISCONNECT ||
							event.getEventType() == EventType.EVT_UNANSWERED || event.getEventType() == EventType.SIP_UNANSWERED || event.getEventType() == EventType.EVT_CALLFAIL || event.getEventType() == EventType.SIP_CALLFAIL) {
						unstageAndUploadEvent(compEvent, event);
					}
				} catch (Exception ex) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "runEvent", "Exception occured starting event " + eventType.name(), ex);
					return;
				}
			}
		});
	}

	/**
	 * When a new piece of data is received (like signal strength) it usually associates
	 * itself with the currently staged event. This is achieved by publicizing the id
	 * of the currently staged event through this method.
	 * @return The ID (in the local sqlite database) of the currently staged event
	 */
	public long getStagedEventId() {
		try
		{
			if(stagedEvent == null) {
				return -1;
			}
			else {
				return Long.parseLong(stagedEvent.getUri().getLastPathSegment());
			}
		}
		catch (Exception ex)
		{
		}
		return -1;
	}
	
	/**
	 * Returns a reference to the ongoing events list.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<EventCoupleOld> getOngoingEventCouples(){
		return (List<EventCoupleOld>) ((ArrayList<EventCoupleOld>)this.ongoingEventCouples).clone();
	}

	/**
	 * At the start of the event, stores like-timestamped entries in the tables for signals, cells and locations
	 * This is so that querying the timespan of the event recording will easily return records for the beginning of the event
	 * among other things
	 * @param event
	 */
	public void signalSnapshot (EventOld event)
	{
		// In case the signal hasn't changed recently (last 30 sec or so), 
		// the signal won't be retrieved by the cursor unless we timestamp the last known signal now
		try {

			TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService (Context.TELEPHONY_SERVICE);
			// Update the database and intents with a snapshot of the last known signal, cell, location
			MMCSignalOld signal = context.getPhoneStateListener().getLastMMCSignal();
			long timestamp = System.currentTimeMillis();
			if (event != null)
				timestamp = event.getEventTimestamp();
			if (signal != null)
			{
				signal.setTimestamp(timestamp);
				context.getPhoneStateListener().clearLastMMCSignal();  // to force a duplicate signal to be added
				context.getPhoneStateListener().processNewMMCSignal(signal);
			}

			MMCCellLocationOld cell = context.getPhoneStateListener().getLastCellLocation();
			if (cell != null)
			{
				cell.setCellIdTimestamp(timestamp);
				context.getPhoneStateListener().clearLastCellLocation();
				context.getPhoneStateListener().processNewCellLocation(cell);
			}

			if (event == null)
				return;

			Location location = context.getLastLocation();
			int satellites = context.getLastNumSatellites();
			if (location != null)
			{
				location.setTime(timestamp);
				context.setLastLocation(null);  // to force a duplicate location to be added
				MMCService.processNewFilteredLocation(location, satellites, context);
			}

			// set an initial location on the event if it is recent, but update it if a better location becomes available
			location = context.getLastLocation();
			event.setSignal(context.getPhoneStateListener().getLastMMCSignal());
			event.setCell(context.getPhoneStateListener().getLastCellLocation());
			event.setServiceState(context.getPhoneStateListener().getLastServiceStateObj());

			if (location != null && location.getTime() - 10000 > System.currentTimeMillis())
				event.setLocation(location, context.getLastNumSatellites());

			
			// Also, set the coverage flags at the time of the event
			int tier = context.getPhoneStateListener().getNetworkGeneration();
			int servicestate = context.getPhoneStateListener().getLastServiceState();
			int datastate = telephonyManager.getDataState();
			int activeConnection = context.ActiveConnection();
			if ( activeConnection == 10)
				event.setFlag (EventOld.SERVICE_WIFI, true);
			else if (activeConnection == 11)
				event.setFlag (EventOld.SERVICE_WIMAX, true);
			
			if (datastate == TelephonyManager.DATA_CONNECTED || activeConnection > 1)
				event.setFlag (EventOld.SERVICE_DATA, true);
			else if (datastate == TelephonyManager.DATA_SUSPENDED && servicestate == ServiceState.STATE_IN_SERVICE) // not counted as data outage if data turned off
				event.setFlag (EventOld.SERVICE_DATA, true);
			else
				event.setFlag (EventOld.SERVICE_DATA, false);
			if (servicestate == ServiceState.STATE_OUT_OF_SERVICE || servicestate == ServiceState.STATE_EMERGENCY_ONLY)
				event.setFlag (EventOld.SERVICE_VOICE, false);
			else
				event.setFlag (EventOld.SERVICE_VOICE, true);
			if (tier > 2)
				event.setFlag (EventOld.SERVICE_3G, true);
			if (tier > 4)
				event.setFlag (EventOld.SERVICE_4G, true);
			if (context.getPhoneStateListener().isCallConnected() || context.getPhoneStateListener().isCallDialing() || event.getEventType().getIntValue() <= 6)
				event.setFlag (EventOld.PHONE_INUSE, true);
			
			// dropped call detection support using logcat or precise call state?
			if (event.getEventType().getIntValue() <= 7)
			{
				String pname = context.getPackageName();
				int permissionForReadLogs = context.getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS, pname); //0 means allowed
				int permissionForPrecise = context.getPackageManager().checkPermission("android.permission.READ_PRECISE_PHONE_STATE", pname); // 0 means allowed
				if (permissionForPrecise == 0)
					event.setFlag (EventOld.CALL_PRECISE, true);
				else if (permissionForReadLogs == 0)
					event.setFlag (EventOld.CALL_LOGCAT, true);
			}
			
			event.setBattery(DeviceInfoOld.battery);
			if (event.getEventType() == EventType.COV_VOD_NO || event.getEventType() == EventType.COV_VOD_YES || 
					event.getEventType() == EventType.COV_UPDATE || event.getEventType() == EventType.TRAVEL_CHECK)
			{
				long duration = 0;
				if (mLastServiceStateChangeTimeStamp != 0)
				{
					duration  = System.currentTimeMillis() - mLastServiceStateChangeTimeStamp;
					mLastServiceStateChangeTimeStamp = System.currentTimeMillis();
					event.setDuration (duration);
				}
				else
					duration = mLastScreenOffDuration;

			}
			if (event.getEventType() == EventType.COV_3G_NO || event.getEventType() == EventType.COV_3G_YES || 
					event.getEventType() == EventType.COV_UPDATE || event.getEventType() == EventType.TRAVEL_CHECK)
			{
				long duration = 0;
				if (mLast3GStateChangeTimeStamp != 0)
				{
					duration  = System.currentTimeMillis() - mLast3GStateChangeTimeStamp;
					if (event.getEventType() == EventType.COV_UPDATE || event.getEventType() == EventType.TRAVEL_CHECK)
						event.setEventIndex(duration);
					else
						event.setDuration (duration);
					mLast3GStateChangeTimeStamp = System.currentTimeMillis();
				}
			}
			//context.getCellHistory ().snapshotHistory();
			String conn = context.getConnectionHistory().updateConnectionHistory(telephonyManager.getNetworkType(),
					telephonyManager.getDataState(), telephonyManager.getDataActivity(), context.getPhoneStateListener().getLastServiceStateObj(),context.getConnectivityManager().getActiveNetworkInfo());
			context.getIntentDispatcher().updateConnection (conn, true);
			
			WifiInfo wifiinfo = getWifiInfo ();
			WifiConfiguration wifiConfig = getWifiConfig ();
			event.setWifi(wifiinfo, wifiConfig);
			
			
			localReportEvent (event);
		} 
		catch (Exception e) 
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "signalSnapshot", "error", e);	
		}
	}
	
	
	private boolean bScreenIsOn = true;
	/*
	 * Prevent contributing to coverage % durations when screen is off
	 * Android doesnt properly detect outages when screen is off, so we dont want to count it as being in or out of coverage
	 * When screen is turned off for more than 2 minutes, set timer = 0 so events send duration = 0
	 * When screen is turned back on, set the current time, so events send durations back to the time screen was turned on
	 */
	public void screenChanged (boolean bScreenOn)
	{
		bScreenIsOn = bScreenOn;
		// If screen turns off and remains off for 2 minutes, and not on phone call, stop counting duration
		if (bScreenOn == false && !context.getPhoneStateListener().isCallConnected())
		{
			context.handler.postDelayed(new Runnable() {
				  @Override
				  public void run() {
					  // If screen is still off after 2 minutes, clear timer
					  if (bScreenIsOn == false)
					  {
						  if (mLastServiceStateChangeTimeStamp != 0)
							  mLastScreenOffDuration = System.currentTimeMillis() - mLastServiceStateChangeTimeStamp;
						  mLast3GStateChangeTimeStamp = 0;
						  mLastServiceStateChangeTimeStamp = 0;
					  }
				  }
				}, 120000);
		}
		else   // if screen is on, and times were cleared because of screen off, start duration time again
		{
			if (mLastServiceStateChangeTimeStamp == 0)
				mLastServiceStateChangeTimeStamp = System.currentTimeMillis();
			if (mLast3GStateChangeTimeStamp == 0)
				mLast3GStateChangeTimeStamp = System.currentTimeMillis();
		}
	}
//	public void updateEventDBFieldWhere (String field, String value, String where, String[] whereArgs)
//	{
//		ContentValues values = new ContentValues();
//		values.put(field, value);
//		context.getContentResolver().update(TablesEnumOld.EVENTS.getContentUri(), values, where, whereArgs);
//	}
//	public void updateEventDBField (Uri uri, String feild, String value)
//	{
//		ContentValues values = new ContentValues();
//		values.put(feild, value);
//		context.getContentResolver().update(uri, values, null, null);
//	}
//	public void deleteEventDB (Uri uri, String feild, String value)
//	{
//		context.getContentResolver().delete(uri, null, null);
//	}
	
	public void dumpEvents ()
	{
		for (EventCoupleOld eventCouple: ongoingEventCouples)
		{
			if (eventCouple.getStopEvent() != null)
			{
				temporarilyStageEvent(eventCouple.getStopEvent(), eventCouple.getStartEvent(), null);
			}
		}
	}
	
	public void localReportEvent (EventOld event)
	{
		EventUploaderOld uploader = new EventUploaderOld(event, null, context, true);
		uploader.report(true, null);
	}
	/**
	 * This method temporarily stages the method so that incoming data (like signal strength) would be able 
	 * to associate itself with the staged event. The complimentary event is also stored alongside.
	 * @param event
	 * @param complimentaryEvent
	 */
	public void temporarilyStageEvent(EventOld event, EventOld complimentaryEvent, Location location){

		int satellites = 0;
		if (context.getGpsManager() != null && location != null){
				satellites = context.getGpsManager().getNumberOfSatellites();
		}
		if (event.getStageTimestamp() > 0)  // already staged
			return;

		if (location != null)
			event.setLocation(location, satellites);
		// If a connect event didnt get a good location before call disconnected, update its location
		if (complimentaryEvent != null && complimentaryEvent.getEventType().getGenre() == EventTypeGenreOld.START_OF_COUPLE && location != null && location.getAccuracy() < 100
					&& location.getTime()-120000 < complimentaryEvent.getEventTimestamp() && (complimentaryEvent.getLocation() == null || complimentaryEvent.getLocation().getAccuracy() > 100))
			complimentaryEvent.setLocation(location, satellites);
		else if (complimentaryEvent != null && complimentaryEvent.getEventType().getGenre() == EventTypeGenreOld.START_OF_COUPLE && location != null 
				&& location.getTime()-120000 < complimentaryEvent.getEventTimestamp() && complimentaryEvent.getLocation() == null)
			complimentaryEvent.setLocation(location, satellites);
		
		//if (event.getEventType() == EventType.APP_MONITORING)
		//	return;  // let APP_MONITORING be expicitly unstaged

		// start a an uploader for local reporting at the first fix 
		if (!event.getEventType().waitsForSpeed())// != EventType.MAN_SPEEDTEST && event.getEventType() != EventType.APP_MONITORING)
		{
			EventUploaderOld uploader = new EventUploaderOld(event, null, context, true);
			uploader.report(true, location);
		}
		// If Manual tracking (drive test) is in progress, immediately upload the new event on first fix, and resume the tracking
		if (stagedEvent != null && stagedEvent.getEventType() == EventType.MAN_TRACKING && event.getEventType() != EventType.MAN_TRACKING)
		{
			unstageAndUploadEvent(event, complimentaryEvent);
			return;
		}
		else if (event.getEventType().waitsForSpeed() && (event.getDownloadSpeed() != 0 || location == null))
		{
			//if (stagedEvent == null)
			//	stagedEvent = event;
			unstageAndUploadEvent(event, null);
			return;
		}
		//now unstage the previously staged event (if any).
		else if (stagedEvent != null)
		{
			unstageAndUploadEvent(stagedEvent, stagedComplimentaryEvent);
		}
		
		//first disable the timer as it will be reset soon (if necessary)
		if (stageTimerTask != null){
			stageTimerTask.cancel();
		}	
		//now stage the new event and its complimentary
		stagedEvent = event;
		stagedComplimentaryEvent = complimentaryEvent;
		
		if (event.getEventType().getPostEventStageTime() == 0 || !context.isServiceRunning() || event.gpsListener == null)
		{
			unstageAndUploadEvent(event, complimentaryEvent);
			stagedEvent = null;
			stagedComplimentaryEvent = null;
			return;
		}
		//now set the timer to schedule the newly-staged-event's un-staging
		stageTimerTask = new StageTimerTask(event, complimentaryEvent);
		int stageTime = event.getEventType().getPostEventStageTime();
		if ((event.getEventType() == EventType.EVT_CONNECT || event.getEventType() == EventType.SIP_CONNECT) && context.getUsageLimits().getUsageProfile() == 0)
			stageTime = 60000;
		if ((event.getEventType() == EventType.EVT_CONNECT || event.getEventType() == EventType.SIP_CONNECT) && context.getUsageLimits().getUsageProfile() == 1)
			stageTime = 900000;
		stageTimer.schedule(stageTimerTask, stageTime);	
		
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "temporarilyStageEvent", "staged event type=" + event.getEventType().getEventName() + " id=" + event.getUri().getLastPathSegment());
	}
	
	
	/**
	 * This method uses the {@link #eventCache} to check whether the event is currently running.
	 * @param startEventType
	 * @return
	 */
	public boolean isEventRunning(EventType startEventType){
		return eventCache.contains(startEventType);
	}

	public boolean isEventRunning(int iEventType){
		EventType eventType = EventType.get(iEventType);
		return eventCache.contains(eventType);
	}
	
	/**
	 * This method uses the {@link #eventCache} to check whether the event is currently running.
	 */
	public boolean areEventsRunning(){
		return !eventCache.isEmpty();
	}
	
	/**
	 * Returns the event couple with the matching start event type and stop event type.
	 * @param startEventType
	 * @param stopEventType
	 * @return
	 */
	public EventCoupleOld getEventCouple(EventType startEventType, EventType stopEventType){
		for (EventCoupleOld item : ongoingEventCouples){
			if (item.getStartEventType() == startEventType && item.getStopEventType() == stopEventType)
				return item;
		}
		return null;
	}
	
	/**
	 * This method "un-stages" the currently staged event and then attempts to upload that event
	 * to the MMC server. Although, if the staged event is null, then this method just returns.
	 */
	public EventOld unstageAndUploadEvent(EventOld event, EventOld complimentaryEvent){
		//EventOld event = stagedEvent;
		try
		{
			//synchronized (this)    // removed synchronized because this relies on passed arguments rather than global stagedEvent
			{
				if (event == null)
				{
					MMCLogger.logToFile(MMCLogger.Level.ERROR, "EventManager", "unstageAndUploadEvent", "stagedEvent=null" );
					return null;
				}
				
				// Just don't remove a start-of-couple event until the end-of-couple is removed
				if(event.getEventType().getGenre() == EventTypeGenreOld.SINGLETON || event.getEventType().getGenre() == EventTypeGenreOld.END_OF_COUPLE) {
					eventCache.remove(event.getEventType());
				}
				
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, "EventManager", "unstageAndUploadEvent", "event=" + event.getEventType() );
				event.setStageTimestamp (System.currentTimeMillis());
				event.setFlag (EventOld.SERVER_READY, true);
				MMCService.getGpsManager().unregisterListener(event.gpsListener);
				// dont upload an event if in roaming
				final EventUploaderOld uploader = new EventUploaderOld(event, complimentaryEvent, context, false);
				if (uploader.event != null)
				{
					
					if (context.isServiceRunning())
					{
						new Thread((Runnable)uploader, EventUploaderOld.class.getSimpleName()).start();
					}
					else
						uploader.run();
				}
				if (stagedEvent == event)
					stagedEvent = null;
				if (stagedComplimentaryEvent == complimentaryEvent)
					stagedComplimentaryEvent = null;
				
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "unstageAndUploadEvent", "error", e);	
		}
		return event;
	}
	
	/**
	 * This method "un-stages" the currently staged event, so it stops capturing data
	 * received from the listeners, and removes it from the list of currently running events.
	 * This does not upload the event.
	 */
	public void unstageEvent(EventOld event) {
		
		if (stagedEvent == event)
			stagedEvent = null;
		if (event == null)
			return;
		event.setStageTimestampToNow();
		MMCService.getGpsManager().unregisterListener(event.gpsListener);
		eventCache.remove(event.getEventType());
	}
	
	public void cancelCouple (EventCoupleOld cancelCouple)
	{
		ongoingEventCouples.remove(cancelCouple);
	}
	
	/**
	 * This is the timer task that the stage timer invokes (when necessary) to "un-stage"
	 * an event. In addition, this task also removes the event from the {@link #eventCache}
	 * and attempts to upload the event to the MMC Server.
	 */
	class StageTimerTask extends TimerTask {
		EventOld thisevent, complimentaryEvent;
		public StageTimerTask (EventOld evt, EventOld cevt)
		{
			thisevent = evt;
			complimentaryEvent = cevt;
		}
		@Override
		public void run() {
			
			EventOld event = unstageAndUploadEvent(thisevent, complimentaryEvent);
			
		}
	}
	
	/**
	 * This is the timer task that the upload timer invokes to upload the event to the server
	 * using the provided {@link EventUploaderOld}.
	 * @author Abhin
	 *
	 */
	
	class UploadTimerTask extends TimerTask {
		private EventUploaderOld eventUploader;
		
		public UploadTimerTask (EventUploaderOld _eventUploader){
			this.eventUploader = _eventUploader;
		}
		
		@Override
		public void run() {
			new Thread((Runnable)eventUploader, UploadTimerTask.class.getSimpleName()).run();
		}
	}

	public void sendQueuedEvents() {
		// run an event uploader thread with a null event
		// this will simply check the queue and send any events now eligable to be sent
		EventUploaderOld uploader = new EventUploaderOld(null, null, context, false);
		uploadTimer.schedule(new UploadTimerTask(uploader), 20000);
	}

	public static ConcurrentLinkedQueue<EventDataEnvelope> getEventQueue() {
		return eventQueue;
	}

	public static void setEventQueue(ConcurrentLinkedQueue<EventDataEnvelope> eventQueue) {
		EventManagerOld.eventQueue = eventQueue;
	}

	public WifiInfo getWifiInfo ()
	{
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		
		return wifiInfo;
	}
	
	
	public WifiConfiguration getWifiConfig ()
	{
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		WifiConfiguration activeConfig = null;
		if (wifiManager.getConfiguredNetworks() == null)
			return null;
		
		for ( WifiConfiguration conn: wifiManager.getConfiguredNetworks())
		{
			if(( conn.BSSID != null && conn.BSSID.equals( wifiInfo.getBSSID() )) || (conn.SSID != null && conn.SSID.equals( wifiInfo.getSSID() )))
	        {
	            activeConfig = conn;
	            break;
	        }
		}
		if (activeConfig != null)
		{
			return activeConfig;
		}
		return null;
	}

	class ActiveTestItem {
		public int trigger;
		public EventType eventType;

		public  ActiveTestItem(EventType _eventType, int _trigger) {
			eventType = _eventType;
			trigger = _trigger;
		}
	}
	private ConcurrentLinkedQueue<ActiveTestItem> mTestQueue = new ConcurrentLinkedQueue<ActiveTestItem>();
	private Thread testQueueThread = null;
	private static CountDownLatch testingLatch = null;
	public void setSpeedtestUrls(String downloadUrl, String uploadUrl, String latencyUrl) {
		mSpeedTestTrigger.setCarrierDownloadUrl(downloadUrl);
		mSpeedTestTrigger.setCarrierUploadUrl(uploadUrl);
		mSpeedTestTrigger.setCarrierLatencyUrl(latencyUrl);
	}

	public void handleSMSDeliverNotification(int smsID, Long deliveryTime)
	{
		mSpeedTestTrigger.handleSMSDeliveryNotification(deliveryTime, smsID);
	}

	public void receivedSMSTestReply(JSONObject smsMsg) throws JSONException
	{
		try{
			String smsID = smsMsg.getString("smsID");
			String smsServerSentTime = smsMsg.getString("serverSendTime");
			long deliveredTime = PreferenceManager.getDefaultSharedPreferences(context).getLong(PreferenceKeys.Miscellaneous.SMS_DELIVERY_TIME, 0);
			PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PreferenceKeys.Miscellaneous.SMS_DELIVERY_TIME, 0).commit();

			mSpeedTestTrigger.handleSMSReply(smsID,"0", smsServerSentTime, String.valueOf(deliveredTime));
		}
		catch (Exception e){
			System.out.println (e);
		}
	}

	public boolean queueActiveTest (EventType eventType, int trigger)
	{
		if (!MMCCommand.isEventEnabled(context, eventType) || !MMCCommand.isEventPermitted(context, eventType, trigger))
			return false; // Event has not been enabled by server

		// If no test is in progress, run now
		// Add test test to the queue, waiting for tests in progress
		if (mTestQueue.size() < 12 && context.getPhoneStateListener().isCallConnected() == false && context.getPhoneStateListener().isCallDialing() == false)
			mTestQueue.add(new ActiveTestItem(eventType, trigger));
		else {
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "skipping active test");
			return false;
		}
		// Ensure one worker thread to run each test in the queue, then the thread ends when empty
		if (testQueueThread == null) {
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "begin worker thread");
			testQueueThread = new Thread() {
				public void run() {
					while (!mTestQueue.isEmpty()) {
						if (testingLatch != null)
							try {
								boolean res = testingLatch.await(500, TimeUnit.SECONDS);
							} catch (InterruptedException e) {
								MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "testingLatch InterruptedException", e);
							}
						testingLatch = null;
						ActiveTestItem item = mTestQueue.poll();

						if (item.trigger == 2 && context.isInTracking() == false)
						{
							MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "drive test expired before test completed:" + item.eventType.toString());
							continue;
						}
						triggerActiveTest (item.eventType, item.trigger);
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "trigger test from queue:" + item.eventType.toString());
					}
					testQueueThread = null;
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "queueActiveTest", "end worker thread");
				}
			};
			testQueueThread.start();
		}
		return true;
	}

	// Each test must call this when finsihed to allow next test to start
	public void setActiveTestComplete (int evtType)
	{

		if (testingLatch != null)
		{
			testingLatch.countDown();
		}

		if (mTestQueue.isEmpty() && context.getTrackingManager().isAdvancedTrackingWaiting())
		{

			context.getTrackingManager().runAdvancedTrackingTests();
		}

		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "setActiveTestComplete", "test=" + evtType + ", latch=" + testingLatch);


	}

	/**
	 * 'Update' or Check-In Event called every few hours
	 * @param isCheckin
	 * @param isStartup
	 */
	public void triggerUpdateEvent(boolean isCheckin, boolean isStartup){
		EventOld event;

		if (!context.getResources().getBoolean(R.bool.ALLOW_CHECK_INS) && isCheckin)
			event = new EventOld(EventType.COV_UPDATE);
		else
			event = triggerSingletonEvent(EventType.COV_UPDATE);

		event.isCheckin = isCheckin;
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "triggerUpdateEvent", "starting ");
		String reason = "";
		if (isCheckin)
		{
			reason = "periodic 3 hour check in";
			context.pruneDB();
			context.getCellHistory().clearCellHistory();
			context.getConnectionHistory().clearHistory();
		}
		else if (isStartup)
			reason = "checking in on startup";
		else
			reason = "user triggered update";
		if (!isStartup)
			context.verifyRegistration ();
		String apikey = context.getApiKey(context);

		PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PreferenceKeys.Miscellaneous.LAST_TIME, System.currentTimeMillis()).commit();

	}
	private void triggerActiveTest (final EventType eventType, final int trigger)
	{
		testingLatch = new CountDownLatch(1);
		// trigger the test on the main thread and it will run a background task
		context.handler.post(new Runnable() {
			// @Override
			public void run() {

				int iEventType = eventType.getIntValue();
				if (eventType == EventType.EVT_VQ_CALL)
					context.getVQManager().runTest(trigger);
				else if (eventType == EventType.MAN_SPEEDTEST)
					mSpeedTestTrigger.runTest(true, trigger, iEventType);
				else if (eventType == EventType.SMS_TEST)
					mSpeedTestTrigger.runTest(true, trigger, iEventType);
				else if (eventType == EventType.LATENCY_TEST)
					mSpeedTestTrigger.runTest(false, trigger, iEventType);
				else if (eventType == EventType.VIDEO_TEST)
					mVideoTestTrigger.runTest(true, trigger, iEventType);
				else if (eventType == EventType.AUDIO_TEST)
					mVideoTestTrigger.runTest(true, trigger, iEventType);
				else if (eventType == EventType.WEBPAGE_TEST)
					mVideoTestTrigger.runTest(true, trigger, iEventType);
				else if (eventType == EventType.YOUTUBE_TEST)
					mVideoTestTrigger.runTest(true, trigger, iEventType);
			}
		});
	}

	public void triggerSpeedTest(int trigger){
		mSpeedTestTrigger.runTest(true, trigger, EventType.MAN_SPEEDTEST.getIntValue());
	}

	public void triggerSMSTest(int trigger){
		mSpeedTestTrigger.runTest(true, trigger, EventType.SMS_TEST.getIntValue());
	}

	public void triggerFillinEvent() {
		triggerSingletonEvent(EventType.EVT_FILLIN);
	}

	public String runLatencyTest(boolean background)  {
		String latency = "-1";
		try {
//			latency =  String.valueOf(mSpeedTestTrigger.updateTestLatency());
			int trigger = 0;
			if (background == true)
			{
				String reason = null;
				int allow = PreferenceManager.getDefaultSharedPreferences(context).getInt(PreferenceKeys.Miscellaneous.AUTO_CONNECTION_TESTS, 1);

				if (context.isNetworkWifi())
					reason = "on wifi";
				if (!context.isOnline())
					reason = "offline";
				if (allow != 1 && !MMCLogger.isDebuggable())
					reason = "not enabled";
				if (context.getPhoneStateListener().isCallConnected() || context.getPhoneStateListener().isCallDialing() == true)
					reason = "phone call";
				if (context.getUsageLimits().getUsageProfile () <= UsageLimits.MINIMAL)
					reason = "minimal";
				if (context.getUsageLimits().getDormantMode () >= 1)
					reason = "dormant";
				if (reason != null)
				{
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "runLatencyTest cancelled ", reason);
					return reason;
				}

				trigger = 1;
			}
			final int ftrigger = trigger;
			context.handler.post(new Runnable() {
				// @Override
				public void run() {
					mSpeedTestTrigger.runTest(false, ftrigger, EventType.LATENCY_TEST.getIntValue());
				}
			});
		} catch (Exception e) {
			latency = e.toString();
		}
		return latency;
	}

	public void killSpeedTest(){
		MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "killSpeedTest", "calling mSpeedTestTrigger.killTest()");
		mSpeedTestTrigger.killTest();
	}

	public void killActiveTest(int testType){
		MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "killVideoTest", "calling mVideoTestTrigger.killTest()");
		mVideoTestTrigger.killTest(testType);
	}

	public UsageLimits getUsageLimits ()
	{
		return context.getTravelDetector().getUsageLimits();
	}

	/**
	 * This class encapsulates the data and the logic for gps management for the {@link MMCService} service.
	 * This class is intended to be used for turning on the gps when an event takes place.
	 */
	public class GpsListenerForEvent extends GpsListenerOld {
		public static final int MIN_SATELLITE_COUNT_FOR_FF_TIMEOUT = 3;
		public static final int MIN_BATTERY_LEVEL_FOR_FF_TIMEOUT = 20;	//in percentage
		public static final int DEFAULT_FF_TIMEOUT_EXTENSION = 90000;	//in milliseconds
		public EventOld thisEvent;
		public EventOld complimentaryEvent;
		private long tmNeighborUpdate = 0;

		public GpsListenerForEvent(EventOld event, EventOld compEvent){
			super("GpsListenerForEvent");
			this.thisEvent = event;
			this.complimentaryEvent = compEvent;

			//now adjust the operationTimeout for the GpsListener
			//if (compEvent == null || compEvent.getUri() == null){
			int stageTime = event.getEventType().getPostEventStageTime();
			if (event.getEventType() == EventType.EVT_CONNECT || event.getEventType() == EventType.SIP_CONNECT)
			{
				if (getUsageLimits().getUsageProfile() == 0)
					stageTime = 60000;
				else if (getUsageLimits().getUsageProfile() == 1)
					stageTime = 600000;

				this.setFirstFixTimeout(stageTime);
			}
			if (getUsageLimits ().exceededGps (event.getIEventType(), true))
			{
				this.setFirstFixTimeout(20000);
			}

		}

		/**
		 * If the number of satellites is greater than or equal to {@value #MIN_SATELLITE_COUNT_FOR_FF_TIMEOUT}
		 * and if the battery level is above {@value #MIN_BATTERY_LEVEL_FOR_FF_TIMEOUT} percent, then renew the
		 * timeout by {@value #DEFAULT_FF_TIMEOUT_EXTENSION} milliseconds.
		 */
		@Override
		public int attemptToRenewFirstFixTimeout(int numberOfSatellites) {
			if (numberOfSatellites > MIN_SATELLITE_COUNT_FOR_FF_TIMEOUT && DeviceInfoOld.battery > 20){
				if (this.getFirstFixTimeout() <= 30000)
					return 30000;
				return DEFAULT_FF_TIMEOUT_EXTENSION;
			}
			return 0;
		}

		/**
		 * For events, the chaining property of the GpsManager is not utilised. Instead, we rely on the timeout
		 * to stop the gps for us. Therefore, after processing the location, we simply return true.
		 */
		@Override
		public boolean onLocationUpdate(Location location) {

			if (location != null)
				this.setLastLocation (location);

			//lastSatellites = getGpsManager().getNumberOfSatellites();
			context.processNewFilteredLocation(location, context.getGpsManager().getNumberOfSatellites(), context);
			if (location != null && location.getAccuracy() < GpsManagerOld.LOCATION_UPDATE_MIN_EVENT_ACCURACY)  // We'll settle for 75 meters to go in the database, but only <45 goes in the trend string
			{
				// Poor accuracy GPS will be stored in the table, but event will only use it if no accurate fix was obtained, and it wont go in trend
				context.setLastLocation (location);
				context.setLastSatellites (context.getGpsManager().getNumberOfSatellites());

			}
			// abort initial travelling event if last location is close to this location
			if (thisEvent.getEventType() == EventType.TRAVEL_CHECK)
			{
				if (!context.getTravelDetector().confirmTravelling(location))
				{
					unstageEvent(thisEvent);
					return false;
				}
			}
			if (location != null)
				stageEventIfNeeded(location);

			// Can we force extra neighbor list updates by requesting more Cell Locations?
			if (tmNeighborUpdate + 15000 < System.currentTimeMillis())
			{
				CellLocation.requestLocationUpdate();
				//cellHistory.updateNeighborHistory (null, null);
				tmNeighborUpdate = System.currentTimeMillis();
			}
			return true;
		}

		/**
		 * Even if a timeout occurs, it is important that the event be staged and sent to the server.
		 */
		@Override
		public void onTimeout() {
//			if(thisEvent.getEventType() == EventType.MAN_TRACKING) {
//				//if we cannot get a location for a manual tracking event, it should not be sent to the server
//				eventManager.unstageEvent(thisEvent);
//			}
//			else
			{
				//we don't have a location, but stage the event regardless
				thisEvent.gpsListener = null;
				stageEventIfNeeded(null);
				getUsageLimits().addGpsFails();
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, "GpsListenerForEvent", "onTimeout", "startingEvent type=" + startingEvent.getEventType() + " id=" + startingEvent.getUri().getLastPathSegment() + "endingEvent " + (endingEvent==null ? "null" : "id=" + endingEvent.getEventType() + " id=" + (endingEvent.getUri()==null ? "null" : endingEvent.getUri().getLastPathSegment())));
			}
		}

		private void stageEventIfNeeded(Location location){

			// Sfaegaurd. if any event other than phone connect is still runnign after 10 minutes, kill by unstaging it
			if (thisEvent.getEventTimestamp() + 10*60000 < System.currentTimeMillis() && thisEvent.getEventType() != EventType.EVT_CONNECT && thisEvent.getEventType() != EventType.SIP_CONNECT)
			{
				MMCLogger.logToFile(MMCLogger.Level.ERROR, "GpsListenerForEvent", "KILLED event running longer than 10 minutes", "event=" + thisEvent.getEventType() );
				unstageEvent(thisEvent);
				return;
			}

			// If this is a speed test, return unless ready with a download speed and a good enough location
			if (thisEvent.getEventType().waitsForSpeed())
				if ((thisEvent.getDownloadSpeed() == 0 && thisEvent.getLatency() == 0 && thisEvent.getDuration() == 0) || (location != null && location.getAccuracy() > 100))
					return;
//			//if the location exists but is not good enough, then don't stage the event just yet
				else if (location != null && location.getAccuracy() > GpsManagerOld.LOCATION_UPDATE_MIN_TREND_ACCURACY)
					return;	//staging is not needed just yet

			if (thisEvent.getStageTimestamp() == -1L){
				//thisEvent.setStageTimestampToNow();
				temporarilyStageEvent(thisEvent, complimentaryEvent, location);
				thisEvent.setStageTimestampToNow();
				// When a travel_check has been confirmed, might run a latency test too
				if (thisEvent.getEventType() == EventType.TRAVEL_CHECK && location != null)
				{
					runLatencyTest(true);
				}
			}

		}
		@Override
		public void gpsStarted ()
		{
			// Wake the device while Gps is recording an event so that we get signal updates
			// But not for travelling check events (unless they lead to fill-ins)
			//if (startingEvent != null && startingEvent.getEventType() != EventType.TRAVEL_CHECK)
			//keepAwake (true);

			context.clearLastGoodLocation();  // clear last known location when the gps starts, so that if the gps fails to get a fix, we know it failed afterward
			context.updateNumberOfSatellites(0, 0);
		}
		@Override
		public void gpsStopped ()
		{
		}
		public EventOld getEvent ()
		{
			return thisEvent;
		}
	}

	/**
	 * This class encapsulates the data and the logic for gps management for the {@link MMCService} service.
	 * This class is intended to be used for turning on the gps when an event takes place.
	 */
	class LocationListenerForEvent extends GpsListenerOld {
		EventOld event = null;
		public LocationListenerForEvent(EventOld _event){
			super("LocationListenerForEvent");
			setProvider( LocationManager.NETWORK_PROVIDER);
			this.setFirstFixRenewalAllowed(false);
			this.setFirstFixTimeout(30000);
			event = _event;
		}
		/**
		 * Store the coarse location in the location database
		 */
		@Override
		public boolean onLocationUpdate(Location location)
		{
			// Don't mistake this for a good GPS fix for an event, it often understates the network location error
			// Prevents the location from travel detection or fill-ins
			if (location != null && location.getAccuracy() < GpsManagerOld.LOCATION_UPDATE_MIN_EVENT_ACCURACY)
				location.setAccuracy(GpsManagerOld.LOCATION_UPDATE_MIN_EVENT_ACCURACY+1);
			if (location != null && location.getAccuracy() == 0 )
				location.setAccuracy(1);
			context.setLastLocation(location);
			location.setTime(System.currentTimeMillis());
			int satellites = context.getGpsManager().getNumberOfSatellites();
			context.setLastSatellites(satellites);
			// Poor accuracy GPS will be stored in the table, but event will only use it if no accurate fix was obtained, and it wont go in trend
			ContentValues values = ContentValuesGeneratorOld.generateFromLocation(location, 0, satellites);
			context.getDBProvider().insert(TablesEnumOld.LOCATIONS.getContentUri(), values);

			if (event.getLocation() == null && location != null)
				event.setLocation(location, context.getGpsManager().getNumberOfSatellites());

			if (event.getLocalID() > 0 && location != null)
			{
				context.getReportManager().updateEventField (event.getLocalID(), LocalStorageReporter.Events.KEY_LATITUDE, String.valueOf(location.getLatitude()));
				context.getReportManager().updateEventField (event.getLocalID(), LocalStorageReporter.Events.KEY_LONGITUDE, String.valueOf(location.getLongitude()));
			}

			return false;  // stop listening after the first network location
		}

		@Override
		public void gpsStarted ()
		{
		}
		@Override
		public void gpsStopped ()
		{
		}
	}
}
