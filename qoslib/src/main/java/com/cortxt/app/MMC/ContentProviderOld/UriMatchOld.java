package com.cortxt.app.MMC.ContentProviderOld;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;

/**
 * This enum lists all the match codes that the uri matcher could use for all 
 * the possible uri cases.
 * @author Abhin
 *
 */
public enum UriMatchOld {
	SIGNAL_STRENGTHS(
			TablesEnumOld.SIGNAL_STRENGTHS, 
			0, 
			TablesEnumOld.SIGNAL_STRENGTHS.RelativeUri
		),
		
	SIGNAL_STRENGTH_ID(
			TablesEnumOld.SIGNAL_STRENGTHS, 
			1, 
			TablesEnumOld.SIGNAL_STRENGTHS.RelativeUri + "/#"
		),
//	BASE_STATIONS_CDMA(
//		TablesEnumOld.BASE_STATIONS_CDMA, 
//		4, 
//		TablesEnumOld.BASE_STATIONS_CDMA.RelativeUri
//	),
//	
//	BASE_STATION_CDMA_ID(
//		TablesEnumOld.BASE_STATIONS_CDMA, 
//		5, 
//		TablesEnumOld.BASE_STATIONS_CDMA.RelativeUri + "/#"
//	),
//	
//	BASE_STATIONS_GSM(
//		TablesEnumOld.BASE_STATIONS_GSM, 
//		6, 
//		TablesEnumOld.BASE_STATIONS_GSM.RelativeUri
//	),
//	
//	BASE_STATION_GSM_ID(
//		TablesEnumOld.BASE_STATIONS_GSM, 
//		7, 
//		TablesEnumOld.BASE_STATIONS_GSM.RelativeUri + "/#"
//	),
		
	BASE_STATIONS(
		TablesEnumOld.BASE_STATIONS, 
		2, 
		TablesEnumOld.BASE_STATIONS.RelativeUri
	),
	
	BASE_STATION_ID(
		TablesEnumOld.BASE_STATIONS, 
		3, 
		TablesEnumOld.BASE_STATIONS.RelativeUri + "/#"
	),
	
	LOCATIONS(
		TablesEnumOld.LOCATIONS, 
		4, 
		TablesEnumOld.LOCATIONS.RelativeUri
	),
	
	LOCATION_ID(
		TablesEnumOld.LOCATIONS, 
		5, 
		TablesEnumOld.LOCATIONS.RelativeUri + "/#"
	);
	
//	EVENTS(
//		TablesEnumOld.EVENTS, 
//		10, 
//		TablesEnumOld.EVENTS.RelativeUri
//	),
//	
//	EVENT_ID(
//		TablesEnumOld.EVENTS, 
//		11, 
//		TablesEnumOld.EVENTS.RelativeUri + "/#"
//	),
//	
//	EVENT_COUPLES(
//		TablesEnumOld.EVENT_COUPLES, 
//		12, 
//		TablesEnumOld.EVENT_COUPLES.RelativeUri
//	),
//	
//	EVENT_COUPLE_ID(
//		TablesEnumOld.EVENT_COUPLES, 
//		13, 
//		TablesEnumOld.EVENT_COUPLES.RelativeUri + "/#"
//	),
//	
//	SPEED_TEST_RESULTS(
//		TablesEnumOld.SPEED_TEST_RESULTS,
//		14,
//		TablesEnumOld.SPEED_TEST_RESULTS.RelativeUri
//	),
//	
//	SPEED_TEST_RESULTS_ID(
//		TablesEnumOld.SPEED_TEST_RESULTS,
//		15,
//		TablesEnumOld.SPEED_TEST_RESULTS.RelativeUri + "/#"
//	);
	
	public final TablesEnumOld Table;
	public final int Code;
	public final String Path;
	
	// create a map for quick reverse lookup
	private static final Map<Integer, UriMatchOld> lookup = new HashMap<Integer, UriMatchOld>();
	static {
		for (UriMatchOld match : UriMatchOld.values())
			lookup.put(match.Code, match);
	}
	
	UriMatchOld(TablesEnumOld table, int code, String path){
		this.Table = table;
		this.Code = code;
		this.Path = path;
	}
	
	/**
	 * This method does a reverse lookup on the enum using the code and
	 * returns the appropriate enum.
	 * @param code
	 * @return
	 */
	public static UriMatchOld get(int code){
		return lookup.get(code);
	}
	
	public Uri getContentUri(){
		return Uri.parse("content://" + TablesOld.AUTHORITY + "/" + Table.RelativeUri);
	}
}
