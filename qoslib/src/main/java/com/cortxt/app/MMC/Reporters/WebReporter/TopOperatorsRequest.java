package com.cortxt.app.MMC.Reporters.WebReporter;

import android.util.Pair;

import java.net.URL;
import java.util.LinkedList;

import com.cortxt.app.MMC.Utils.MMCLogger;

public class TopOperatorsRequest
{
	public static final String TAG = TopOperatorsRequest.class.getSimpleName();
	
	public static URL getURL(String path, String apiKey, double latitude, double longitude, int radius, int mcc, int limit, String ccode)
	{
		LinkedList<Pair> params = new LinkedList<Pair>();
		
		params.add(new Pair(WebReporter.JSON_API_KEY, apiKey));
		//if (!((MMCApplication) mContext.getApplicationContext()).useLinux())
		{  
			params.add(new Pair("latitude", Double.toString(latitude))); // latitude
			params.add(new Pair("longitude", Double.toString(longitude))); // longitude
		}
//		else
//		{
//			params.add(new BasicNameValuePair("center[]", Double.toString(latitude))); // latitude 
//			params.add(new BasicNameValuePair("center[]", Double.toString(longitude))); // longitude
//		}
		params.add(new Pair("radius", Integer.toString(radius)));
		params.add(new Pair("mcc", Integer.toString(mcc)));
		params.add(new Pair("limit", Integer.toString(limit)));
		if (ccode != null)
			params.add(new Pair("ccode", ccode.toUpperCase()));
		
		String paramsString = WebReporter.URLEncodedFormat(params);
		
		try 
		{
			return new URL(path + "?" + paramsString);
		} 
		catch (Exception e)
		{
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "constructor", "invalid uri", e);
			throw new RuntimeException(e);
		}
	}	
}
