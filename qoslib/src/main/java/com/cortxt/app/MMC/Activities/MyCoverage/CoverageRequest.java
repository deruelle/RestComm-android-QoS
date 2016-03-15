package com.cortxt.app.MMC.Activities.MyCoverage;

import org.apache.http.client.methods.HttpGet;

import android.content.Context;

import com.cortxt.app.MMC.UtilsOld.Carrier;
import com.google.android.maps.GeoPoint;

public class CoverageRequest extends HttpGet {
	private static final String TAG = CoverageRequest.class.getSimpleName();
	
	private static final String END_POINT = "/api/overlays";

	private static final String KEY_TYPE = "type";
	private static final String KEY_SW = "sw[]";
	private static final String KEY_NE = "ne[]";	
	
	public static final String TYPE_RSSI = "rssi";
	public static final String TYPE_VARIANCE = "variance";
	public static final String TYPE_RSSI_VARIANCE= "rssi-variance";
	
	/**
	 * Coordinates of south-west corner of area to request image for
	 */
	protected GeoPoint mSW;
	
	/**
	 * Coordinates of north-east corner of area to request image for
	 */
	protected GeoPoint mNE;
	
	/**
	 * Constructs a CoverageRequest object
	 * @param context
	 * @param type type of coverage to request
	 * @param mcc
	 * @param mnc
	 * @param carrier
	 * @param sw coordinates of south-west corner of area to request image for
	 * @param ne coordinates of north-east corner of area to request image for
	 */
	public CoverageRequest(Context context, String type, Carrier carrier, GeoPoint sw, GeoPoint ne) {
		mSW = sw;
		mNE = ne;
		/*
		LinkedList<NameValuePair> params = new LinkedList<NameValuePair>();
		params.add(new BasicNameValuePair(WebReporter.JSON_API_KEY, PreferenceManager.getDefaultSharedPreferences(context).getString(API_KEY_PREFERENCE, null)));
		params.add(new BasicNameValuePair(KEY_TYPE, type));
		params.add(new BasicNameValuePair(KEY_SW, Double.toString(sw.getLatitudeE6()/1000000.0)));
		params.add(new BasicNameValuePair(KEY_SW, Double.toString(sw.getLongitudeE6()/1000000.0)));
		params.add(new BasicNameValuePair(KEY_NE, Double.toString(ne.getLatitudeE6()/1000000.0)));
		params.add(new BasicNameValuePair(KEY_NE, Double.toString(ne.getLongitudeE6()/1000000.0)));
		
		for(Map.Entry<String, String> entry : carrierProperties.entrySet()) {
			params.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		
		String paramsString = URLEncodedUtils.format(params, "utf-8");
		
		try {
			setURI(new URI(context.getString(R.string.MMC_URL_LIN) + END_POINT + "?" + paramsString));
		}
		catch (URISyntaxException e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "constructor", "invalid uri", e);
			throw new RuntimeException(e);
		}
		*/
	}

	public GeoPoint getSW() {
		return mSW;
	}

	public GeoPoint getNE() {
		return mNE;
	}
	
	
}
