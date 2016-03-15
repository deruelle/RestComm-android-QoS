package com.cortxt.app.MMC.Activities;

import java.io.File;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.FragmentSupport.SlidingAdapter;
import com.cortxt.app.MMC.Activities.MyCoverage.MyCoverageActivityGroup;
import com.cortxt.app.MMC.Activities.TroubleSpot.TroubleSpot;
import com.cortxt.app.MMC.ActivitiesOld.DeveloperScreenOld;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.NerdScreen;
import com.cortxt.app.MMC.ActivitiesOld.WebsiteLink;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.CirclePageIndicator;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.DropDownMenuWindow;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Odometer;
import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.Sampling.Building.ManualMapping;
import com.cortxt.app.MMC.Sampling.Transit.TransitSamplingMain;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCDevice;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MMCSystemUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.app.MMC.UtilsOld.CommonIntentBundleKeysOld;
import com.cortxt.app.MMC.UtilsOld.DeviceInfoOld;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.securepreferences.SecurePreferences;

@SuppressLint("CutPasteId")
public class Dashboard extends MMCTrackedActivityOld {

	private TextView mDidYouKnow;
	private LinearLayout didyouknow;
	public static final String TAG = Dashboard.class.getSimpleName();
	private JSONObject mDidYouKnowFact;
	private AsyncTask<Void, Void, JSONObject> mDidYouKnowTask;
	private Odometer mOdometer;
	private TextView mDidYouKnowTitle;
	private boolean bStoppedService;
	private ImageButton menuButton = null;
	private DropDownMenuWindow dashBoardMenu = null;
	public long down; // 3 second down touch on settings enables dev screen
	private Context context;
	private View rootview;
	private BB10PromptTimerTask bb10PromptTimerTask;
	private Timer bb10PromptTimer;
	private Handler handler;
	private TextView engineeringText;
	private Button engineeringIcon;
	private String forceDidYouKnow = null; // If we need to tell using something (BB app needs to be running)
	private static boolean askedBB10Install = false, askedQ10Hidebar = false;
	public static boolean showSurvey = false, showMapping = false, onlyBaseIcons = false, showTransit = false;
	int customIcons = 0, customLabels = 0, lcolor = -1;
	private AlphaAnimation alphaDown;
	private ViewPager mViewPager = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		int customDash = (this.getResources().getInteger(R.integer.CUSTOM_DASHXML));
		int customBackground = (this.getResources().getInteger(R.integer.CUSTOM_BACKGROUND));
		int customTitleLogo = (this.getResources().getInteger(R.integer.CUSTOM_TITLELOGO));
		int customDidyouknow = (this.getResources().getInteger(R.integer.CUSTOM_DIDYOUKNOW));
		int dashAD = (this.getResources().getInteger(R.integer.DASH_ADVERTIZE));
		int dashDYK = (this.getResources().getInteger(R.integer.DASH_DIDYOUKNOW));

		String dashLabelColor = (this.getResources().getString(R.string.DASH_LABELCOLOR));
		String didyouknowColor = (this.getResources().getString(R.string.DIDYOUKNOW_COLOR));
		String didyouknowBackColor = (this.getResources().getString(R.string.DIDYOUKNOW_BACKCOLOR));
		if(!isPortrait) {
			dashLabelColor = dashLabelColor.length() > 0 ? dashLabelColor : "939597";
		}
		didyouknowColor = didyouknowColor.length() > 0 ? didyouknowColor : "";

		//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		int tbcolor = -1, ttcolor = -1;
		ttcolor = getResources().getInteger(R.integer.DIDYOUKNOW_TEXT_COLOR);
		if (ttcolor >= 0 && ttcolor <= 0xffffff) {
			ttcolor = ttcolor + 0xff000000;
		}

		View view = null;
		if (customDash == 0)
			view = inflater.inflate(R.layout.new_dashboard_layout, null, false);
		else
			view = inflater.inflate(R.layout.custom_dashboard_layout, null, false);
		
		rootview = view;
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
		MMCActivity.customizeTitleBar(this, view, R.string.dashboard_title, R.string.dashcustom_title);

		engineeringText = (TextView) view.findViewById(R.id.engineeringText);
		engineeringIcon = (Button) view.findViewById(R.id.engineeringIcon);
		didyouknow = (LinearLayout) view.findViewById(R.id.didyouknow);
		ImageView advertisement = (ImageView) view.findViewById(R.id.advertisement);
		menuButton = (ImageButton) view.findViewById(R.id.actionbarMenuIcon);
		
		findUserPermission(); //TODO: enable this later
		onlyBaseIcons = getResources().getBoolean(R.bool.BASE_ICONS_ONLY);

		String permission = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.USER_PERMISSION, "");

		customIcons = this.getResources().getInteger(R.integer.CUSTOM_DASHICONS);
		customLabels = this.getResources().getInteger(R.integer.CUSTOM_DASHLABELS);
		lcolor = -1;

		String ff = MmcConstants.font_Regular;
		if(!isPortrait) { 
			// apply landscape-specific font
			ff = MmcConstants.font_MEDIUM; 
		}
		TextView dashBoardHeading = (TextView) findViewById(R.id.headerText);
		if (dashBoardHeading != null)
			FontsUtil.applyFontToTextView(ff, dashBoardHeading, this);

		handleIcons();
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "onCreated", "Android Platform: " + MMCService.getPlatform() + " Build:" + Build.VERSION.SDK_INT);

//		if (MMCService.getPlatform() != 3) {  // rooted androids, not BB10
//			final String pname = getPackageName();
//			boolean dontAskForRoot = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_ROOT_ACCESS, false);
//			boolean permissionForRoot = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.Miscellaneous.ROOT_ACCESS_SWITCH, false);
//			int askForRoot = this.getResources().getInteger(R.integer.ASKFOR_SUPERUSER);
//            int permissionForReadLogs = getPackageManager().checkPermission(android.Manifest.permission.READ_LOGS, pname); // 0 means allowed
//			int permissionForPrecise = getPackageManager().checkPermission("android.permission.READ_PRECISE_PHONE_STATE", pname); // 0 means allowed
//            //permissionForReadLogs =0;
//			if (android.os.Build.VERSION.SDK_INT >= 16 && permissionForReadLogs != 0 && permissionForPrecise != 0)
//			{
//				Intent intent = new Intent();
//				intent.setPackage("com.cortxt.grantlg");
//				intent.setAction("com.cortxt.grantlg.GrantListener");
//				intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
//				sendBroadcast(intent);
//			}																												// means
//
//		}
		
		bStoppedService = !isMMCServiceRunning();
		mDidYouKnow = (TextView) findViewById(R.id.dashboard_TextViewDidYouKnow);
		mOdometer = (Odometer) findViewById(R.id.odometer);
		mDidYouKnowTitle = (TextView) findViewById(R.id.dashboard_titleDidYouKnow);

		int dcolor = -1, dbcolor = -1;
		if (dashLabelColor.length() > 1)
			lcolor = Integer.parseInt(dashLabelColor, 16) + (0xff000000);
		if (didyouknowColor.length() > 1)
			dcolor = Integer.parseInt(didyouknowColor, 16) + (0xff000000);
		if (didyouknowBackColor.length() > 1) {
			dbcolor = Integer.parseInt(didyouknowBackColor, 16) + (0xff000000);
		} else {
			// if no color specified, make bg transparent
			dbcolor = 0x00000000;
		}

		if (dashDYK == 1)
		{
			TextView didyouknowText = (TextView) view.findViewById(R.id.dashboard_TextViewDidYouKnow);
			TextView didyouknowTitle = (TextView) view.findViewById(R.id.dashboard_titleDidYouKnow);
			LinearLayout didyouknowBack = (LinearLayout) view.findViewById(R.id.didyouknow);

			FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, didyouknowTitle, this);
			FontsUtil.applyFontToTextView(MmcConstants.font_Light, didyouknowText, this);

			if (didyouknowBack != null && dbcolor != -1) {
				didyouknowBack.setBackgroundColor(dbcolor);
			}

			if (didyouknowText != null) {
				if (ttcolor != -1)
					didyouknowText.setTextColor(ttcolor);
			}
			if (didyouknowTitle != null) {
				if (dcolor != -1)
					didyouknowTitle.setTextColor(dcolor);
				if (customDidyouknow == 1)
					didyouknowTitle.setText(R.string.dashcustom_didyouknow);
			}
		}
		else if(isPortrait && mViewPager != null) {
			didyouknow.setVisibility(View.GONE);			
			MarginLayoutParams layoutParams = (MarginLayoutParams) mViewPager.getLayoutParams();
			layoutParams.bottomMargin = layoutParams.topMargin; 
			View indicator = findViewById(R.id.indicator);
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) indicator.getLayoutParams();
			lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			lp.bottomMargin = 10;
		}

		if (dashAD == 0)
			advertisement.setVisibility(View.GONE);

		Intent newintent = getIntent();
		if (newintent.hasExtra("eventId")) {
			int eventId = newintent.getIntExtra("eventId", 0);
			if (eventId != 0) {
				Intent eventintent = new Intent(this, EventDetail.class);
				eventintent.putExtra(EventDetail.EXTRA_EVENT_ID, eventId);
				startActivity(eventintent);
			}
		}	
		CheckServiceStopped(false);
	}
	
	/**
	 * Use orientation-specific icons and apply customizations.
	 */
	private void handleIcons () {
		int appVersion = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getInt(PreferenceKeys.User.VERSION, -1);
		int allow = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getInt(PreferenceKeys.Miscellaneous.ALLOW_BUILDINGS, 0);
		showMapping = allow == 1 || appVersion == 0;// MMCLogger.isDebuggable();
		int allowT = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getInt(PreferenceKeys.Miscellaneous.ALLOW_TRANSIT, 0);
		showTransit = allowT == 1 || appVersion == 0;//MMCLogger.isDebuggable();
        int surveyid = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getInt("surveyid", 0);
        showSurvey = (surveyid > 0);

		mViewPager = (ViewPager) findViewById(R.id.viewPager);

        try {
            if (!isPortrait || mViewPager == null) {
                // its landscape
                Button mapsIcon = (Button) findViewById(R.id.mapIcon);
                Button speedIcon = (Button) findViewById(R.id.speedIcon);
                Button compareStatsIcon = (Button) findViewById(R.id.CompareStatsIcon);
                Button troubleTweetIcon = (Button) findViewById(R.id.TroubleTweetIcon);

                TextView mapsText = (TextView) findViewById(R.id.mapText);
                TextView speedText = (TextView) findViewById(R.id.SpeedText);
                TextView compareText = (TextView) findViewById(R.id.CompareText);
                TextView troubleTweetText = (TextView) findViewById(R.id.TroubleTweetText);

				int hideCompare = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.HIDE_COMPARE, 0);
				int hideMap = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.HIDE_MAP, 0);

				customizeTabletIcon(mapsIcon, mapsText, getResources().getInteger(R.integer.DASH_MAPS) == 0 || hideMap == 1, R.drawable.dashcustom_mycoverage, R.string.dashcustom_maps);
                customizeTabletIcon(speedIcon, speedText, getResources().getInteger(R.integer.DASH_SPEED) == 0, R.drawable.dashcustom_speedtest, R.string.dashcustom_speed);
                customizeTabletIcon(compareStatsIcon, compareText, getResources().getInteger(R.integer.DASH_COMPARE) == 0 || hideCompare == 1, R.drawable.dashcustom_mystats, R.string.dashcustom_compare);
                customizeTabletIcon(troubleTweetIcon, troubleTweetText, getResources().getInteger(R.integer.DASH_TROUBLETWEET) == 0, R.drawable.dashcustom_troublespot, R.string.dashcustom_trouble);

                if (onlyBaseIcons) {
                    // hide all dashboard icons except the 4 base icons
                    hideView(findViewById(R.id.samplingIconContainer));
                    hideView(findViewById(R.id.settingsIconContainer));
                    hideView(findViewById(R.id.surveyIconContainer));
                    hideView(findViewById(R.id.rawDataIconContainer));
                    hideView(findViewById(R.id.engineeringIconContainer));
                    return;
                } else {
                    // customize settings icon, rawData icon, engineering icon
                    Button settingsIcon = (Button) findViewById(R.id.SettingsIcon);
                    Button rawDataIcon = (Button) findViewById(R.id.RawDataIcon);
                    Button engineeringIcon = (Button) findViewById(R.id.engineeringIcon);

                    TextView settingsText = (TextView) findViewById(R.id.SettingsText);
                    TextView rawDataText = (TextView) findViewById(R.id.RawDataText);
                    TextView engineeringText = (TextView) findViewById(R.id.engineeringText);

                    customizeTabletIcon(settingsIcon, settingsText, getResources().getInteger(R.integer.DASH_SETTINGS) == 0, R.drawable.dashcustom_settings, R.string.dashcustom_settings);
                    customizeTabletIcon(rawDataIcon, rawDataText, getResources().getInteger(R.integer.DASH_RAWDATA) == 0, R.drawable.dashcustom_rawdata, R.string.dashcustom_rawdata);
                    customizeTabletIcon(engineeringIcon, engineeringText, getResources().getInteger(R.integer.DASH_ENGINEER) == 0, R.drawable.dashcustom_engineering, R.string.dashcustom_engineer);

                    if (!showSurvey) {
                        // Survey icon and text are in its own container (parent), hide that parent view to hide icon and text
                        hideView(findViewById(R.id.surveyIconContainer));
                    } else {
                        // customize surveys icon
                        Button surveyIcon = (Button) findViewById(R.id.SurveysIcon);
                        TextView surveyText = (TextView) findViewById(R.id.SurveysText);
                        customizeTabletIcon(surveyIcon, surveyText, getResources().getInteger(R.integer.DASH_SURVEYS) == 0, R.drawable.dashcustom_surveys, R.string.dashcustom_surveys);
                    }
                    if (!showMapping) {
                        // Sampling icon and text are in its own container (parent), hide that parent view to hide icon and text
                        hideView(findViewById(R.id.samplingIconContainer));
                    } else {
                        // customize sampling icon
                        Button samplingIcon = (Button) findViewById(R.id.mappingIcon);
                        TextView samplingText = (TextView) findViewById(R.id.mappingText);
                        customizeTabletIcon(samplingIcon, samplingText, getResources().getInteger(R.integer.DASH_SAMPLING) == 0, R.drawable.dashcustom_sampling, R.string.dashcustom_sampling);
                    }
                    // Tablet needs Transit icons
                    //				if(!showTransit) {
                    //					// Sampling icon and text are in its own container (parent), hide that parent view to hide icon and text
                    //					hideView(findViewById(R.id.transitIconContainer));
                    //				} else {
                    //					// customize sampling icon
                    //					Button transitIcon = (Button) findViewById(R.id.transitIcon);
                    //					TextView transitText = (TextView) findViewById(R.id.transitText);
                    //					customizeTabletIcon(transitIcon, transitText, getResources().getInteger(R.integer.DASH_TRANSIT) == 0, R.drawable.dashcustom_transit, R.string.dashcustom_transit);
                    //				}
                }
            } else {
                // Child views in mViewPager will handle their icons for portrait orientation
                mViewPager.removeAllViewsInLayout();
                mViewPager.setClipToPadding(false);
                //			mViewPager.setPageMargin((int) (10 * getResources().getDisplayMetrics().density));
                mViewPager.setOffscreenPageLimit(2);
                SlidingAdapter adapter = new SlidingAdapter(getSupportFragmentManager());
                adapter.setFullWidth(true);
                mViewPager.setAdapter(adapter);

                CirclePageIndicator pageIndicator = (CirclePageIndicator) findViewById(R.id.indicator);
                pageIndicator.setViewPager(mViewPager);
                pageIndicator.setSnap(true);

				int optionalIcons = 0;
                optionalIcons += showMapping ? 1 : 0;
                optionalIcons += showTransit ? 1 : 0;
                optionalIcons += showSurvey ? 1 : 0;
                // its portrait and we are using view flipper with 4 icons per view
                if (onlyBaseIcons) {
                    // show only base icons
                    // remove all pages except page 1
                    adapter.removeThirdScreen();
                    adapter.removeSecondScreen();
                    adapter.setFullWidth(true);
                    mViewPager.setPageMargin(0);
                    pageIndicator.setVisibility(View.GONE);
				} else if (optionalIcons <= 1 && showTransit == false) {
                    adapter.removeThirdScreen();
                }
                adapter.notifyDataSetChanged();
            }
        }
        catch (Exception e)
        {}

	}

	private void hideView(View v) {
		if(v == null)
			return;
		v.setVisibility(View.GONE);
	}
	/**
	 * Hides the icon (along with text), if disabled via feature toggle. Applies custom icon, text and/or text color according, if applicable
	 *
	 * @param icon
	 * @param text
	 * @param hide whether to hide this icon
	 * @param customIconId resource id for custom icon to use
	 * @param customTextId resource text for custom icon to use
	 */
	private void customizeTabletIcon(Button icon, TextView text, boolean hide, int customIconId, int customTextId) {
		if(icon == null) {
			return;
		}
		// hide the icon if disabled via feature toggle
		if (hide) {
			icon.setVisibility(View.GONE);
			if (text != null)
				text.setVisibility(View.GONE);
			if(icon.getParent() != null) {
				hideView((View) icon.getParent());
			}
			return;
		}
		// apply custom icon, text and/or color if selected via feature toggle
		if (customIcons == 1)
			icon.setBackgroundResource(customIconId);
		if (customLabels == 1 && text != null)
			text.setText(getString(customTextId));
		if (lcolor != -1)
			text.setTextColor(lcolor);

		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, text, this);
	}

	/*
	 *  Delay the prompts for BB10 to make sure the screen size is known
	 */
	class BB10PromptTimerTask extends TimerTask {
		EventOld thisevent, complimentaryEvent;
		public BB10PromptTimerTask ()
		{
		}
		@Override
		public void run() {
			
			handler.post(new Runnable() {
				@Override
				public void run(){
					
					int bbInstalled = isBBInstalled ();
					if (Build.VERSION.SDK_INT >= 18)
						bbInstalled = 1;
					if (bbInstalled == 0) // if BB MMC stub hasnt written a file yet, it may not be installed
					{
						boolean dontAskForBB10 = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_BB10, false);
						if (!dontAskForBB10 && !askedBB10Install) {
							askedBB10Install = true;
							LinearLayout aview = new LinearLayout(Dashboard.this);
							aview.setOrientation(LinearLayout.VERTICAL);
							aview.setPadding(5, 0, 5, 0);
							TextView messageView = new TextView(Dashboard.this);
							messageView.setMovementMethod(LinkMovementMethod.getInstance());
							String title = getString (R.string.BB10_Install);
							Spanned messagehtml = Html.fromHtml(getString (R.string.BB10_Install_Message));
							String appname = getString(R.string.app_label);
							if (!appname.equals("MyMobileCoverage")) {
								title = title.replaceAll("MyMobileCoverage", appname);
							//	message = message.replaceAll("MyMobileCoverage", appname);
							}
							//Spanned messagehtml = Html.fromHtml(message);
							messageView.setText(messagehtml);
							messageView.setTextSize(16);
							aview.addView(messageView);
							CheckBox dontAskAgain = new CheckBox(Dashboard.this);
							dontAskAgain.setText(R.string.GenericText_NoAskAgain);
							dontAskAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_BB10, isChecked).commit();
								}
							});
							aview.addView(dontAskAgain);
							
							new AlertDialog.Builder(Dashboard.this).setTitle(title).setView(aview).setNeutralButton(getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							}).show();
						}
					}
					else if (bbInstalled == 2)  // BB App is installed but not running
					{
						LinearLayout didyouknow = (LinearLayout) Dashboard.this.rootview.findViewById(R.id.didyouknow);
						TextView didyouknowTitle = (TextView) Dashboard.this.rootview.findViewById(R.id.dashboard_titleDidYouKnow);
						TextView didyouknowText = (TextView) Dashboard.this.rootview.findViewById(R.id.dashboard_TextViewDidYouKnow);
						
						forceDidYouKnow = Dashboard.this.getString(R.string.dashboard_run_blackberry);
						mOdometer.setVisibility(View.GONE);
						didyouknow.setVisibility(View.VISIBLE);
						didyouknowTitle.setText(Dashboard.this.getString(R.string.dashboard_please_note));
						didyouknowText.setText(forceDidYouKnow);
					}
					Display display = Dashboard.this.getWindowManager().getDefaultDisplay();
					int width = display.getWidth();
					//int height = display.getHeight();
					//DisplayMetrics dm = getResources().getDisplayMetrics();
					View dashBackground = (View) Dashboard.this.rootview.findViewById(R.id.dashBackground);
					int height = Dashboard.this.rootview.getBottom();
					MarginLayoutParams linearParams = (MarginLayoutParams) dashBackground.getLayoutParams();
					
					
					// Detect running on Blackberry Q10 with bottom bar visible
					//if (height == 620 && width == 720)
					if (height < width*9/10)
					{
						boolean dontAskForQ10 = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_Q10, false);
						if (!dontAskForQ10 && !askedQ10Hidebar) {
							askedQ10Hidebar = true;
							LinearLayout aview = new LinearLayout(Dashboard.this);
							aview.setOrientation(LinearLayout.VERTICAL);
							aview.setPadding(5, 0, 5, 0);
							TextView messageView = new TextView(Dashboard.this);
							messageView.setText(R.string.Q10_Bar_Message);
							messageView.setTextSize(16);
							aview.addView(messageView);
							CheckBox dontAskAgain = new CheckBox(Dashboard.this);
							dontAskAgain.setText(R.string.GenericText_NoAskAgain);
							dontAskAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_Q10, isChecked).commit();
								}
							});
							aview.addView(dontAskAgain);
							
							new AlertDialog.Builder(Dashboard.this).setTitle(R.string.Q10_Bar).setView(aview).setNeutralButton(getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							}).show();
						}
					}
				}});
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mOdometer.stop();
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		super.finish();
		ScalingUtility.invalidate();
	}

	@Override
	protected void onResume() {
		super.onResume();

		handler = new Handler();
/*		handler.postDelayed( 
		new Runnable() {
		    public void run() {
		    	checkDYKFits();
		    }
		}, 500);
*/		
		if(android.os.Build.BRAND.toLowerCase().contains("blackberry"))
		{
			bb10PromptTimer = new Timer ();
			bb10PromptTimerTask = new BB10PromptTimerTask();
			bb10PromptTimer.schedule(bb10PromptTimerTask, 1000);	
		}
        // Check device for Play Services APK.
        //ReportManager reportManager = ReportManager.getInstance(getApplicationContext());
        //reportManager.checkPlayServices(this, true);

        boolean previousValue = showSurvey;
        showSurvey = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getBoolean(PreferenceKeys.Miscellaneous.SURVEY_COMMAND, false);
        //if(previousValue != showSurvey)
        handleIcons();

		if (this.getResources().getInteger(R.integer.DASH_DIDYOUKNOW) == 0 || mDidYouKnow == null)
		{
			didyouknow.setVisibility(View.GONE);
			return;
		}
		//didyouknow.setVisibility(View.VISIBLE);
		try {
			TextView didyouknowTitle = (TextView) Dashboard.this.rootview.findViewById(R.id.dashboard_titleDidYouKnow);
			didyouknowTitle.setText(R.string.dashboard_didyouknow);
			if (mDidYouKnowFact == null || mDidYouKnowFact.getString("tag").indexOf("counter") > 0) {
				mOdometer.setVisibility(View.GONE);
				String[] didYouKnowMessages = getResources().getStringArray(R.array.dashboard_didyouknow_messages);
				int length = didYouKnowMessages.length;
				int index = new Random().nextInt(length - 1);
				String dykText = didYouKnowMessages[index];
				String appname = getString(R.string.app_label);
				if (!appname.equals("MyMobileCoverage")) {
					dykText = dykText.replaceAll("MyMobileCoverage", appname);
				}
				mDidYouKnow.setText(dykText);
			} else
				displayFact(false);
		} catch (Exception e1) {
		}

		if (mDidYouKnowTask == null || mDidYouKnowTask.getStatus() != AsyncTask.Status.RUNNING) {
			/**
			 * This task gets the a random fact/statistic from the server
			 */
			mDidYouKnowTask = new AsyncTask<Void, Void, JSONObject>() {
				@Override
				protected JSONObject doInBackground(Void... params) {
					try {
						return ReportManager.getInstance(getApplicationContext()).getDidYouKnow();
					} catch (MMCException e) {
						//MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "GetCarrierLogoTask", "exeption", e);
						return null;
					}
				}

				@Override
				protected void onPostExecute(JSONObject result) {
					if (result != null && forceDidYouKnow == null) {
						mDidYouKnowFact = result;
						TextView didyouknowTitle = (TextView) Dashboard.this.rootview.findViewById(R.id.dashboard_titleDidYouKnow);
						didyouknowTitle.setVisibility(View.VISIBLE);
						displayFact(true);
						/*
						handler.postDelayed( 
								new Runnable() {
								    public void run() {
								    	checkDYKFits();}}, 500);
						 */
					}
				}
			};
			TaskHelper.execute(mDidYouKnowTask);
		}


	}


	private void displayFact(boolean bLive) {
		try {
			if (mDidYouKnowFact != null) {
				String tag = mDidYouKnowFact.getString("tag");
				String counters = mDidYouKnowFact.getString("counters");
				String fact = mDidYouKnowFact.getString("fact");
				if (counters != null && counters.length() > 1) {
					String[] vals = counters.split(" ");
					fact = fact.replace("%d", vals[0]);

					long value1 = 0;
					long value0 = Long.parseLong(vals[0]);
					if (vals.length > 1 && bLive)
						value1 = Long.parseLong(vals[1]);
					if (tag.equals("coveragekm counter")) {
						value0 = value0 / 50;
						value1 = value1 / 50;
					}
					long value2 = value0 * 2 - value1; // predicted value at the
														// end of the day
					int digits = (int) Math.min(10, Long.toString(value2).length());
					mOdometer.setDigits(digits);
					mOdometer.setVisibility(View.VISIBLE);
					mOdometer.setValue(value0, value1);
				} else
					mOdometer.setVisibility(View.GONE);
				
				String appname = getString(R.string.app_label);
				if (!appname.equals("MyMobileCoverage")) {
					fact = fact.replaceAll("MyMobileCoverage", appname);
				}
				
				mDidYouKnow.setText(fact);
			} else
				mOdometer.setVisibility(View.GONE);
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "displayFact", "error in displayFact ", e);
			mOdometer.setVisibility(View.GONE);
		}
	}

	@Override
	public void onNewIntent(Intent newintent) {
		if (newintent.hasExtra("eventId")) {
			int eventId = newintent.getIntExtra("eventId", 0);
			if (eventId != 0) {
				Intent eventintent = new Intent(this, EventDetail.class);
				eventintent.putExtra(EventDetail.EXTRA_EVENT_ID, eventId);
				startActivity(eventintent);
			}
		}
	}

	protected void setTitle(int whiteText1, int blueText, int whiteText2) {
		String whiteText1String = getString(whiteText1);
		String blueTextString = getString(blueText);
		String whiteText2String = getString(whiteText2);

		if (bStoppedService)
			whiteText2String += " " + getString(R.string.GenericText_Stopped);

		SpannableString text = new SpannableString(whiteText1String + blueTextString + whiteText2String);

		int white = getResources().getColor(R.color.white);
		int blue = getResources().getColor(R.color.title_blue);

		text.setSpan(new ForegroundColorSpan(white), 0, whiteText1String.length(), 0);
		text.setSpan(new ForegroundColorSpan(blue), whiteText1String.length(), whiteText1String.length() + blueTextString.length(), 0);
		text.setSpan(new ForegroundColorSpan(white), whiteText1String.length() + blueTextString.length(), text.length(), 0);

		// mTitle.setText(text);
	}

	public void nerdModeClicked(View button) {
		if (CheckServiceStopped(true))
			return;
		Intent intent = new Intent(this, NerdScreen.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		
		//intent = new Intent();
		//intent.setPackage("com.cortxt.grantlg");
		//intent.setAction("com.cortxt.grantlg.GrantListener");
		//sendBroadcast(intent);
	}

	public void myCoverageClicked(View button) {
		// Intent intent = new Intent(this, MapActivityGroupOld.class);
		Intent intent = new Intent(this, MyCoverageActivityGroup.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra("mapState", _savedMapState);
		startActivityForResult(intent, 1);
	}
	
	public void myMappingClicked(View button) {
		Intent intent = new Intent(Dashboard.this, ManualMapping.class);
		intent.putExtra("type", MmcConstants.MANUAL_MENU);
		startActivity(intent);
	}
	
	public void myTransitClicked(View button) {
		Intent intent = new Intent(Dashboard.this, TransitSamplingMain.class);
		startActivity(intent);
	}
	
	public void myEngineeringClicked(View button) {
		nerdModeClicked(null);
	}

	private Bundle _savedMapState;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (data == null)
			return;
		Bundle mapState = data.getBundleExtra("mapState");
		_savedMapState = mapState;
	}

	public void myStatsClicked(View button) {
		Integer newCompare = this.getResources().getInteger(R.integer.NEW_COMPAREVIEWS);
		Intent intent;
		if (newCompare == 1)
			intent = new Intent(this, CompareNew.class);
		else
			intent = new Intent(this, Compare.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void troubleSpotClicked(View button) {
		if (CheckServiceStopped (true))
			return; 
		Intent intent = new Intent(this, TroubleSpot.class);

		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}

	public void speedTestClicked(View button) {
		Intent intent = new Intent(this, SpeedTest.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
	
	public void rawDataClicked(View button){
		if (CheckServiceStopped(true))
			return;
		sendEmail();
		trackEvent("EmailCsv", "send", "", 0);
	}
	
	public void surveysClicked(View button){
		Intent intent = new Intent(Dashboard.this, SatisfactionSurvey.class);
		startActivity(intent);
	}
	
	public void settingsClicked(View button){
		Intent startSettingsIntent = new Intent(Dashboard.this, Settings.class);
		startActivity(startSettingsIntent);
		trackEvent("Settings", "start", "", 0);
	}

	public void actionBarbackClik(View view) {
		// closeApplication();
	}

	public void MenuButtonClickListener(View menuButton) {
		if (DropDownMenuWindow.isWindowAlreadyShowing && dashBoardMenu != null) {
			dashBoardMenu.dismissWindow();
			return;
		}
		long currentTime = System.currentTimeMillis();
		if (currentTime - DropDownMenuWindow.lastWindowDismissedTime > 200) {
			LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View menuOptionsView = inflater.inflate(R.layout.dashboard_menu_options, null, false);
			ScalingUtility.getInstance(this).scaleView(menuOptionsView, 0.9f);

			final TextView engineeringOption = (TextView) menuOptionsView.findViewById(R.id.EngineeringModeOption);
			final View seperator = (View) menuOptionsView.findViewById(R.id.Separator);
			TextView settingsOption = (TextView) menuOptionsView.findViewById(R.id.SettingsOption);
			TextView signOutOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_signout);
            TextView toggleServiceOption = (TextView) menuOptionsView.findViewById(R.id.ToggleserviceOption);
            TextView logOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_log);
			TextView emailCsvOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_Email_Csv);
			TextView helpOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_help);
			TextView menuitem = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_feedback);
			TextView feedbackOption = (TextView) menuitem;
			TextView aboutOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_about);
			TextView surveyOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_survey);
			TextView mappingOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_sampling);
			//TextView transitOption = (TextView) menuOptionsView.findViewById(R.id.dashboard_menu_transit);
			// TextView feedbackOption=(TextView)menuOptionsView.findViewById(R.id.das);

			TextView transitLog = (TextView) menuOptionsView.findViewById(R.id.dashboard_transit_log);
			View transitSeperator = (View) menuOptionsView.findViewById(R.id.tenthSeparator);
/*
  			boolean showSurvey = PreferenceManager.getDefaultSharedPreferences(Dashboard.this).getBoolean(PreferenceKeys.Miscellaneous.SURVEY_COMMAND, false);
			if(!showSurvey && surveyOption != null) {
				surveyOption.setVisibility(View.GONE);
				eighthSeparator.setVisibility(View.GONE);
			}
			else if(surveyOption != null) {
				surveyOption.setVisibility(View.VISIBLE);
				eighthSeparator.setVisibility(View.VISIBLE);
			}
			
			int height = ScalingUtility.getInstance(this).getCurrentHeight();
			int width = ScalingUtility.getInstance(this).getCurrentWidth();
			
//			new Runnable() {
//			    public void run() {
//			    	if(!checkDYKFits()) {
//			    		engineeringOption.setVisibility(View.VISIBLE);
//						seperator.setVisibility(View.VISIBLE);
//			    	}
//			    }
//			};
			if (height <= width && engineeringOption != null || engineeringText.getLineCount() > 1) {
				engineeringOption.setVisibility(View.VISIBLE);
				seperator.setVisibility(View.VISIBLE);
			}
*/			

			boolean notOnlyBaseIcons = !onlyBaseIcons;
			if(notOnlyBaseIcons && findViewById(R.id.SettingsIcon) != null && findViewById(R.id.SettingsIcon).getVisibility() == View.VISIBLE) {
				// SettingsIcon is not on Dashboard. Show it in options menu
				settingsOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.secondSeparator).setVisibility(View.GONE);
			}
			if(notOnlyBaseIcons && findViewById(R.id.engineeringIcon) != null && findViewById(R.id.engineeringIcon).getVisibility() == View.VISIBLE) {
				// SettingsIcon is not on Dashboard. Show it in options menu
				engineeringOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.Separator).setVisibility(View.GONE);
			}
			if(notOnlyBaseIcons && findViewById(R.id.RawDataIcon) != null && findViewById(R.id.RawDataIcon).getVisibility() == View.VISIBLE) {
				// RawDataIcon is not visible on Dashboard. Show it in options menu
				emailCsvOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.sixthSeparator).setVisibility(View.GONE);
			}

			if(!showSurvey || (notOnlyBaseIcons && findViewById(R.id.SurveysIcon) != null && findViewById(R.id.SurveysIcon).getVisibility() == View.VISIBLE)) {
				// SurveysIcon is not on Dashboard. Show it in options menu
				surveyOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.eighthSeparator).setVisibility(View.GONE);
			}

            String signoutText = getString(R.string.GenericText_ClearLogin);
            if (signoutText.equals(""))
            {
                View second = (View) menuOptionsView.findViewById(R.id.secondSeparator);
                second.setVisibility(View.GONE);
                signOutOption.setVisibility(View.GONE);
            }

			boolean mappingIconVisible = false;
			mappingIconVisible = notOnlyBaseIcons && findViewById(R.id.mappingIcon) != null && findViewById(R.id.mappingIcon).getVisibility() == View.VISIBLE;
			View mappingIcon3 = findViewById(R.id.mappingIconOn3rd);
			if(mappingIcon3 != null) {
				mappingIconVisible = mappingIconVisible || mappingIcon3.getVisibility() == View.VISIBLE;
			}
			if(!showMapping || (notOnlyBaseIcons && mappingIconVisible)) {
				// mappingIcon is not on Dashboard. Show it in options menu
				mappingOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.samplingSeparator).setVisibility(View.GONE);
			}
			
			boolean transitIconVisible = false;
			transitIconVisible = notOnlyBaseIcons && findViewById(R.id.transitIcon) != null && findViewById(R.id.transitIcon).getVisibility() == View.VISIBLE;
			View transitIcon3 = findViewById(R.id.transitIconOn3rd);
			if(transitIcon3 != null) {
				transitIconVisible = transitIconVisible || transitIcon3.getVisibility() == View.VISIBLE;
			}

			String feedbackurl = getResources().getString(R.string.dashboard_feedback_url);
			if (feedbackurl != null && feedbackurl.length() > 3) {
				feedbackOption.setOnClickListener(menuOptionItemClickListener);
			} else {
				feedbackOption.setVisibility(View.GONE);
				View seventhSep = menuOptionsView.findViewById(R.id.seventhSeparator);
				if(seventhSep != null)
					seventhSep.setVisibility(View.GONE);
			}
			toggleServiceOption.setOnClickListener(menuOptionItemClickListener);
			logOption.setOnClickListener(menuOptionItemClickListener);
			helpOption.setOnClickListener(menuOptionItemClickListener);
			aboutOption.setOnClickListener(menuOptionItemClickListener);
			if(engineeringOption != null)
				engineeringOption.setOnClickListener(menuOptionItemClickListener);
			if(settingsOption != null)
				settingsOption.setOnClickListener(menuOptionItemClickListener);

			//toggleServiceOption.setOnClickListener(menuOptionItemClickListener);
			//logOption.setOnClickListener(menuOptionItemClickListener);
			transitLog.setOnClickListener(menuOptionItemClickListener);
            signOutOption.setOnClickListener(menuOptionItemClickListener);

			if(emailCsvOption != null)
				emailCsvOption.setOnClickListener(menuOptionItemClickListener);
			if(surveyOption != null)
				surveyOption.setOnClickListener(menuOptionItemClickListener);
			if(mappingOption != null)
				mappingOption.setOnClickListener(menuOptionItemClickListener);
			//if(transitOption != null)
			//	transitOption.setOnClickListener(menuOptionItemClickListener);

			String ff = MmcConstants.font_Light;
			if(!isPortrait) {
				ff = MmcConstants.font_Regular;
			}
			if(engineeringOption != null)
				FontsUtil.applyFontToTextView(ff, engineeringOption, this);
			if(settingsOption != null)
				FontsUtil.applyFontToTextView(ff, settingsOption, this);
			if(toggleServiceOption != null)
				FontsUtil.applyFontToTextView(ff, toggleServiceOption, this);
            if(signOutOption != null)
                FontsUtil.applyFontToTextView(ff, signOutOption, this);
			if(logOption != null)
				FontsUtil.applyFontToTextView(ff, logOption, this);
			if(emailCsvOption != null)
				FontsUtil.applyFontToTextView(ff, emailCsvOption, this);
			if(helpOption != null)
				FontsUtil.applyFontToTextView(ff, helpOption, this);
			if(aboutOption != null)
				FontsUtil.applyFontToTextView(ff, aboutOption, this);
			if(menuitem != null)
				FontsUtil.applyFontToTextView(ff, menuitem, this);
			if(surveyOption != null)
				FontsUtil.applyFontToTextView(ff, surveyOption, this);
			if(mappingOption != null)
				FontsUtil.applyFontToTextView(ff, mappingOption, this);
			if(transitLog != null)
				FontsUtil.applyFontToTextView(ff, transitLog, this);

			if (isMMCServiceRunning()) {
				toggleServiceOption.setText(R.string.dashboard_menu_stopmonitoring);
			} else {
				toggleServiceOption.setText(R.string.dashboard_menu_startmonitoring);

			}
			if (!MMCLogger.isDebuggable()) {
				logOption.setVisibility(View.GONE);
				transitLog.setVisibility(View.GONE);
				transitSeperator.setVisibility(View.GONE);
			}
			if (getResources().getInteger(R.integer.FEEDBACK_MENU) == 0) {
				feedbackOption.setVisibility(View.GONE);
				menuOptionsView.findViewById(R.id.seventhSeparator).setVisibility(View.GONE);
			}
			dashBoardMenu = new DropDownMenuWindow(menuOptionsView, this, MmcConstants.DASHBOARD_MENU_OFFSET, MmcConstants.GENERAL_MENU_WINDOW_WIDTH);
			dashBoardMenu.showCalculatorMenu(menuButton);

			aboutOption.setOnTouchListener(new TextView.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					String title = "";
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						down = System.currentTimeMillis();
						break;
					case MotionEvent.ACTION_UP:
						long diff = System.currentTimeMillis() - down;
						boolean showExtra = false;
						if (diff >= 3000) {
							showExtra = true;
							title = getString (R.string.dashboard_about);;
						} else {
							showExtra = false;
							title = getString (R.string.dashboard_about); //"About";
						}
						dashBoardMenu.dismissWindow();

						try {
							AlertDialog.Builder builder1 = new AlertDialog.Builder(Dashboard.this);
                            String devInfo = getDevInfo(showExtra);
                            int customPrivacy = (getResources().getInteger(R.integer.CUSTOM_PRIVACY));
                            if (customPrivacy == 1)
                                devInfo += getString(R.string.getstarted_custom_policy_links);
                            else
                                devInfo += getString (R.string.getstarted_policy_links);
                            devInfo = devInfo.replace ("\n", "<br>");
                            builder1.setMessage(Html.fromHtml(devInfo));
							builder1.setTitle(title);
							builder1.setCancelable(true);
							AlertDialog alert11 = builder1.create();
							alert11.show();
                            // Make the textview clickable. Must be called after show()
                            ((TextView)alert11.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
						} catch (Exception e) {
							MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "CreateDevInfoAlertDialog", "exeption", e);
						}
						break;
					}
					return true;
				}
			});
		}
	}

	private String getDevInfo(boolean showExtra) {

        String devInfo = "";

		SharedPreferences preferenceSettings = MMCService.getSecurePreferences(this);
		String appName = getString(R.string.app_label);
		String username = preferenceSettings.getString(PreferenceKeys.User.USER_EMAIL, null);
		//String contactEmail = preferenceSettings.getString(PreferenceKeys.User.CONTACT_EMAIL, null);
		//if (contactEmail == null || contactEmail.equals("KEY_SETTINGS_CONTACT_EMAIL")) {
		//	contactEmail = getString (R.string.about_noemail);
		//}
		String apiKey = MMCService.getApiKey(this);
		int appVersion = preferenceSettings.getInt(PreferenceKeys.User.VERSION, -1);
		int androidVersion = DeviceInfoOld.getAndroidVersion();
		String serverURL = getApplicationContext().getString(R.string.MMC_URL_LIN);
		int userID = MMCService.getUserID(this);
		ReportManager reportManager = ReportManager.getInstance(getApplicationContext());

		String carrierName = reportManager.getCurrentCarrier() != null ? reportManager.getCurrentCarrier().Name : getString(R.string.GenericText_Unknown);
		String operatorID = reportManager.getCurrentCarrier() != null ? reportManager.getCurrentCarrier().OperatorId : getString(R.string.GenericText_Unknown);
		String infotemplate = getString(R.string.about_info);
		devInfo = String.format (infotemplate, appName, username, appVersion, androidVersion, carrierName);
		//devInfo = "Application: " + appName + "\nUsername: " + username + "\nContact email: " + contactEmail + "\nApplication version: " + appVersion + "\nAndroid version: " + androidVersion + "\nCarrier: " + carrierName;
		if (showExtra) {
			String extratemplate = getString(R.string.about_extrainfo);
			devInfo += String.format (extratemplate, operatorID, apiKey, serverURL, userID);
		}
        devInfo += "\n";

        return devInfo;
	}

	/*
	 * Check and remind user to have the MMC service running to s=do tracking or
	 * update event
	 */
	private boolean CheckServiceStopped(boolean forAction) {
		if (!isMMCServiceRunning()) {
            final SecurePreferences securePrefs = MMCService.getSecurePreferences (Dashboard.this);
            boolean stopped = securePrefs.getBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, false);
            if (!stopped)
                return false;

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setCancelable(true);
			// builder.setIcon(R.drawable.ic_mmclauncher);
			String strTitle = getString(R.string.GenericText_AskStartService);
			String strText = getString(R.string.GenericText_AskStartServiceDescription);

			// if user is performing an action (like speedtest), keep the part
			// about MMC need to be running to perform this action
			// otherwise remove it
			if (!forAction) {
				int pos = strText.indexOf('\n');
			    if (pos > 0)
			    	strText = strText.substring(pos+1);
			}
			
			String appname = getString(R.string.app_label);
			if (!appname.equals("MyMobileCoverage")) {
				strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
				strText = strText.replaceAll("MyMobileCoverage", appname);
			}
			builder.setTitle(strTitle);
			String strMessage = strText;

			builder.setMessage(strMessage);
			builder.setInverseBackgroundForced(true);
			builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// launch settings screen
					Intent bgServiceIntent = new Intent(Dashboard.this, MMCService.class);
					startService(bgServiceIntent);
					trackEvent("StartStop", "start", "SpeedTest", 0);
					dialog.dismiss();
					bStoppedService = false;
					// getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
					// R.layout.window_title_main);
					// setTitle(R.string.dashboard_title_my,
					// R.string.dashboard_title_mobile,
					// R.string.dashboard_title_coverage);
                    securePrefs.edit().putBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, bStoppedService).commit();

				}
			});
			builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			AlertDialog alert = builder.create();
			alert.show();
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy ()
	{
		
//		boolean bStoppedService = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, false);
//		if (!bStoppedService)
//		{
//		    Intent restartService = new Intent(Dashboard.this, MMCService.class);
//		    restartService.setPackage(getPackageName());
//		    PendingIntent restartServicePI = PendingIntent.getService(
//		            getApplicationContext(), 1, restartService,
//		            PendingIntent.FLAG_ONE_SHOT);
//		    AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//		    alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +2000, restartServicePI);
//		}
//		System.exit(0);
		
		super.onDestroy();
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {

			MenuButtonClickListener(menuButton);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (dashBoardMenu != null && DropDownMenuWindow.isWindowAlreadyShowing) {
				dashBoardMenu.dismissWindow();
				return true;
			
			}else
			{
                SecurePreferences securePrefs = MMCService.getSecurePreferences (Dashboard.this);
				boolean bStoppedService = securePrefs.getBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, false);
				if (!bStoppedService)
				{
					Intent intent = new Intent(MMCIntentHandlerOld.RESTART_MMC_SERVICE);
					sendBroadcast(intent);
				}
				
	//			Intent bgServiceIntent = new Intent(Dashboard.this, MMCService.class);
	//			this.stopService(bgServiceIntent);
	//			
	//		    Intent restartService = new Intent(Dashboard.this, MMCService.class);
	//		    restartService.setPackage(getPackageName());
	//		    PendingIntent restartServicePI = PendingIntent.getService(
	//		            getApplicationContext(), 1, restartService,
	//		            PendingIntent.FLAG_ONE_SHOT);
	//		    AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
	//		    alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +2000, restartServicePI);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private void closeApplication() {
		// android.os.Process.killProcess(android.os.Process.myPid());
	}

	OnClickListener menuOptionItemClickListener = new OnClickListener() {

		@Override
		public void onClick(View view) {

			if (view.getId() == R.id.SettingsOption) { // Implemented in onTouch
														// now in
														// MenuButtonClickListner()
														// line
														// 499
				Intent startSettingsIntent = new Intent(Dashboard.this, Settings.class);
				startActivity(startSettingsIntent);
				trackEvent("Settings", "start", "", 0);
			} else if (view.getId() == R.id.EngineeringModeOption) {
				if (CheckServiceStopped(true))
					return;
				nerdModeClicked(null);

			} else if (view.getId() == R.id.dashboard_menu_Email_Csv) {
				if (CheckServiceStopped(true))
					return;
				sendEmail();
				trackEvent("EmailCsv", "send", "", 0);

			} else if (view.getId() == R.id.dashboard_menu_signout) {
                stopMMCService (true, view);

            } else if (view.getId() == R.id.dashboard_menu_log) {
				Intent startDevScreenIntent = new Intent(Dashboard.this, DeveloperScreenOld.class);
				startActivity(startDevScreenIntent);
			} else if (view.getId() == R.id.dashboard_transit_log) {
				Intent startDevScreenIntent = new Intent(Dashboard.this, DeveloperScreenOld.class);
				startDevScreenIntent.putExtra("transit", true);
				startActivity(startDevScreenIntent);
			} else if (view.getId() == R.id.dashboard_menu_help) {
				Intent intent = new Intent(Dashboard.this, WebsiteLink.class);
				String link = "http://www.mymobilecoverage.com/client/android/";
				String helpFolder = (Dashboard.this.getResources().getString(R.string.HELP_FOLDER));
				if (helpFolder.length() > 1)
					link += helpFolder + "/";
				else
					link += "3.0/";
				if (MMCService.getPlatform() == 3)
					link += "bb10/";
				
				link += "gettingstarted.html";
				if (helpFolder.indexOf("http") >= 0)
					link = helpFolder;
				intent.putExtra(CommonIntentBundleKeysOld.Miscellaneous.WEB_ADDRESS, link);
				startActivity(intent);

			} else if (view.getId() == R.id.ToggleserviceOption) {
				if (isMMCServiceRunning()) {
                    stopMMCService (false, view);


				} else {
                    Intent bgServiceIntent = new Intent(Dashboard.this, MMCService.class);
					startService(bgServiceIntent);
					((TextView) view).setText(R.string.dashboard_menu_stopmonitoring);
					trackEvent("StartStop", "start", "", 0);
					bStoppedService = false;
					ImageButton btn = (ImageButton) findViewById(R.id.button_expert);
					if (btn != null)
						btn.setVisibility(View.VISIBLE);
					// TODO : set icon for stop monitoring
                    SecurePreferences securePrefs = MMCService.getSecurePreferences (Dashboard.this);
                    securePrefs.edit().putBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, bStoppedService).commit();
				}
//				PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, bStoppedService).commit();

			} else if (view.getId() == R.id.dashboard_menu_feedback) {
				String feedbackurl = getResources().getString(R.string.dashboard_feedback_url);
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(feedbackurl));
				startActivity(intent);
			}
			else if(view.getId()==R.id.dashboard_menu_survey){
				Intent intent = new Intent(Dashboard.this, SatisfactionSurvey.class);
				startActivity(intent);
			} else if(view.getId() == R.id.dashboard_menu_sampling) {
				myMappingClicked(null);
			}
			else if(view.getId()==R.id.dashboard_menu_sampling){
				Intent intent = new Intent(Dashboard.this, ManualMapping.class);
				startActivity(intent);
			}
			if(dashBoardMenu!=null){
				dashBoardMenu.dismissWindow();
			}
			return;
		}
	};

    private void stopMMCService (final boolean bSignOut, View view)
    {
        /* stopService(bgServiceIntent);
					((TextView) view).setText(R.string.dashboard_menu_startmonitoring);
					trackEvent("StartStop", "stop", "", 0);
					bStoppedService = true;
					ImageButton btn = (ImageButton) findViewById(R.id.button_expert);

					if (btn != null)
						btn.setVisibility(View.GONE); */
        AlertDialog.Builder builder = new AlertDialog.Builder(Dashboard.this);
        builder.setCancelable(true);
        // builder.setIcon(R.drawable.ic_mmclauncher);

        String strTitle = getString(R.string.GenericText_ShutDownPrompt);
        String strText = getString(R.string.GenericText_ShutDownDescription);
        if (bSignOut)
        {
            strTitle = getString(R.string.GenericText_SignOutPrompt);
            strText = getString(R.string.GenericText_SignOutDescription);
        }
        String appname = getString(R.string.app_label);
        if (!appname.equals("MyMobileCoverage")) {
            strTitle = strTitle.replaceAll("MyMobileCoverage", appname);
            strText = strText.replaceAll("MyMobileCoverage", appname);
        }
        builder.setTitle(strTitle);
        String strMessage = strText;

        final View menuview = view;
        builder.setMessage(strMessage);
        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent bgServiceIntent = new Intent(Dashboard.this, MMCService.class);
                stopService(bgServiceIntent);
                ((TextView) menuview).setText(R.string.dashboard_menu_startmonitoring);
                trackEvent("StartStop", "stop", "", 0);
                bStoppedService = true;
                ImageButton btn = (ImageButton) findViewById(R.id.button_expert);

                if (btn != null)
                    btn.setVisibility(View.GONE);
                SecurePreferences prefs = MMCService.getSecurePreferences (Dashboard.this);
                prefs.edit().putBoolean(PreferenceKeys.Miscellaneous.STOPPED_SERVICE, bStoppedService).commit();
                if (bSignOut)
                {
                    prefs.edit().putBoolean(PreferenceKeys.Miscellaneous.SIGNED_OUT, true).commit();
                    Intent intent = new Intent(Dashboard.this, GetStarted2.class);
                    startActivity(intent);
                    finish();
                }
            }

        });
        builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

	private boolean isMMCServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (MMCService.class.getCanonicalName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void sendEmail() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setCancelable(true);
		// builder.setIcon(R.drawable.nerdmode);
		String title = getString(R.string.GenericText_EmailRaw);
		String appname = getString(R.string.app_label);
		String str = getString(R.string.GenericText_EmailRawDescription);
		if (!appname.equals("MyMobileCoverage")) {
			title = title.replaceAll("MyMobileCoverage", appname);
			str = str.replaceAll("MyMobileCoverage", appname);
		}
		builder.setTitle(R.string.GenericText_EmailRaw);
        //SharedPreferences preferenceSettings = MMCService.getSecurePreferences(this);
		SharedPreferences preferenceSettings =PreferenceManager.getDefaultSharedPreferences(this);
		String strEmail = preferenceSettings.getString(PreferenceKeys.User.CONTACT_EMAIL, "no email");
		String strMessage = String.format(str, strEmail);

		builder.setMessage(strMessage);
		builder.setInverseBackgroundForced(true);
		builder.setPositiveButton(R.string.GenericText_Send, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// tell server to send email with raw data
				Intent intent = new Intent(MMCIntentHandlerOld.EMAIL_CSV);
				sendBroadcast(intent);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(R.string.GenericText_Cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	public void onStart() {
		super.onStart();
		final boolean collectPreferences = false;// PreferenceManager.getDefaultSharedPreferences(this)
		// .getBoolean(PreferenceKeys.Miscellaneous.MISC_OPT_COLLECT_USAGE_DATA, true);
		if (MMCService.getPlatform() != 3) {  // rooted androids, not BB10
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	
			boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
			boolean locEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
			if (!gpsEnabled || !locEnabled) {
				boolean dontAskForGPS = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_GPS, false);
				if (!dontAskForGPS) {
					LinearLayout view = new LinearLayout(this);
					view.setOrientation(LinearLayout.VERTICAL);
					view.setPadding(5, 0, 5, 0);
					TextView messageView = new TextView(this);
					messageView.setText(R.string.GPS_Disabled_Message);
					messageView.setTextSize(16);
					view.addView(messageView);
					CheckBox dontAskAgain = new CheckBox(this);
					dontAskAgain.setText(R.string.GenericText_NoAskAgain);
					dontAskAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
						@Override
						public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
							PreferenceManager.getDefaultSharedPreferences(Dashboard.this).edit().putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_GPS, isChecked).commit();
							
						}
					});
					view.addView(dontAskAgain);
					
					new AlertDialog.Builder(this).setTitle(R.string.GPS_Disabled).setView(view).setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						}
					}).setNeutralButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							startActivity(intent);
						}
					}).show();
				}
			}
		}

		
	}
	
	public int isBBInstalled () {
		int bbRunning = 0;
		String signalsFile = "/sdcard/BBSignals.txt";
		if (Build.VERSION.SDK_INT >= 18)
			return 0;
		
		//This method will read the shared file between Android and BB and then pass on the data to Android
		StringBuilder sb = null;
		if ((Environment.getExternalStorageState().toString()).equals(Environment.MEDIA_MOUNTED)) {	
			try{
				int bytesToRead = 4;				
					
				if(signalsFile != null) {	
					File file = new File (signalsFile);
					if (file != null && file.length() > 0)
					{
						bbRunning = 1;
						if (file.lastModified() + 300000 < System.currentTimeMillis())
						{
							bbRunning = 2;  // installed but not running
						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		return bbRunning;
	}


	public void findUserPermission() {
		final String apiKey = MMCService.getApiKey(this);
		String permission = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.USER_PERMISSION, null);
		if(apiKey != null && permission == null) {
			new Thread(new Runnable() {
				public void run() {					
					try{
						String url = getString(R.string.MMC_URL_LIN) + "/api/user?apiKey=" + apiKey;
						String responseContents = WebReporter.getHttpURLResponse(url, false);
						readResponse(responseContents);
					}
					catch(Exception e) {
						System.out.println(e);
					}
				}
			}).start();
		}
	}
	
	public void readResponse(String response) {
		JSONObject json = null;
		try {
			json = new JSONObject(response);		 
			String permission = json.optString("feat");
			PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PreferenceKeys.User.USER_PERMISSION, permission).commit();
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/*
	@SuppressLint("NewApi")
	public void centerEngineeringIcon(Button engineeringIcon) {		
		try {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int padding = size.x;
			
			padding /= 4; //half screen width
			int width = engineeringIcon.getBackground().getMinimumWidth();
			padding -= width/5;		
			
			MarginLayoutParams layoutParams = (MarginLayoutParams) engineeringIcon.getLayoutParams();
////			LayoutParams layoutParams = (LayoutParams) engineeringIcon.getLayoutParams();
			layoutParams.leftMargin = 190; 
			engineeringIcon.setLayoutParams(layoutParams);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	public void checkDYKFits() {
		int[] locDYK = new int[2];
		int[] locEngg = new int[2];
		didyouknow.getLocationOnScreen(locDYK);
		engineeringText.getLocationOnScreen(locEngg);
		TextView didyouknowTitle = (TextView) Dashboard.this.rootview.findViewById(R.id.dashboard_titleDidYouKnow);
		didyouknowTitle.setVisibility(View.VISIBLE);
		didyouknow.setVisibility(View.VISIBLE);
		double screenDensityScale = this.getResources().getDisplayMetrics().density;

		int y1 = locEngg[1] + (int)(screenDensityScale * 40);
		int y2 = locDYK[1];
		int y3 = locDYK[1] + (int)(screenDensityScale * 40);
		if(y1 + 1 > y2)
		{
			if (y1 > y3)
				didyouknow.setVisibility(View.GONE);
			else
				didyouknowTitle.setVisibility(View.GONE);
		}
	}
	*/

}
