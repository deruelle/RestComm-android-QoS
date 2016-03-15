package com.cortxt.app.MMC.ServicesOld.Location;

import com.cortxt.app.MMC.Utils.MMCLogger;

import android.location.Location;
import android.location.LocationManager;

/**
 * This class contains all the data and callbacks belonging to a listener of this class. This is the
 * class that is extended by any piece of code that wants to use the gps through the {@link GpsManagerOld}.
 * @author Abhin
 *
 */
public class GpsListenerOld {
	/**
	 * This is the time in milliseconds that the gps is to be granted by
	 * the party interested in a location for connecting to satellites and getting a first fix. If by
	 * this time the gps is not able to get a first fix, then the {@link GpsListenerOld#attemptToRenewFirstFixTimeout(int)} callback
	 * method is called.
	 */
	private int firstFixTimeout;
	/**
	 * This is the time in milliseconds that the gps will run after getting its first
	 * fix. After this time, this {@link GpsListenerOld} will be unregistered.
	 */
	private int operationTimeout;
	/**
	 * This flag is used to determine if the Listener is allowed to renew its firstFixTimeout. This is
	 * useful because after turning on the gps, the "first fix" event can happen multiple times (I know... weird).
	 * This is because over a period of time, the gps turns off and on multiple times.
	 */
	private boolean firstFixRenewalAllowed = true;
	/**
	 * This boolean is turned on when the listener receives its first location. This is useful because if the 
	 * gps is already on when a listener registers to this receiver, then a "first fix" event could be from
	 * a regular location update. 
	 * 
	 * This flag is also useful for dealing with timers. That is, if a timeout has already been scheduled for the
	 * first fix and a location update happens before it, it is very hard to cancel that timeout (that has already
	 * been scheduled). It is much simpler to just use a flag in such cases.
	 */
	private boolean firstFixReceived = false;
	
	private Location lastLocation = null;
	/**
	 * Name used for debugging (when printing to log using toString).
	 */
	private String name = "";
	private String provider = LocationManager.GPS_PROVIDER;
	
	public int getFirstFixTimeout() {
		return firstFixTimeout;
	}
	public void setFirstFixTimeout(int firstFixTimeout) {
		this.firstFixTimeout = firstFixTimeout;
	}
	public int getOperationTimeout() {
		return operationTimeout;
	}
	public void setOperationTimeout(int operationTimeout) {
		this.operationTimeout = operationTimeout;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public boolean isFirstFixRenewalAllowed() {
		return firstFixRenewalAllowed;
	}
	public void setFirstFixRenewalAllowed(boolean firstFixRenewalAllowed) {
		this.firstFixRenewalAllowed = firstFixRenewalAllowed;
	}
	public boolean isFirstFixReceived() {
		return firstFixReceived;
	}
	public void setFirstFixReceived(boolean firstFixReceived) {
		this.firstFixReceived = firstFixReceived;
	}	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Location getLastLocation ()
	{
		return lastLocation;
	}
	
	public void setLastLocation (Location location)
	{
		lastLocation = location;
	}
	
	/**
	 * Constructor.
	 * @param firstFixTimeout This is the time in milliseconds that the gps is to be granted by
	 * the party interested in a location for connecting to satellites and getting a first fix. If by
	 * this time the gps is not able to get a first fix, then the {@link GpsListenerOld#attemptToRenewFirstFixTimeout(int)} callback
	 * method is called.
	 * @param operationTimeout This is the time in milliseconds that the gps will run after getting its first
	 * fix. After this time, this {@link GpsListenerOld} will be unregistered.
	 */
	public GpsListenerOld(int firstFixTimeout, int operationTimeout, String name){
		this.firstFixTimeout = firstFixTimeout;
		this.operationTimeout = operationTimeout;
		this.name = name;
	}
	/**
	 * Default constructor. Calls the parameterized constructor {@link GpsListenerOld#Listener(int, int)}
	 * with the default values {@value GpsManagerOld#DEFAULT_GPS_FIRST_FIX_TIMEOUT} and {@value GpsManagerOld#DEFAULT_GPS_TIMEOUT}.
	 */
	public GpsListenerOld(){
		this(GpsManagerOld.DEFAULT_GPS_FIRST_FIX_TIMEOUT, GpsManagerOld.DEFAULT_GPS_TIMEOUT, "");
	}
	public GpsListenerOld(String name){
		this(GpsManagerOld.DEFAULT_GPS_FIRST_FIX_TIMEOUT, GpsManagerOld.DEFAULT_GPS_TIMEOUT, name);
	}
	
	/**
	 * This method is called when the original timeout supplied to the {@link GpsManagerOld} via this {@link GpsListenerOld}
	 * expires. This method gives the calling party a chance to evaluate whether they want to give the gps some more time
	 * (and if so, then how much).
	 * <b>Note: This callback method will be called from a timer task. Therefore, it is higly recommended that no timeconsuming
	 * operations be performed inside this callback. In case such a method is necessary, please perform it in a separate thread
	 * or an async task.</b>
	 * @param numberOfSatellites The number of satellites that the gps is currently connected to. This is a useful parameter
	 * for checking whether the gps has to be given some more time. Other parameters that should be taken into consideration
	 * are the "current battery level" and the "usage profile" the user is running.
	 * @return If a positive number is returned, then the {@link GpsManagerOld} renews the timeout to that many milliseconds. If
	 * a zero or a negative number is returned, then the {@link GpsManagerOld} unregisters this listener.
	 */
	public int attemptToRenewFirstFixTimeout(int numberOfSatellites) {
		return 0;
	}
	/**
	 * This method is called by the {@link GpsManagerOld} everytime a new gps location 
	 * comes around.
	 * @param location The location received by the gps
	 * @return True if the caller wants the gps to stay on for longer; false otherwise.
	 */
	public boolean onLocationUpdate(Location location) {
		if (location != null)
			MMCLogger.logToFile(MMCLogger.Level.DEBUG, "GpsListener", "onLocationUpdate", "lat=" + location.getLatitude() + ", lng=" + location.getLongitude() + ", acc=" + location.getAccuracy() + ", type=" + getProvider() );
		
		return false;
	}
	
	/* 
	 * overridden by GpsListenerForEvent to start wakelock and clear previous location
	 */
	public void gpsStarted ()
	{
	}
	/* 
	 * overridden by GpsListenerForEvent to release wakelock
	 */
	public void gpsStopped ()
	{
	}
	/**
	 * This method is called by the {@link GpsManagerOld} when it finally gives up on getting a location fix from the gps.
	 * <b>Note: This callback method will be called from a timer task. Therefore, it is higly recommended that no timeconsuming
	 * operations be performed inside this callback. In case such a method is necessary, please perform it in a separate thread
	 * or an async task.</b>
	 */
	public void onTimeout(){
		
	}
	
	public String toString(){
		return "(" + name + ") firstFixTimeout: " + Integer.toString(firstFixTimeout) + " operationTimeout: " + Integer.toString(operationTimeout);
	}
}
