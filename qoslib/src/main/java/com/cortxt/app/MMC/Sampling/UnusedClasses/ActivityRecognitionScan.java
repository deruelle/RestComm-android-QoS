package com.cortxt.app.MMC.Sampling.UnusedClasses;

//import com.google.android.gms.common.ConnectionResult;
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.location.ActivityRecognitionClient;
//
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.os.Bundle;

public class ActivityRecognitionScan /*implements GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener */ {
	
/*	private Context context;
	private static final String TAG = "ActivityRecognition";
	private static ActivityRecognitionClient mActivityRecognitionClient;
	private static PendingIntent callbackIntent;
	
	public ActivityRecognitionScan(Context context) {
		this.context = context;
	}	
	
	public void startActivityRecognitionScan() {
		mActivityRecognitionClient	= new ActivityRecognitionClient(context, this, this);
		mActivityRecognitionClient.connect();
	}
	
	public void stopActivityRecognitionScan(){
		try{
			mActivityRecognitionClient.removeActivityUpdates(callbackIntent);	
		} catch (IllegalStateException e){
//			e.printStackTrace();
		}
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult result) {
	
	}
	
	// Connection established - start listening now  s
	@Override
	public void onConnected(Bundle connectionHint) {
		Intent intent = new Intent(context, ActivityRecognitionService.class);
		callbackIntent = PendingIntent.getService(context, 0, intent,
		PendingIntent.FLAG_UPDATE_CURRENT);
		mActivityRecognitionClient.requestActivityUpdates(0, callbackIntent); 
		// 0 sets it to update as fast as possible, just use this for testing!
	}
	
	@Override
	public void onDisconnected() {
		
	} */
}
