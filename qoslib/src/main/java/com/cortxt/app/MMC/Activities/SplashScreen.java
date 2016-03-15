package com.cortxt.app.MMC.Activities;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.securepreferences.SecurePreferences;

public class SplashScreen extends MMCActivity {
	/**
	 * Time to display splash screen for (milliseconds)
	 */
	private static final long SPLASH_SCREEEN_DELAY = 1000;
		
	private Handler mHandler = new Handler();
	private Runnable launchNextScreen;
    
	public static final String TAG = SplashScreen.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = null;
		int customSplash = (this.getResources().getInteger(R.integer.CUSTOM_SPLASH));
		if (customSplash == 0)
			view = inflater.inflate(R.layout.splashscreen, null, false);
		else 
			view = inflater.inflate(R.layout.splashscreencustom, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
        convertSecurePreferences ();
				
		try{
			ConnectivityManager connectivityManager = null;
//			connectivityManager.isActiveNetworkMetered(); //for testing connectivity error
			connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		}
		catch (Exception e)
		{
			AlertDialog alertDialog = new AlertDialog.Builder(SplashScreen.this).create();

            // Setting Dialog Title
            alertDialog.setTitle("Exception occurred during connectivity.");
            alertDialog.setMessage("Unable to detect connectivity status.");

            // Showing Alert Message
            alertDialog.show();			
			this.finish();
			return;
		}

		launchNextScreen = new Runnable() {
		
			@Override
			public void run() {
				SharedPreferences preferenceSettings = MMCService.getSecurePreferences(SplashScreen.this);

				ReportManager reportManager = ReportManager.getInstance(SplashScreen.this);
				boolean isRegistered = ReportManager.getInstance(getApplicationContext()).isAuthorized();
				if (!isRegistered)
				{
					//boolean isUpgrading = attemptUpgradeUser ();
					//if (isUpgrading)
					//	return;
					int userID = MMCService.getUserID(SplashScreen.this);
                    SecurePreferences prefs = MMCService.getSecurePreferences(SplashScreen.this);

                    if (userID > 0 && prefs.getBoolean(PreferenceKeys.Miscellaneous.SIGNED_OUT, false) == false)
						isRegistered = true;				
					
				}	
						
				if(isRegistered) {
					boolean bStoppedService = preferenceSettings.getBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, false);

					if (!bStoppedService)
					{
						MMCService.start(SplashScreen.this);
					}
					// when existing user upgrades from v91 or less, his userid < 59000
					// we'll continue to show the MMC icon at the top because they are used to it
//					int iLastVersion = preferenceSettings.getInt(PreferenceKeys.User.VERSION, -1);
//					if (iLastVersion < 92 && iLastVersion > 0)
//					{
//						int iUserID = preferenceSettings.getInt(PreferenceKeys.User.USER_ID, -1);
//						if (iUserID < 59000)
//							preferenceSettings.edit().putBoolean(PreferenceKeys.Miscellaneous.ICON_ALWAYS, true).commit();
//					}

					Intent dashboardIntent = new Intent(SplashScreen.this, Dashboard.class);
					startActivity(dashboardIntent);
				}
				else {
					Intent intent = new Intent(SplashScreen.this, GetStarted2.class);
					startActivity(intent);
				}
				preferenceSettings.edit().putInt(PreferenceKeys.User.VERSION, getAppVersionCode()).commit();
			}
		};
		
		boolean isInstalled = isPackageAlreadyInstalled();		
		if (isInstalled == false) {
			mHandler.postDelayed(launchNextScreen, SPLASH_SCREEEN_DELAY);
		}
	}

	private boolean isPackageAlreadyInstalled() {
		
	    ArrayList<String> apps = getInstalledApps(false); /* false = no system packages */
	    final int max = apps.size();
	    PackageManager pm = getPackageManager();
	   
	    String currentMMCPackage = this.getPackageName();
	    String otherMMCPackages = "com.cortxt.app.MMC_";
	    for (int i=0; i<max; i++) {
	         //Log.v("Installed application: " + apps.get(i), currentMMCPackage); //for testing 
	         boolean bConflict = false;
	         String pkgName = apps.get(i);
	         if(currentMMCPackage.equals("com.cortxt.app.MMC") && pkgName.indexOf(otherMMCPackages) >= 0) 
	        	 bConflict = true;
	         else if (!currentMMCPackage.equals("com.cortxt.app.MMC") && pkgName.equals("com.cortxt.app.MMC"))
	        	 bConflict = true;
	         if (bConflict) { 
	        	
	        	//String appName = apps.get(i-1);
	    	    PackageInfo packinfo = null;
				try {
					packinfo = pm.getPackageInfo(pkgName, 0);
					CharSequence cAppInfo = packinfo.applicationInfo.loadLabel(pm);
		    	    String appName = cAppInfo.toString();
		    	    
		        	//Log.v("Package already installed: " + apps.get(i), otherMMCPackages); //for testing 
				    AlertDialog alertDialog = new AlertDialog.Builder(SplashScreen.this).create();
			
			        // Setting Dialog Title
			        alertDialog.setTitle("Conflict with " + appName);/*R.string.GenericText_ConflictMMC); */ // 
			        alertDialog.setMessage("Please uninstall " + appName + " and then restart this application.");//getText(R.string.GenericText_UninstallMMC));
			
			        // Showing Alert Message
			        alertDialog.show();	
			        return true;	
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	    	        		      
	        }		        
	    }
	    return false;
	}	
	 
	private ArrayList<String> getInstalledApps(boolean getSysPackages) {
	    ArrayList<String> res = new ArrayList<String>();    
	    PackageManager pm = getPackageManager();
	    List<PackageInfo> packs = pm.getInstalledPackages(0);
	    
	    for(int i=0;i<packs.size();i++) {
	        PackageInfo p = packs.get(i);
	        if ((!getSysPackages) && (p.versionName == null)) {
	            continue ;
	        } 	
	        //CharSequence cAppInfo = p.applicationInfo.loadLabel(pm);
	        //String sAppInfo = cAppInfo.toString();
	        //res.add("");
	        res.add(p.packageName);
	    }
	    return res; 
	}
	
	private int getAppVersionCode(){
		try {
			return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode;
		} catch(NameNotFoundException e){
			MMCLogger.logToFile(MMCLogger.Level.ERROR, "SplashScreen", "getAppVerionCode", "Could not find app version" + e.getMessage());
		}
		return -1;
	}

	@Override
	public void onBackPressed() {
		if(launchNextScreen != null) {
			mHandler.removeCallbacks(launchNextScreen);
		}

		super.onBackPressed();
	}

    // For backwards compatibility, if some secure preferences were not secure when updating, copy those to secure
    private void convertSecurePreferences ()
    {
        try {
            SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences securePreferences = MMCService.getSecurePreferences(this);
            SharedPreferences.Editor secureEditor = securePreferences.edit();
            SharedPreferences.Editor defaultEditor = defaultPreferences.edit();
            String apikey = defaultPreferences.getString(PreferenceKeys.User.APIKEY, null);
            if (apikey != null && apikey.length() > 10) {
                secureEditor.putString(PreferenceKeys.User.APIKEY, apikey);
                defaultEditor.putString(PreferenceKeys.User.APIKEY, null);

                String pref = defaultPreferences.getString(PreferenceKeys.User.USER_EMAIL, null);
                secureEditor.putString(PreferenceKeys.User.USER_EMAIL, pref);
                defaultEditor.remove(PreferenceKeys.User.USER_EMAIL);

                int ipref = defaultPreferences.getInt(PreferenceKeys.User.USER_ID, 0);
                secureEditor.putInt(PreferenceKeys.User.USER_ID, ipref);
                defaultEditor.remove(PreferenceKeys.User.USER_ID);

                pref = defaultPreferences.getString(PreferenceKeys.User.TWITTER, null);
                secureEditor.putString(PreferenceKeys.User.TWITTER, pref);
                defaultEditor.remove(PreferenceKeys.User.TWITTER);

                pref = defaultPreferences.getString(PreferenceKeys.User.CONTACT_EMAIL, null);
                secureEditor.putString(PreferenceKeys.User.CONTACT_EMAIL, pref);
                defaultEditor.remove(PreferenceKeys.User.CONTACT_EMAIL);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPOP_LAT, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPOP_LAT, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPOP_LAT);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPOP_LNG, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPOP_LNG, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPOP_LNG);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPOP_RESPONSE, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPOP_RESPONSE, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPOP_RESPONSE);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPSTATS_LAT, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPSTATS_LAT, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPSTATS_LAT);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPSTATS_LNG, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPSTATS_LNG, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPSTATS_LNG);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.TOPSTATS_RESPONSE, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.TOPSTATS_RESPONSE, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.TOPSTATS_RESPONSE);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.EVENTS_QUEUE, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.EVENTS_QUEUE, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.EVENTS_QUEUE);

                boolean bPref = defaultPreferences.getBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, false);
                secureEditor.putBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, bPref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.STOPPED_SERVICE);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST);

                pref = defaultPreferences.getString(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE, "");
                secureEditor.putString(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE, pref);
                defaultEditor.remove(PreferenceKeys.Miscellaneous.OPERATOR_RESPONSE);

                pref = defaultPreferences.getString(PreferenceKeys.User.USER_PASSWORD, null);
                defaultEditor.remove(PreferenceKeys.User.USER_PASSWORD);

                pref = defaultPreferences.getString(PreferenceKeys.User.USER_OBJECTID, null);
                defaultEditor.remove(PreferenceKeys.User.USER_OBJECTID);

                defaultEditor.commit ();
                secureEditor.commit ();
            }
        }
        catch (Exception e)
        {

        }
    }

//	protected boolean attemptUpgradeUser ()
//	{
//		// Retrieve email and password for older version users
//		String username = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.USER_EMAIL, null);
//		String password = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.USER_PASSWORD, null);
//		int userID = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.User.USER_ID, -1);
//		if (username != null && password != null)
//		{
//			//Intent intent = new Intent(this, Login.class);
//			//intent.putExtra(CommonIntentBundleKeysOld.Miscellaneous.AUTO_LOGIN, true);
//			//startActivity(intent);	
//			login(username, password);
//
//			return true;
//		}
//		return false;
//	}

	/**
	 * Function to login a user
	 * @param user the user name
	 * @param password  the password
	 */
//	private Handler handler = new Handler();
//	private void login(String username, String password) {
//		// If the activity is already signing in then don't create another thread
//		Thread userLoginThread = new Thread(
//				new UserLoginThread(
//						username,
//						password,
//						this,
//						handler
//						)
//				);
//		userLoginThread.start();
//
//	}
}
