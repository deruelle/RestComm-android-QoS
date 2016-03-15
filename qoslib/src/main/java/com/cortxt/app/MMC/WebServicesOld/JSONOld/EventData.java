package com.cortxt.app.MMC.WebServicesOld.JSONOld;

import java.lang.reflect.Method;
import java.util.BitSet;
import java.util.TimeZone;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiConfiguration;

import com.cortxt.app.MMC.ServicesOld.Events.EventOld;

import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.TrendStringGenerator.CoverageSamplesSend;

public class EventData {
	
	private int dataSpeed;
	/**
	 * The original intention was to calculate the milliseconds between the attempt to start
	 * a call and the actual 'ring'. But currently on the blackberry the value sent is 0.
	 * 
	 * This is deprecated because the event data envelope does have this variable.
	 */
	private int iConnectTime;
	/**
	 * The quality of serice rating. An integer from -1 to 5.
	 * -1 stands for unknown which should be used during an average call or 
	 * when not prompting for voice quality.
	 * 
	 * 0 means dropped call and 1-5 is when the user supplies a star rating.
	 */
	//@Deprecated
	//private int QOSRating;
	/**
	 * 
	 */
	
	private long WifiID;
	/**
	 * 
	 */
	
	private long WifiSec;
	/**
	 * 
	 */
	
	private long WifiFreq;
	/**
	 * 
	 */
	//@Deprecated
	//private int WifiSig1;
	/**
	 * 
	 */
	//@Deprecated
	//private int WifiSig2;
	/**
	 * 
	 */
	//@Deprecated
	//private int WifiSig3;
	/**
	 * The phone number that you called.
	 */
	private String txtPhoneNumber;
	/**
	 * A string that describes the carrier.
	 */
	private String Carrier;
	/**
	 * "Android" + <the model name of the phone>
	 */
	private String Handset;
	/**
	 * IP Address when event was staged
	 */
	private String IPv4;
	/**
	 * For generating the statistics on coverage, the server depends on 
	 * the clients (mobile phones) to chain the various event durations
	 * properly. What this means is that there are some "event groups"
	 * that have to have their durations depend on other events within
	 * the group. Lets take an example.
	 * 
	 * Lets assume that the user is in the middle of a call.
	 * Lets assume that the signal strength drops to below -110dbm in
	 * the middle of the call at 5:00pm. Lets assume that the signal strength
	 * returns to a value higher than -90dbm at 5:10pm. This second event would
	 * count as a QOS_STRONG event. The duration of this event would be reported
	 * as 10 minutes (calculated as the difference between 5:10pm and 5:00pm).
	 * 
	 * Lets say that a regular update event had occurred between these 2 events
	 * at 5:05pm. Then this update event would be reported to the server with a 
	 * duration of 5 minutes (calculated as the difference between 5:05pm and 5:00pm).
	 * The next event (when signal strength rises above 5:10pm) would be reported 
	 * with a duration of 5 minutes as well (calculated as the difference between
	 * 5:10pm and 5:05pm).
	 * 
	 * This process of keeping the duration of an event dependent on the timestamp
	 * of the previous event does not universally apply to all the events. Event 
	 * chaining is done for EVT_QOS_WEAK, UPDATE, EVT_QOS_STRONG as one group, 
	 * 3g_lost, 3g_regained and UPDATE as another group and 4g_lost, 4g_regained
	 * and UPDATE as yet another group.
	 */
	private long iDuration;
	/**
	 * The main trend string. It contains the signal lat/long, signal strength, repeat count etc.
	 * 
	 * {Signal/Flags/<Repeat Count>/<Base station ID (in hex)>},Latitude, Longitude
	 * 
	 * The signal is dbm.
	 * Flags is a 4-bit integer in the format { 4g coverage, 3g coverage, data coverage, voice coverage}
	 * Repeat count is the number of times the group needs to be repeated. That is, it would be 0 if there was
	 * no repetition required and 1 if one repetition was required.
	 * The base station is in hex where the higher word is the major identifier (LAC/SID depending on the technology being used)
	 * and the lower word is the minor identifier (CID/BID depending on the technology being used). 
	 * The base station parameter is very likely to change in the future and this document will be updated then.
	 */
	private CoverageSamplesSend Trend;
	/**
	 * Trend2 is the additional trend information for the user statistics for the 3 hour period.
	 * 
	 * For the Android alpha this would be left blank.
	 */
	private String Trend2;
	/**
	 * List of neighbor cells detected during event.
	 */
	private String Neighbors;
	/**
	 * List of connection states detected during event.
	 */
	private String Connects;
	/**
	 * That is the timestamp of the start of the event (in mills and in UTC).
	 */
	private long lTimestamp, lTimeLocal;
	/**
	 * This is the timestamp of the gps fix used in this event (in mills and in UTC).
	 */
	private long lGpsTimestamp;
	/**
	 * The timestamp (in milliseconds) of the first event (in UTC). This has to use System.currentTimeMillis().
	 */
	//private long lStartTime;
	/**
	 * Event Longitude in float
	 */
	private float fltEventLng;
	/**
	 * Event Latitude in float
	 */
	private float fltEventLat;
	/**
	 * The event type integer that comes from {@link EventType}.
	 */
	private int iEventType;
	/**
	 * The server-side event id of this event.
	 */
	private long iEvtID;
	/**
	 * Generated by the server.
	 * This is the id of the complimentary event that may accompany this event in the event-couple.
	 */
	private long RefEvtID;
	/**
	 * This variable can contain event-type specific information.
	 * 
	 * When a number of outages occur side-by-side, then they are combined into one single outage. The 
	 * number of outages combined to do so are stored in this variable.
	 * 
	 * In the case of a dropped call, this can be used to suggest the reason behind the call failure.
	 */
	private int EventIndex;
	/**
	 * 
	 */
	@Deprecated
	private int SampleInterval;
	
	/**
	 * The altitude at which the event took place. (in meters)
	 */
	private int iAltitude;
	/**
	 * The uncertainty of the location of this event.(in meters)
	 */
	private int iUncertainty;
	/**
	 * The speed of the device when this event took place.(in meters/second)
	 */
	private int iSpeed;
	/**
	 * The heading of the device when this event took place. (in degrees east of north)
	 */
	private int iHeading;
	/**
	 * The number of satellites used in obtaining the fix.
	 */
	private int iNumSatellites;
	/**
	 * The signal strength (or rssi) in dbm.
	 */
	private int iSignal;
	/**
	 * The cell Id for gsm and bid for cdma.
	 * This is the cell ID at the exact time at the event.
	 */
	private int iCellId;
	/**
	 * This is the Network ID for CDMA and will be 0 for gsm phones.
	 */
	private int NID;
	private int prevNID;
	/**
	 * The PSC
	 */
	private int PSC;
	/**
	 * This is the version number of the Android app.
	 */
	private int Version;
	/**
	 * The battery level. On a scale of 0-100.
	 */
	private int Battery;
	/**
	 * The frequency band
	 */
	private int band;
	/**
	 * 
	 */
	@Deprecated
	private int Rssi;
	/**
	 * Flags is a 4-bit integer in the format { 4g coverage, 3g coverage, data coverage, voice coverage}
	 */
	private int Flags;
	private int tier;
	private long id;
	/**
	 * This is internal to the server and is almost always 0.
	 */
	@Deprecated
	private long CallID;
	/**
	 * The ID of the user. This does not have to be filled in by the client but will be present when the 
	 * client requests this event from the server.
	 */
	private int iUser;
	/**
	 * The MCC.
	 */
	private int MCC;
	/**
	 * The MNC
	 */
	private int MNC;
	/**
	 * The LAC.
	 */
	private int LAC;
	
	/**
	 * the API Key, to identify the device and the user
	 */
	private String ApiKey;
	
	/**
	 * The cause of a dropped call (usually 'UNSPECIFIED').
	 */
	private String comment;
	/**
	 * 
	 */
	//private String zip;
	/*
	 * downloadSpeed in bytes/sec
	 */
	private Integer downloadSpeed;
	/*
	 * uploadSpeed in bytes/sec
	 */
	private Integer uploadSpeed;
	/*
	 * downloadSpeed in bytes/sec
	 */
	private Integer latency;
	/*
	 * downloadSpeed in bytes/sec
	 */
	private Integer downloadSize;
	/*
	 * uploadSpeed in bytes/sec
	 */
	private Integer uploadSize;
	
	// Linux is expecting eventIndex to give the duration for 3G, but windows gets it in the eventIndex
	private Integer duration3G;
	private String Stats;
	private String APs;
	private String Apps, AppName;
	//private String ThreeHRLatency;
	
	//These are used for Transit Sampling
	private long lookupid1;
	private long lookupid2;
	
	///////////////////////////////////
	// Getters and Setters
	///////////////////////////////////
	
	public int getDataSpeed() {
		return dataSpeed;
	}
	public void setDataSpeed(int dataSpeed) {
		this.dataSpeed = dataSpeed;
	}
	public int getiConnectTime() {
		return iConnectTime;
	}
	public void setiConnectTime(int iConnectTime) {
		this.iConnectTime = iConnectTime;
	}
//	public int getQOSRating() {
//		return QOSRating;
//	}
//	public void setQOSRating(int qOSRating) {
//		QOSRating = qOSRating;
//	}
//	public long getWifiID1() {
//		return WifiID;
//	}
	public void setWifi(WifiInfo wifiInfo, WifiConfiguration wifiConfig) {
		if (wifiInfo == null)
			return;
		
		String macid = wifiInfo.getBSSID();
		if (macid == null)
			return;
		String[] bytes = macid.split(":");
		long bssid = 0;
		for (int i=0; i<6; i++)
		{
			if (i < bytes.length)
			{
				long v = hexval(bytes[i]);
				bssid = bssid + (v<<((5-i)*8));
			}
		}
		WifiID = bssid;
		
		if (wifiConfig != null)
		{
			int bits = 0;
			for (int i=0; i<4; i++)
			{
				if (wifiConfig.allowedKeyManagement.get(i))
					bits += 1<<i;
			}
			BitSet bs = new BitSet ();
			//bs.set(WifiConfiguration.KeyMgmt.NONE);
			//if (wifiConfig.allowedKeyManagement..allowedAuthAlgorithms.intersects(bs))
				WifiSec = bits;
		}
		
		int freq = getWifiFrequency(wifiInfo);
		if (freq != -1)
			WifiFreq = freq;
		
	}
//	public long getWifiID2() {
//		return WifiSec;
//	}
//	public void setWifiID2(long wifiID2) {
//		WifiSec = wifiID2;
//	}
//	public long getWifiID3() {
//		return WifiFreq;
//	}
//	public void setWifiID3(long wifiID3) {
//		WifiFreq = wifiID3;
//	}
//	public int getWifiSig1() {
//		return WifiSig1;
//	}
//	public void setWifiSig1(int wifiSig1) {
//		WifiSig1 = wifiSig1;
//	}
//	public int getWifiSig2() {
//		return WifiSig2;
//	}
//	public void setWifiSig2(int wifiSig2) {
//		WifiSig2 = wifiSig2;
//	}
//	public int getWifiSig3() {
//		return WifiSig3;
//	}
//	public void setWifiSig3(int wifiSig3) {
//		WifiSig3 = wifiSig3;
//	}
	public String getTxtPhoneNumber() {
		return txtPhoneNumber;
	}
	public void setTxtPhoneNumber(String txtPhoneNumber) {
		this.txtPhoneNumber = txtPhoneNumber;
	}
	public String getCarrier() {
		return Carrier;
	}
	public void setCarrier(String carrier) {
		Carrier = carrier;
	}
	public String getHandset() {
		return Handset;
	}
	public void setHandset(String handset) {
		Handset = handset;
	}
	public long getiDuration() {
		return iDuration;
	}
	public void setiDuration(long iDuration) {
		this.iDuration = iDuration;
	}
	public CoverageSamplesSend getTrend() {
		return Trend;
	}
	public void setTrend(CoverageSamplesSend trend) {
		Trend = trend;
	}
	public String getTrend2() {
		return Trend2;
	}
	public void setTrend2(String trend2) {
		Trend2 = trend2;
	}
	public String getIPv4() {
		return IPv4;
	}
	public void setIPv4(String IP) {
		IPv4 = IP;
	}
	public long getlTimestamp() {
		return lTimestamp;
	}
	public void setlTimestamp(long lTimestamp) {
		this.lTimestamp = lTimestamp;
	}
	public long getlGpsTimestamp() {
		return lGpsTimestamp;
	}
	public void setlGpsTimestamp(long lGpsTimestamp) {
		this.lGpsTimestamp = lGpsTimestamp;
	}
	
	public float getFltEventLng() {
		return fltEventLng;
	}
	public void setFltEventLng(float fltEventLng) {
		this.fltEventLng = fltEventLng;
	}
	public float getFltEventLat() {
		return fltEventLat;
	}
	public void setFltEventLat(float fltEventLat) {
		this.fltEventLat = fltEventLat;
	}
	public int getEventType() {
		return iEventType;
	}
	public void setEventType(int iEventType) {
		this.iEventType = iEventType;
	}
	public long getiEvtID() {
		return iEvtID;
	}
	public void setiEvtID(long iEvtID) {
		this.iEvtID = iEvtID;
	}
	public long getRefEvtID() {
		return RefEvtID;
	}
	public void setRefEvtID(long refEvtID) {
		RefEvtID = refEvtID;
	}
	public int getEventIndex() {
		return EventIndex;
	}
	public void setEventIndex(int eventIndex) {
		EventIndex = eventIndex;
	}
	public int getSampleInterval() {
		return SampleInterval;
	}
	public void setSampleInterval(int sampleInterval) {
		SampleInterval = sampleInterval;
	}
	
	public int getiAltitude() {
		return iAltitude;
	}
	public void setiAltitude(int iAltitude) {
		this.iAltitude = iAltitude;
	}
	public int getiUncertainty() {
		return iUncertainty;
	}
	public void setiUncertainty(int iUncertainty) {
		this.iUncertainty = iUncertainty;
	}
	public int getiSpeed() {
		return iSpeed;
	}
	public void setiSpeed(int iSpeed) {
		this.iSpeed = iSpeed;
	}
	public int getiHeading() {
		return iHeading;
	}
	public void setiHeading(int iHeading) {
		this.iHeading = iHeading;
	}
	public int getiNumSatellites() {
		return iNumSatellites;
	}
	public void setiNumSatellites(int iNumSatellites) {
		this.iNumSatellites = iNumSatellites;
	}
	public int getiSignal() {
		return iSignal;
	}
	public void setiSignal(int iSignal) {
		this.iSignal = iSignal;
	}
	public int getiCellId() {
		return iCellId;
	}
	public void setiCellId(int iCellId) {
		this.iCellId = iCellId;
	}
	public int getNID() {
		return NID;
	}
	public void setNID(int nID) {
		NID = nID;
	}
	public String getMCC() {
		return Integer.toString(MCC);
	}
	public void setMCC(int mCC) {
		MCC = mCC;
	}
	public int getMNC() {
		return MNC;
	}
	public void setMNC(int mNC) {
		MNC = mNC;
	}
//	public int getPrevCell() {
//		return prevCell;
//	}
//	public void setPrevCell(int prevCell) {
//		this.prevCell = prevCell;
//	}
	public int getPrevNID() {
		return prevNID;
	}
	public void setPrevNID(int prevNID) {
		this.prevNID = prevNID;
	}
	public int getVersion() {
		return Version;
	}
	public void setVersion(int version) {
		Version = version;
	}
	public int getBattery() {
		return Battery;
	}
	public void setBattery(int battery) {
		Battery = battery;
	}
	public int getBand() {
		return band;
	}
	public void setBand(int band) {
		this.band = band;
	}
	public int getRssi() {
		return Rssi;
	}
	public void setRssi(int rssi) {
		Rssi = rssi;
	}
	public int getFlags() {
		return Flags;
	}
	public void setFlags(int flags) {
		Flags = flags;
		tier = 0;
		if ((flags & EventOld.SERVICE_4G) == EventOld.SERVICE_4G)
			tier = 5;
		else if ((flags & EventOld.SERVICE_3G) == EventOld.SERVICE_3G)
			tier = 3;
		else if ((flags & EventOld.SERVICE_DATA) == EventOld.SERVICE_DATA)
			tier = 2;	
		else if ((flags & EventOld.SERVICE_VOICE) == EventOld.SERVICE_VOICE)
			tier = 1;	
	}
	public int getTier ()
	{
		return tier;
	}
	public void setTier (int value)
	{
		tier = value;
	}
	public long getCallID() {
		return CallID;
	}
	public void setCallID(long callID) {
		CallID = callID;
	}
	public int getiUser() {
		return iUser;
	}
	public void setiUser(int iUser) {
		this.iUser = iUser;
	}
	public String getApiKey() {
		return ApiKey;
	}
	public void setApiKey(String api) {
		this.ApiKey = api;
	}
	
	public int getLAC() {
		return LAC;
	}
	public void setLAC(int lAC) {
		LAC = lAC;
	}
	
	public int getDownloadSpeed() {
		if (downloadSpeed == null)
			return 0;
		return downloadSpeed;
	}
	public void setDownloadSpeed(int speed) {
		downloadSpeed = speed;
	}
	
	public int getUploadSpeed() {
		if (uploadSpeed == null)
			return 0;
		return uploadSpeed;
	}
	public void setUploadSpeed(int speed) {
		uploadSpeed = speed;
	}
	
	public int getDownloadSize() {
		if (downloadSize == null)
			return 0;
		return downloadSize;
	}
	public void setDownloadSize(int size) {
		downloadSize = size;
	}
	
	public int getUploadSize() {
		if (uploadSize == null)
			return 0;
		return uploadSize;
	}
	public void setUploadSize(int size) {
		uploadSize = size;
	}
	
	public int getLatency() {
		if (latency == null)
			return 0;
		return latency;
	}
	public void setLatency(int speed) {
		latency = speed;
	}
	
	public String getCause() {
		return comment;
	}
	public void setCause(String _comment) {
		this.comment = _comment;
	}
	public String getStats() {
		return Stats;
	}
	public void setStats(String stats) {
		Stats = stats;
	}
	public String getAccessPoints() {
		return APs;
	}
	public void setAccessPoints(String accessPoints) {
		APs = accessPoints;
	}
	public String getRunningApps() {
		return APs;
	}
	public void setRunningApps(String runningApps) {
		Apps = runningApps;
	}
	public void setAppName(String app) {
		AppName = app;
	}
	
//	public String getThreeHRLatency() {
//		return ThreeHRLatency;
//	}
//	
//	public void setLatency(String newThreeHRLatency) {
//		ThreeHRLatency = newThreeHRLatency;
//	}	
	
	public void setEventId (long evtid)
	{
		id = evtid;
	}
	
	private int hexval (String s)
	{
		int val = 0;
		if (s.length() < 2)
			return 0;
		char a = s.charAt(0);
		if (a>='0' && a<='9')
			val = (int)(a-'0')<<4;
		else if (a>='a' && a<= 'f')
			val = (int)(a-'a'+10)<<4;
		else if (a>='A' && a<= 'F')
			val = (int)(a-'A'+10)<<4;
		
		a = s.charAt(1);
		if (a>='0' && a<='9')
			val += (int)(a-'0');
		else if (a>='a' && a<= 'f')
			val += (int)(a-'a'+10);
		else if (a>='A' && a<= 'F')
			val += (int)(a-'A'+10);
		return val;
	}
	

	public void setLookupid1(long l) {
		this.lookupid1 = l;
	}
	
	public long getLookupid1() {
		return lookupid1;
	}
	
	public void setLookupid2(int lookupid2) {
		this.lookupid2 = lookupid2;
	}
	
	public long getLookupid2() {
		return lookupid2;
	}

	private static int getWifiFrequency (WifiInfo wifiInfo)
	{
		int returnValue = -1;
		try {
			Method freqMethod = WifiInfo.class.getMethod("getFrequency", (Class[]) null);
			if (freqMethod != null){
				//now we're in business!
				returnValue = (Integer) freqMethod.invoke(wifiInfo, (Object[]) null);
			}
		} catch (Exception e) {
		} 
		return returnValue;
		
	}

	///////////////////////////
	// Constructors
	///////////////////////////
	
	
	public EventData(
			int dataSpeed, 
			int iConnectTime,
			String txtPhoneNumber, 
			String carrier,
			String handset, 
			long iDuration, 
			CoverageSamplesSend trend, 
			String trend2,
			String neighbors,
			String connections,
			long lTimestamp, 
			long lGpsTimestamp, 
			long lStartTime,
			float fltEventLng, 
			float fltEventLat, 
			int iEventType, 
			long iEvtID,
			long refEvtID, 
			int eventIndex, 
			int sampleInterval, 
			int iAltitude, 
			int iUncertainty, 
			int iSpeed,
			int iHeading, 
			int iNumSatellites, 
			int iSignal, 
			int iCellId,
			int nID, 
			int psc, 
			int version, 
			int battery, 
			int rssi,
			int flags, 
			long callID, 
			int iUser, 
			int mCC, 
			int mNC, 
			int lAC,
			String _comment,
			String _apikey,
			String IP,
			String stats,
			String accessPoints,
			String runningApps,
			String threeHRLatency,
			long stationFrom,
			long lookupid2) {

		//this.dataSpeed = dataSpeed;
		this.iConnectTime = iConnectTime;
		//QOSRating = qOSRating;
		//WifiID1 = wifiID1;
		//WifiID2 = wifiID2;
		//WifiID3 = wifiID3;
		//WifiSig1 = wifiSig1;
		//WifiSig2 = wifiSig2;
		//WifiSig3 = wifiSig3;
		this.txtPhoneNumber = txtPhoneNumber;
		this.Carrier = carrier;
		this.Handset = handset;
		this.IPv4 = IP;
		this.iDuration = iDuration;
		this.Trend = trend;
		this.Trend2 = trend2;
		this.Neighbors = neighbors;
		this.Connects = connections;
		this.lTimestamp = lTimestamp;
		this.lGpsTimestamp = lGpsTimestamp;
		//this.lStartTime = lStartTime;
		this.fltEventLng = fltEventLng;
		this.fltEventLat = fltEventLat;
		this.iEventType = iEventType;
		this.iEvtID = iEvtID;
		this.RefEvtID = refEvtID;
		this.EventIndex = eventIndex;
		//SampleInterval = sampleInterval;
		this.iAltitude = iAltitude;
		this.iUncertainty = iUncertainty;
		this.iSpeed = iSpeed;
		this.iHeading = iHeading;
		this.iNumSatellites = iNumSatellites;
		this.iSignal = iSignal;
		this.iCellId = iCellId;
		NID = nID;
		this.PSC = psc;
		Version = version;
		this.Battery = battery;
		//Rssi = rssi;
		setFlags ( flags);
		//CallID = callID;
		this.iUser = iUser;
		this.MCC = mCC;
		this.MNC = mNC;
		this.LAC = lAC;
		this.comment = _comment;
		this.ApiKey = _apikey;

		int gmtOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis());
		this.lTimeLocal = (this.lTimestamp + gmtOffset/1000);// / 1000;
		this.Stats = stats;	
		this.APs = accessPoints;
		this.Apps = runningApps;
		this.lookupid1 = stationFrom;
		this.lookupid2 = lookupid2;

		
		if (eventIndex == EventType.COV_UPDATE.getIntValue() && eventIndex > 0)
			duration3G = eventIndex;
	}
}
