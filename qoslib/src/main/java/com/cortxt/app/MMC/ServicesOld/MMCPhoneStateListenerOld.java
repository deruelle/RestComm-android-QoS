package com.cortxt.app.MMC.ServicesOld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.media.audiofx.BassBoost.Settings;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoLte;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.Time;
import android.util.Log;

import com.cortxt.app.MMC.ActivitiesOld.NerdScreen;
import com.cortxt.app.MMC.ContentProviderOld.ContentValuesGeneratorOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesEnumOld;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.RestCommManager;
import com.cortxt.com.mmcextension.PhoneHeuristic;
import com.cortxt.com.mmcextension.utils.PreciseCallCodes;
import com.cortxt.com.mmcextension.utils.TimeDataPoint;
import com.cortxt.com.mmcextension.utils.TimeSeries;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.LocalStorageReporter.LocalStorageReporter.Events;
import com.cortxt.app.MMC.ServicesOld.Buffers.MMCCellLocationOld;
import com.cortxt.app.MMC.ServicesOld.Buffers.MMCSignalOld;
import com.cortxt.app.MMC.ServicesOld.Events.EventCoupleOld;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.DataMonitorStats;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
//import android.telephony.CellInfo;
import com.cortxt.com.mmcextension.datamonitor.AppDataStatisticsRunnable;
import com.cortxt.com.mmcextension.UsageLimits;

import org.json.JSONObject;

/**
 * @author abhin
 * This is the class that MMC_Service instantiates and registers as 
 * a phone state listener for the following events
 * <ol>
 * 	<li>PhoneStateListener.LISTEN_CALL_STATE</li>
 * 	<li>PhoneStateListener.LISTEN_CELL_LOCATION</li>
 * 	<li>PhoneStateListener.LISTEN_DATA_ACTIVITY</li>
 * 	<li>PhoneStateListener.LISTEN_DATA_CONNECTION_STATE</li>
 * 	<li>PhoneStateListener.LISTEN_SERVICE_STATE</li>
 * 	<li>PhoneStateListener.LISTEN_SIGNAL_STRENGTHS</li>
 * </ol>
 */
public class MMCPhoneStateListenerOld extends PhoneStateListener {
	private final AppDataStatisticsRunnable dataActivityRunnable;
	private MMCService owner;
	private DataMonitorStats dataMonitorStats;
	private RestCommManager restcommManager;
	public boolean validSignal = false;
	public static final String TAG = MMCPhoneStateListenerOld.class.getSimpleName();
	/**
	 * If a call ends and the last signal strength read was below this number
	 * and if that signal strength was recorded in the last {@link #SIGNAL_STRENGTH_EXPIRY_FOR_DROPPED_CALL}
	 * milliseconds, then the call is declared a "dropped call".
	 */
	public static final int SIGNAL_STRENGTH_THRESHOLD_FOR_DROPPED_CALL = -115;
	/**
	 * If a call ends and the last signal strength is below {@link #SIGNAL_STRENGTH_THRESHOLD_FOR_DROPPED_CALL}
	 * dbm and if the signal strength was recording was not older than this number
	 * (in milliseconds), then the call is declared a "dropped call".
	 */
	public static final int SIGNAL_STRENGTH_EXPIRY_FOR_DROPPED_CALL = 30000;
	/**
	 * If a call ends and the a cell ID was changed within this number of
	 * milliseconds in the past, then the call is flagged as a potential
	 * dropped call.
	 */
	public static final int CELL_LOCATION_EXPIRY_FOR_DROPPED_CALL = 5000;
	
	/*
	 *  NETWORK_TYPE values that don't or may not exist in our lower version of the API, these can still be returned by the phone
	 *  
	 */
	public static final int NETWORK_NEWTYPE_EVDOB = 12;
	public static final int NETWORK_NEWTYPE_LTE = 13;
	public static final int NETWORK_NEWTYPE_EHRPD = 14;
	public static final int NETWORK_NEWTYPE_HSPAP = 15;
	public static final int NETWORK_NEWTYPE_GSM = 16;
	public static final int NETWORK_NEWTYPE_TD_SCDMA = 17;
	public static final int NETWORK_NEWTYPE_IWLAN = 18;

	public static final int NETWORK_NEWTYPE_WIFI = 100;

	public static final int LISTEN_VOLTE_STATE =        0x00004000;
	public static final int LISTEN_OEM_HOOK_RAW_EVENT = 0x00008000;
	public static final int LISTEN_PRECISE_CALL_STATE = 0x00000800;

	public static final int SERVICE_STATE_AIRPLANE = 9;

	public static final int TYPE_WIMAX = 6;
	public static final int TYPE_ETHERNET = 9;
	public static final int TYPE_BLUETOOTH = 7;

	public static final int MMC_DROPPED_NOTIFICATION = 1001;


	//other variables
	/**
	 * This variable stores a copy of the previously received network generation.
	 * This is used so that if the phone changes from one network type
	 * to another within the same network generation, an event isn't generated.
	 * 
	 * Note: This was used first in Rogers networks where the data connection
	 * swtiches very rapidly between UMTS and HSPA.
	 */
	private int previousNetworkTier = -1;
	private int previousNetworkType = -1;
	/**
	 * This variable stores a copy of the previously received network state.
	 */
	public int previousNetworkState = -1;
	private int previousServiceState = -1;
	private ServiceState previousServiceStateObj = null;
	private int previousServiceStateAirplane = 99;
	public static TelephonyManager telephonyManager;
	
	private long disconnectTime = 0, offhookTime = 0, timeLTEOutage = 0;  // to undo LTE outages due to phone calls
	private String heurCause = null;
	private Handler dataActivtyHandler;

	// keeps track of whether a global ServiceMode Panel has been manually closed by the user, its displayed during ServiceMode in debug
	public static boolean closedServicePanel = false;
	private boolean lastCallDropped = false;
	private String lastDroppedCause = "";
	private boolean bOffHook = false;
	private static JSONObject mServicemode = null;
	private String prevSvcValues = "";
	private int networkType = TelephonyManager.NETWORK_TYPE_UNKNOWN;
	private boolean callConnected = false, callDialing = false, callRinging = false;
	private long timeConnected = 0, timeRinging = 0, timeDialed = 0;
	private int lastKnownCallState;
	/**
	 * Constructor that gets a copy of the owner object so that it can 
	 * manipulate the variables of the owner.
	 */
	public MMCPhoneStateListenerOld(MMCService owner) {
		this.owner = owner;
		telephonyManager = (TelephonyManager)owner.getSystemService(Context.TELEPHONY_SERVICE);
		restcommManager = new RestCommManager(this, owner);
		dataActivtyHandler = new Handler();
		dataActivityRunnable = new AppDataStatisticsRunnable(owner.getExtensionManager(), dataActivtyHandler);
		mySensorManager = (SensorManager)owner.getSystemService(
				owner.SENSOR_SERVICE);

		// Proximity sensor code exists in case we want to go back to blacking out screen and forcing screen on during phone calls
        //myProximitySensor = mySensorManager.getDefaultSensor(
        //		Sensor.TYPE_PROXIMITY);

	}

	//last data caches
	private MMCCellLocationOld lastKnownMMCCellLocation;
	private long tmLastCell = 0;


	protected ServiceState mLastServiceState;
	protected long mLastServiceStateChangeTimeStamp =0;
	protected long mLastDataNetworkChangeTimeStamp =0;

	protected boolean mStateWasPowerOff = false;
	private MMCSignalOld lastKnownMMCSignal, prevMMCSignal;
	private SignalStrength lastKnownSignalStrength;
	private long tmLastCellUpdate = 0;
	private String lastCellString = "";

	private SensorManager mySensorManager;
	private Timer disconnectTimer = new Timer ();
	private long totalRxBytes = 0, totalTxBytes = 0;


	/**
	 * When the cell location gets changed, the new cellId is added to the cell id buffer in the 
	 * owner. At the same time, the CELLCHANGE event is stored.
	 */
	@Override
	public void onCellLocationChanged(CellLocation location) {
		super.onCellLocationChanged(location);
			
		try {
            checkCDMACellSID (location);
			processNewCellLocation(new MMCCellLocationOld(location));
			
			// See if this cellLocation has inner GsmLocation
			checkInnerGsmCellLocation (location);

			
		} catch (InterruptedException intEx){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onCellLocationChanged", "InterruptedException: " + intEx.getMessage());
		}
		catch (Exception ex){
			String err = ex.toString();
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onCellLocationChanged", "InterruptedException: " + err);
		}
	}

	protected boolean proximityNear = false;
	protected boolean lastNear = false;

	SensorEventListener proximitySensorEventListener
	    = new SensorEventListener(){
		
	  @Override
	  public void onAccuracyChanged(Sensor sensor, int accuracy) {
	   
	  }
	
	  // Uses Proximity sensor during phone calls to force the screen on dim and black. 
	  // The only way to enable signal measurements during phone calls
	  @Override
	  public void onSensorChanged(SensorEvent event) {
	   // TODO Auto-generated method stub
	
	   if(event.sensor.getType()==Sensor.TYPE_PROXIMITY){
		   boolean bNearFace = event.values[0] < 1 ? true : false;
		   proximityNear = bNearFace;
		   // TODO Auto-generated method stub
		   //if (owner.bOffHook)
		   {
			   // For OS 4.1, we are able to hold the screen on during a call by coming to foreground
			//if (Build.VERSION.SDK_INT < 16)
			//		return;
			   // server can specify whether a confirmation can be invoked on a low rated potentially-dropped call
	           //int phoneScreen = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.SERVERPHONESCREEN_ENABLE, 0);
       		   //if (phoneScreen == 0)
       		//	   return;
			   
			   if (bNearFace && lastNear != bNearFace)
			   {
				   MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onSensorChanged", "launch black screen");
				   //TimerTask launchConnectTask = new LaunchConnectTask("mmc");
				   //launchTimer.schedule(launchConnectTask, 1000);
				   lastNear = true;
			   }
			   else if (!bNearFace && lastNear != bNearFace)
			   {
				   MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onSensorChanged", "launch phone screen");
				   //TimerTask launchConnectTask = new LaunchConnectTask("phone");
				   //launchTimer.schedule(launchConnectTask, 1);
				   lastNear = false;
			   }
			   
		   }
	   }
	  }
    };
    
	@Override
	public void onDataActivity(int data){
		super.onDataActivity(data);
		if (owner.getUsageLimits().getDormantMode() > 0)
			return;
		String activity = null;
		try
		{
			activity = owner.getConnectionHistory().updateConnectionHistory(telephonyManager.getNetworkType(), telephonyManager.getDataState(), telephonyManager.getDataActivity(), previousServiceStateObj, owner.getConnectivityManager().getActiveNetworkInfo());
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onDataActivity", "ex " + e.getMessage());
		}
		if (data == TelephonyManager.DATA_ACTIVITY_IN || data == TelephonyManager.DATA_ACTIVITY_INOUT)
		{
			if (activity != null) {
				owner.getIntentDispatcher().updateConnection(activity, true);
			}
			//User allows - default yes
			if(PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.User.PASSIVE_SPEED_TEST, true)) {
				//Don't allow if a speedtest is in progress
				if(PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.SPEEDTEST_INPROGRESS, false)) {
					return;
				}
				if(PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.VIDEOTEST_INPROGRESS, false)) {
					return;
				}
				if (owner.getUsageLimits().getUsageProfile () == UsageLimits.MINIMAL)
					return;
				//server allows - default no
				int allow = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.PASSIVE_SPEEDTEST_SERVER, 0);
				if(allow > 0) {
					dataThrougput();  
				}
			}				
		}
		else
		{
			if (activity != null) {
				owner.getIntentDispatcher().updateConnection(activity, false);
			}
		}

	}		
	
	public void dataThrougput() {
		synchronized(this) {
			totalRxBytes = TrafficStats.getTotalRxBytes();		
			totalTxBytes = TrafficStats.getTotalTxBytes();		
			if (dataActivityRunnable.hasDataActivity == 0) {
				//dataActivityRunnable.initializeHasDataActivity(1);
				dataActivityRunnable.init(totalRxBytes, totalTxBytes, true);
			}	
			else if (dataActivityRunnable.hasDataActivity == 1)
			{
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDataActivity", "in sampling");
			}
			else if (dataActivityRunnable.hasDataActivity == 2)
			{
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDataActivity", "already in download");
			}
		}
	}
	
	@Override
	public void onDataConnectionStateChanged(int state, int networkType){
		super.onDataConnectionStateChanged(state, networkType);
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDataConnectionStateChanged", String.format("Network type: %d, State: %d", networkType, state));
	
		//notify MMCService of the new network type
		updateNetworkType(networkType);
		
		int datastate = telephonyManager.getDataState();
		// disregard network change events if data is disabled or in airplane mode
		if (datastate == TelephonyManager.DATA_SUSPENDED || previousServiceState == SERVICE_STATE_AIRPLANE)  
			return;
		
		if (owner.ActiveConnection() > 1) {// 10=Wifi, 11=Wimax, 12=Ethernet, 0=other
			previousNetworkTier = -1; 
			return;		
		}
		
		// Ignore any data outages that occur just after turning screen off, these are probably not to be blamed on the carrier
		if (owner.getTravelDetector().getScreenOnTime(false) + 30000 > System.currentTimeMillis())
			return;
		
		try{

			String conn = owner.getConnectionHistory().updateConnectionHistory (networkType, state, telephonyManager.getDataActivity(), previousServiceStateObj, owner.getConnectivityManager().getActiveNetworkInfo());
			if (conn != null)
		   		owner.getIntentDispatcher().updateConnection(conn, false);
			
		} catch (Exception e) {}
		

		int networkGeneration = getNetworkGeneration(networkType);
		
		// The 3G outage will be handled by the Service state outage
		if (previousServiceState == ServiceState.STATE_OUT_OF_SERVICE || previousServiceState == ServiceState.STATE_EMERGENCY_ONLY)
			return; 
		//if the network generation hasn't changed, then don't cause an event
		if (previousNetworkTier == networkGeneration && previousNetworkState == state){
			return;
		}


		MMCSignalOld signal = getLastMMCSignal();
		if (signal != null)
		{
			signal.setTimestamp(System.currentTimeMillis());
			clearLastMMCSignal();  // to force a duplicate signal to be added
			processNewMMCSignal(signal);
		}
		//this was falsely reporting outages when screen turned off, and not coupling them to regained
		//if (datastate == TelephonyManager.DATA_DISCONNECTED)
		//	networkGeneration = 0;
		// First network state
		if (previousNetworkType == -1)
			previousNetworkType = networkType;
		else
		{
			switch (networkGeneration){
				case 3:	//3g
				case 4:	//3g
					stateChanged_3g(state);
					break;
				case 5:	//3g
					stateChanged_4g(state);
					break;
					
				case 1:
				case 2:	//2g
					stateChanged_2g(state);
					break;
					
				// disconnected data without disconnecting service?
				case 0:
					stateChanged_0g(state);
					break;
				
			}
		}
		//update the previous network generation and state
		if (state == TelephonyManager.DATA_CONNECTED && networkGeneration != 0)
			previousNetworkTier = networkGeneration;
		// If there is truly an outage, the service state listener will update the previousNetworkTier to 0
		previousNetworkState = state;
		previousNetworkType = networkType;
		
	}
	public void processLastSignal ()
	{
		if (lastKnownSignalStrength != null)
			onSignalStrengthsChanged(lastKnownSignalStrength);
	}

	public void onVoLteServiceStateChanged (Object lteState)
	{
		if (lteState != null)
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onVoLteServiceStateChanged", lteState.toString());
		else
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onVoLteServiceStateChanged", "null");
	}

	public void onOemHookRawEvent (byte[] oemData)
	{
		if (oemData != null)
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onOemHookRawEvent", "length = " + Integer.toString(oemData.length));
		else
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onOemHookRawEvent", "null");
	}

	public void onPreciseCallStateChanged (Object preciseCallState)
	{
		if (preciseCallState != null)
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onPreciseCallStateChanged", preciseCallState.toString());
		else
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onPreciseCallStateChanged", "null");
	}

	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength) {
		super.onSignalStrengthsChanged(signalStrength);
		if (owner.getPlatform() != 1)  //Not an Android device
			return;

		if (!owner.isMMCActiveOrRunning())
		{
			lastKnownSignalStrength = signalStrength;
			return;
		}
		lastKnownSignalStrength = null;
		//if (signalStrength != null)
		//	MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onSignalStrengthsChanged", signalStrength.toString());
		int pref = networkPreference(owner.getApplicationContext());
		try {
			if (previousServiceState == ServiceState.STATE_IN_SERVICE || previousServiceState == ServiceState.STATE_EMERGENCY_ONLY)
			{
				MMCSignalOld mmcSignal = new MMCSignalOld(signalStrength);
				processNewMMCSignal(mmcSignal);
				
			}
			else
			{
				MMCSignalOld mmcSignal = new MMCSignalOld();
				processNewMMCSignal(mmcSignal);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			List<CellInfo> cells = telephonyManager.getAllCellInfo();
			if (cells != null)
				onCellInfoChanged(cells);
//				for (int c =0; c<cells.size(); c++)
//				{ 
//					String msg =  "cells[" + c + "]=" + cells.get(c).toString();
//					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onSignalStrengthsChanged", "cells[" + c + "]=" + cells.get(c).toString());
//					//Log.d(TAG, "cells[" + c + "]=" + cells.get(c).toString());
//				}
			}
			
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onSignalStrengthsChanged", "Exception " + e.getMessage());
		}
	}

	// delay by 1 second so that it can check call log if needed to verify call connected
	class VerifyConnectTask extends TimerTask {
		
		@Override
		public void run() {
			if(owner.getPlatform() == 3) {
				return;
			}
			EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
			
			MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "VerifyConnectTask", "call connected=" + isCallConnected() + " event=" + targetEventCouple);

            String pname = owner.getPackageName();
            int permissionForReadLogs = owner.getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS, pname); //0 means allowed
            int permissionForPrecise = owner.getPackageManager().checkPermission("android.permission.READ_PRECISE_PHONE_STATE", pname); // 0 means allowed

            // For OS 4.1, need to use CALL LOG rather than logcat to determine if call connected or not
			if (targetEventCouple != null && Build.VERSION.SDK_INT >= 16 && !isCallConnected())//permissionForReadLogs != 0 && permissionForPrecise != 0 ) // && !owner.isCallConnected())  //  && !owner.getUsageLimits().getUseRadioLog())
				checkCallLog();

			if (lastCallDropped == true)
			{ 
				lastCallDropped = false;
				if (targetEventCouple == null)
				{
					EventOld evt = owner.getEventManager().triggerSingletonEvent(EventType.EVT_CALLFAIL);
					popupDropped(EventType.EVT_CALLFAIL, 5, evt.getLocalID());
					evt.setCause (lastDroppedCause);
					evt.setEventTimestamp(disconnectTime);
				}
				else if (isCallConnected() && targetEventCouple != null)
				{
					int rating = 7;
					if (lastDroppedCause.equals("error_unspecified"))
						rating = 5;
					
					targetEventCouple.setStopEventType(EventType.EVT_DROP);
					owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_DROP);
					
					popupDropped(EventType.EVT_DROP, rating, targetEventCouple.getStopEvent().getLocalID());
					targetEventCouple.getStopEvent().setCause (lastDroppedCause);
					targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
					//owner.getEventManager().updateEventDBField(targetEventCouple.getStopEvent().getUri(), TablesOld.Events.TIMESTAMP, Long.toString(disconnectTime));
				}
				else if (targetEventCouple != null)
				{
					//EventOld evt = owner.triggerSingletonEvent(EventType.EVT_CALLFAIL);
					targetEventCouple.setStopEventType(EventType.EVT_CALLFAIL);
					owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_CALLFAIL);
					popupDropped(EventType.EVT_CALLFAIL, 5, targetEventCouple.getStopEvent().getLocalID());
					targetEventCouple.getStopEvent().setCause (lastDroppedCause);
					targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
					//owner.getEventManager().updateEventDBField(evt.getUri(), TablesOld.Events.TIMESTAMP, Long.toString(disconnectTime));
				}
				setCallConnected (false);
				lastCallDropped = false;
				if (!bOffHook)
				{
					setCallDialing (false);
				}
			}
			else if (disconnectTime - offhookTime < 2000 )   // if connect wasnt detected, use the time the call was dialed
			{	
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "off hook too short: " + (disconnectTime - offhookTime));
				// If call did not connect, undo the call connect event
				if (targetEventCouple != null && targetEventCouple.getStartEvent() != null)
				{
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "Undo CONNECT event");
					owner.getEventManager().unstageEvent(targetEventCouple.getStartEvent());
					owner.getEventManager().cancelCouple (targetEventCouple);
					//MMCService.getGpsManager().unregisterListener(targetEventCouple.getStartEvent().gpsListener);
					//owner.getEventManager().deleteEventDB(targetEventCouple.getStartEvent().getUri(), null, null);

					setCallConnected (false);
					int eventId = ReportManager.getInstance(owner).getEventId(targetEventCouple.getStartEvent().getEventTimestamp(), EventType.EVT_CONNECT.getIntValue());
  				    if (eventId != 0)
  				    	 ReportManager.getInstance(owner).deleteEvent (eventId);
  				    owner.startRadioLog (false, null, EventType.EVT_CONNECT);
				
				}
			}

		}
	}
	/**
	 * Waits 10 Seconds after a phone call is disconnected to look at signal changes to decide if a call ended normally or dropped
	 * There is often a delay before changes in signal are reported to the listener, which is why we wait before deciding
	 * This also gives a chance for other method to weigh-in, such as the logcat
	 */
	class DisconnectTimerTask extends TimerTask {
		int count = 0;
		public DisconnectTimerTask (int _count)
		{
			count = _count;
		}
		@Override
		public void run() {

			EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
			
			MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call connected=" + isCallConnected() + " event=" + targetEventCouple);
			
			heurCause = null;
			
			if(isCallConnected() || isCallDialing() || isCallRinging())
			{
				
				int rating = 0;

				if (MMCService.getPlatform() != 3)
				{
					PhoneHeuristic heur = new PhoneHeuristic (owner.getExtensionManager());
					rating = heur.heuristicDropped (disconnectTime, offhookTime, timeConnected);
					heurCause = heur.getCause();
				}
				
				// Detected dropped call based on logcat cause, or proximity (phone against ear at disconnect time)
				if (lastCallDropped == true)
				{
					rating = 5;
					heurCause = lastDroppedCause;
					if (targetEventCouple == null)
					{
						EventOld evt = owner.getEventManager().triggerSingletonEvent(EventType.EVT_CALLFAIL);
						popupDropped (EventType.EVT_CALLFAIL, rating, evt.getLocalID());
						evt.setCause (lastDroppedCause);
						evt.setEventTimestamp(disconnectTime);
					}
				}
				
				if (rating  > 2 && targetEventCouple != null)
				{
					MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call dropped heuristic=" + heurCause);
					
					//there is now a good chance that the call was dropped
					if(!isCallConnected()) //  || (owner.isCallConnected() == true && owner.getTimeConnected() + 2000+count*2000 > System.currentTimeMillis()))
					{
						// failed call based on heuristic
						targetEventCouple.setStopEventType(EventType.EVT_CALLFAIL);
						owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_CALLFAIL);
						targetEventCouple.getStopEvent().setCause (heurCause);
						targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
						targetEventCouple.getStopEvent().setEventIndex(rating); // something has to hold the confidence rating. This field will be sent to server as 'eventIndex'
						ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), Events.KEY_TIER, Integer.toString(rating));
						ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));
					    
						MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call changed to IDLE while call was dialing/ringing (CALL FAILED)");
						
						int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
		        		if (allowConfirm > 0)
		        			popupDropped (EventType.EVT_CALLFAIL, rating, targetEventCouple.getStopEvent().getLocalID());
					}
					else if (isCallConnected() && targetEventCouple != null)
					{
						MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "DisconnectTimerTask", "call changed to IDLE with low signal while during call (CALL DROPPED)");
						targetEventCouple.setStopEventType(EventType.EVT_DROP);
						owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_DROP);
						
						targetEventCouple.getStopEvent().setCause (heurCause);
						targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
						targetEventCouple.getStopEvent().setEventIndex(rating); // something has to hold the confidence rating. This field will be sent to server as 'eventIndex'
						
						ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), Events.KEY_TIER, Integer.toString(rating));
						ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));
						int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
		        		if (allowConfirm > 0)
		        			popupDropped (EventType.EVT_DROP, rating, targetEventCouple.getStopEvent().getLocalID());
					}
					
				} 
				else if (targetEventCouple != null)
				{  
					EventType evtType = EventType.EVT_DISCONNECT;
					if (isCallConnected())
						owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
					else
					{
						evtType = EventType.EVT_UNANSWERED;
						targetEventCouple.setStopEventType(EventType.EVT_UNANSWERED);
						owner.getEventManager().stopPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_UNANSWERED);
					}

					if (heurCause == null || heurCause.length() == 0)
						targetEventCouple.getStopEvent().setCause ("IDLE");
					else
						targetEventCouple.getStopEvent().setCause (heurCause);
					targetEventCouple.getStopEvent().setEventIndex(rating);
					targetEventCouple.getStopEvent().setEventTimestamp(disconnectTime);
					ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), Events.KEY_TIER, Integer.toString(rating));
					ReportManager.getInstance(owner).updateEventField(targetEventCouple.getStopEvent().getLocalID(), "timeStamp", Long.toString(disconnectTime));
				    
					//owner.getEventManager().updateEventDBField(targetEventCouple.getStopEvent().getUri(), TablesOld.Events.TIMESTAMP, Long.toString(disconnectTime));

					// server can specify whether a confirmation can be invoked on a low rated potentially-dropped call
	            	int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
	        		if (allowConfirm > 0)
	        			popupDropped (evtType, rating, targetEventCouple.getStopEvent().getLocalID());
				}
			}
			setCallConnected (false);
			lastCallDropped = false;
			if (!bOffHook)
			{
				setCallDialing (false);
			}
			else
				phoneOffHook (TelephonyManager.CALL_STATE_OFFHOOK);
			//owner.startRadioLog (false, null);
		}
	}

	/*
	*  Called when phone state is Off-Hook (dialing out) or ringing (incoming call)
	 */
	public void phoneOffHook (int iPhoneState)
	{
		final EventOld event = owner.getEventManager().startPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
		if (event != null)
		{
			if (bOffHook)
			{
				if (iPhoneState == TelephonyManager.CALL_STATE_OFFHOOK && (event.getFlags() & EventOld.CALL_INCOMING) > 0)
					setCallConnected(true);
//				if (iPhoneState == TelephonyManager.CALL_STATE_RINGING && (event.getFlags() & EventOld.CALL_INCOMING) == 0)
//					setCallWaiting(true);
				return;
			}
			owner.startRadioLog (true, "call", EventType.EVT_CONNECT); // "monitoring signal strength");
			if (iPhoneState == TelephonyManager.CALL_STATE_RINGING)
			{
				event.setFlag(EventOld.CALL_INCOMING, true);
				setCallRinging(true);
			}
			else
			{
				setCallDialing(true); // in case it is an outgoing call (not sure), dialing time will start now
				setCallRinging(false); // in case it is an outgoing call (not sure), dialing time will start now
			}
		}
		bOffHook = true;
		offhookTime = System.currentTimeMillis();

		lastCallDropped = false;
		
		Intent intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_CONNECT);
		owner.sendBroadcast(intent);

		// Delay for a few seconds and then check the voice network to detect if we have a VoLTE call
		owner.handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				int tech = getVoiceNetworkType ();
				if (tech == NETWORK_NEWTYPE_LTE && event != null) {
					event.setFlag (EventOld.CALL_VOLTE, true);
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getRilVoiceRadioTechnology", "VOLTE CALL DETECTED");
				}
				else if ((tech <= 0 || tech == NETWORK_NEWTYPE_IWLAN) && event != null)
				{
					event.setFlag (EventOld.CALL_OFFNET, true);
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getRilVoiceRadioTechnology", "WIFI CALL DETECTED?");
				}
			}
		}, 3500);
	}

	// Check the Voice Network type using new Hidden TelephonyManager method
	// This static version is for the connection history to access
	public static int getVoiceNetworkType (ServiceState serviceState)
	{
		Method m = null;
		try {
			// Java reflection to gain access to TelephonyManager's
			// ITelephony getter
			Class c = Class.forName(serviceState.getClass().getName());
			Method mI = c.getDeclaredMethod("getRilVoiceRadioTechnology");
			mI.setAccessible(true);
			int voiceTechRil = (Integer)mI.invoke(serviceState);
			int voiceTech = rilRadioTechnologyToNetworkType (voiceTechRil);
			return voiceTech;
		}
		catch (Exception e)
		{
			String s = e.toString();
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getVoiceNetworkType", "exception", e);
		}
		return -1;
	}

	// Check the Voice Network type using new Hidden TelephonyManager method
	private int getVoiceNetworkType ()
	{
		// we're going to get the voice network type from the last ServiceState
		Method m = null;
		TelephonyManager tm = (TelephonyManager) owner
				.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			// Java reflection to gain access to TelephonyManager's
			Class c = Class.forName(tm.getClass().getName());
			Method mI = c.getDeclaredMethod("getVoiceNetworkType");
			mI.setAccessible(true);
			int voiceTech = (Integer)mI.invoke(tm);
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getVoiceNetworkType", "Voice Network = " + voiceTech);

			Method mI2 = c.getDeclaredMethod("getDataNetworkType");
			mI2.setAccessible(true);
			int dataTech = (Integer)mI2.invoke(tm);
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getDataNetworkType", "Data Network = " + dataTech);


			return voiceTech;
		}
		catch (Exception e)
		{
			String s = e.toString();
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getVoiceNetworkType", "exception", e);
		}
		return previousNetworkType;
	}

	// Listener for connected and disconnected phone calls
	// Android Detects only on-hook and off-hook. To better detect, it starts timer tasks
	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);

		try
		{
			Intent intent;

			switch (state){
				case TelephonyManager.CALL_STATE_IDLE:  
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "IDLE");

					if (bOffHook == false)
					{	
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "not off hook");
						return;
					}
				disconnectTime = System.currentTimeMillis();
				bOffHook = false;

				HashMap<String, Integer> handset = owner.getHandsetCaps ();
				// If phone needs heuristics, check the signal for a dropped call
				int heurDelay = 9;
				if (handset.containsKey("capHeurDelay") )
					heurDelay = handset.get("capHeurDelay");
				if (owner.getPlatform() == 3)
					heurDelay = 2;
				
				bOffHook = false;
				TimerTask verifyConnectTask = new VerifyConnectTask();
				disconnectTimer.schedule(verifyConnectTask, 2000); // 1300
				TimerTask disconnectTimerTask1 = new DisconnectTimerTask(1);
				disconnectTimer.schedule(disconnectTimerTask1, heurDelay*1000);	
				intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_DISCONNECT);
				owner.sendBroadcast(intent);
				if (disconnectLatch != null)
					disconnectLatch.countDown();
				
				
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "OFFHOOK");
				//if (owner.bOffHook)
				//	return;

				phoneOffHook (TelephonyManager.CALL_STATE_OFFHOOK);
				//intent = new Intent(MMCIntentHandlerOld.PHONE_CALL_CONNECT);
				//owner.sendBroadcast(intent);
				if (connectLatch != null)
					connectLatch.countDown();
				
				//TimerTask launchConnectTask = new LaunchConnectTask();
				//disconnectTimer.schedule(launchConnectTask, 2000);
				break;
				
			case TelephonyManager.CALL_STATE_RINGING:
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "RINGING");
				if (bOffHook)
					return;
				phoneOffHook (TelephonyManager.CALL_STATE_RINGING);
				//if (incomingNumber != null && incomingNumber.length() > 1)
				//	txtIncomingNumber = incomingNumber;

				break;
			}					
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCallStateChanged", "Exception", e);
		}
	}
	
	CountDownLatch connectLatch, disconnectLatch;
	
	public boolean waitForConnect ()
	{
		connectLatch = new CountDownLatch(1);
		try {
			boolean res = connectLatch.await (30,TimeUnit.SECONDS);
			boolean b = res;
			return res;
		} catch (InterruptedException e) {
			//if (connectLatch.getCount() <= 0)
			//	return true;
			return false;
		}
	}
	public boolean waitForDisconnect ()
	{
		disconnectLatch = new CountDownLatch(1);
		try {
			return disconnectLatch.await (50,TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			//if (connectLatch.getCount() <= 0)
			//	return true;
			return false;
		}
	}

	@SuppressLint("InlinedApi")
	@SuppressWarnings("deprecation")
	public boolean isAirplaneModeOn(Context context) {

		//int disp = Settings.System.getInt(context.getContentResolver(), 
        //        Settings.System.SCREEN_OFF_TIMEOUT, 0) ;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
	        return Settings.System.getInt(context.getContentResolver(), 
	                Settings.System.AIRPLANE_MODE_ON, 0) != 0;          
	    } else {
	        return Settings.Global.getInt(context.getContentResolver(), 
	                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
	    }
	    
	}
	
	@SuppressLint("InlinedApi")
	@SuppressWarnings("deprecation")
	public int networkPreference(Context context) {
		
		String pref = null;
		ConnectivityManager con = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		int attemptTwo = con.getNetworkPreference();

	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
	    	return Settings.System.getInt(context.getContentResolver(), 
	    			Settings.System.NETWORK_PREFERENCE, 0);      
	    } else {
	    	   return Settings.Global.getInt(context.getContentResolver(), 
		                Settings.Global.NETWORK_PREFERENCE, 0) ;
	    }       
	}
	
	
	@Override
	public void onServiceStateChanged(ServiceState serviceState) {
		super.onServiceStateChanged(serviceState);	
		if (serviceState == null)
			return;

		boolean isRoaming = serviceState.getRoaming();
		String operator = serviceState.getOperatorAlphaLong();
		String mccmnc = serviceState.getOperatorNumeric();
		
		//owner.getConnectionHistory().updateConnectionHistory(cellnettype, state, activity, networkInfo)
		try{
		String activity = owner.getConnectionHistory().updateConnectionHistory(telephonyManager.getNetworkType(), telephonyManager.getDataState(), telephonyManager.getDataActivity(), serviceState, owner.getConnectivityManager().getActiveNetworkInfo());
		if (activity != null)
	   		owner.getIntentDispatcher().updateConnection(activity, false);
		} catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onServiceStateChanged", "exception with updateConnectionHistory:", e);
		}

		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", String.format("State: %s, roaming: %s, operator: %s, mccmnc: %s",
		//			serviceState, isRoaming, operator, mccmnc));
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "Reflected: " + listServiceStateFields(serviceState));

		boolean wasRoaming = PreferenceManager.getDefaultSharedPreferences(owner).getBoolean(PreferenceKeys.Miscellaneous.WAS_ROAMING, false);
		
		//If roaming, track time spend doing this			
		if (isRoaming() != wasRoaming) {
			int roamValue = 2; //off
			String status = "off";
			if(isRoaming()) {
				roamValue = 1; //on
				status = "on";
				//For DataMonitor tracking
				Intent intent = new Intent(MMCIntentHandlerOld.ROAMING_ON);
				owner.sendBroadcast(intent);
			}
			else {
				Intent intent = new Intent(MMCIntentHandlerOld.ROAMING_OFF);
				owner.sendBroadcast(intent);
			}
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "roaming status: " + status);
			owner.trackAccessPoints(roamValue);
			owner.getEventManager().triggerUpdateEvent(false, false);
			PreferenceManager.getDefaultSharedPreferences(owner).edit().putBoolean(PreferenceKeys.Miscellaneous.WAS_ROAMING, isRoaming()).commit();
		}		
		
		//If wimax, track time spend doing this
		if(owner.ActiveConnection() == 12) {
			Intent intent = new Intent(MMCIntentHandlerOld.WIMAX_STATE_CHANGE);
			owner.sendBroadcast(intent);
		}
			
		//in airplane mode
		if(isAirplaneModeOn(owner.getApplicationContext()) == true){
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "airplane mode on");
			previousServiceState = SERVICE_STATE_AIRPLANE;		
			try {
				MMCSignalOld mmcSignal = new MMCSignalOld();
				processNewMMCSignal(mmcSignal);
			} catch (Exception e) {
			}
			return;
		}		

		if(serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
			if(previousServiceState != ServiceState.STATE_IN_SERVICE) {
				
				//state changed from OUT_OF_SERVICE to IN_SERVICE
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "trigger regain service");
				owner.getEventManager().stopPhoneEvent(EventType.COV_VOD_NO, EventType.COV_VOD_YES);
				mLastServiceStateChangeTimeStamp = System.currentTimeMillis();

			}
		}
		else if(serviceState.getState() == ServiceState.STATE_OUT_OF_SERVICE) {// || serviceState.getState() == ServiceState.STATE_EMERGENCY_ONLY) {
			if(previousServiceState == SERVICE_STATE_AIRPLANE)
				return;  // discard 'no-service' occurring after exiting airplane mode
			
			if(previousServiceState == ServiceState.STATE_IN_SERVICE){ // && previousServiceState != SERVICE_STATE_AIRPLANE) {
				
				previousServiceState = serviceState.getState();
				MMCSignalOld signal = getLastMMCSignal();
				processNewMMCSignal(signal);

				// Outage needs to last longer than 10 seconds to actually trigger
				owner.handler.postDelayed(new Runnable() {
					  @Override
					  public void run() {
						  // If longer outage after 2 seconds, do nothing
						  if (previousServiceState != ServiceState.STATE_OUT_OF_SERVICE) //  && previousServiceState != ServiceState.STATE_EMERGENCY_ONLY)
						  {
							  MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "Outage lasted < 4 sec, ignoring");
							  return;
						  }
						    // Officially an outage now
							// If service dropped straight from 3G to nothing, trigger a 3G outage as well
							// If was connected to wifi when service was lost, does not count as a 3G outage
							if (owner.ActiveConnection() <= 1 && previousNetworkTier >= 3) // 10=Wifi, 11=Wimax, 12=Ethernet, 0=other
							{			
								owner.getEventManager().startPhoneEvent(EventType.COV_3G_NO, EventType.COV_3G_YES);
								if (previousNetworkTier >= 5)
									owner.getEventManager().startPhoneEvent(EventType.COV_4G_NO, EventType.COV_4G_YES);
							}	
							mLastServiceStateChangeTimeStamp = System.currentTimeMillis();
							
							MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceStateChanged", "trigger lost service");
							//state changed from IN_SERVICE to OUT_OF_SERVICE 
							owner.getEventManager().startPhoneEvent(EventType.COV_VOD_NO, EventType.COV_VOD_YES);
							if (previousNetworkTier >= 2)
								owner.getEventManager().startPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
							MMCSignalOld signal = getLastMMCSignal();
						  	processNewMMCSignal(signal);
							previousNetworkTier = 0;
							//previousNetworkState = 0;
					  }
					}, 10000);
					
			}
		}
		previousServiceState = serviceState.getState();
		previousServiceStateObj = serviceState;
		
	}
	
	public int getLastServiceState ()
	{
		return previousServiceState;
	}
	
	public ServiceState getLastServiceStateObj ()
	{
		return previousServiceStateObj;
	}

	public void onPreciseCallState (PreciseCallCodes preciseCall)
	{
		int state = preciseCall.getRingingCallState();
		int fstate = preciseCall.getForegroundCallState();
		if (preciseCall.getDisconnectCause() != -1)
			onDisconnect("", preciseCall.getDisconnectCauseString());
		else if (fstate == PreciseCallCodes.PRECISE_CALL_STATE_DIALING ||
				fstate == PreciseCallCodes.PRECISE_CALL_STATE_ALERTING ||
				fstate == PreciseCallCodes.PRECISE_CALL_STATE_ACTIVE)
			onConnect ("", preciseCall.getForegroundCallStateString());
		owner.getConnectionHistory().updatePreciseCallHistory (preciseCall);
	}

	public void onServiceMenu ( String _timestamp, String values, String name) {
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceMenu", name);
		if (!closedServicePanel && owner.isMonitoringActive())
			NerdScreen.updateSvcPanel(owner, values, name);
	}

	private long tmSvcUpdate = 0;
	public void onServiceMode ( String _timestamp, JSONObject servicemode, String values, String name) {
		//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onServiceMode", servicemode.toString());
		if (!closedServicePanel && owner.isMonitoringActive())
			NerdScreen.updateSvcPanel(owner, values, name);

		if (tmSvcUpdate + 2000 > System.currentTimeMillis())
			return;
		tmSvcUpdate = System.currentTimeMillis();

		try {
			if (name.equals("BASIC")) {
				if (!prevSvcValues.equals(values)) {
					boolean bCellChanged = false, bNeighbors = false;

					MMCSignalOld signal = this.getLastMMCSignal();
					long timestamp = System.currentTimeMillis();

					if (mServicemode != null) {
						if (servicemode.has("psc") && mServicemode.has("psc") && !servicemode.getString("psc").equals(mServicemode.getString("psc")))
							bCellChanged = true;
						if (servicemode.has("pci") && mServicemode.has("pci") && !servicemode.getString("pci").equals(mServicemode.getString("pci")))
							bCellChanged = true;
					} else
						bCellChanged = true;
					mServicemode = servicemode;
					prevSvcValues = values;
					servicemode.put("time", timestamp);
					if (signal != null) {
						signal.setTimestamp(timestamp);
						this.clearLastMMCSignal();  // to force a duplicate signal to be added
						this.processNewMMCSignal(signal);
					}
					if (bCellChanged == true) {
						MMCCellLocationOld cell = getLastCellLocation();
						if (cell != null) {
							cell.setCellIdTimestamp(timestamp);
							this.clearLastCellLocation();
							this.processNewCellLocation(cell);
						}
					}
					// if event is in progress, update it with service mode values

				}
			}
			else if (name.equals("NEIGHBOURS")) {
				if (servicemode.has("neighbors")) {
					String neighbors = owner.getCellHistory().updateNeighborHistory(servicemode.getJSONArray("neighbors"));
					if (neighbors != null && neighbors.length() > 2)
						owner.getIntentDispatcher().updateNeighbors(neighbors);
				}
			}

			owner.getConnectionHistory().updateServiceModeHistory(values, name);
		}
		catch(Exception x){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onServiceMode", "exception", x);
		}
	}
	/*
	*  Precise Information about phone-call-disconnect cause, if available from logcat or other priviledged means
	 */
	public void onDisconnect ( String _timestamp, String _cause)
	{
		_cause = _cause.trim();
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", _cause);
		if (!isCallDialing() && !isCallConnected())
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "ignoring because no call was dialing or connected");
			return;
		}
		HashMap<String, Integer> handsetcaps = owner.getHandsetCaps ();
		int useDropCause = 2, useFailedCause = 2;
		int causeCode = 1;
		String cause = _cause;
		if (_cause.startsWith("FAIL") || _cause.startsWith("CAUSE"))
		{
			int space = _cause.indexOf (" ");
			String[] causes = _cause.substring(space+1).split(",");
			if (causes != null && causes.length > 0 && causes[0].length() > 0)
			{
				try{
					causeCode = Integer.parseInt(causes[0].trim(), 10);
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "cause code: " + causeCode);
				}catch (Exception e){
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "error parse cause code from: " + _cause);
				}
				if (causeCode == 65535 || causeCode == 0)
				{
					cause = "err_" + causeCode;//unspecified";
					causeCode = 1;
				}
			}
		}


		if (cause.equalsIgnoreCase("congestion") || cause.equalsIgnoreCase("call_drop") || (causeCode> 31 && causeCode != 510) || cause.equalsIgnoreCase("lost_signal") ||
				cause.equalsIgnoreCase("cdma_drop") || cause.equalsIgnoreCase("out_of_service") || cause.equalsIgnoreCase("icc_error"))
		{
			lastCallDropped = true;
			lastDroppedCause = _cause;
		}
		else if (cause.equalsIgnoreCase("error_unspecified") || cause.indexOf("err_") == 0)
		{
			//boolean bUseCause = true;
			if (handsetcaps.containsKey("capDropCause"))
				useDropCause = handsetcaps.get("capDropCause");

			if (handsetcaps.containsKey("capFailedCause"))
				useFailedCause = handsetcaps.get("capFailedCause");
			if ((isCallConnected() && useDropCause != 0) ||
					(!isCallConnected() && (useFailedCause != 0 && !callRinging)))
			{
				lastCallDropped = true;
				lastDroppedCause = _cause;
			}
			else
			{
				if (!isCallConnected())
				{
					if (callRinging)
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "unspecified cause not considered failed because it rang (call may have been rejected)");
					else if (useFailedCause == 0)
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "unspecified cause not considered failed because handset doesnt support unspecified failed cause");
				}
				else if (useDropCause == 0)
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "unspecified cause not considered dropped because handset doesnt support unspecified dropped cause");
				else
					MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onDisconnect", "unspecified cause not considered dropped or failed for unknown reason");

			}
		}
		else
			_cause = "";
	}

	/*
	*  Precise Information about phone-call-connect states, if available from logcat or other priviledged means
	 */
	public void onConnect ( String _timestamp, String _state)
	{
		//start a phone connected event
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onConnect", _state);

		if(_state.equalsIgnoreCase("active"))
		{
			if (bOffHook == true)
			{
				if (callConnected == false) {
					setCallConnected(true);
					timeConnected = System.currentTimeMillis();
					lastCallDropped = false;
					callDialing = false;
					//start a phone connected event
					EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
					if (targetEventCouple != null && targetEventCouple.getStartEvent() != null) {
						targetEventCouple.getStartEvent().setEventTimestamp(System.currentTimeMillis());
						//start a phone connected event
						long connectDuration = 0;
						// The duration on the connected Call event will represent the time it took the call to begin ringing
						if (callRinging = true && timeRinging > timeDialed && timeDialed > 0 && timeDialed > timeRinging - 100000)
							connectDuration = timeRinging - timeDialed;
						connectDuration = timeConnected - timeDialed;
						targetEventCouple.getStartEvent().setConnectTime((int) connectDuration);
					}
				}
				else
				{
					MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "onConnect", "call active but already connected");
				}

				//startPhoneEvent(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
			}
			else
			{
				callDialing = false;
				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "onConnect", "call active but not offhook");
			}
			callRinging = false;
		}

		if (bOffHook == true && callRinging == false && callConnected == false &&
				(_state.equalsIgnoreCase("dialing") || _state.equalsIgnoreCase("alerting")))
		{

			if (_state.equalsIgnoreCase("dialing") && callDialing == false)
			{
				callDialing = true;
				timeDialed = System.currentTimeMillis();
			}
			if (_state.equalsIgnoreCase("alerting") && callRinging == false)
			{
				callRinging = true;
				timeRinging = System.currentTimeMillis();
			}
		}

	}

	/*
	 * Check the Android call log after phone hangs up to see if and when a phone call began and ended
	 * This is only needed in Android 4.1 because they removed permission to access the radio logcat
	 * Android 4.1 requires a new permission called PERMISSION_READ_CALL_LOG
	 */
	public EventOld checkCallLog ()
	{
		EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.EVT_CONNECT, EventType.EVT_DISCONNECT);
		MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "VerifyConnectTask", "checkCallLog: targetEventCouple" + targetEventCouple);

		if (targetEventCouple == null || targetEventCouple.getStartEvent() == null )
			return null;
		EventOld connectEvent = targetEventCouple.getStartEvent();
		String[] strFields = {
				android.provider.CallLog.Calls.NUMBER,
				android.provider.CallLog.Calls.TYPE,
				android.provider.CallLog.Calls.DATE,
				android.provider.CallLog.Calls.DURATION
		};
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC LIMIT 1";

		try
		{
			Cursor callCursor = owner.getContentResolver().query(
					android.provider.CallLog.Calls.CONTENT_URI,
					strFields,
					null,
					null,
					strOrder
			);


			if (callCursor != null && callCursor.moveToFirst())
			{
				String number = callCursor.getString(0);
				int type = callCursor.getInt(1);

				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "checkCallLog", "type: " + type);

				if (type != android.provider.CallLog.Calls.MISSED_TYPE)
				{
					setCallConnected (true);
					long callDate = callCursor.getLong(2);
					int callDuration = callCursor.getInt(3);
					long callEnd = callDate + (long)callDuration;

					MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "checkCallLog", "callDate: " + callDate + ", duration: " + callDuration);

					//if (callEnd < System.currentTimeMillis() + 5000)
					if (callDate >= connectEvent.getEventTimestamp()-12000 && callDuration > 0)
					{
						timeConnected = System.currentTimeMillis() - 2000 - callDuration*1000; // callDate;
						connectEvent.setEventTimestamp(timeConnected);
						long connectDuration = timeConnected - timeDialed;
						targetEventCouple.getStartEvent().setConnectTime((int) connectDuration);
						//eventManager.updateEventDBField(connectEvent.getUri(), TablesOld.Events.TIMESTAMP, Long.toString(timeConnected));
					}
					else
					{
						timeConnected = 0;
						setCallConnected (false);
						return null;
					}

				}
			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "checkCallLog", "exception:", e);
		}
		return connectEvent;
	}
	public boolean isCallConnected ()
	{
		return callConnected;
	}
	public long getTimeConnected ()
	{
		return timeConnected;
	}
	public long getTimeDialed ()
	{
		return timeDialed;
	}
	public void setTimeConnected (long time)
	{
		timeConnected = time;
	}
	public void setCallConnected (boolean connected)
	{
		callConnected = connected;
//		if (connected == true)
//			timeConnected = System.currentTimeMillis();
//		else
//			timeConnected = 0;
//        MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "setCallConnected", connected + " " + timeConnected);

	}
	public boolean isCallDialing ()
	{
		return callDialing;
	}

	public boolean isCallRinging ()
	{
		return callRinging;
	}

	public void setCallRinging (boolean bRinging)
	{
		if (bRinging && callRinging == false)
		{
			callRinging = true;
			timeRinging = System.currentTimeMillis();
		}
		else if (bRinging == false)
			callRinging = false;
	}
	public void setCallDialing (boolean bDialing)
	{
		callDialing = bDialing;
		if (callDialing)
			timeDialed = System.currentTimeMillis();
		else
			timeDialed = 0;

	}

	public void onNeighbors ( String _timestamp, int[] _list, int[] _list_rssi)
	{
		int i;
		if (_list == null || _list_rssi == null)
			return;

		if (_list.length > 0 && _list[0] != 0)
		{
			if (owner.getCellHistory() != null)
			{
				owner.getCellHistory().updateNeighborHistory (_list, _list_rssi);
			}
		}
	}

	public void popupDropped (final EventType droptype, final int rating, final int evtId)
	{
		if (rating == 0)
			return;
		owner.handler.post(new Runnable() {
			// @Override
			public void run() {
				String message = "";
				int icon;
				icon = R.drawable.ic_stat_dropped;
				String title = "";
				String msg = "";

				// server can specify whether a confirmation can be invoked on a low rated potentially-dropped call
				int allowConfirm = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_CONFIRMATION, 5);
				String noConfirm = (owner.getResources().getString(R.string.NO_CONFIRMATION));
				int allowPopup = PreferenceManager.getDefaultSharedPreferences(owner).getInt(PreferenceKeys.Miscellaneous.ALLOW_DROP_POPUP, 2);
				if (allowPopup == 1 && !owner.getUsageLimits().getUseRadioLog())
					allowPopup = 0;
				if (allowPopup == 0)
					return;

				if (noConfirm.equals("1"))
					allowConfirm = 0;
				if (allowConfirm > 0 && rating < allowConfirm && rating < 4)  // if confirmation allow, must be above threshold or high rating dropped call
					return;
				else if (allowConfirm == 0 && rating < 4)  // drop call silently if marginal with no confirmation
					return;
				// allowConfirm>=5 disables the confirmation because rating always <= 5
				// allowConfirm=1 hits the 'else' and invokes confirmation if rating >= 1 and <5
				// allowConfirm=3 hits the 'else' and invokes confirmation if rating >= 3 and <5
				int expiry = 60000  * 2 * 60;
				int customText = (owner.getResources().getInteger(R.integer.CUSTOM_EVENTNAMES));
				message = owner.getString((customText == 1) ? R.string.sharecustom_speedtest_wifi : R.string.sharemessage_speedtest_wifi);

				if (rating >= 5 || allowConfirm == 0)
				{
					title = owner.getString(R.string.app_label);
					msg = "mmc detected ";
					if (droptype == EventType.EVT_CALLFAIL)
						message = owner.getString((customText == 1) ? R.string.Custom_Notification_call_failed : R.string.MMC_Notification_call_failed);
					else
						message = owner.getString((customText == 1) ? R.string.Custom_Notification_call_dropped : R.string.MMC_Notification_call_dropped);
					message += ": " + owner.getString(R.string.MMC_Notification_view_event);
					msg += message;
				}
				else if (rating >= allowConfirm && rating > 1)
				{
					if (droptype == EventType.EVT_CALLFAIL)
					{
						title = owner.getString((customText == 1) ? R.string.Custom_Notification_did_you_fail : R.string.MMC_Notification_did_you_fail);
						message = owner.getString((customText == 1) ? R.string.Custom_Notification_did_failed : R.string.MMC_Notification_did_failed);
					}
					else if (droptype == EventType.EVT_DROP)
					{
						title = owner.getString((customText == 1) ? R.string.Custom_Notification_did_you_drop : R.string.MMC_Notification_did_you_drop);
						message = owner.getString((customText == 1) ? R.string.Custom_Notification_did_dropped : R.string.MMC_Notification_did_dropped);
					}
					else if (droptype == EventType.EVT_DISCONNECT || droptype == EventType.EVT_UNANSWERED)
					{
						expiry = 60000;
						icon = R.drawable.ic_stat_disconnect;
						title = owner.getString((customText == 1) ? R.string.Custom_Notification_did_you_disconnect : R.string.MMC_Notification_did_you_disconnect);
						message = owner.getString((customText == 1) ? R.string.Custom_Notification_did_disconnect : R.string.MMC_Notification_did_disconnect);
					}
					msg = message;
				}

				java.util.Date date = new java.util.Date();
				String time = date.toLocaleString();
				msg += " at " + time;
				//Toast toast = Toast.makeText(MMCService.this, msg, Toast.LENGTH_LONG);
				//toast.show();

				NotificationManager notificationManager = (NotificationManager) owner.getSystemService(Service.NOTIFICATION_SERVICE);
				Notification notification = new Notification(icon, message, System.currentTimeMillis());
				notification.flags |= Notification.FLAG_AUTO_CANCEL;
				//Intent notificationIntent = new Intent(MMCService.this, Dashboard.class);
				Intent notificationIntent = new Intent();//, "com.cortxt.app.MMC.Activities.Dashboard");
				notificationIntent.setClassName(owner, "com.cortxt.app.MMC.Activities.Dashboard");
				notificationIntent.putExtra("eventId", evtId);

				notificationIntent.setData((Uri.parse("foobar://" + SystemClock.elapsedRealtime())));
				notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				PendingIntent pendingIntent = PendingIntent.getActivity(owner, MMC_DROPPED_NOTIFICATION + evtId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				notification.setLatestEventInfo(owner, title, message, pendingIntent);
				notificationManager.notify(MMC_DROPPED_NOTIFICATION, notification);
				long expirytime = System.currentTimeMillis() + expiry;
				PreferenceManager.getDefaultSharedPreferences(owner).edit().putLong(PreferenceKeys.Monitoring.NOTIFICATION_EXPIRY, expirytime).commit();

			}});

	}

	public void processNewCellLocation(MMCCellLocationOld cellLoc) throws InterruptedException {
		if (cellLoc.getCellLocation() != null && lastKnownMMCCellLocation != null && tmLastCellUpdate + 60000 > System.currentTimeMillis() && cellLoc != null &&  cellLoc.getCellLocation().toString().equals(lastCellString))
			return;

		tmLastCellUpdate = System.currentTimeMillis();

		if (cellLoc == null) // This is so that when each event is staged, it associates the last known cell with it
			cellLoc = lastKnownMMCCellLocation;
		else if (owner.getTravelDetector() != null)
		{
			CellLocation lastCellloc = null;
			if (lastKnownMMCCellLocation != null)
				lastCellloc = lastKnownMMCCellLocation.getCellLocation();
			owner.getTravelDetector().detectTravellingFromCellId(owner.getPhoneStateListener().getPhoneType(), cellLoc.getCellLocation(), lastCellloc);
			//store the new cell location in the internal cache
			lastKnownMMCCellLocation = cellLoc;
		}
		if (cellLoc == null || cellLoc.getCellLocation() == null) //|| cellLoc.getCellLocationLte() == null)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "processNewCellLocation", "cellLoc=null");
			return;
		}

		lastCellString = cellLoc.getCellLocation().toString();

		try
		{
			int bsHigh = cellLoc.getBSHigh(), bsMid = cellLoc.getBSMid(), bsLow = cellLoc.getBSLow();
			if (bsLow == 65535)  //cellid = -1
				bsLow = -1;

			tmLastCell = System.currentTimeMillis();
			//push the new location into the sqlite database
			long stagedEventId = owner.getEventManager().getStagedEventId();


			ContentValues values = ContentValuesGeneratorOld.generateFromCellLocation(cellLoc, stagedEventId);
			owner.getDBProvider(owner).insert(TablesEnumOld.BASE_STATIONS.getContentUri(), values);
			owner.getIntentDispatcher().updateCellID(bsHigh, bsMid, bsLow);

			String neighbors = owner.getCellHistory().updateNeighborHistory(null, null);
			if (neighbors != null && neighbors != "")
			{
				owner.getIntentDispatcher().updateNeighbors (neighbors);
			}
			else if (android.os.Build.VERSION.SDK_INT >= 10 && telephonyManager.getNetworkType() == MMCPhoneStateListenerOld.NETWORK_NEWTYPE_LTE && (cellLoc.getCellLocation() instanceof GsmCellLocation) == true)
			{
				int cid = bsLow + (bsMid<<16);
				int pci = ((GsmCellLocation)cellLoc.getCellLocation()).getPsc();
				if (pci <= 0)
				{
					pci = cellLoc.getBSCode();
				}
				neighbors = owner.getCellHistory().updateLteNeighborHistory(bsHigh,cid,pci);
				owner.getIntentDispatcher().updateLTEIdentity (neighbors);
				owner.getReportManager().setNeighbors(neighbors);
			}

			Intent intent = new Intent(MMCIntentHandlerOld.HANDOFF);
			owner.sendBroadcast(intent);
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "processNewCellLocation", "Exception", e);
		}

	}

	/*
	 *  Store the new signal strength in the SQLite DB and update the live status screen
	 *  This may be called by SignalStrength listener as well as on service outage or screen off
	 *  signal = null means no service / signal.getSignalStrength() = null means unknown due to screen off
	 */
	private long tmlastSig = 0;
	public void processNewMMCSignal(MMCSignalOld signal)  {
		ContentValues values = null;
		// if in a service outage, store a null signal
		// (I've seen cases where phone was out of service yet it was still returning a signal level)
		try
		{
			if (this.getLastServiceState() == ServiceState.STATE_OUT_OF_SERVICE)
				signal = null;

			// avoid storing repeating identical signals
			if (lastKnownMMCSignal != null && lastKnownMMCSignal.getSignalStrength() != null && signal != null && signal.getSignalStrength() != null)
				if (lastKnownMMCSignal.getSignalStrength().toString().equals(signal.getSignalStrength().toString()) && tmlastSig + 3000 > System.currentTimeMillis())
					return;
			tmlastSig = System.currentTimeMillis();
			Integer dbmValue = 0;
			boolean isLTE = false;
			if (signal == null)
				dbmValue = -256;
			else if (signal.getSignalStrength() == null)
				dbmValue = 0;

			//store the last known signal
			if (signal != null && signal.getSignalStrength() != null)
			{
				prevMMCSignal = lastKnownMMCSignal; // used for looking at signal just before a call ended
				lastKnownMMCSignal = signal;
			}
			else if (signal == null)
				lastKnownMMCSignal = null;

			//push the new signal level into the sqlite database
			long stagedEventId = owner.getEventManager().getStagedEventId();
			int serviceState = this.getLastServiceState();
			int wifiSignal = -1;
			WifiManager wifiManager = (WifiManager)owner.getSystemService(Context.WIFI_SERVICE);
			WifiInfo wifiinfo = wifiManager.getConnectionInfo() ;
			if (wifiinfo != null && wifiinfo.getBSSID() != null)
				wifiSignal =  wifiManager.getConnectionInfo().getRssi();

			if (signal != null)
			{
				values = ContentValuesGeneratorOld.generateFromSignal(signal, telephonyManager.getPhoneType(), telephonyManager.getNetworkType(),
						serviceState, telephonyManager.getDataState(), stagedEventId, wifiSignal, mServicemode);
				Integer valSignal= (Integer)values.get("signal");
				if (getNetworkType() == MMCPhoneStateListenerOld.NETWORK_NEWTYPE_LTE) // && phoneStateListener.previousNetworkState == TelephonyManager.DATA_CONNECTED)
					valSignal= (Integer)values.get("lteRsrp");
				if (valSignal != null && dbmValue != null && valSignal > -130 && valSignal < -30) //  && (dbmValue <= -120 || dbmValue >= -1))
					dbmValue = valSignal;
				if (dbmValue > -120 && dbmValue < -40)
					this.validSignal = true;
				if (this.validSignal) // make sure phone has at least one valid signal before recording
					owner.getDBProvider(owner).insert(TablesEnumOld.SIGNAL_STRENGTHS.getContentUri(), values);

			}
			//update the signal strength percentometer, chart, and look for low/high signal event
			if (dbmValue != null){
				if (dbmValue < -120) // might be -256 if no service, but want to display as -120 on livestatus chart
					dbmValue = -120;

				owner.getIntentDispatcher().updateSignalStrength(
						dbmValue, getNetworkType(), owner.bWifiConnected, wifiSignal
				);

				// Store signal in a sharedPreference for tools such as Indoor/Transit sample mapper, which dont have reference to service
				if (isLTE == true)   // improve the value of the signal for LTE, so that Indoor samples don't look redder in LTE
					PreferenceManager.getDefaultSharedPreferences(owner).edit().putInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, (dbmValue+10)).commit();
				else
					PreferenceManager.getDefaultSharedPreferences(owner).edit().putInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, dbmValue).commit();

			}
		}
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "processNewMMCSignal", "exception", e);
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "processNewMMCSignal", "values: " + values);
		}
		catch (Error err)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "processNewMMCSignal", "error"+ err.getMessage());
		}
	}

	public MMCSignalOld getLastMMCSignal(){
		return lastKnownMMCSignal;
	}
	public void clearLastMMCSignal(){
		lastKnownMMCSignal = null;
	}

	public MMCCellLocationOld getLastCellLocation(){
		if (lastKnownMMCCellLocation != null)
			return lastKnownMMCCellLocation;
		CellLocation cellLoc = telephonyManager.getCellLocation();
		if (cellLoc != null)
		{
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getLastCellLocation", "null cell, getCellLocation() = " + cellLoc.toString());

			MMCCellLocationOld mmcCell = new MMCCellLocationOld(cellLoc);
			try {
				processNewCellLocation(mmcCell);
			} catch (InterruptedException e) {
			}
			return mmcCell;
		}
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getLastCellLocation", "null cell, getCellLocation() = null");
		return null;
	}
	public void clearLastCellLocation(){
		lastKnownMMCCellLocation = null;
	}

	/*******************************************
	 * HELPER FUNCTIONS
	 *******************************************/

	public void updateNetworkType(int networkType) {
		this.networkType = networkType;
	}

	public int getNetworkType(){
		this.networkType = telephonyManager.getNetworkType();
		return this.networkType;
	}

	public static void setServicemode (JSONObject svcmode){ mServicemode = svcmode; }
	public static JSONObject getServiceMode () { return mServicemode;}
	public boolean isOffHook () { return bOffHook;}


	/*
	 * Return the number the server knows for network type
	 * 1 = GSM, 2 = UMTS/HSPA, 3 = CDMA
	 * This lets it know how to interpret the cell identifiers
	 */
	public int getNetworkTypeNumber()
	{
		// Any UMTS based technologies, return 2
		switch (getNetworkType())
		{
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_UMTS:
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_HSPAP:
				return 2;
		}
		if (getPhoneType() == TelephonyManager.PHONE_TYPE_GSM)
			return 1;
		// TODO: Can we detect UMTS and return 2? Otherwise it returns 1 for GSM which is fine
		if (getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA)
			return 3;
		return 0;
	}

	public int getNetworkGeneration() {
		return getNetworkGeneration(telephonyManager.getNetworkType());
	}

	public int getPhoneType() {
		return telephonyManager.getPhoneType();
	}

	public String getNetworkOperatorName(){
		return telephonyManager.getNetworkOperatorName();
	}

	public boolean isRoaming() {

		if (telephonyManager != null)
		{
			Boolean roaming = telephonyManager.isNetworkRoaming();
			return roaming;
		}
		else return false;
	}

	public int getLastKnownCallState() {
		return lastKnownCallState;
	}

	protected void setLastKnownCallState(int lastKnownCallState) {
		this.lastKnownCallState = lastKnownCallState;
	}
	/**
	 * Check if SIM changed, and update server if it id.
	 */
//	protected void checkForSimChange() {
//
//		if(telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
//			String lastIMSI = PreferenceManager.getDefaultSharedPreferences(this).getString("KEY_IMSI", null);
//			Context c = getApplicationContext();
//			MMCGSMDevice device = (MMCGSMDevice) getDevice();
//			String currentIMSI = device.getIMSI();
//
//			if(!currentIMSI.equals(lastIMSI) && lastIMSI != null) {
//				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "MMCService", "checkForSimChange", "imsi changed from " + lastIMSI + " to " + currentIMSI);
//				mReportManager.reportSimChange(device);
//				PreferenceManager.getDefaultSharedPreferences(this).edit().putString("KEY_IMSI", currentIMSI).commit();
//			}
//			else if(lastIMSI == null) {
//				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "MMCService", "checkForSimChange", "imsi was null, now " + currentIMSI);
//				PreferenceManager.getDefaultSharedPreferences(this).edit().putString("KEY_IMSI", currentIMSI).commit();
//			}
//		}
//	}

	// data stall (disconnected data while still in service)
	private void stateChanged_0g(int state) {
		// No such thing as DATA outage event
		EventOld event = null;
		// DATA Outage defined as switching to and connecting to 1G (GPRS) from > 1G (EDGE or higher)
		if (state == TelephonyManager.DATA_DISCONNECTED && previousNetworkTier > 0){
			//event = owner.startPhoneEvent(EventType.COV_DATA_DISC, EventType.COV_DATA_CONN);
			// 4G Outage defined as switching to and connecting to 2G from 4G
			if (previousNetworkTier > 4){
				event = owner.getEventManager().startPhoneEvent(EventType.COV_4G_NO, EventType.COV_4G_YES);
			} 
			if (previousNetworkTier > 2){
				event = owner.getEventManager().startPhoneEvent(EventType.COV_3G_NO, EventType.COV_3G_YES);
			} 
			if (previousNetworkTier > 0){
				event = owner.getEventManager().startPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
			} 
		} 
		
	}
	
	private void stateChanged_2g(int state) {
		// No such thing as DATA outage event
		EventOld event = null;
		//if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier == 0 && owner.getNetworkGeneration() == 1){
		//	event = owner.stopPhoneEvent(EventType.COV_DATA_DISC, EventType.COV_DATA_CONN);
		//} 
		// DATA Outage defined as switching to and connecting to 1G (GPRS) from > 1G (EDGE or higher)
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier > 1 && getNetworkGeneration() == 1){
			event = owner.getEventManager().startPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
		} 
		// 3G Outage defined as switching to and connecting to 2G from >2G
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier > 2){
			event = owner.getEventManager().startPhoneEvent(EventType.COV_3G_NO, EventType.COV_3G_YES);
		} 
		// 4G Outage defined as switching to and connecting to 2G from 4G
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier > 4){
			event = owner.getEventManager().startPhoneEvent(EventType.COV_4G_NO, EventType.COV_4G_YES);
		} 
		
		
	}

	private void stateChanged_3g(int state) {
		EventOld event = null;
		//if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier == 0 && owner.getNetworkGeneration() == 1){
		//	event = owner.stopPhoneEvent(EventType.COV_DATA_DISC, EventType.COV_DATA_CONN);
		//} 
		// 3G Regained defined as switching to and connecting to 3G+ from <3G
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier < 1)
			event = owner.getEventManager().stopPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier < 3){
			if (previousNetworkTier <= 1)
				event = owner.getEventManager().stopPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
			event = owner.getEventManager().stopPhoneEvent(EventType.COV_3G_NO, EventType.COV_3G_YES);
		} 
		// 4G Outage defined as switching to and connecting to 3G from LTE 4G
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier > 4 && !bOffHook){
			event = owner.getEventManager().startPhoneEvent(EventType.COV_4G_NO, EventType.COV_4G_YES);
		} 
		
	}
	
	private void stateChanged_4g(int state) {
		EventOld event = null;
		//if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier == 0 && owner.getNetworkGeneration() == 1){
		//	event = owner.stopPhoneEvent(EventType.COV_DATA_DISC, EventType.COV_DATA_CONN);
		//} 
		// 4G Regained defined as switching to and connecting to 4G+ from <3G
		// If it switches to and from LTE too often, this could result in excessive events, but it appears to hold LTE steady
		if (state == TelephonyManager.DATA_CONNECTED && previousNetworkTier < 5){
			if (previousNetworkTier <= 1)
				event = owner.getEventManager().stopPhoneEvent(EventType.COV_DATA_NO, EventType.COV_DATA_YES);
			if (previousNetworkTier < 3)
				event = owner.getEventManager().stopPhoneEvent(EventType.COV_3G_NO, EventType.COV_3G_YES);
			
			String pref = getNetworkTypesAndroidPreference ();
			if (pref.indexOf("LTE") >= 0)
				return;
			// disregard and undo an LTE outage if it regains just after a phone call disconnects
			EventCoupleOld targetEventCouple = owner.getEventManager().getEventCouple(EventType.COV_4G_NO, EventType.COV_4G_YES);
			
			if (this.disconnectTime + 120000 > System.currentTimeMillis() && targetEventCouple != null 
					&& targetEventCouple.getStartEvent() != null && targetEventCouple.getStartEvent().getEventTimestamp() + 30000 > offhookTime
					&& offhookTime > 0)
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "stateChanged_4g", "Undo LTE outage event");
				// 4G outage wont be staged anymore, but it still needs to remove COV_4G_NO from 'eventCache'
				owner.getEventManager().unstageEvent(targetEventCouple.getStartEvent());
				owner.getEventManager().cancelCouple (targetEventCouple);
				//owner.getEventManager().deleteEventDB(targetEventCouple.getStartEvent().getUri(), null, null);
				int eventId = ReportManager.getInstance(owner).getEventId(targetEventCouple.getStartEvent().getEventTimestamp(), EventType.COV_4G_NO.getIntValue());
			    if (eventId != 0)
			    	 ReportManager.getInstance(owner).deleteEvent (eventId);
			}
			else
			{
				event = owner.getEventManager().stopPhoneEvent(EventType.COV_4G_NO, EventType.COV_4G_YES);
				//if (event != null)
				//	owner.queueActiveTest(EventType.LATENCY_TEST, 1);
			}
		} 
		
	}
	
	public String getNetworkTypesAndroidPreference ()
	{
		String pref = "";//Settings.Global.getString(owner.getContentResolver(), Settings.Global.NETWORK_PREFERENCE);
		return pref;
	}
	public static int getNetworkGenerationAsFlags(int networkType)
	{
		int tier = getNetworkGeneration (networkType);
		int flags = 0;
		if (tier == 5)
			flags = 0x1F;
		else if (tier >= 3)
			flags = 0x7;
		else if (tier > 1)
			flags = 0x3;
		else
			flags = 0x1;
		return flags;
	}
	/**
	 * Returns an integer to represent the generation of the network type.
	 * Changed to a 5 tier designation where GPRS=tier1 and LTE=tier5
	 * @param networkType
	 * @return 0 for unknown, 2 for 2G and 3 for 3G.
	 */
	public static int getNetworkGeneration(int networkType){
		switch(networkType){
			case TelephonyManager.NETWORK_TYPE_GPRS:	// < 2g - tier 1 because data rate is <64 kbps
				return 1;
		
			case TelephonyManager.NETWORK_TYPE_1xRTT:	//2g  (aka CDMA 2000)
			case TelephonyManager.NETWORK_TYPE_CDMA:	//2g  (havent decided if plain cdma should be tier 1)
			case TelephonyManager.NETWORK_TYPE_EDGE:	//2g
				return 2;
				
			case TelephonyManager.NETWORK_TYPE_EVDO_0:	//3g
			case TelephonyManager.NETWORK_TYPE_EVDO_A:	//3g
			case TelephonyManager.NETWORK_TYPE_UMTS:	//3g
				return 3;
				
			// NEW NETWORK_TYPES - We need to rconsider these as 3G for now until we are sure of how to handle 4G 'outages'
			// because these technologies might only be active when transferring data and we don't want to treat as 4G outage when it reverts back to 3G
			case TelephonyManager.NETWORK_TYPE_HSDPA:	//3.5g
			case TelephonyManager.NETWORK_TYPE_HSPA:	//3.5g
			case TelephonyManager.NETWORK_TYPE_HSUPA:	//3.5g
			
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_HSPAP:	//3.5g HSPA+
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_EVDOB:	//3.5g
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_EHRPD:	//3.5g
				return 4;
				
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_LTE:	// true 4g
				return 5;
				
			case TelephonyManager.NETWORK_TYPE_UNKNOWN:
				return 0;
			default:
				return 1;
				
		}
	}
	
	/**
	 * Returns an integer to represent the generation of the network type.
	 * Changed to a 5 tier designation where GPRS=tier1 and LTE=tier5
	 * @param networkType
	 * @return 0 for unknown, 2 for 2G and 3 for 3G.
	 */
	public static String getNetworkName (int networkType){
		switch(networkType){
			case TelephonyManager.NETWORK_TYPE_GPRS:	// < 2g - tier 1 because data rate is <64 kbps
				return "GPRS";
		
			case TelephonyManager.NETWORK_TYPE_1xRTT:	//2g  (aka CDMA 2000)
				return "1xRTT";
			case TelephonyManager.NETWORK_TYPE_CDMA:	//2g  (havent decided if plain cdma should be tier 1)
				return "CDMA";
			case TelephonyManager.NETWORK_TYPE_EDGE:	//2g
				return "EDGE";
				
			case TelephonyManager.NETWORK_TYPE_EVDO_0:	//3g
				return "EVDO0";
			case TelephonyManager.NETWORK_TYPE_EVDO_A:	//3g
				return "EVDOA";
			case TelephonyManager.NETWORK_TYPE_HSDPA:	//3g
				return "HSDPA";
			case TelephonyManager.NETWORK_TYPE_HSPA:	//3g
				return "HSPA";
			case TelephonyManager.NETWORK_TYPE_HSUPA:	//3g
				return "HSUPA";
			case TelephonyManager.NETWORK_TYPE_UMTS:	//3g
				return "UMTS";
				
			// NEW NETWORK_TYPES - We need to rconsider these as 3G for now until we are sure of how to handle 4G 'outages'
			// because these technologies might only be active when transferring data and we don't want to treat as 4G outage when it reverts back to 3G
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_HSPAP:	//3.5g HSPA+
				return "HSPA+";
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_EVDOB:	//3.5g
				return "EVDOB";
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_EHRPD:	//3.5g
				return "eHRPD";
				
			case MMCPhoneStateListenerOld.NETWORK_NEWTYPE_LTE:	// true 4g
				return "LTE";
				
			case TelephonyManager.NETWORK_TYPE_UNKNOWN:
			default:
				return "Unknown";
		}
	}

	/**
	 * Returns whether a cell ID was recently changed. "Recently" here is defined
	 * using {@link #CELL_LOCATION_EXPIRY_FOR_DROPPED_CALL} milliseconds before now. 
	 * @return
	 */
	private boolean wasCellIdRecentlyChanged(){
		MMCCellLocationOld cellLocation = getLastCellLocation();
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();
		
		if (cellLocation == null)
			return false;
		
		//is the cell location timestamp recent?
		return (now.toMillis(true) - CELL_LOCATION_EXPIRY_FOR_DROPPED_CALL < cellLocation.getCellIdTimestamp());
	}

	public RestCommManager getRestCommManager ()
	{
		return restcommManager;
	}

	/**
	 * When the cell location gets changed, the new cellId is added to the cell id buffer in the 
	 * owner. At the same time, the CELLCHANGE event is stored.
	 */
	private long tmLastCellInfoUpdate = 0;
	private String lastCellInfoString = "";
	private List<Object> lastKnownCellInfo = null;
	
	@TargetApi(17) @Override
	public void onCellInfoChanged(List<CellInfo> cellinfos) {
		super.onCellInfoChanged(cellinfos);
		try {
			if (!owner.isMMCActiveOrRunning())
				return;
			if (tmLastCellInfoUpdate + 60000 > System.currentTimeMillis() && cellinfos != null && cellinfos.size() > 0 && cellinfos.get(0).toString().equals(lastCellInfoString))
				return;
			if (cellinfos != null && cellinfos.size() > 0)
				lastCellInfoString = cellinfos.get(0).toString();
			else
				lastCellInfoString = "";	
			tmLastCellInfoUpdate = System.currentTimeMillis();
			
			if (getNetworkType() == this.NETWORK_NEWTYPE_LTE)
			{
				String neighbors = owner.getCellHistory().updateLteNeighborHistory(cellinfos);
				if (neighbors != null)
				{
					owner.getIntentDispatcher().updateLTEIdentity (neighbors);
					owner.getReportManager().setNeighbors(neighbors);
				}
				
			}
			
			if (cellinfos != null && cellinfos.size() > 0 && cellinfos.get(0) != null)
				for (int i=0; i<cellinfos.size(); i++)
				{
					//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCellInfoChanged", "cellinfos["+i+"]: " + cellinfos.get(i).toString());
					if (getNetworkType() == this.NETWORK_NEWTYPE_LTE)
					{
						if (cellinfos.get(i) instanceof CellInfoLte)
						{
							CellIdentityLte cellIDLte = ((CellInfoLte)cellinfos.get(i)).getCellIdentity();
							//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCellInfoChanged", "Reflected: " + listCellInfoFields(cellIDLte));
						}
					}
				}
			//else
			//	MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onCellInfoChanged", "cellinfos: null");
			
		} catch (Exception intEx){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "onCellInfoChanged", "InterruptedException: " + intEx.getMessage());
		} 
	}

    // If this is a CDMACellLocation without SID and NID, see if we can extract it from the ServiceState
    private void checkCDMACellSID (CellLocation cell)
    {
        if (cell instanceof CdmaCellLocation)
        {
            CdmaCellLocation cdmaCell = (CdmaCellLocation)cell;
            if (cdmaCell.getSystemId() <= 0)
            {
                Field getSIDPointer = null;
                Field getNIDPointer = null;
                int SID = 0, NID = 0, BID = cdmaCell.getBaseStationId();
                try {
                    getSIDPointer = previousServiceStateObj.getClass().getDeclaredField("mSystemId");
                    if (getSIDPointer != null)
                    {
                        getSIDPointer.setAccessible(true);
                        SID = (int) getSIDPointer.getInt(cdmaCell);
                    }
                    getNIDPointer = previousServiceStateObj.getClass().getDeclaredField("mNetworkId");
                    if (getNIDPointer != null)
                    {
                        getNIDPointer.setAccessible(true);
                        NID = (int) getNIDPointer.getInt(cdmaCell);
                    }
                    cdmaCell.setCellLocationData(BID, cdmaCell.getBaseStationLatitude(), cdmaCell.getBaseStationLongitude(),
                                                SID, NID); // Update the SID and NID that we read from teh Servicestate
                } catch (Exception e) {
                    //MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "checkInnerGsmCellLocation","Field does not exist - mGsmCellLoc");
                }
            }
        }
    }
	// See if this cellLocation has inner GsmLocation
	private void checkInnerGsmCellLocation (CellLocation cell)
	{
		if (cell != null)
		{
			String strCells = "";
			
			Field getFieldPointer = null;
			try {
				getFieldPointer = cell.getClass().getDeclaredField("mGsmCellLoc"); //NoSuchFieldException 

			} catch (Exception e) {
				//MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "checkInnerGsmCellLocation","Field does not exist - mGsmCellLoc");
			}
			if (getFieldPointer != null){
				//now we're in business!
				try {
                    getFieldPointer.setAccessible(true);
                    GsmCellLocation gsmCell = (GsmCellLocation) getFieldPointer.get(cell);
					if (gsmCell != null)
					{
						int bsHigh = gsmCell.getLac();
						int bsLow = gsmCell.getCid();
						MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "checkInnerGsmCellLocation","Obtained mGsmCellLoc LAC=" + gsmCell.getLac() + " toString=" + gsmCell.toString() );
						
						if (getNetworkType() == this.NETWORK_NEWTYPE_LTE)
						{
                            int psc = 0;
                            if (android.os.Build.VERSION.SDK_INT >= 10)
                                psc = gsmCell.getPsc();
							String neighbors = owner.getCellHistory().updateLteNeighborHistory(bsHigh,bsLow,psc);
							owner.getIntentDispatcher().updateLTEIdentity (neighbors);
							owner.getReportManager().setNeighbors(neighbors);
						}
					}
				} catch (Exception e) {
					Log.d(TAG, "Could not get the inner GSM", e);
				}
			}
		}
	}

	private String listCellLocationFields (CellLocation cell)
	{
		if (cell != null)
		{
			String strCells = "";
			
			Field[] fields = null;
			try {
				fields = cell.getClass().getDeclaredFields();
				int i;
				for (i=0; i<fields.length; i++)
				{
					fields[i].setAccessible(true);
					if (!fields[i].getName().equals("CREATOR") && !fields[i].getName().equals("LOG_TAG") &&
							fields[i].getName().indexOf("INVALID") == -1 && fields[i].getName().indexOf("STRENGTH") == -1)
					{
						strCells += fields[i].getName() + "=";
						if (fields[i].get(cell) == null)
							strCells += "null";
						else
							strCells += fields[i].get(cell).toString() + ",";
					}
				}
				
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", strSignals);
				return strCells;
			} catch (SecurityException e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listCellFields", "SecurityException", e);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listCellFields", "exception", e);
			}
		}
		return "";
	}
	
	private String listCellInfoFields (CellIdentityLte cell)
	{
		if (cell != null)
		{
			String strCells = "";
			
			Field[] fields = null;
			try {
				fields = cell.getClass().getDeclaredFields();
				int i;
				for (i=0; i<fields.length; i++)
				{
					fields[i].setAccessible(true);
					if (!fields[i].getName().equals("CREATOR") && !fields[i].getName().equals("LOG_TAG") &&
							fields[i].getName().indexOf("INVALID") == -1 && fields[i].getName().indexOf("STRENGTH") == -1)
					{
						strCells += fields[i].getName() + "=";
						if (fields[i].get(cell) == null)
							strCells += "null";
						else
							strCells += fields[i].get(cell).toString() + ",";
					}
				}
				
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listSignalFields", strSignals);
				return strCells;
			} catch (SecurityException e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listCellInfoFields", "SecurityException", e);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listCellInfoFields", "exception", e);
			}
		}
		return "";
	}
	
	private String listServiceStateFields (ServiceState cell)
	{
		if (cell != null)
		{
			String strCells = "";
			
			Field[] fields = null;
			try {
				fields = cell.getClass().getDeclaredFields();
				int i;
				for (i=0; i<fields.length; i++)
				{
					fields[i].setAccessible(true);
					if (fields[i].getName().indexOf("m") == 0 )
					{
						strCells += fields[i].getName() + "=";
						if (fields[i].get(cell) == null)
							strCells += "null";
						else
							strCells += fields[i].get(cell).toString() + ",";
					}
				}
				
				return strCells;
			} catch (SecurityException e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listServiceStateFields", "SecurityException", e);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "listServiceStateFields", "exception", e);
			}
		}
		return "";
	}

	/**
	 * Available radio technologies for GSM, UMTS and CDMA. Needs to be Converted to NETWORK_TYPE
	 */
	public static final int RIL_RADIO_TECHNOLOGY_UNKNOWN = 0;
	public static final int RIL_RADIO_TECHNOLOGY_GPRS = 1;
	public static final int RIL_RADIO_TECHNOLOGY_EDGE = 2;
	public static final int RIL_RADIO_TECHNOLOGY_UMTS = 3;
	public static final int RIL_RADIO_TECHNOLOGY_IS95A = 4;
	public static final int RIL_RADIO_TECHNOLOGY_IS95B = 5;
	public static final int RIL_RADIO_TECHNOLOGY_1xRTT = 6;
	public static final int RIL_RADIO_TECHNOLOGY_EVDO_0 = 7;
	public static final int RIL_RADIO_TECHNOLOGY_EVDO_A = 8;
	public static final int RIL_RADIO_TECHNOLOGY_HSDPA = 9;
	public static final int RIL_RADIO_TECHNOLOGY_HSUPA = 10;
	public static final int RIL_RADIO_TECHNOLOGY_HSPA = 11;
	public static final int RIL_RADIO_TECHNOLOGY_EVDO_B = 12;
	public static final int RIL_RADIO_TECHNOLOGY_EHRPD = 13;
	public static final int RIL_RADIO_TECHNOLOGY_LTE = 14;
	public static final int RIL_RADIO_TECHNOLOGY_HSPAP = 15;
	// GSM radio technology only supports voice. It does not support data.
	public static final int RIL_RADIO_TECHNOLOGY_GSM = 16;
	public static final int RIL_RADIO_TECHNOLOGY_TD_SCDMA = 17;
	// IWLAN
	public static final int RIL_RADIO_TECHNOLOGY_IWLAN = 18;
	private static int rilRadioTechnologyToNetworkType(int rt) {
		switch(rt) {
			case RIL_RADIO_TECHNOLOGY_GPRS:
				return TelephonyManager.NETWORK_TYPE_GPRS;
			case RIL_RADIO_TECHNOLOGY_EDGE:
				return TelephonyManager.NETWORK_TYPE_EDGE;
			case RIL_RADIO_TECHNOLOGY_UMTS:
				return TelephonyManager.NETWORK_TYPE_UMTS;
			case RIL_RADIO_TECHNOLOGY_HSDPA:
				return TelephonyManager.NETWORK_TYPE_HSDPA;
			case RIL_RADIO_TECHNOLOGY_HSUPA:
				return TelephonyManager.NETWORK_TYPE_HSUPA;
			case RIL_RADIO_TECHNOLOGY_HSPA:
				return TelephonyManager.NETWORK_TYPE_HSPA;
			case RIL_RADIO_TECHNOLOGY_IS95A:
			case RIL_RADIO_TECHNOLOGY_IS95B:
				return TelephonyManager.NETWORK_TYPE_CDMA;
			case RIL_RADIO_TECHNOLOGY_1xRTT:
				return TelephonyManager.NETWORK_TYPE_1xRTT;
			case RIL_RADIO_TECHNOLOGY_EVDO_0:
				return TelephonyManager.NETWORK_TYPE_EVDO_0;
			case RIL_RADIO_TECHNOLOGY_EVDO_A:
				return TelephonyManager.NETWORK_TYPE_EVDO_A;
			case RIL_RADIO_TECHNOLOGY_EVDO_B:
				return TelephonyManager.NETWORK_TYPE_EVDO_B;
			case RIL_RADIO_TECHNOLOGY_EHRPD:
				return TelephonyManager.NETWORK_TYPE_EHRPD;
			case RIL_RADIO_TECHNOLOGY_LTE:
				return TelephonyManager.NETWORK_TYPE_LTE;
			case RIL_RADIO_TECHNOLOGY_HSPAP:
				return TelephonyManager.NETWORK_TYPE_HSPAP;
			case RIL_RADIO_TECHNOLOGY_GSM:
				return NETWORK_NEWTYPE_GSM;
			case RIL_RADIO_TECHNOLOGY_TD_SCDMA:
				return NETWORK_NEWTYPE_TD_SCDMA;
			case RIL_RADIO_TECHNOLOGY_IWLAN:
				return NETWORK_NEWTYPE_IWLAN;
			default:
				return TelephonyManager.NETWORK_TYPE_UNKNOWN;
		}
	}
	
}
