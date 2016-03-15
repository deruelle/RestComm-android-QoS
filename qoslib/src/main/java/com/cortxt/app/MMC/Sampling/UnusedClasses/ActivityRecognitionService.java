package com.cortxt.app.MMC.Sampling.UnusedClasses;

//import android.app.IntentService;
//import android.content.Intent;
//import android.preference.PreferenceManager;
//import android.widget.Toast;
//
//import com.cortxt.app.MMC.Utils.MMCLogger;
//import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
//import com.google.android.gms.location.ActivityRecognitionResult;
//import com.google.android.gms.location.DetectedActivity;

public class ActivityRecognitionService /*extends IntentService*/ {
	
/*	public ActivityRecognitionService() {
		super("ActivityRecognitionService");
	}
	
    @Override
    public void onCreate() {
        super.onCreate();
//        System.out.println("ActivityRecognitionService oncreat()");
    }
	
	@Override
	protected void onHandleIntent(Intent intent) {	
		if (ActivityRecognitionResult.hasResult(intent)) {			
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
      
            int confidence = mostProbableActivity.getConfidence(); 
            int activityType = mostProbableActivity.getType();
            boolean walking = isWalking(activityType); 
            if(confidence < 75)
            	walking = false;
            if(walking == true) {
	            PreferenceManager.getDefaultSharedPreferences(this).edit()
	            	.putBoolean(PreferenceKeys.Miscellaneous.SAMPLING_WALKING, true).commit();      	
//	            Toast.makeText(this, "Walking", Toast.LENGTH_SHORT).show();
	            MMCLogger.logToFile(MMCLogger.Level.DEBUG, "ActivityRecognitionService", 
	            		"onHandleIntent", "Walking, confidence: " + confidence + ", activityType " +
	            		activityType);
            }
		}
	}

	private static boolean isWalking(int type){
		switch (type ) {
			case DetectedActivity.IN_VEHICLE:
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "ActivityRecognitionService", 
	            		"isWalking", "Vehicle");
				return true;
			case DetectedActivity.ON_BICYCLE:
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "ActivityRecognitionService", 
	            		"isWalking", "Bicycle");
				return true;
			case DetectedActivity.ON_FOOT:
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, "ActivityRecognitionService", 
	            		"isWalking", "Foot");
				return true;
			case DetectedActivity.TILTING:
				return false;
			case DetectedActivity.STILL:
				return false;
			default:
				return false;
		}
	} */
}
