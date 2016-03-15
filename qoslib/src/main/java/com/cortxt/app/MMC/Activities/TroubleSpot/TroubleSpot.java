package com.cortxt.app.MMC.Activities.TroubleSpot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Activities.Settings;
import com.cortxt.app.MMC.Activities.ShareTask;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.DropDownMenuWindow;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Fragments.TroubleImpactFragment;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Fragments.TroubleImpactFragment.TroubleImpactSelectListener;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Fragments.TroubleTypeFragment;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Fragments.TroubleTypeFragment.TroubleTypeSelectListener;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;

public class TroubleSpot extends MMCTrackedActivityOld implements TroubleTypeSelectListener, TroubleImpactSelectListener{

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

	String troubleTypeFragmentTag = "troubletypefragment";
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view  = inflater.inflate(R.layout.trouble_tweet, null, false);

		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
		MMCActivity.customizeTitleBar (this,view,R.string.dashboard_trouble, R.string.dashcustom_trouble);

		FragmentManager fm = getSupportFragmentManager();
		if(isPortrait) {
			Fragment troubleType = new TroubleTypeFragment();
			Bundle b = new Bundle(1);
			b.putBoolean("highlightItem", false);
			troubleType.setArguments(b);
			if(findViewById(R.id.fragment_container) != null) {
				fm.beginTransaction().add(R.id.fragment_container, troubleType, troubleTypeFragmentTag).commit();
			}
		}else{
//			Toast.makeText(this, "TABLET_MODE", Toast.LENGTH_SHORT).show();
			Fragment f = fm.findFragmentByTag(troubleTypeFragmentTag);
			if(f != null){
				fm.beginTransaction().remove(f).commit();
			}
		}
		
//		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
//			view.setBackgroundColor(Color.parseColor("#FFFFFF"));
//		}
		TroubleSpotMapFragment mapFrag = (TroubleSpotMapFragment) getSupportFragmentManager().findFragmentById(R.id.troublespot_map_fragment);
		mMapView = ((TroubleSpotMapActivity) mapFrag.getHostedActivity()).mMapView;
		mIssueIcon = (ImageView) findViewById(R.id.troublespot_issueicon);

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
		mMapView.getOverlays().add(mMyLocationOverlay);
		mMapView.setSatellite(false);

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

//	@Override
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
		TextView step1=(TextView)view.findViewById(R.id.issueText);
		TextView step2=(TextView)view.findViewById(R.id.issueText2);
		
	    String labelColor = (getResources().getString(R.string.TROUBLE_CUSTOM_LABEL_COLOR));
        MMCActivity.customizeSimpleLabelsColor(view,new int[]{R.id.droppedTroubleName,R.id.FailedTroubleName,R.id.DataTroubleName,R.id.NoCoverageTroubleName,R.id.lowImpactText,R.id.MediumImpactText,R.id.HighImpactTroubleName},labelColor);
        MMCActivity.customizeHeadings(this, view, new int[]{R.id.issueText, R.id.issueText2});
	    
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
			if (lowImpactText != null)
				lowImpactText.setText(R.string.troublecustom_impactlow);
			if (mediumImpact != null)
				mediumImpact.setText(R.string.troublecustom_impactmedium);
			if (highImpact != null)
				highImpact.setText(R.string.troublecustom_impacthigh);
			if (step1 != null)
				step1.setText(R.string.troublecustom_step1_message);
			if (step2 != null)
				step2.setText(R.string.troublecustom_step2_message);
			//if (step3 != null)
			//	step3.setText(R.string.troublecustom_step3_message);
		}
		
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, heading, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, droppedtrouble, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, failedtrouble, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, dataSessionText, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, noCoverageTrouble, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, lowImpactText, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, mediumImpact, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, highImpact, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, headingText, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, troubleEventDate, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, troubleEventTime, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, troubleLocation, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, mName, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, step1, TroubleSpot.this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, step2, TroubleSpot.this);
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
	
	// TODO: fix this -- should support older apis
	@SuppressLint("NewApi")
	private boolean isTablet() {
		troubleTypeLayout = (RelativeLayout) findViewById(R.id.TroubleTypeLayout);
		troubleImpactLayout = (RelativeLayout) findViewById(R.id.TroubleImpactLayout);
		// EventHistoryFragment f = new EventHistoryFragment();

		View troubleImpactView = findViewById(R.id.fragTroubleImpact);
		if (troubleImpactView != null && troubleImpactView.getVisibility() == View.VISIBLE) {
			// its tablet/landscape mode
			// getFragmentManager().beginTransaction().remove(f).commit();
			return true;
		} else {
			// its phone/portrait mode
			// getFragmentManager().beginTransaction().add(R.id.mycoverage_mapview_container,
			// f).commit();

			return false;
		}
	}

	public void submit () 
	//Clicked(View button) 
	{
		report();
		Toast toast = Toast.makeText(this, R.string.troublespot_thankyou, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
		trackEvent ("TroubleTweet", "submit", "", mSelectedIssueType.getIntValue());

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
				DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
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
		//mIssueIcon.setVisibility(View.GONE);
		mName.setText(NAMES.get(mSelectedIssueType.getIntValue()));
		//		mName.setCompoundDrawablesWithIntrinsicBounds(mSelectedIssueType.getMapImageResource(), 0, 0, 0);
		//		mBubbleComment.setText(mCommentText);

		if(isPortrait) {
			int hideTweet = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.HIDE_TWEET_SHARE, 1);
			if (hideTweet == 0)
				mShareButtonImg.setVisibility(View.VISIBLE);
			View mapContainer = findViewById(R.id.troublespot_map_fragment);
			LayoutParams params = mapContainer.getLayoutParams();
			params.height = LayoutParams.MATCH_PARENT;
			mapContainer.setLayoutParams(params);
			
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run(){
					int height = ScalingUtility.getInstance(TroubleSpot.this).getCurrentHeight();
					MarginLayoutParams linearParams = (MarginLayoutParams) mIssueIcon.getLayoutParams();
					linearParams.topMargin = height/2 - mIssueIcon.getHeight()*3/2;
					mIssueIcon.setLayoutParams(linearParams);
					
				}},100);
		}else{
			findViewById(R.id.actionbarShareIcon).setVisibility(View.VISIBLE);
		}
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
		
		String strTwitter = PreferenceKeys.getSecurePreferenceString(PreferenceKeys.User.TWITTER, "", this);
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
					Intent startSettingsIntent = new Intent(TroubleSpot.this, Settings.class);
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
				int customSocialText = (getResources().getInteger(R.integer.CUSTOM_SOCIALTEXT));

				if(mSelectedIssueType == EventType.TT_DROP) {
					message = getString((customSocialText == 1)?R.string.sharecustom_troublespot_droppedcall:R.string.sharemessage_troublespot_droppedcall);
					subject = getString((customSocialText == 1)?R.string.sharecustomsubject_troublespot_droppedcall:R.string.sharemessagesubject_troublespot_droppedcall);
				}
				else if(mSelectedIssueType == EventType.TT_FAIL) {
					message = getString((customSocialText == 1)?R.string.sharecustom_troublespot_failedcall:R.string.sharemessage_troublespot_failedcall);
					subject = getString((customSocialText == 1)?R.string.sharecustomsubject_troublespot_failedcall:R.string.sharemessagesubject_troublespot_failedcall);
				}
				else if(mSelectedIssueType == EventType.TT_DATA) {
					message = getString((customSocialText == 1)?R.string.sharecustom_troublespot_datasession:R.string.sharemessage_troublespot_datasession);
					subject = getString((customSocialText == 1)?R.string.sharecustomsubject_troublespot_datasession:R.string.sharemessagesubject_troublespot_datasession);
				}
				else {
					message = getString((customSocialText == 1)?R.string.sharecustom_troublespot_coverage:R.string.sharemessage_troublespot_coverage);
					subject = getString((customSocialText == 1)?R.string.sharecustomsubject_troublespot_coverage:R.string.sharemessagesubject_troublespot_coverage);
				}
				TaskHelper.execute(
						new ShareTask(TroubleSpot.this, message, subject,
						findViewById(R.id.troublespot_container)));
			}
		}, 500);
	}
	
	public void submitBtnClicked(View btn){
		// hide trouble type and trouble impact fragments
		View fContainer = findViewById(R.id.fragment_container);
		fContainer.setVisibility(View.GONE);
		// submit issue
		submit();
	}
	
	public void temporarilyDisableButton(final View button) {
		if (button == null)
			return;
		
		button.setEnabled(false);
		
		new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(2000);  
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                TroubleSpot.this.runOnUiThread(new Runnable() {

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
				mCommentText.toString(), (GeoPoint) mMapView.getMapCenter());
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

	public boolean typeSelected = false;

	@Override
	public void onTroubleTypeSelected(String selectedType) {
//		Toast.makeText(this, isPhone + "Trouble Type selected: " + selectedType, Toast.LENGTH_SHORT).show();
		if(selectedType.equals(getString(R.string.troublespot_droppedcall))) {
			mSelectedIssueType = EventType.TT_DROP;
		}
		else if(selectedType.equals(getString(R.string.troublespot_failedcall))) {
			mSelectedIssueType = EventType.TT_FAIL;
		}
		else if(selectedType.equals(getString(R.string.troublespot_datasesion))) {
			mSelectedIssueType = EventType.TT_DATA;
		}
		else if(selectedType.equals(getString(R.string.troublespot_coverage))) {
			mSelectedIssueType = EventType.TT_NO_SVC;
		}
		showIssueIcon(mSelectedIssueType.getTroubleImageResource());
		mMapView.setClickable(false);
		typeSelected = true;

		if(isPortrait){
			Fragment f = new TroubleImpactFragment();
			Bundle b = new Bundle(1);
			b.putBoolean("highlightItem", false);
			f.setArguments(b);
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, f).commit();
		}

	}
	

	@Override
	public void onTroubleImpactSelected(String selectedImpact) {
		// Toast.makeText(this, "Trouble Impact selected: " + selectedImpact, Toast.LENGTH_SHORT).show();
		if(selectedImpact.equals(getString(R.string.troublespot_impactlow))) {
			mImpactLevel = 1;
			mCommentText = getString(R.string.troublespot_comment_impactlow);
		}
		else if(selectedImpact.equals(getString(R.string.troublespot_impactmedium))) {
			mImpactLevel = 2;
			mCommentText = getString(R.string.troublespot_comment_impactmedium);
		}
		else if(selectedImpact.equals(getString(R.string.troublespot_impacthigh))) {
			mImpactLevel = 3;
			mCommentText = getString(R.string.troublespot_comment_impacthigh);
		}
		if(isPortrait){
			findViewById(R.id.fragment_container).setVisibility(View.GONE);
			submit();
			return;
		}
		Button b = (Button) findViewById(R.id.submitButton);
		b.setEnabled(true);
		b.setBackgroundResource(R.drawable.start_button_selector);
	}
}
