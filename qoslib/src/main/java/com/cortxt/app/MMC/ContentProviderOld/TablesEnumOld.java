package com.cortxt.app.MMC.ContentProviderOld;

import android.net.Uri;

/**
 * This enum lists the various tables that can be used in the database along with
 * a link to the class that has more details on the enum.
 * 
 * TODO might want to move the data of the Tables.java class into this enum.
 * @author Abhin
 *
 */
public enum TablesEnumOld {
	SIGNAL_STRENGTHS(
		"signalStrengths", 
		TablesOld.SignalStrengths.class, 
		"signalStrengths", 
		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.SignalStrength",
		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.SignalStrength"
	),
//	BASE_STATIONS_GSM(
//		"baseStations", 
//		TablesOld.BaseStations.GSMVersion.class,
//		"baseStations/gsm",
//		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.BaseStation.GSM",
//		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.BaseStation.GSM"
//	),
//	BASE_STATIONS_CDMA(
//		"baseStations", 
//		TablesOld.BaseStations.CDMAVersion.class,
//		"baseStations/cdma",
//		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.BaseStation.CDMA",
//		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.BaseStation.CDMA"
//	),
	BASE_STATIONS(  
		"baseStations", 
		TablesOld.BaseStations.class,
		"baseStations",
		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.BaseStation",
		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.BaseStation"
	),
	LOCATIONS(
		"locations", 
		TablesOld.Locations.class,
		"locations",
		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.Location",
		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.Location"
	);
//	EVENTS(
//		"events", 
//		TablesOld.Events.class,
//		"events",
//		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.Event",
//		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.Event"
//	),
//	EVENT_COUPLES(
//		"eventCouples", 
//		TablesOld.EventCouples.class,
//		"eventCouples",
//		"vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.EventCouple",
//		"vnd.android.cursor.item/vnd.com.cortxt.app.MMC.EventCouple"
//	),
//  SPEED_TEST_RESULTS(
//    "speedTestResults",
//    TablesOld.SpeedTestResults.class,
//    "speedTestResults",
//    "vnd.android.cursor.dir/vnd.com.cortxt.app.MMC.SpeedTestResults",
//    "vnd.android.cursor.item/vnd.com.cortxt.app.MMC.SpeedTestResults"
//  );


	public final String Name;
	public final Class Template;
	public final String RelativeUri;
	public final String ContentType;
	public final String ContentItemType;
	
	TablesEnumOld(String name, Class template, String relativeUri, String contentType, String contentItemType){
		this.Name = name;
		this.Template = template;
		this.RelativeUri = relativeUri;
		this.ContentType = contentType;
		this.ContentItemType = contentItemType;
	}
	
	public Uri getContentUri(){
		return Uri.parse("content://" + TablesOld.AUTHORITY + "/" + this.RelativeUri);
	}
}
