package com.cortxt.app.MMC.Reporters.WebReporter;

import android.util.Pair;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import com.cortxt.app.MMC.Utils.MMCLogger;

public class RequestServerGet {
	public static final String TAG = StatsRequest.class.getSimpleName();
	private static  String END_POINT = "/api/";
	
	/**
	 * Constructs a StatsRequest object with the given parameters\
	 */
	public static URL getURL (String host, String apiKey, String email, String type, HashMap<String, String> query)
	{
		END_POINT += type;

		LinkedList<Pair> params = new LinkedList<Pair>();
		params.add(new Pair(WebReporter.JSON_API_KEY, apiKey));
		if (email != null) {
			params.add(new Pair("emails[]", email));
			if (query != null) {
				// add each name value pair in the query hash map
				for (Map.Entry<String, String> entry : query.entrySet()) {
					params.add(new Pair(entry.getKey(), entry.getValue()));
				}
			}
		}

		String paramsString = WebReporter.URLEncodedFormat(params);
		
		try {
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "RequestServerGet " + type, paramsString);
			return new URL(host + END_POINT + "?" + paramsString);

		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "RequestServerGet", "exception", e);
			throw new RuntimeException(e);
		}
	}
}
