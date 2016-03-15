package com.cortxt.app.MMC.ServicesOld.Intents;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.TrafficStats;

//import com.cortxt.app.MMC.Activities.SpeedTest;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.UtilsOld.CommonIntentBundleKeysOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.com.mmcextension.VQ.VQManager;

/**
 * Whenever MMCService wants to send information to any activity, it has
 * to do so through the 'intents'. This class takes care of those intents.
 * @author abhin
 *
 */
public class MMCServiceIntentDispatcherOld {
	private static final String TAG = MMCServiceIntentDispatcherOld.class.getSimpleName();
	
	MMCService owner;
	/*
	 * =================================================================
	 * Starts constructors
	 */
	public MMCServiceIntentDispatcherOld(MMCService owner){
		this.owner = owner;
	}
	/*
	 * End constructors
	 * =================================================================
	 * Start miscellaneous public methods
	 */
	
	public void updateLiveStatusSatelliteCount(int numberOfSatellites, int numberOfSatellitesUsedInFix){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_SATELLITE_COUNT, numberOfSatellites);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_SATELLITES_USED_IN_FIX, numberOfSatellitesUsedInFix);
		owner.sendBroadcast(intent);
	}
	
	/**
	 * This method sends the signal strength update to all listeners of the signal strength update action.
	 * This update includes the absolute signal strength (in dbm) and the percentage it has been assigned.
	 * @param signalStrength
	 * @param percentage
	 */
	public void updateSignalStrength(int signalStrength, int nettype, boolean bWifiConnected, int wifiSignal){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_SIGNAL_STRENGTH_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_SIGNAL_STRENGTH_DBM, signalStrength);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_NETTYPE, nettype);

		//if (owner.wsConnected) {
			long totalRxBytes = TrafficStats.getTotalRxBytes();
			long totalTxBytes = TrafficStats.getTotalTxBytes();
			intent.putExtra(CommonIntentBundleKeysOld.KEY_RX, totalRxBytes);
			intent.putExtra(CommonIntentBundleKeysOld.KEY_TX, totalTxBytes);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_WIFI_CONNECTED, bWifiConnected);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_WIFI_SIGNAL, wifiSignal);


		//}

		owner.sendBroadcast(intent);
	}
	
	/**
	 * This method is used by the members of MMCService to send a location update to any activity.
	 * @param location Location to be sent
	 * @param locationKey The key that will be used in the extra of the intent
	 */
	public void updateLocation(Location location){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_LOCATION_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_LOCATION_UPDATE, location);
		owner.sendBroadcast(intent);
	}

	public void updateNetwork ()
	{
		SharedPreferences securePref = MMCService.getSecurePreferences(owner);
		String apn = securePref.getString(PreferenceKeys.Miscellaneous.KEY_APN, null);
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_NETWORK_UPDATE);
		intent.putExtra("APN", apn);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_NETTYPE, owner.getPhoneStateListener().getNetworkType());
		owner.sendBroadcast(intent);
	}
	
	/**
	 * Lets the recipient know whether the gps is ON (true) or OFF (false).
	 * @param status
	 */
	public void updateGpsStatus(boolean status){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_GPS_STATUS_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_GPS_STATUS_UPDATE, status);
		owner.sendBroadcast(intent);
	}
	
	/**
	 * Lets the recipient know whether the gps is ON (true) or OFF (false).
	 * @param bsHigh
	 * @param bsMid
	 * @param bsLow
	 */
	public void updateCellID(int bsHigh, int bsMid, int bsLow){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_CELL_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_BS_HIGH, bsHigh);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_BS_MID, bsMid);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_BS_LOW, bsLow);
		owner.sendBroadcast(intent);
	}
	/**
	 * Send the neighbor list to activities
	 * @param status
	 */
	public void updateNeighbors(String sneighbors){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_NEIGHBOR_UPDATE);
		// Split neighborlist twice and build an array
		String[] parts = sneighbors.split(",");
		int[] neighbors = new int[parts.length*2];
		int i =0;
		try{
		for (String part: sneighbors.split(","))
			for (String part2: part.split("@"))
				neighbors[i++] = Integer.parseInt(part2);
		} catch (Exception e) {}
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_NEIGHBORS, neighbors);
		owner.sendBroadcast(intent);
	}
	/**
	 * Send the neighbor list to activities
	 * @param status
	 */
	public void updateLTEIdentity(String lte_ids){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_NEIGHBOR_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_LTEID, lte_ids);
		owner.sendBroadcast(intent);
	}
	
	/**
	 * Send the connection activity list to activities
	 * @param status
	 */
	public void updateConnection(String conn, boolean updateRxTx){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_CONNECTION_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_CONNECTION, conn);
		owner.sendBroadcast(intent);
//		if (updateRxTx)
//		{
//			long totalRxBytes = TrafficStats.getTotalRxBytes();
//			long totalTxBytes = TrafficStats.getTotalTxBytes();
//
//			Intent intent2 = new Intent(CommonIntentBundleKeysOld.ACTION_RX_TX);
//			intent2.putExtra(CommonIntentBundleKeysOld.KEY_RX, totalRxBytes);
//			intent2.putExtra(CommonIntentBundleKeysOld.KEY_TX, totalTxBytes);
//			owner.sendBroadcast(intent2);
//
//		}
	}
	public void updateConnection(String conn)
	{
		updateConnection (conn, false);
	}
	
	/**
	 * This method sends the Bluetooth download progress for the Voice Quality recording 
	 * This update includes the current packet number and total number of packets
	 * @param packetnum
	 * @param total
	 */
	public void updateBluetoothDownload(int packetnum, int total){
		Intent intent = new Intent(VQManager.ACTION_BLUETOOTH_DOWNLOAD);
		intent.putExtra(VQManager.KEY_EXTRA_BLUETOOTH_PACKET, packetnum);
		intent.putExtra(VQManager.KEY_EXTRA_BLUETOOTH_TOTAL, total);
		owner.sendBroadcast(intent);
	}
	
	/**
	 * This method lets the recipient (Livestatus activity) know that a new
	 * event has to be marked on its chart.
	 * @param event
	 */
	public void markEvent(EventType event){
		Intent intent = new Intent(CommonIntentBundleKeysOld.ACTION_EVENT_UPDATE);
		intent.putExtra(CommonIntentBundleKeysOld.KEY_UPDATE_EVENT, event);
		intent.putExtra(MMCIntentHandlerOld.EXTRA_EVENTTYPE, event.getIntValue());
		owner.sendBroadcast(intent);
	}
	
	/*
	 * End miscellaneous public methods
	 * =================================================================
	 */
}
