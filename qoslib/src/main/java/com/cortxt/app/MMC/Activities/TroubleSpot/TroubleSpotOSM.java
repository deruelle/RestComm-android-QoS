package com.cortxt.app.MMC.Activities.TroubleSpot;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Settings;
import com.cortxt.app.MMC.Activities.ShareTask;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedMapActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.DropDownMenuWindow;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class TroubleSpotOSM extends MMCTrackedMapActivityOld {

	private static final HashMap<Integer, Integer> NAMES = new HashMap<Integer, Integer>();
	static {
		NAMES.put(EventType.TT_DROP.getIntValue(), R.string.troublespot_droppedcall);
		NAMES.put(EventType.TT_FAIL.getIntValue(), R.string.troublespot_failedcall);
		NAMES.put(EventType.TT_DATA.getIntValue(), R.string.troublespot_datasesion);
		NAMES.put(EventType.TT_NO_SVC.getIntValue(), R.string.troublespot_coverage);
	}

	private static final int DEFAULT_ZOOM_LEVEL = 16;

	private MapView mMapView;
	private MyLocationOverlay mMyLocationOverlay;
	//private ViewFlipper mViewFlipper;
	//private ImageButton mCenterButton;
	//	private LinearLayout mInfoBubble;
	//	private TextView mBubbleComment;
	//	private ImageView mCarrierLogo;
	//	private ImageView mCarrierLogoBg;
	//	private TextView mShareButtonText;
	private ImageView mIssueIcon;
	private String mCommentText;
	private TextView troubleLocation=null;
	private TextView troubleEventTime=null;
	private TextView troubleEventDate=null;
	private RelativeLayout loggedTroubleEventLayout=null;
	private ImageView mBubbleImpactIcon;
	private TextView mName;
	private Button mShareButtonImg;
	private RelativeLayout troubleTypeLayout=null;
	private RelativeLayout troubleImpactLayout=null;
	private DropDownMenuWindow troubleMenu=null;
	private Button menuButton=null;
	private boolean mOpenedSettings = false;
	private TextView headingText=null;
	private AsyncTask<Void, Void, Bitmap> mGetCarrierLogoTask;

	private Handler mHandler = new Handler();

	/**
	 * Type of issue selected in step 1
	 */
	private EventType mSelectedIssueType;
	/**
	 * Impact level selected in step 2
	 */
	private int mImpactLevel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view  = inflater.inflate(R.layout.trouble_tweet_osm, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
        MMCActivity.customizeTitleBar (this,view,R.string.dashboard_trouble, R.string.dashcustom_trouble);
		
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			view.setBackgroundColor(Color.parseColor("#FFFFFF"));
		}
		mMapView = (MapView) findViewById(R.id.troublespot_mapview);
		mIssueIcon = (ImageView) findViewById(R.id.troublespot_issueicon);
		troubleTypeLayout=(RelativeLayout)findViewById(R.id.TroubleTypeLayout);
		troubleImpactLayout=(RelativeLayout)findViewById(R.id.TroubleImpactLayout);
		headingText=(TextView)findViewById(R.id.issueText);
		mShareButtonImg = (Button) findViewById(R.id.shareButton);
		loggedTroubleEventLayout=(RelativeLayout)findViewById(R.id.loggedTroubleEventlayout);
		troubleLocation = (TextView) findViewById(R.id.troubleLocation);
		mName = (TextView) findViewById(R.id.troubleEvent_name);
		troubleEventTime=(TextView)findViewById(R.id.troubleEvent_time);
		troubleEventDate=(TextView)findViewById(R.id.troubleEvent_date);
		mBubbleImpactIcon=(ImageView)findViewById(R.id.troubleEvent_icon);
		//menuButton=(Button)findViewById(R.id.actionbarMenuIcon);
		applyFonts(view);
		//mViewFlipper = (ViewFlipper) findViewById(R.id.troublespot_viewflipper);
		//mCenterButton = (ImageButton) findViewById(R.id.troublespot_centerbutton);

		//mComment = (EditText) findViewById(R.id.troublespot_comment);
		//		mTimeAndLocation = (TextView) findViewById(R.id.troublespot_timeandlocation);
		//
		//		mInfoBubble = (LinearLayout) findViewById(R.id.troublespot_infobubble);
		//		
		//		mBubbleComment = (TextView) findViewById(R.id.troublespot_bubble_comment);
		//		mBubbleImpactIcon = (ImageView) findViewById(R.id.troublespot_bubble_impacticon);
		//		mCarrierLogo = (ImageView) findViewById(R.id.troublespot_bubble_carrierlogo);
		//		mCarrierLogoBg = (ImageView) findViewById(R.id.troublespot_bubble_carrierlogobg);

		//		mShareButtonText = (TextView) findViewById(R.id.troublespot_bubble_sharebuttontext);
		//
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);
		//mMapView.getOverlays().add(mMyLocationOverlay);
		
//		final ITileSource tileSource = new XYTileSource("SomeName", null, 5,22, 256, ".png",
//                "http://tiles.mymobilecoverage.com/osm_tiles/");
//		mMapView.setTileSource (tileSource);

		centerOnLastKnownLocation();
		Toast toast = Toast.makeText(this, R.string.troublespot_intromessage, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();

		/**
		 * This task gets the carrier logo
		 */
		//		mGetCarrierLogoTask = new AsyncTask<Void, Void, Bitmap>() {
		//			@Override
		//			protected Bitmap doInBackground(Void... params) {
		//				try {
		//					HashMap<String, String> carrier = ((MMCApplication) getApplicationContext()).getDevice().getCarrierProperties();
		//					return ReportManager.getInstance(getApplicationContext()).getCarrierLogo(carrier);
		//				} catch (MMCException e) {
		//					return null;
		//				}
		//			}
		//
		//			@Override
		//			protected void onPostExecute(Bitmap result) {
		//				if(result != null) {
		//					mCarrierLogo.setImageBitmap(result);
		//				}
		//			}
		//		}.execute((Void[])null); 
	}

	private AsyncTask<Void, Void, String> mGetAddressTask;

	@Override
	protected void onPause() {
		super.onPause();

		mMyLocationOverlay.disableMyLocation();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mOpenedSettings)
		{
			mOpenedSettings = false;
			shareClicked (null);
		}
		else if (mMyLocationOverlay != null)
			mMyLocationOverlay.enableMyLocation();
		//
		//		if (mCarrierLogo.getVisibility() == View.VISIBLE)
		//		{
		//			mShareButtonImg.setVisibility(View.VISIBLE);
		//			mShareButtonText.setVisibility(View.VISIBLE);
		//			mCarrierLogo.setVisibility(View.GONE);
		//			mCarrierLogoBg.setVisibility(View.GONE);
		//		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();

	}

   

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if(mGetAddressTask != null) {
			mGetAddressTask.cancel(true);
		}

		if(mGetCarrierLogoTask != null) {
			mGetCarrierLogoTask.cancel(true);
		}
	}

    public void backActionClicked(View button){
    	this.finish();
    }

	//@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	private void applyFonts(View view){
		TextView heading =(TextView)view.findViewById(R.id.actionbartitle);
		TextView droppedtrouble=(TextView)view.findViewById(R.id.droppedTroubleName);
		TextView failedtrouble=(TextView)view.findViewById(R.id.FailedTroubleName);
		TextView dataSessionText=(TextView)view.findViewById(R.id.DataTroubleName);
		TextView noCoverageTrouble=(TextView)view.findViewById(R.id.NoCoverageTroubleName);
		TextView lowImpactText=(TextView)view.findViewById(R.id.lowImpactText);
		TextView mediumImpact=(TextView)view.findViewById(R.id.MediumImpactText);
		TextView highImpact=(TextView)view.findViewById(R.id.HighImpactTroubleName);

	    String labelColor = (getResources().getString(R.string.TROUBLE_CUSTOM_LABEL_COLOR));
        MMCActivity.customizeSimpleLabelsColor(view,new int[]{R.id.droppedTroubleName,R.id.FailedTroubleName,R.id.DataTroubleName,R.id.NoCoverageTroubleName,R.id.lowImpactText,R.id.MediumImpactText,R.id.HighImpactTroubleName},labelColor);
        MMCActivity.customizeHeadings(this, view, new int[]{R.id.issueText});
	    
	    int customTitles = getResources().getInteger(R.integer.CUSTOM_EVENTNAMES);
		if (customTitles == 1)
		{
			if (droppedtrouble != null)
				droppedtrouble.setText(R.string.troublecustom_droppedcall);
			if (failedtrouble != null)
				failedtrouble.setText(R.string.troublecustom_failedcall);
			if (dataSessionText != null)
				dataSessionText.setText(R.string.troublecustom_datasesion);
			if (noCoverageTrouble != null)
				noCoverageTrouble.setText(R.string.troublecustom_coverage);
		}
		
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, heading, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, droppedtrouble, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, failedtrouble, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, dataSessionText, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, noCoverageTrouble, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, lowImpactText, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, mediumImpact, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, highImpact, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, headingText, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, troubleEventDate, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, troubleEventTime, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, troubleLocation, TroubleSpotOSM.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, mName, TroubleSpotOSM.this);
	}
	/**
	 * Change map center to last known location
	 * @return true if there was a last known location and the map was successfully centered on it, false otherwise
	 */
	private boolean centerOnLastKnownLocation() {
		if(mMyLocationOverlay.getMyLocation() != null) {
			int latitudeE6 = mMyLocationOverlay.getMyLocation().getLatitudeE6();
			int longitudeE6 = mMyLocationOverlay.getMyLocation().getLongitudeE6();
			System.out.println(latitudeE6 + " " + longitudeE6);
			
			if(latitudeE6 == 0 && longitudeE6 == 0)
				return false;
			mMapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
			return true;
		}
		else if(mMyLocationOverlay.getLastFix() != null) {
			int latitudeE6 = (int)(mMyLocationOverlay.getLastFix().getLatitude() * 1000000.0);
			int longitudeE6 = (int)(mMyLocationOverlay.getLastFix().getLongitude() * 1000000.0);
			System.out.println(latitudeE6 + " " + longitudeE6);
			
			if(latitudeE6 == 0 && longitudeE6 == 0)
				return false;
			mMapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
			return true;
		}
		else {
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(lastKnownLocation != null) {
				int latitudeE6 = (int)(lastKnownLocation.getLatitude() * 1000000.0);
				int longitudeE6 = (int)(lastKnownLocation.getLongitude() * 1000000.0);
				System.out.println(latitudeE6 + " " + longitudeE6);
				
				if(latitudeE6 == 0 && longitudeE6 == 0)
					return false;
				mMapView.getController().setZoom(DEFAULT_ZOOM_LEVEL);
				mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
				return true;
			}
		}
		return false;
	}

	public void centerOnCurrentLocationClicked(View v) {
		boolean worked = centerOnLastKnownLocation();

		if(!worked) {
			Toast toast = Toast.makeText(this, R.string.mycoverage_unknownlocation, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
	}

	public void issueButtonClicked(View button) {
		if(button.getTag().equals(getString(R.string.troublespot_droppedcall))) {
			mSelectedIssueType = EventType.TT_DROP;
		}
		else if(button.getTag().equals(getString(R.string.troublespot_failedcall))) {
			mSelectedIssueType = EventType.TT_FAIL;
		}
		else if(button.getTag().equals(getString(R.string.troublespot_datasesion))) {
			mSelectedIssueType = EventType.TT_DATA;
		}
		else if(button.getTag().equals(getString(R.string.troublespot_coverage))) {
			mSelectedIssueType = EventType.TT_NO_SVC;
		}

		showIssueIcon(mSelectedIssueType.getMapImageResource());
		mMapView.setClickable(false);
		troubleImpactLayout.setVisibility(View.VISIBLE);
		troubleTypeLayout.setVisibility(View.GONE);
		headingText.setText(R.string.troublespot_step2_message);
		//mCenterButton.setVisibility(View.GONE);
		//mViewFlipper.showNext();
	}

	public void impactButtonClicked(View button) {
		if(button.getTag().equals(getString(R.string.troublespot_impactlow))) {
			mImpactLevel = 1;
			mCommentText = getString(R.string.troublespot_comment_impactlow);
		}
		else if(button.getTag().equals(getString(R.string.troublespot_impactmedium))) {
			mImpactLevel = 2;
			mCommentText = getString(R.string.troublespot_comment_impactmedium);
		}
		else if(button.getTag().equals(getString(R.string.troublespot_impacthigh))) {
			mImpactLevel = 3;
			mCommentText = getString(R.string.troublespot_comment_impacthigh);
		}
		submit();
		troubleImpactLayout.setVisibility(View.GONE);
		headingText.setVisibility(View.GONE);
		mShareButtonImg.setVisibility(View.VISIBLE);
		LayoutParams params=mMapView.getLayoutParams();
		params.height=LayoutParams.MATCH_PARENT;
		mMapView.setLayoutParams(params);

	}

	public void submit () 
	//Clicked(View button) 
	{
		report();
		Toast toast = Toast.makeText(this, R.string.troublespot_thankyou, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		//trackEvent ("TroubleTweet", "submit", "", mSelectedIssueType.getIntValue());

		//TODO: get address of location
		mGetAddressTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				double latitude = ((double) mMapView.getMapCenter().getLatitudeE6()) / 1000000.0;
				double longitude = ((double) mMapView.getMapCenter().getLongitudeE6())/ 1000000.0;
				return WebReporter.geocode(latitude, longitude);
			}

			@Override
			protected void onPostExecute(String result) {
				long timeStamp = System.currentTimeMillis();
				DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
				DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
				Date dateValue = new Date(timeStamp);
				troubleEventDate.setText(" / "+dateFormat.format(dateValue));
				troubleEventTime.setText(timeFormat.format(dateValue));

				String address = "";
				if(result != null)
					address = "@ " + result;

				String phone = "";
				if(android.os.Build.BRAND.length() > 0) {
					String brand = android.os.Build.BRAND.substring(0, 1).toUpperCase() + android.os.Build.BRAND.substring(1);
					phone = "\n" + getString(R.string.eventdetail_phone) + " " +  brand + " " + android.os.Build.MODEL;
				}
				troubleLocation.setText(address);

			}

		}.execute((Void[])null);

		mMapView.getOverlays().remove(mMyLocationOverlay);
		mMyLocationOverlay.disableMyLocation();

		//mInfoBubble.setVisibility(View.VISIBLE);
		mIssueIcon.setVisibility(View.GONE);
		mName.setText(NAMES.get(mSelectedIssueType.getIntValue()));
		//		mName.setCompoundDrawablesWithIntrinsicBounds(mSelectedIssueType.getMapImageResource(), 0, 0, 0);
		//		mBubbleComment.setText(mCommentText);

		int impactLevelIcon;
		if(mImpactLevel == 3)
			impactLevelIcon = R.drawable.problem_3_icon_light;
		else if(mImpactLevel == 2)
			impactLevelIcon = R.drawable.problem_2_icon_light;
		else
			impactLevelIcon = R.drawable.problem_1_icon_light;
		mBubbleImpactIcon.setImageResource(impactLevelIcon);
		loggedTroubleEventLayout.setVisibility(View.VISIBLE);

	}
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU ) {
			//topMenuActionClicked(menuButton);
			return true;
		}else if(keyCode==KeyEvent.KEYCODE_BACK){
			if(troubleMenu!=null && DropDownMenuWindow.isWindowAlreadyShowing){
				troubleMenu.dismissWindow();
				return true;
			}
			this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}
	public void topMenuActionClicked(View button){
		if(DropDownMenuWindow.isWindowAlreadyShowing && troubleMenu!=null){
			troubleMenu.dismissWindow();
			return;
		}
		long currentTime=System.currentTimeMillis();
		if(currentTime-DropDownMenuWindow.lastWindowDismissedTime>200){
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View menuOptionsView = inflater.inflate(R.layout.trouble_tweet_menu_options, null, false);
			ScalingUtility.getInstance(this).scaleView(menuOptionsView);
			TextView recentEventText=(TextView)menuOptionsView.findViewById(R.id.recentEventText);
			TextView allEventsText=(TextView)menuOptionsView.findViewById(R.id.allEventText);
			FontsUtil.applyFontToTextView(MmcConstants.font_Regular, recentEventText, this);
			FontsUtil.applyFontToTextView(MmcConstants.font_Regular, allEventsText, this);
			troubleMenu=new DropDownMenuWindow(menuOptionsView,this,MmcConstants.MAP_MENU_OFFSET,MmcConstants.TROUBLE_TWEET_MENU_WINDOW_WIDTH);
			troubleMenu.showCalculatorMenu(menuButton);

		}
	}
	public void shareClicked(View button) {
		
		temporarilyDisableButton(button);
		
		String strTwitter = PreferenceKeys.getSecurePreferenceString(PreferenceKeys.User.TWITTER, "",this);
		boolean bAskedTwitter = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.User.ASKED_TWITTER, false);
		if (strTwitter.length() == 0 && bAskedTwitter == false)
		{
			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PreferenceKeys.User.ASKED_TWITTER, true).commit();

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(true);
			builder.setIcon(R.drawable.tweeter_icon);
			builder.setTitle(R.string.GenericText_AskTwitter);
			String strMessage = getString(R.string.GenericText_AskTwitterDescription);

			builder.setMessage(strMessage);
			builder.setInverseBackgroundForced(true);
			builder.setPositiveButton(R.string.GenericText_Settings, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// launch settings screen
					Intent startSettingsIntent = new Intent(TroubleSpotOSM.this, Settings.class);
					startActivity(startSettingsIntent);
					mOpenedSettings = true;
					dialog.dismiss();
				}
			});
			builder.setNegativeButton(R.string.GenericText_Skip, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					shareClicked (null);
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return;
		}
		
		//		mShareButtonImg.setVisibility(View.GONE);
		//		mShareButtonText.setVisibility(View.GONE);
		//		mCarrierLogo.setVisibility(View.VISIBLE);
		//		mCarrierLogoBg.setVisibility(View.VISIBLE);

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				String message;
				String subject;

				if(mSelectedIssueType == EventType.TT_DROP) {
					message = getString(R.string.sharemessage_troublespot_droppedcall);
					subject = getString(R.string.sharemessagesubject_troublespot_droppedcall);
				}
				else if(mSelectedIssueType == EventType.TT_FAIL) {
					message = getString(R.string.sharemessage_troublespot_failedcall);
					subject = getString(R.string.sharemessagesubject_troublespot_failedcall);
				}
				else if(mSelectedIssueType == EventType.TT_DATA) {
					message = getString(R.string.sharemessage_troublespot_datasession);
					subject = getString(R.string.sharemessagesubject_troublespot_datasession);
				}
				else {
					message = getString(R.string.sharemessage_troublespot_coverage);
					subject = getString(R.string.sharemessagesubject_troublespot_coverage);
				}

				new ShareTask(TroubleSpotOSM.this, message, subject,
						findViewById(R.id.troublespot_container)).execute((Void[])null);
			}
		}, 500);
	}
	
	public void temporarilyDisableButton(final View button) {
		
		button.setEnabled(false);
		
		new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);  
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                TroubleSpotOSM.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        button.setEnabled(true);

                    }
                });
            }
        }).start();
	}

	private void report() {
		/*
		boolean roaming = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).isNetworkRoaming();
		HashMap<String, String> carrierProperties = ((MMCApplication) getApplicationContext()).getDevice().getCarrierProperties();

		TroubleSpotData troubleSpot = new TroubleSpotData(System.currentTimeMillis(), mSelectedIssueType, mImpactLevel,
				mCommentText.toString(), mMapView.getMapCenter(), carrierProperties, roaming);

		ReportManager.getInstance(getApplicationContext()).reportTroubleSpot(troubleSpot);
		 */
		ReportManager.getInstance(getApplicationContext()).reportTroubleTweet(mSelectedIssueType, mImpactLevel,
				mCommentText.toString(), (GeoPoint)mMapView.getMapCenter());
	}


	private void showIssueIcon(int imageResource) {
		float density = getResources().getDisplayMetrics().density;
		TranslateAnimation animation = new TranslateAnimation(0, 0, (mMapView.getHeight()/-2), 0);

		animation.setDuration(350);
		mIssueIcon.setImageResource(imageResource);
		mIssueIcon.startAnimation(animation);

	}

	private void resetToStep1() {
		//mViewFlipper.setDisplayedChild(0);
		mMapView.setClickable(true);
		//		mCenterButton.setVisibility(View.VISIBLE);
		mIssueIcon.setImageResource(R.drawable.troublespot_crosshairs);
	}
}