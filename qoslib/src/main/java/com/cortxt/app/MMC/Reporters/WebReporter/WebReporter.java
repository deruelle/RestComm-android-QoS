package com.cortxt.app.MMC.Reporters.WebReporter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Pair;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MMCGSMDevice;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.UtilsOld.Carrier;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventDataEnvelope;
import com.cortxt.app.MMC.WebServicesOld.JSONOld.EventResponse;
import com.google.gson.Gson;
import com.securepreferences.SecurePreferences;

/**
 * WebReporter is in charge of reporting events to a web server 
 * @author estebanginez
 *
 */
public class WebReporter  {

	public static final String TAG = WebReporter.class.getSimpleName();
	public static final String EVENTS_QUEUE_KEY_PREFERENCE = "EVENTS_QUEUE_KEY_PREFERENCE";
	private static final String PREFERENCE_KEY_ENABLE_DATA_ROAMING = "KEY_SETTINGS_ENABLE_DATA_ROAMING";
	
	public static final String JSON_API_KEY= "apiKey";
	public static final String USER_ID_KEY= "userID";
	private static final String JSON_ERROR_KEY= "errors";
	private static final String JSON_NETWORK_KEY = "operator";
	private static final String JSON_NETWORKS_KEY = "networks";
	private static final String JSON_NETWORK_ID_KEY = "_id";
	private static final String JSON_NETWORK_LOGOPATH_KEY = "path";
	private static final String JSON_NETWORK_TWITTERHANDLE_KEY = "twitter";
	
	protected Context mContext;
	//protected HttpClient mHttpClient;
	protected String mHost;
	protected static String mStaticAssetURL;
	
	/**
	 * It is used to make sure the queue of events gets sent, by keeping the cpu awake
	 */
	private WakeLock mFlushQueueWakeLock;

	protected ConcurrentLinkedQueue<Request> mRequestQueue;
	/**
	 * The api key that the server requires for each request that
	 * gets sent to the server
	 */
	private String mApiKey;

//	protected ReportingThread mReportingThread;
//
//	/**
//	 * Thread that iterates through {@link WebReporter#mRequestQueue} and sends every request to the server,
//	 * and waits when the queue is empty or there is no network connection
//	 * @author nasrullah
//	 *
//	 */
//	protected class ReportingThread extends Thread {
//		public ReportingThread() {
//
//		}
//
//		@Override
//		public void run() {
//
//			while(!mRequestQueue.isEmpty()) {
//				try {
//					Request request = mRequestQueue.peek();
//                    MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "ReportingThread", "request = " + request.getURI().toString());
//
//                    HttpClient mHttpClient = HttpUtils.createHttpClient();
//					HttpResponse response = mHttpClient.execute(request);
//
//                    WebReporter.this.verifyResponse(response);
//					mRequestQueue.poll();
//				} catch (ClientProtocolException e) {
//					MMCLogger.logToFile(MMCLogger.Level.ERROR, "ReportingThread", "run", "Could not send event", e);
//					//the error was in the request data (not the connection), so remove it from the queue
//					mRequestQueue.poll();
//				} catch (IOException e) {
//					MMCLogger.logToFile(MMCLogger.Level.WARNING, "ReportingThread", "run", "Could not send event", e);
//					//could not connect over network
//					//leave request in queue to send when next event is sent
//					break;
//				} catch (MMCException e) {
//					//already logged in verifyResponse
//					//the error was in the request data (not the connection), so remove it from the queue
//					mRequestQueue.poll();
//				}
//			}
//			WebReporter.this.mFlushQueueWakeLock.release();
//		}
//	}



	public WebReporter(Context context) {
		mContext = context;

		//Retrieve api key if it exists
		mApiKey = MMCService.getApiKey(mContext);
		//Retrieve the host from the strings.xml

		mHost = mContext.getString(R.string.MMC_URL_LIN);
		mStaticAssetURL = mContext.getString(R.string.MMC_STATIC_ASSET_URL);

		mRequestQueue = new ConcurrentLinkedQueue<Request>();
		
		mFlushQueueWakeLock = ((PowerManager)mContext.getSystemService(Context.POWER_SERVICE))
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mFlushQueueWakeLock.setReferenceCounted(false);
	}

	/**
	 * Cleans up any used resources. Saves all events that have not been sent
	 */
	public void stop() {
		saveEvents();
	}

	/**
	 * Persists the queue of events to the phone's preferences
	 */
	protected void saveEvents(){
		JSONArray jsonQueue= new JSONArray();
		for(Request request: mRequestQueue){
			try {
				jsonQueue.put(request.serialize());
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "persistQueue", "failed to persist event request", e);
			}
		}

		SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
		String stringQueue = jsonQueue.toString();

		MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "saveEvents", stringQueue);

		preferenceSettings.edit().putString(EVENTS_QUEUE_KEY_PREFERENCE, jsonQueue.toString()).commit();
	}

	
	/**
	 * Sends the registration request, receives the request and parses the response back
	 */
	public void authorizeDevice(MMCDevice device, String email, boolean bFailover)  throws MMCException {
		String host = mHost;
		//if (bFailover == true)
		//	host = host + "fail";
		//if(!isAuthorized() || mApiKey.equals("0"))
		{
			SharedPreferences preferenceSettings = MMCService.getSecurePreferences(mContext);
			preferenceSettings.edit().putString(PreferenceKeys.User.USER_EMAIL, email).commit();
			preferenceSettings.edit().putString(PreferenceKeys.Miscellaneous.KEY_GCM_REG_ID, "").commit();

			//RegistrationRequest request = new RegistrationRequest(host, device, email, true);

			//if (bFailover && false)
			//	AuthorizeWindows (email);
			//else
			try {

				HttpURLConnection connection = RegistrationRequest.POSTConnection(host, device, email, true);
				//HttpClient mHttpClient = HttpUtils.createHttpClient();
				//HttpResponse response = mHttpClient.execute(request);
				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "authorizeDevice", .request.toString());

                //verifyResponse throws an MMCException
				if(verifyConnectionResponse(connection)) {

					String contents = readString(connection); //getResponseString(response);
					JSONObject json = new JSONObject(contents);
					mApiKey = json.optString(JSON_API_KEY, "");

					int dormant = json.optInt("dormant", 0);
					int userID = json.optInt(USER_ID_KEY, 0);
					if(mApiKey.length() > 0 ) {
						//SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
                        preferenceSettings.edit().putString(PreferenceKeys.User.APIKEY, mApiKey).commit();
						// Also save email for this version so it can be copied to the contact email setting for email raw data
						preferenceSettings.edit().putString(PreferenceKeys.User.USER_EMAIL, email).commit();
						preferenceSettings.edit().putString(PreferenceKeys.User.CONTACT_EMAIL, email).commit();
						PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(PreferenceKeys.User.CONTACT_EMAIL, email).commit();
                        preferenceSettings.edit().putBoolean(PreferenceKeys.Miscellaneous.SIGNED_OUT, false).commit();
						PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(PreferenceKeys.Miscellaneous.USAGE_DORMANT_MODE, (int) dormant).commit();


					} else {
						MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "authorizeDevice","api key is empty");
						throw new MMCException("api key was empty");
					}
					if (userID > 0)
					{
                        preferenceSettings.edit().putInt(PreferenceKeys.User.USER_ID, userID).commit();
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "authorizeDevice", "userID=" + userID );	
					}
					else
					{
						MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "authorizeDevice","user id is empty");
					}
				}
			} catch (IOException e) {
				//Just log the exception for now
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "authorizeDevice", "fail to send authorization", e);
//				if (bFailover)
//					AuthorizeWindows (email);
				throw new MMCException(e);
			} catch (JSONException e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "authorizeDevice", "fail to recieve authorization", e);	
//				if (bFailover)
//					AuthorizeWindows (email);
				throw new MMCException(e);
			} 
			catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "authorizeDevice", "fail to recieve authorization", e);	
//				if (bFailover)
//					AuthorizeWindows (email);
				throw new MMCException(e);
			} 
			
		}
		//else
		//	MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "authorizeDevice", "User was already authorized. apikey = " + mApiKey);
	}
	/*
	 * Update the windows server when registration is successful so that it can sync (and create a linux user lookup)
	 */
	/*
	private void updateWinUserDevice (MMCDevice device, String email, String apikey) 
	{
		try
		{
			WSManagerOld wsManager = new WSManagerOld(mContext);
			int userId = wsManager.updateUserDevice(email,apikey);
			if (userId > 0)
			{
				PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt(PreferenceKeys.User.USER_ID, userId).commit();
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateWinUserDevice", "Synchronized user with Windows. userID=" + userId + ", apikey=" + apikey);	
			}
			else
			{
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "updateWinUserDevice", "New user did not sync to Windows. userID=" + userId + ", apikey=" + apikey);	
			}
		}
		catch (IOException e) {
			//Just log the exception for now
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "updateWinUserDevice", "IOException synchronizing user with Windows", e);
		} 
		catch (Exception e) {
			//Just log the exception for now
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "updateWinUserDevice", "error synchronizing user with Windows", e);
		} 
	}
	*/
//	public static void safeClose(HttpClient client)
//	{
//	    if(client != null && client.getConnectionManager() != null)
//	    {
//	        client.getConnectionManager().shutdown();
//	    }
//	}
	
//	private void AuthorizeWindows (String email) throws MMCException 
//	{
//		WSManagerOld wsManager = new WSManagerOld(mContext);
//		try
//		{
//			LoginResponseOld loginResponse = wsManager.login(email, "Linux!5$2E");
//			int iUserID = loginResponse.getContents().getIUser();
//			if (iUserID == 0)
//				throw new MMCException("Registration was not allowed");
//			else
//			{
//				SharedPreferences preferenceSettings = PreferenceManager.getDefaultSharedPreferences(mContext);
//				
//				preferenceSettings.edit().putString(API_KEY_PREFERENCE, "0").commit();
//				preferenceSettings.edit().putString(PreferenceKeys.User.USER_EMAIL, email).commit();
//				preferenceSettings.edit().putInt(PreferenceKeys.User.USER_ID, iUserID).commit();
//				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "AuthorizeWindows", "Registered with Windows");
//				
//			}
//		} catch (IOException e) {
//			//Just log the exception for now
//			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "AuthorizeWindows", "fail to send authorization", e);
//			throw new MMCException(e);
//		} catch (Exception e) {
//			//Just log the exception for now
//			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "AuthorizeWindows", "server error in registrartion", e);
//			throw new MMCException(e);
//		} 
//		
//	}
	
	/**
	 * Sends the registration request, receives the request and parses the response back
	 */
	public JSONArray getServerObjects(String type, HashMap<String, String> query) throws MMCException 
	{
		try {
			String email = PreferenceKeys.getSecurePreferenceString(PreferenceKeys.User.USER_EMAIL, null, mContext);
			
			URL request = RequestServerGet.getURL(mHost, mApiKey, email, type, query);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect ();
			verifyConnectionResponse(connection);
			String responseString = readString(connection);

			JSONArray objects = new JSONObject(responseString).getJSONArray(type);
			return objects;
			
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new MMCException(e);
		}
	}
		
	public boolean isAuthorized() {
		boolean authorized = false;
        int userId = MMCService.getUserID(mContext);
		
		if (mApiKey != null && mApiKey.length() > 10 && userId > 0)
			authorized = true;
        // Unless user signed himself out
        SecurePreferences prefs = MMCService.getSecurePreferences(mContext);
        if (prefs.getBoolean(PreferenceKeys.Miscellaneous.SIGNED_OUT, false) == true)
            authorized = false;

		if (authorized == true && MMCService.isOnline() && Looper.myLooper() != Looper.getMainLooper())
		{
			//mApiKey += "a";
			//RequestServerGet request = new RequestServerGet(mHost, mApiKey, null, "user", null);

			URL request = RequestServerGet.getURL(mHost, mApiKey, null, "user", null);
			try {
				HttpURLConnection connection = (HttpURLConnection) request.openConnection();
				connection.connect();
				if (connection.getResponseCode() == 401)
					return false;
			}
			catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "isAuthorized", "exception", e);
			}
		}
		return authorized; //  && mApiKey.length() > 10);
	}
	
	public void setAuthorized (String apiKey)
	{
		mApiKey = apiKey;
	}

	public void unAuthorizeDevice(MMCDevice device, String email) throws MMCException {
		SharedPreferences preferenceSettings = MMCService.getSecurePreferences(mContext);
		preferenceSettings.edit().remove(PreferenceKeys.User.APIKEY);
		preferenceSettings.edit().commit();
		mApiKey = null;
	}

	/*
	public void reportSpeedTest(SpeedTestEvent speedTestEvent) {
		//if(speedTestEvent.getNetworkType() != NetworkConnectivityMonitor.NETWORK_TYPE_WIFI) {
		//reportEvent(speedTestEvent);
		//}
		reportSpeedToWin (speedTestEvent);
	}
	
	private void reportSpeedToWin (SpeedTestEvent speedTestEvent)
	{
		WSManagerOld wsManager = new WSManagerOld(mContext);
		try
		{
			wsManager.reportSpeedTest (speedTestEvent);
		} catch (IOException e) {
			//Just log the exception for now
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "reportSpeedToWin", "IOException", e);
		} catch (Exception e) {
			//Just log the exception for now
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "reportSpeedToWin", "Exception", e);
		} 
	}
	*/
	
	/**
	 * Report that the phone's SIM has changed
	 * @param device
	 */
	public void reportSimChange(MMCGSMDevice device) {
		try {
			HttpURLConnection conn = ServerUpdateRequest.PUTSimChangeRequest(mHost, ServerUpdateRequest.DEVICE, mApiKey, device);
			verifyConnectionResponse(conn);
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "updateSim", "error rendering request", e);
		}
	}
	
	/**
	 * Report that the user has changed the "share with carrier" setting
	 */
	public boolean reportSettingChange(String type, String key, Object value, HashMap<String, String> carrier) {
		try {
			HttpURLConnection conn = ServerUpdateRequest.PUTSettingChangeRequest(mHost, type, mApiKey, key, value, carrier);
			return verifyConnectionResponse(conn);

		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "reportSettingChange", "error rendering request", e);
            return false;
		}
	}
	
	/**
	 * Gets a carriers stats (dropped, failed, normally ended calls, data speeds) between startTime and endTime.
	 * Keys are defined in {@link ReportManager.StatsKeys}
	 */
	/*
	public HashMap<String, Integer> getCarrierStats(HashMap<String, String> carrier, long startTime, long endTime,
			double latitude, double longitude, float radius) throws MMCException {
		try {
			NetworkRequest networksRequest = new NetworkRequest(mHost, mApiKey, carrier);
			HttpResponse networksResponse = mHttpClient.execute(networksRequest); 
			verifyResponse(networksResponse);
			String networksResponseString = EntityUtils.toString(networksResponse.getEntity());
			//JSONArray networks = new JSONObject(networksResponseString).getJSONArray(JSON_NETWORK_KEY);
			JSONObject network = new JSONObject(networksResponseString).getJSONObject(JSON_NETWORK_KEY);
			String networkId = network.getString(JSON_NETWORK_ID_KEY);
			
			StatsRequest statsRequest = new StatsRequest(mHost, mApiKey, startTime, endTime,
					latitude, longitude, radius, networkId);
			HttpResponse statsResponse = mHttpClient.execute(statsRequest);
			verifyResponse(statsResponse);
			String statsResponseString = EntityUtils.toString(statsResponse.getEntity());
			JSONObject statsJson = new JSONObject(statsResponseString).getJSONArray(JSON_NETWORKS_KEY).getJSONObject(0);
			
			HashMap<String, Integer> stats = new HashMap<String, Integer>();
			stats.put(ReportManager.StatsKeys.DROPPED_CALLS, statsJson.getInt(JSON_DROPPEDCALLS_KEY));
			stats.put(ReportManager.StatsKeys.FAILED_CALLS, statsJson.getInt(JSON_FAILEDCALLS_KEY));
			int normallyEndedCalls = statsJson.getInt(JSON_CONNECTEDCALLS_KEY) - statsJson.getInt(JSON_DROPPEDCALLS_KEY);
			stats.put(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS, normallyEndedCalls);
			stats.put(ReportManager.StatsKeys.DOWNLOAD_SPEED_AVERAGE, statsJson.getInt(JSON_DOWNLOADSPEED_KEY));
			stats.put(ReportManager.StatsKeys.UPLOAD_SPEED_AVERAGE, statsJson.getInt(JSON_UPLOADSPEED_KEY));
			int latency = 0;
			if (!statsJson.isNull(JSON_LATENCY_KEY))
				latency = statsJson.getInt(JSON_LATENCY_KEY);
			stats.put(ReportManager.StatsKeys.LATENCY_AVERAGE, latency);
			
			return stats;
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (JSONException e) {
			throw new MMCException(e);
		}
	}
	
	public HashMap<String, Integer> getAllCarrierAverageStats(String mcc, long startTime, long endTime,
			double latitude, double longitude, float radius) throws MMCException {
		try {
			//NetworksRequest networksRequest = new NetworksRequest(mHost, mApiKey, mcc);
			//HttpResponse networksResponse = mHttpClient.execute(networksRequest);
			//verifyResponse(networksResponse);
			//String networksResponseString = EntityUtils.toString(networksResponse.getEntity());
			//JSONArray networks = new JSONObject(networksResponseString).getJSONArray(JSON_NETWORKS_KEY);

			//String[] networkIds = new String[networks.length()];
			//for(int i=0; i<networks.length(); i++) {
			//	networkIds[i] = networks.getJSONObject(i).getString(JSON_NETWORK_ID_KEY);
			//}
			
			StatsRequest statsRequest = new StatsRequest(mHost, mApiKey, startTime, endTime,
					latitude, longitude, radius, null);
			HttpResponse statsResponse = mHttpClient.execute(statsRequest);
			verifyResponse(statsResponse);
			String statsResponseString = EntityUtils.toString(statsResponse.getEntity());
			//JSONArray statsJsonArray = new JSONObject(statsResponseString).getJSONArray(JSON_NETWORKS_KEY);
			JSONObject statsJson = new JSONObject(statsResponseString).getJSONObject("stats");
			
			HashMap<String, Integer> stats = new HashMap<String, Integer>();
			stats.put(ReportManager.StatsKeys.DROPPED_CALLS, statsJson.getInt(JSON_DROPPEDCALLS_KEY));
			stats.put(ReportManager.StatsKeys.FAILED_CALLS, statsJson.getInt(JSON_FAILEDCALLS_KEY));
			int normallyEndedCalls = statsJson.getInt(JSON_CONNECTEDCALLS_KEY) - statsJson.getInt(JSON_DROPPEDCALLS_KEY);
			stats.put(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS, normallyEndedCalls);
			stats.put(ReportManager.StatsKeys.DOWNLOAD_SPEED_AVERAGE, statsJson.getInt(JSON_DOWNLOADSPEED_KEY));
			stats.put(ReportManager.StatsKeys.UPLOAD_SPEED_AVERAGE, statsJson.getInt(JSON_UPLOADSPEED_KEY));
			
			int latency = 0;
			if (!statsJson.isNull(JSON_LATENCY_KEY))
				latency = statsJson.getInt(JSON_LATENCY_KEY);
			stats.put(ReportManager.StatsKeys.LATENCY_AVERAGE, latency);
			
			return stats;
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (JSONException e) {
			throw new MMCException(e);
		}
	}
	
	private int calculateAverageStat(String statKey, JSONArray statsJsonArray) throws JSONException {
		int j=0;
		int sum = 0;
		
		for(int i=0; i<statsJsonArray.length(); i++) {
			int statValue = statsJsonArray.getJSONObject(i).getInt(statKey);
			if(statValue != 0) {
				sum += statValue;
				j++;
			}
		}
		
		int average = 0;
		if (j != 0)
			average = sum/j;
		return average;

	}
	*/
	/*
	 *  Get a DidYouKnow fact from the server
	 *  Some facts return a counter to be displayed in a special counter view
	 *  or two counters to interpolate
	 */
	public Long confirmEvent (long ltime, int evttype, int newtype, int rating, int userid) throws MMCException
	{
		try {
			String path = mHost + "/api/confirmevent";
		
			//if (!((MMCApplication) mContext.getApplicationContext()).useLinux())
			//	path = mContext.getString(R.string.MMC_URL_WIN) + "ConfirmEvent.aspx";

			URL request = ConfirmEventRequest.getURL(path, mApiKey, ltime, evttype, newtype, rating, userid);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect();
			verifyConnectionResponse(connection);
			String responseString = readString(connection);

		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new  MMCException(e);
		}
		return 1l;
	}
	
	/*
	 *  Get a DidYouKnow fact from the server
	 *  Some facts return a counter to be displayed in a special counter view
	 *  or two counters to interpolate
	 */
	public JSONObject getDidYouKnow (String opid) throws MMCException
	{
		JSONObject jsonfact;
		try {
			String path = mHost + "/api/didyouknow";
		
			URL request = DidYouKnowRequest.getURL(path, mApiKey, opid);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect ();
			String responseString = readString(connection);
			jsonfact = new JSONObject(responseString);

			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "DidYouKnowRequest", request.getURI().toString()); 
				
//			HttpClient mHttpClient = HttpUtils.createHttpClient();
//			HttpResponse response = mHttpClient.execute(request);
//			verifyResponse(response);
//			String responseString = getResponseString(response);
//			jsonfact = new JSONObject(responseString);
			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "DidYouKnowResponse", responseString); 
			
			
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new  MMCException(e);
		}
		return jsonfact;
	}
	
	public List<Carrier> getTopOperators(double latitude, double longitude, int radius, int mcc, int limit) throws MMCException 
	{	
		List<Carrier> carriers = new ArrayList<Carrier>();
		try {
			String path = mHost + "/api/topop";
		
			TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
			String ccode = telephony.getNetworkCountryIso();
            URL request = TopOperatorsRequest.getURL(path, mApiKey, latitude, longitude, radius, mcc, limit, ccode);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect ();
			verifyConnectionResponse(connection);
			String topResponseString = readString(connection);
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getTopOperators", request.toString());

            //HttpClient mHttpClient = HttpUtils.createHttpClient();
			//HttpResponse topResponse = mHttpClient.execute(topRequest);
			//verifyResponse(topResponse);
			if (topResponseString.length() > 2)
			{
                SharedPreferences securePreferences = MMCService.getSecurePreferences(mContext);
                securePreferences.edit().putString(PreferenceKeys.Miscellaneous.TOPOP_RESPONSE, topResponseString).commit();
                securePreferences.edit().putString(PreferenceKeys.Miscellaneous.TOPOP_LAT, Double.toString(latitude)).commit();
                securePreferences.edit().putString(PreferenceKeys.Miscellaneous.TOPOP_LNG, Double.toString(longitude)).commit();
			}
		
			JSONArray operators = new JSONObject(topResponseString).getJSONArray("operators");
			for(int i=0; i<operators.length(); i++) 
			{
				Carrier carrier = new Carrier(operators.getJSONObject(i));
				carrier.loadLogo (mContext);
                if (carrier != null)
				    carriers.add(carrier);
			}
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new  MMCException(e);
		}
		return carriers;
	}
		
	public JSONObject getAreaStats(double latitude, double longitude, int radius, int months, String ops) throws MMCException 
	{	
		JSONObject areastats = null;
		try {
			String path = mHost + "/api/stats";
		
			//if (!((MMCApplication) mContext.getApplicationContext()).useLinux())
			//	path = mContext.getString(R.string.MMC_URL_WIN) + "AreaStats.aspx";

			URL request = AreaStatsRequest.getURL(path, mApiKey, latitude, longitude, radius, months, ops);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect ();
			verifyConnectionResponse(connection);
			String statResponseString = readString(connection);

//			AreaStatsRequest statRequest = new AreaStatsRequest(path, mApiKey, latitude, longitude, radius, months, ops);
//			HttpClient mHttpClient = HttpUtils.createHttpClient();
//			HttpResponse statResponse = mHttpClient.execute(statRequest);
//			verifyResponse(statResponse);
//			String statResponseString = getResponseString(statResponse);
			if (statResponseString.length() > 2)
			{
                SharedPreferences secureSettings = MMCService.getSecurePreferences(mContext);
                secureSettings.edit().putString(PreferenceKeys.Miscellaneous.TOPSTATS_RESPONSE, statResponseString).commit();
                secureSettings.edit().putString(PreferenceKeys.Miscellaneous.TOPSTATS_LAT, Double.toString(latitude)).commit();
                secureSettings.edit().putString(PreferenceKeys.Miscellaneous.TOPSTATS_LNG, Double.toString(longitude)).commit();
			}
		
			areastats = new JSONObject(statResponseString); // .getJSONObject("stat");
			
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new  MMCException(e);
		}
		return areastats;
	}
	/**
	 * Gets the logo of the carrier specified by <code>carrier</code>
	 * @param carrier
	 * @return carrier logo
	 * @throws MMCException
	 */
	Carrier carrierCurr;
	public Bitmap getCarrierLogo(HashMap<String, String> carrierparams) throws MMCException {
        SharedPreferences securePreferences = MMCService.getSecurePreferences(mContext);
		try {
			//NetworkRequest networksRequest = new NetworkRequest(mHost, mApiKey, carrierparams);
			URL request = NetworkRequest.getURL(mHost, mApiKey, carrierparams);

			String networksResponseString = null;
 			String opresponse = securePreferences.getString(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE, null);
 			String oprequest = securePreferences.getString(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST, null);
 			if (opresponse != null && oprequest != null)
			{
 				if (oprequest.equals(request.getQuery()))
 				{
 					networksResponseString = opresponse;
 				}
			}
 			if (networksResponseString == null)
 			{
 				//HttpClient mHttpClient = HttpUtils.createHttpClient();
 				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "NetworksRequest", networksRequest.getURI().toString()); 
 				
 				//HttpResponse networksResponse = mHttpClient.execute(networksRequest);
				//verifyResponse(networksResponse);
 				//networksResponseString = getResponseString(networksResponse);
				HttpURLConnection connection = (HttpURLConnection) request.openConnection();
				connection.connect ();
				verifyConnectionResponse(connection);
				networksResponseString = readString(connection);
                securePreferences.edit().putString(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE, networksResponseString).commit();
                securePreferences.edit().putString(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST, request.getQuery()).commit();
 			}JSONObject operator = new JSONObject(networksResponseString).getJSONObject(JSON_NETWORK_KEY);
			carrierCurr = new Carrier(operator);
			
			String carriername = carrierCurr.Name;
	        String logo = carrierCurr.Path;
	        logo = logo.substring(1); // remove leading slash
	        carrierCurr.loadLogo ( mContext);
	    	String logoPath = mContext.getApplicationContext().getFilesDir() + carrierCurr.Path;
			try
			{
				carrierCurr.Logo = BitmapFactory.decodeFile(logoPath);
			}
			catch (OutOfMemoryError e)
			{
				MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "OutOfMemoryError loading logo " + logoPath);	
			}
			return carrierCurr.Logo;
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (JSONException e) {
            securePreferences.edit().putString(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE, null).commit();
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new MMCException(e);
		}
	}
	public Carrier getCurrentCarrier ()
    {
        return carrierCurr;
    }
	
	
    private static String ReplaceCharacters (String str, String c, String r)
    {
    	if (str == null)
    		return "";
        int pos = str.indexOf (c);
        while (pos >= 0)
        {
            str = str.substring (0, pos) + r + str.substring (pos+c.length(), str.length());
            pos = str.indexOf (c);
        }
        return str;
    }
	
	public String getTwitterHandle(HashMap<String, String> carrier) throws MMCException {
		try {
			URL request = NetworkRequest.getURL(mHost, mApiKey, carrier);
			HttpURLConnection connection = (HttpURLConnection) request.openConnection();
			connection.connect ();
			verifyConnectionResponse(connection);
			String networksResponseString = readString(connection);

			//JSONArray networks = new JSONObject(networksResponseString).getJSONArray(JSON_NETWORK_KEY);
			JSONObject network = new JSONObject(networksResponseString).getJSONObject(JSON_NETWORK_KEY);
			String twitter = network.getString(JSON_NETWORK_TWITTERHANDLE_KEY);
			return twitter;
		}
		catch (IOException e) {
			throw new MMCException(e);
		}
		catch (Exception e) {
			throw new MMCException(e);
		}
	}
	protected static boolean verifyConnectionResponse(HttpURLConnection connection) throws MMCException {
		int responseCode = 0;
		try {
			responseCode =connection.getResponseCode();
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "verifyConnectionResponse", "getResponseCode", e);
			throw new MMCException(e);
		}
		String contents = "";
		if(responseCode < HttpURLConnection.HTTP_OK || responseCode >= 400) {
			try {
				contents = readString(connection);
				MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "verifyResponse", "response = " + contents);

				JSONObject json = new JSONObject(contents);
				String message = json.optString(JSON_ERROR_KEY, "response had no error message");
				//MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "verifyResponse", "error in request "
				//		+ responseCode + " " + message);
				throw new MMCException(message);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "verifyResponse", contents, e);
				throw new MMCException(e);
			}
		}
		else {
			//If the response is valid, evaluate if we need to parse the content
			switch(responseCode) {
				case HttpURLConnection.HTTP_OK:
					return true;
				case HttpURLConnection.HTTP_CREATED:
					return true;
				case HttpURLConnection.HTTP_NO_CONTENT:
					return false;
				default:
					return false;
			}
		}
	}
//	/**
//	 * Verifies that the response is not an error, parses the error message out if there is one.
//	 * @param response response to be verified
//	 * @return true if the content of the response needs to be parsed, false otherwise
//	 * @throws MMCException if status code is not in the 200 or 300 range, the error message is contained in the exception
//	 */
//	protected boolean verifyResponse(HttpResponse response) throws MMCException {
//		int responseCode = response.getStatusLine().getStatusCode();
//		String contents = "";
//		if(responseCode < HttpURLConnection.HTTP_OK || responseCode >= 400) {
//			try {
//				HttpEntity enty = response.getEntity();
//				contents = EntityUtils.toString(enty);
//                MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "verifyResponse", "response = " + contents);
//
//                JSONObject json = new JSONObject(contents);
//				String message = json.optString(JSON_ERROR_KEY, "response had no error message");
//				//MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "verifyResponse", "error in request "
//				//		+ responseCode + " " + message);
//				throw new MMCException(message);
//			} catch (Exception e) {
//				MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "verifyResponse", contents, e);
//				throw new MMCException(e);
//			}
//		}
//		else {
//			//If the response is valid, evaluate if we need to parse the content
//			switch(responseCode) {
//			case HttpURLConnection.HTTP_OK:
//				return true;
//			case HttpURLConnection.HTTP_CREATED:
//				return true;
//			case HttpURLConnection.HTTP_NO_CONTENT:
//				return false;
//			default:
//				return false;
//			}
//		}
//	}
	
//	/**
//	 * Add requests to {@link WebReporter#mRequestQueue} and starts the {@link WebReporter#mReportingThread}
//	 * @param requests
//	 */
//	protected void queueRequest(Request... requests) {
//		if(isAuthorized()) {
//			mFlushQueueWakeLock.acquire();
//
//			for(Request request : requests){
//				mRequestQueue.add(request);
//			}
//
//			//if(isNetworkWifi() || !isNetworkRoaming() || isDataRoamingEnabled())
//            {
//				//start reporting thread
//				if(mReportingThread == null || !mReportingThread.isAlive()) {
//					mReportingThread = new ReportingThread();
//					mReportingThread.start();
//				}
//			}
//		}else {
//			MMCLogger.logToFile(MMCLogger.Level.WTF, TAG, "reportEvent", "this reporter was not authorized");
//		}
//	}

	/**
	 * Submits an event to the server.
	 * @param eventDataEnvelope
	 */
	public EventResponse submitEvent(EventDataEnvelope eventDataEnvelope) throws Exception{
		Gson gson = new Gson();
		String requestJSON = gson.toJson(eventDataEnvelope);
		String linuxUrl = mContext.getString(R.string.MMC_URL_LIN);

		String responseJSON = sendJSONPacket(linuxUrl + "/api/events", requestJSON, false);
		EventResponse eventResponse = gson.fromJson(responseJSON, EventResponse.class);
		eventResponse.init();
		return eventResponse;
	}

	/**
	 * Uses the json packet supplied as the arguments to call the web service defined by the
	 * <code>endpoint</code> supplied. The response is returned as a string that is expected to be parsed
	 * later (as a json packet).
	 * @param endpoint
	 * @param jsonPacket
	 * @param log whether to log the <code>jsonPacket</code> to {@link MMCLogger#LOG_FILE}
	 * @return
	 */
	private String sendJSONPacket(String endpoint, String jsonPacket, boolean log) throws Exception {

		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("POST");
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setFixedLengthStreamingMode(jsonPacket.getBytes().length);

		conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
		conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
		conn.setRequestProperty("MMCBrand", mContext.getString(R.string.app_label));
		conn.setRequestProperty("MMCVersion", mContext.getString(R.string.app_versionName));

		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "authorizeDevice", url.toString());

		//open
		conn.connect();
		OutputStream os = new BufferedOutputStream(conn.getOutputStream());
		os.write(jsonPacket.getBytes());
		//clean up
		os.flush();

		String responseContents = readString(conn);
		return responseContents;

//		HttpClient client = new DefaultHttpClient();
//		HttpPost post = new HttpPost(endpoint);
//		post.setHeader("Content-Type", "application/json; charset=utf-8");
//		post.setHeader("MMCBrand",mContext.getString(R.string.app_label));
//		post.setHeader("MMCVersion", mContext.getString(R.string.app_versionName));
//		if(log) {
//			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "sendJsonPacket", jsonPacket);
//		}
//		EntityTemplate entityTemplate = new EntityTemplate(
//				new ContentProducer(){
//					public void writeTo(OutputStream outstream) throws IOException {
//						Writer writer = new OutputStreamWriter(outstream, "UTF-8");
//						writer.write(jsonPacket);
//						writer.flush();
//					}
//				}
//		);
//		post.setEntity(entityTemplate);
//		HttpResponse response = client.execute(post);
//
//		String responseContents = EntityUtils.toString(response.getEntity());
//
//		return responseContents;
	}

	/**
	 * Requests the server to send a Csv to the users email
	 * with a 24 hr start-stop time interval
	 * @return
	 */
	public String requestCsvEmail (int userid, String carrier, int mcc, int mnc, String manufacturer, String model, String device, String appname, String apikey) throws Exception
	{
		SimpleDateFormat format = new SimpleDateFormat("MMM dd yyyy HH:mm:ss");
		Date date = new Date();
		String stoptime = date.toGMTString();
		long startms = date.getTime() - 24*3600000;
		Date datestart = new Date(startms);
		String starttime = datestart.toGMTString();
		String parameters = "?userid=" + userid + "&start=" + starttime + "&stop=" + stoptime;
		parameters += "&carrier=" + carrier + "&mcc=" + mcc + "&mnc=" + mnc + "&manuf=" + manufacturer + "&model=" + model + "&device=" + device + "&email=1";
		parameters += "&appname=" + appname + "&lang=" + MMCDevice.getLanguageCode() + "&apiKey=" + apikey;

		parameters = parameters.replace(" ", "%20");
		String url = mContext.getString(R.string.MMC_URL_LIN) + "/CoverageData.aspx" + parameters;
		URL request = new URL (url);
		HttpURLConnection connection = (HttpURLConnection) request.openConnection();
		connection.connect();
		String strResponse = readString(connection);
		return strResponse;
	}

	/**
	 * @return true if the device is considered roaming on the current network
	 */
	protected boolean isNetworkRoaming() {
		return ((TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE)).isNetworkRoaming();
	}
	
	/**
	 * @return user setting for sending data while roaming
	 */
	protected boolean isDataRoamingEnabled() {
		return PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(PREFERENCE_KEY_ENABLE_DATA_ROAMING, false);
	}
	
	/**
	 * @return true if the current connected network is wifi
	 */
	protected boolean isNetworkWifi() {
		ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(connectivityManager != null) {
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if(networkInfo != null) {
				int wifiState = networkInfo.getType();
				return (wifiState == ConnectivityManager.TYPE_WIFI);
			}
		}
		return false;
	}

	public static String getHttpURLResponse (String path,LinkedList<Pair> params, boolean bVerifyJson) throws Exception
	{
		String paramsString = WebReporter.URLEncodedFormat(params);
		return getHttpURLResponse (path + paramsString, bVerifyJson);
	}

	public static String getHttpURLResponse (String url, boolean bVerifyJson) throws Exception
	{
		URL urlObj = new URL (url);
		HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
		connection.setReadTimeout(10000);
		connection.setConnectTimeout(15000);
		connection.connect();
		if (bVerifyJson)
			verifyConnectionResponse(connection);
		String responseString = readString(connection);

		return responseString;
	}

	public static String readString (HttpURLConnection connection) throws IOException, UnsupportedEncodingException
	{
		InputStream stream = null;
		try {
			stream = connection.getInputStream();
			int len = connection.getContentLength();
			Reader reader = null;
			reader = new InputStreamReader(stream, "UTF-8");
			char[] buffer = new char[len];
			reader.read(buffer);
			return new String(buffer);
		}
		finally
		{
			if (stream != null)
				try{stream.close();} catch (Exception e) {}
		}
	}

	public static String geocode (Location location)
	{
		return geocode (location.getLatitude(), location.getLongitude());
	}
	public static String geocode (double latitude, double longitude)
	{
		String addressString = String.format("%.4f, %.4f", latitude, longitude);
		try {
			String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
			//String url = mStaticAssetURL + "/api/osm/location?apiKey=" + apiKey + "&location=" + latitude + "&location=" + longitude;
			String url = "https://lb1.mymobilecoverage.com" + "/api/osm/location?apiKey=" + apiKey + "&location=" + latitude + "&location=" + longitude;
			String response = WebReporter.getHttpURLResponse(url, false);

			JSONObject json = null;
			JSONArray jsonArray = null;

			if(response == null)
				return addressString;

			try {
				jsonArray = new JSONArray(response);
			} catch (JSONException e) {
				return addressString;
			}
			try {
				for(int i = 0; i < jsonArray.length(); i++) {
					json = jsonArray.getJSONObject(i);
					if(json.has("error")) {
						String error = json.getString("error");
						return null;
					}
					else {
						json = json.getJSONObject("address");
						String number = "";
						if(json.has("house_number")) {
							number = json.getString("house_number");
						}
						String road = json.getString("road");
						//String suburb = "";
						//if(json.has("suburb"))
						//	suburb = ", " + json.getString("suburb");
						addressString = number + " " + road;// + suburb;
						return addressString;
					}
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		} catch(Exception e) {
		}

		return addressString;
	}

	public static String URLEncodedFormat (List <Pair> parameters)
	{
        final StringBuilder result = new StringBuilder();
        for (final Pair parameter : parameters) {
            final String encodedName = encode(parameter.first.toString());
            final String value = parameter.second.toString();
            final String encodedValue = value != null ? encode(value) : "";
            if (result.length() > 0)
                result.append("&");
            result.append(encodedName);
            result.append("=");
            result.append(encodedValue);
        }
        return result.toString();
    }
	private static String encode (final String content) {
        try {
            return URLEncoder.encode(content, "utf-8");
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }

}