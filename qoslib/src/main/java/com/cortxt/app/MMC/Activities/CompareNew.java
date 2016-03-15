package com.cortxt.app.MMC.Activities;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.SpannableString;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.SimpleGestureFilter;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.SimpleGestureFilter.SimpleGestureListener;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatChartNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatViewNew;
import com.cortxt.app.MMC.Exceptions.MMCException;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Reporters.ReportManager.SpeedTestKeys;
import com.cortxt.app.MMC.Reporters.ReportManager.StatsKeys;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.ServicesOld.MMCPhoneStateListenerOld;
import com.cortxt.app.MMC.ServicesOld.Location.LocationRequest;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.com.mmcextension.utils.TaskHelper;
import com.cortxt.app.MMC.UtilsOld.Carrier;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.securepreferences.SecurePreferences;

public class CompareNew extends MMCTrackedActivityOld implements SimpleGestureListener {
	public static final String TAG = CompareNew.class.getSimpleName();
	private static final String KEY_CURRENTLY_DISPLAYED_STAT = "CURRENTLY_DISPLAYED_STAT";

	public static final String ACTION_LOCATION_RESULT = "com.cortxt.app.MMC.intent.ACTION_LOCATION_RESULT";
	public static final String ACTION_LOCATION_ERROR = "com.cortxt.app.MMC.intent.ACTION_LOCATION_ERROR";
	public static final String EXTRA_LOCATION = "gpslocation";
	public static final String CALLSTATS_PAGE_URL = "file:///android_asset/mystats/callstats.html";
	public static final String MYCALLSTATS_PAGE_URL = "file:///android_asset/mystats/mycallstats.html";
	public static final String DOWNLOADSPEED_PAGE_URL = "file:///android_asset/mystats/downloadspeed.html";
	public static final String RANKING_PAGE_URL = "file:///android_asset/mystats/ranking.html";
	/**
	 * Array of urls of all stats that can be displayed.
	 */
	public static final String[] STATS_PAGES = new String[] { "ranking", "mycallstats", "callstats", "downloadspeed" };
	public static final String[] STATS_URLS = new String[] { RANKING_PAGE_URL, MYCALLSTATS_PAGE_URL, CALLSTATS_PAGE_URL, DOWNLOADSPEED_PAGE_URL };

	public static final HashMap<String, Integer> STRINGS = new HashMap<String, Integer>();
	public static final HashMap<String, Integer> CUST_STRINGS = new HashMap<String, Integer>();
	static {
		STRINGS.put("callstats", R.string.mystats_callstats);
		STRINGS.put("mycallstats", R.string.mystats_mycallstats);
		STRINGS.put("dataspeeds", R.string.mystats_dataspeeds);
		STRINGS.put("droppedcalls", R.string.mystats_droppedcalls);
		STRINGS.put("failedcalls", R.string.mystats_failedcalls);
		STRINGS.put("normalcalls", R.string.mystats_normalcalls);
		STRINGS.put("yourphone", R.string.mystats_yourphone);
		STRINGS.put("yourcarrier", R.string.mystats_yourcarrier);
		STRINGS.put("allcarriers", R.string.mystats_allcarriers);
		STRINGS.put("percentofcalls", R.string.mystats_percentofcalls);
		STRINGS.put("downloadspeed", R.string.mystats_downloadspeed);
		STRINGS.put("downloadspeedbps", R.string.mystats_downloadspeedbps);
		STRINGS.put("nostats", R.string.mystats_nostats);
		STRINGS.put("ranking", R.string.mystats_ranking);
	}
	static {
		CUST_STRINGS.put("callstats", R.string.mystats_custom_callstats);
		CUST_STRINGS.put("mycallstats", R.string.mystats_custom_mycallstats);
		CUST_STRINGS.put("droppedcalls", R.string.mystats_custom_droppedcalls);
		CUST_STRINGS.put("failedcalls", R.string.mystats_custom_failedcalls);
		CUST_STRINGS.put("normalcalls", R.string.mystats_custom_normalcalls);
		CUST_STRINGS.put("yourphone", R.string.mystats_custom_yourphone);
		CUST_STRINGS.put("yourcarrier", R.string.mystats_custom_yourcarrier);
		CUST_STRINGS.put("allcarriers", R.string.mystats_custom_allcarriers);
		CUST_STRINGS.put("percentofcalls", R.string.mystats_percentofcalls);
		CUST_STRINGS.put("downloadspeed", R.string.mystats_custom_downloadspeed);
		CUST_STRINGS.put("downloadspeedbps", R.string.mystats_custom_downloadspeedbps);
		CUST_STRINGS.put("nostats", R.string.mystats_nostats);
		CUST_STRINGS.put("ranking", R.string.mystats_custom_ranking);
	}
	private static final float STATS_RADIUS = 4000.0f;

	// private WebView mWebView;
	// private LegendView legendView;
	private StatViewNew[] statView = new StatViewNew[4];
	// private ProgressBar mLoadingIndicator;
	private GetStatsTask mGetStatsTask;
	private AsyncTask<Void, Void, String> mGetAddressTask;
	private ImageView[] mBullets;
	private Carrier currentCarrier;
	// private StatsScrollView statsScrollView;

	private Handler mHandler = new Handler();
	private TextView locationTextView, radiusTextView;
	private TextView dateAndTimeTextView;
	private Location statsLocation = null, nearLocation = null;
	private String mAddress = "", mWithinRadius = "";
	private int speedTier = 0;

	// private LinearLayout mButtons, mBulletLayout;
	// private Bitmap[] bmpArrowSrc = new Bitmap[2], bmpArrowDst = new Bitmap[2];
	private ImageButton[] btnArrow = new ImageButton[2];
	private HashMap<String, Integer> yourStats;
	private HashMap<String, Integer> yourSpeedTestAverage;

	private int lastminuteVariable;
	ViewFlipper page;

	Animation animFlipInForeward;
	Animation animFlipOutForeward;
	Animation animFlipInBackward;
	Animation animFlipOutBackward;
	AlphaAnimation alphaDown;

	/**
	 * Index for currently displayed stat from {@link CompareNew#STATS_URLS}.
	 */
	private int mCurrentStatIndex, firstStatIndex = 1, pages = 4;
	/**
	 * Cache for stats retrieved from server and local db.
	 */
	private JSONObject mStats;
	// gesture filter
	private SimpleGestureFilter filter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.new_compare_new, null, false);
		applyFontsAndScale(view);
		setContentView(view);
        MMCActivity.customizeTitleBar(this, view, R.string.dashboard_compare, R.string.dashcustom_compare);
		Integer showRankings = this.getResources().getInteger(R.integer.COMPARE_RANKINGS);
		Integer showCalls = this.getResources().getInteger(R.integer.COMPARE_CALLS);
		if (showRankings == -1) // allow rankings decided by server
		{
			showRankings = 1;
			int hideRankings = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.HIDE_RANKING, 1);
			if (hideRankings == 1)
				showRankings = 0;
		}
		if (showCalls == -1) // allow calls decided by server
		{
			showCalls = 1;
			int hideCalls = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.HIDE_CALLS, 1);
			if (hideCalls == 1)
				showCalls = 0;
		}

		pages = STATS_PAGES.length;
		// mWebView = (WebView) findViewById(R.id.mystats_webview);
		// mLoadingIndicator = (ProgressBar) findViewById(R.id.mystats_loadingindicator);
		// legendView = (LegendView) findViewById(R.id.stats_legend);

		int width = getResources().getDisplayMetrics().widthPixels;
		int height = getResources().getDisplayMetrics().heightPixels;
		int i, id;
		int tabWidth = width / (showRankings + 3);
		if (showCalls == 0)
			tabWidth = width;
		mBullets = new ImageView[4];
		Resources res = getResources();
		String apk = getPackageName();

		for (i = 0; i < 4; i++) {
			mBullets[i] = (ImageView) this.findViewById(res.getIdentifier("StatIndicator" + i, "id", apk));
			mBullets[i].getLayoutParams().width = tabWidth;
			TextView tv = (TextView) this.findViewById(res.getIdentifier("statText" + i, "id", apk));
			tv.getLayoutParams().width = tabWidth;
		}
		customizeBullets();
		customizeCompareTabsBg();
		customizeCompareTabsText();
		// mBullets[0] = (ImageView) findViewById(R.id.zeroStatIndicator);
		// mBullets[1] = (ImageView) findViewById(R.id.firstStatIndicator);
		// mBullets[2] = (ImageView) findViewById(R.id.secondStatIndicator);
		// mBullets[3] = (ImageView) findViewById(R.id.thirdStatIndicator);

		if (showRankings == 1 && showCalls == 1) {
			firstStatIndex = 0;
			mBullets[0].setVisibility(View.VISIBLE);
			mBullets[1].setVisibility(View.GONE);
		
		} else {
			TextView tv = (TextView) this.findViewById(res.getIdentifier("statText0", "id", apk));
			tv.setVisibility(View.GONE);
			View sv = (View) this.findViewById(res.getIdentifier("Separator0", "id", apk));
			sv.setVisibility(View.GONE);
			if (showCalls != 1)
			{
				tv = (TextView) this.findViewById(res.getIdentifier("statText1", "id", apk));
				tv.setVisibility(View.GONE);
				sv = (View) this.findViewById(res.getIdentifier("Separator1", "id", apk));
				sv.setVisibility(View.GONE);
				tv = (TextView) this.findViewById(res.getIdentifier("statText2", "id", apk));
				tv.setVisibility(View.GONE);
				sv = (View) this.findViewById(res.getIdentifier("Separator2", "id", apk));
				sv.setVisibility(View.GONE);
				firstStatIndex = 3;
			}
		}

		int vSepColor = getResources().getInteger(R.integer.TAB_V_SEP);
		if (vSepColor >= 0 && vSepColor <= 0xffffff) {
			vSepColor = vSepColor + 0xff000000;
			for (int i1 = 0; i1 < 3; i1++) {
				View v = findViewById(res.getIdentifier("Separator" + i1, "id", apk));
				if (v != null) {
					v.setBackgroundColor(vSepColor);
				}
			}
		}
		// mButtons = (LinearLayout) findViewById(R.id.mystats_buttons);
		// mBulletLayout = (LinearLayout) findViewById(R.id.mystats_bullets);

		// bmpArrowSrc[0] = BitmapFactory.decodeResource(getResources(),R.drawable.mystats_arrowleft);
		// bmpArrowSrc[1] = BitmapFactory.decodeResource(getResources(),R.drawable.mystats_arrowleft);
		btnArrow[0] = (ImageButton) findViewById(R.id.mystats_leftArrow);
		btnArrow[1] = (ImageButton) findViewById(R.id.mystats_rightArrow);

		// statsScrollView = (StatsScrollView)findViewById(R.id.stats_scrollview);

		statView[0] = (StatViewNew) findViewById(R.id.stats_webview0);
		statView[1] = (StatViewNew) findViewById(R.id.stats_webview1);
		statView[2] = (StatViewNew) findViewById(R.id.stats_webview2);
		statView[3] = (StatViewNew) findViewById(R.id.stats_webview3);

		// statsScrollView.addView(statWebView[0], width, height*8/10);
		// statsScrollView.addView(statWebView[1], width, height*8/10);

		locationTextView = (TextView) findViewById(R.id.locationText);
		radiusTextView = (TextView) findViewById(R.id.radiusText);
		dateAndTimeTextView = (TextView) findViewById(R.id.dateAndTimeText);
		// mWebView.getSettings().setJavaScriptEnabled(true);
		// mWebView.setBackgroundColor(0);
		// mWebView.addJavascriptInterface(new JavaScriptInterface(), "javaInterface");

		if (savedInstanceState != null)
			mCurrentStatIndex = savedInstanceState.getInt(KEY_CURRENTLY_DISPLAYED_STAT, 0);
		else
			mCurrentStatIndex = firstStatIndex;
		currentCarrier = ReportManager.getInstance(getApplicationContext()).getCurrentCarrier();
        SharedPreferences securePreferences = MMCService.getSecurePreferences(this);
        securePreferences.edit().putString(PreferenceKeys.Miscellaneous.OPERATOR_REQUEST, null).commit();

		page = (ViewFlipper) findViewById(R.id.flipper);
		for (i = 0; i < firstStatIndex; i++) {
			page.removeViewAt(0);
			mBullets[i].setVisibility(View.INVISIBLE);
		}

		float density = getResources().getDisplayMetrics().density;
		float h = height / density;
		// LayoutParams lp = page.getLayoutParams();
		// lp.height = (int) ((h - 150) * density);
		// if (h > 550)
		// lp.height = (int)((h - 130)*density);
		// else if (h < 520)
		// lp.height = (int)((h - 120)*density);
		// page.setLayoutParams(lp);
		/****
		 * To add layouts, we will inflate them as Views for the
		 * 
		 * flipper to know about them and set their index
		 *****/

		// page.addView(View.inflate(this, R.layout.webstats, null), 0);
		// page.addView(View.inflate(this, R.layout.webstats, null), 1);
		// page.addView(View.inflate(this, R.layout.webstats, null), 2);
		// page.addView(new StatWebView(this), 0);
		// page.addView(new StatWebView(this), 1);
		// page.addView(new StatWebView(this), 2);

		// page.setDisplayedChild (0);
		animFlipInForeward = AnimationUtils.loadAnimation(this, R.anim.flipin);
		animFlipOutForeward = AnimationUtils.loadAnimation(this, R.anim.flipout);
		animFlipInBackward = AnimationUtils.loadAnimation(this, R.anim.flipin_reverse);
		animFlipOutBackward = AnimationUtils.loadAnimation(this, R.anim.flipout_reverse);
		// listen for gestures
		this.filter = new SimpleGestureFilter(this, this);
		this.filter.setMode(SimpleGestureFilter.MODE_DYNAMIC);

		for (i = firstStatIndex; i < pages; i++) {
			// page.addView(statView[i], i);
			// statView[i].inflateChild (page, this, i);
			statView[i].init();
			// if (i==0)
			statView[i].setIndex(i);
			statView[i].setParent(this);
		}
		page.setDisplayedChild(0);
		// mWebView.loadUrl(STATS_URLS[mCurrentStatIndex]);
		alphaDown = new AlphaAnimation(1.0f, 0.0f);
		alphaDown.setDuration(4000);
		alphaDown.setFillAfter(true);
		setIndicatorBullets();
		setShareText();
		startLocation();
		getLocalStats();
		// run async to load the other web resources into the views
		// new LoadOffscreenViews().execute();

		// beginAnimation (500, 2000, mHandler);
		
		//IntentFilter locationFilter = new IntentFilter();
		//locationFilter.addAction(MMCIntentHandlerOld.ACTION_GPS_LOCATION_UPDATE);
		//locationFilter.addAction(MMCIntentHandlerOld.ACTION_NETWORK_LOCATION_UPDATE);
		//registerReceiver(broadcastReceiver, locationFilter);
	}

	private void customizeBullets() {
		String bulletsColorString = getResources().getString(R.string.CUSTOM_RANKING_BULLET_COLOR);
		if (bulletsColorString == null || bulletsColorString.length() == 0)
			return;
		int colorCode = Integer.parseInt(bulletsColorString, 16) + (0xff000000);

		for (int count = 0; count < 4; count++) {
			mBullets[count].setBackgroundColor(colorCode);
		}

	}

	private void customizeCompareTabsBg() {
		String tabsBgColorString = getResources().getString(R.string.CUSTOM_COMPARE_TABS_BG_COLOR);
		String tabsFgColorString = getResources().getString(R.string.CUSTOM_COMPARE_TABS_FG_COLOR);
		
		if (tabsBgColorString != null && tabsBgColorString.length() > 0) {
			int colorCode = Integer.parseInt(tabsBgColorString, 16) + (0xff000000);

			LinearLayout statsTabsLayout = (LinearLayout) findViewById(R.id.compare_tabs);
			statsTabsLayout.setBackgroundColor(colorCode);
		}

		if (tabsFgColorString != null && tabsFgColorString.length() > 0) {
			int colorCode = Integer.parseInt(tabsFgColorString, 16) + (0xff000000);

			TextView statText = (TextView) findViewById(R.id.statText0);
			statText.setTextColor(colorCode);
			statText = (TextView) findViewById(R.id.statText1);
			statText.setTextColor(colorCode);
			statText = (TextView) findViewById(R.id.statText2);
			statText.setTextColor(colorCode);
			statText = (TextView) findViewById(R.id.statText3);
			statText.setTextColor(colorCode);
		}

	}
	
	private void customizeCompareTabsText ()
	{
		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
		if (customTitles != 1)
			return;
		TextView statText = (TextView) findViewById(R.id.statText0);
		statText.setText(R.string.mystats_custom_stat0);
		statText = (TextView) findViewById(R.id.statText1);
		statText.setText(R.string.mystats_custom_stat1);
		statText = (TextView) findViewById(R.id.statText2);
		statText.setText(R.string.mystats_custom_stat2);
		statText = (TextView) findViewById(R.id.statText3);
		statText.setText(R.string.mystats_custom_stat3);
	}

	private void applyFontsAndScale(View view) {
		RelativeLayout topActionBarLayout = (RelativeLayout) view.findViewById(R.id.topactionbarLayout);
		LinearLayout statsLayout = (LinearLayout) view.findViewById(R.id.compare_tabs);
		RelativeLayout locationAndTimeLayout = (RelativeLayout) view.findViewById(R.id.locationAndTimeLayout);
		RelativeLayout arrowslayout = (RelativeLayout) view.findViewById(R.id.arrowslayout);

		TextView actionbarheading = (TextView) view.findViewById(R.id.actionbartitle);
		TextView locationText = (TextView) view.findViewById(R.id.locationText);
		TextView dateAndTime = (TextView) view.findViewById(R.id.dateAndTimeText);
		TextView radiusText = (TextView) view.findViewById(R.id.radiusText);
		TextView statsText0 = (TextView) view.findViewById(R.id.statText0);
		TextView statsText1 = (TextView) view.findViewById(R.id.statText1);
		TextView statsText2 = (TextView) view.findViewById(R.id.statText2);
		TextView statsText3 = (TextView) view.findViewById(R.id.statText3);

		ScalingUtility.getInstance(this).scaleView(topActionBarLayout);
		ScalingUtility.getInstance(this).scaleView(statsLayout);

		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, actionbarheading, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, locationText, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, dateAndTime, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, radiusText, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, statsText0, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, statsText1, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, statsText2, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, statsText3, this);

		if (StatChartNew.isTallScreen(this)) {
			radiusText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			locationText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
			dateAndTime.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
		}

		statsText0.setOnClickListener(statZeroSwitchListener);
		statsText1.setOnClickListener(statOneSwitchListener);
		statsText2.setOnClickListener(statTwoSwitchListener);
		statsText3.setOnClickListener(statsThreeSwitchListener);

	}

	OnClickListener statZeroSwitchListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// mBullets[0].setVisibility(View.VISIBLE);
			// mBullets[1].setVisibility(View.GONE);
			// mBullets[2].setVisibility(View.GONE);
			// mBullets[3].setVisibility(View.GONE);

			page.invalidate();
			page.setDisplayedChild(0);
			mCurrentStatIndex = 0;
			setIndicatorBullets();
		}
	};
	OnClickListener statOneSwitchListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// mBullets[0].setVisibility(View.GONE);
			// mBullets[1].setVisibility(View.VISIBLE);
			// mBullets[2].setVisibility(View.GONE);
			// mBullets[3].setVisibility(View.GONE);
			page.invalidate();
			page.setDisplayedChild(1 - firstStatIndex);
			mCurrentStatIndex = 1;
			setIndicatorBullets();
		}
	};
	OnClickListener statTwoSwitchListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// mBullets[0].setVisibility(View.GONE);
			// mBullets[2].setVisibility(View.VISIBLE);
			// mBullets[1].setVisibility(View.GONE);
			// mBullets[3].setVisibility(View.GONE);
			page.invalidate();
			page.setDisplayedChild(2 - firstStatIndex);
			mCurrentStatIndex = 2;
			setIndicatorBullets();
		}
	};
	OnClickListener statsThreeSwitchListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
			// mBullets[0].setVisibility(View.GONE);
			// mBullets[3].setVisibility(View.VISIBLE);
			// mBullets[1].setVisibility(View.GONE);
			// mBullets[2].setVisibility(View.GONE);
			page.invalidate();
			page.setDisplayedChild(3 - firstStatIndex);
			mCurrentStatIndex = 3;
			setIndicatorBullets();
		}
	};

	public void setTitle(int page) {
		TextView title = (TextView) findViewById(R.id.actionbartitle);
		SpannableString titleText = new SpannableString(getString(STATS_PAGES[page]));
		// int black = getResources().getColor(R.color.black);
		// titleText.setSpan(new ForegroundColorSpan(black), 0, titleText.length(), 0);
		title.setText(titleText);
	};

	public String getString(String name) {
		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);

		if (name.equals("downloadspeed")) {
			if (customTitles == 1) {
				int speedtitle = R.string.mystats_custom_downloadspeed;
				switch (getSpeedTier()) {
				case 0:
					speedtitle = R.string.mystats_custom_downloadspeed;
					break;
				case 1:
				case 2:
					speedtitle = R.string.mystats_custom_downloadspeed2G;
					break;
				case 3:
				case 4:
					speedtitle = R.string.mystats_custom_downloadspeed3G;
					break;
				case 5:
					speedtitle = R.string.mystats_custom_downloadspeedLTE;
					break;
				case 10:
					speedtitle = R.string.mystats_custom_downloadspeedWifi;
					break;
				}
				return getString(speedtitle);
			} else {
				int speedtitle = R.string.mystats_downloadspeed;
				switch (getSpeedTier()) {
				case 0:
					speedtitle = R.string.mystats_downloadspeed;
					break;
				case 1:
				case 2:
					speedtitle = R.string.mystats_downloadspeed2G;
					break;
				case 3:
				case 4:
					speedtitle = R.string.mystats_downloadspeed3G;
					break;
				case 5:
					speedtitle = R.string.mystats_downloadspeedLTE;
					break;
				case 10:
					speedtitle = R.string.mystats_downloadspeedWifi;
					break;
				}
				return getString(speedtitle);
			}
		}
		if (customTitles == 1)
			return getString((int) CompareNew.CUST_STRINGS.get(name));
		else
			return getString((int) CompareNew.STRINGS.get(name));
	}

	private LocationRequest locationRequest;
	public void startLocation() {
		locationRequest = new LocationRequest (this, 400);
		locationRequest.setUpdateUI (true);
		locationRequest.setOnNewLocationListener(new LocationRequest.OnLocationListener() {
			@Override
			public void onLocation(LocationRequest locationRequest) {
				if (locationRequest.bLocationChanged == false)
					return;
				Location location = locationRequest.getLocation();
				statsLocation = location;
				nearLocation = location;
				mGetStatsTask = new GetStatsTask();
				mStats = null;
				mGetStatsTask.execute(location);
				showAddress(location);
			}
		});
		locationRequest.start ();
	}
	
	// Handler will be called up to 3 times: instant lastLocation, first networkLocation, first good GPS Fix
	private Handler locationHandler = new Handler() 
	{
		@Override
		public void handleMessage (Message msg) {
			Location location = locationRequest.getLocation();
			statsLocation = location;
			nearLocation = location;
			mGetStatsTask = new GetStatsTask();
			mStats = null;
			mGetStatsTask.execute(location);
			showAddress(location);
		}
	};
	
		/*
		Location location1, location2, location;

		location = ReportManager.getInstance(getApplicationContext()).getLastKnownLocation();
		location1 = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
		location2 = ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if (location1 != null && (location2 == null || location1.getTime() > location2.getTime()))
			location = location1;
		if (location2 != null && (location1 == null || location2.getTime() > location1.getTime()))
			location = location2;
		nearLocation = location;
		showAddress(location);
		if (location != null && location.getLatitude() != 0) {
			mGetStatsTask = new GetStatsTask();
			//mGetStatsTask.execute(location);
			TaskHelper.execute(mGetStatsTask, location);
		}
		brieflyRunLocation(60);
		
	} */

//	public void brieflyRunLocation(int timeoutSeconds) {
//		// Use both GPS and Network location to try to get an improved location for statistics
//		// Indoors, GPS might not work, but in some locations Network might not work
////		GpsListenerForStats locListener = new GpsListenerForStats();
////		locListener.setFirstFixTimeout(timeoutSeconds * 1000); // using our own timeout to force gps off after
////																// timeoutSeconds
////		locListener.setOperationTimeout(0);
////		locListener.setProvider(LocationManager.GPS_PROVIDER);
////		if (MMCService.getGpsManager() != null)
////			MMCService.getGpsManager().registerListener(locListener);
//		Intent intentGPS = new Intent(MMCIntentHandlerOld.ACTION_BRIEFLY_RUN_LOCATION);  
//		intentGPS.putExtra("timeoutSeconds", timeoutSeconds*1000);
//		intentGPS.putExtra("provider", LocationManager.GPS_PROVIDER);
//		intentGPS.putExtra("triggerUpdate", false);
//		sendBroadcast(intentGPS);
////
////		LocationListenerForStats locListener2 = new LocationListenerForStats();
////		locListener2.setFirstFixTimeout(20 * 1000); // using our own timeout to force gps off after timeoutSeconds
////		locListener2.setOperationTimeout(0);
////		locListener2.setProvider(LocationManager.NETWORK_PROVIDER);
////		if (MMCService.getNetLocationManager() != null)
////			MMCService.getNetLocationManager().registerListener(locListener2);
//		Intent intentNetwork = new Intent(MMCIntentHandlerOld.ACTION_BRIEFLY_RUN_LOCATION);  
//		intentNetwork.putExtra("timeoutSeconds", 20*1000);
//		intentNetwork.putExtra("provider", LocationManager.NETWORK_PROVIDER);
//		intentNetwork.putExtra("triggerUpdate", false);
//		sendBroadcast(intentNetwork);
//	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putInt(KEY_CURRENTLY_DISPLAYED_STAT, mCurrentStatIndex);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mGetStatsTask != null) {
			mGetStatsTask.cancel(true);
		}

		if (mGetAddressTask != null) {
			mGetAddressTask.cancel(true);
		}
		
		if (locationRequest != null)
			locationRequest.stop();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// mShareInfo.setVisibility(View.GONE);

		// mButtons.setVisibility(View.VISIBLE);
		for (int i = firstStatIndex; i < pages; i++)
			mBullets[i].setVisibility(View.VISIBLE);

		// Reload the web page when resuming from a share
		for (int i = firstStatIndex; i < pages; i++)
			statView[i].reload();
		loadStatPage();
		// mLoadingIndicator.setVisibility(View.VISIBLE);

	}

	private void loadStatPage() {
		// mWebView.loadUrl(STATS_URLS[mCurrentStatIndex]);
		// mLoadingIndicator.setVisibility(View.VISIBLE);
		// legendView.setStatScreen (mCurrentStatIndex);
		setIndicatorBullets();

		if (STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL)) {
			radiusTextView.setText("");
			locationTextView.setText("");
		} else {
			radiusTextView.setText(mWithinRadius);
			locationTextView.setText(mAddress);
		}
	}

	// override the dispatch
	@Override
	public boolean dispatchTouchEvent(MotionEvent me) {
		this.filter.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}

	// manage swipe animations
	@Override
	public void onSwipe(int direction) {

		switch (direction) {

		case SimpleGestureFilter.SWIPE_RIGHT:
			SwipeRight();
			break;
		case SimpleGestureFilter.SWIPE_LEFT:
			SwipeLeft();
			break;
		case SimpleGestureFilter.SWIPE_DOWN:
		case SimpleGestureFilter.SWIPE_UP:

		}
	}

	// manage double tap
	@Override
	public void onDoubleTap() {
	}

	@Override
	public void onSingleTap(MotionEvent e) {
		statView[mCurrentStatIndex].onSingleTap(e);
	}

	private void SwipeRight() {
		if (mCurrentStatIndex > firstStatIndex) {
			mCurrentStatIndex--;
		} else if (mCurrentStatIndex == firstStatIndex) {
			mCurrentStatIndex = STATS_URLS.length - 1;
		}
		page.setInAnimation(animFlipInBackward);
		page.setOutAnimation(animFlipOutBackward);
		page.showPrevious();
		loadStatPage();

	}

	private void SwipeLeft() {
		if (mCurrentStatIndex < STATS_URLS.length - 1) {
			mCurrentStatIndex++;
		} else if (mCurrentStatIndex == STATS_URLS.length - 1) {
			mCurrentStatIndex = firstStatIndex;
		}
		page.setInAnimation(animFlipInForeward);
		page.setOutAnimation(animFlipOutForeward);
		page.showNext();
		loadStatPage();
	}

	public void leftClicked(View button) {
		SwipeRight();
		// if(mCurrentStatIndex > 0) {
		// mCurrentStatIndex--;
		// }
		// else if(mCurrentStatIndex == 0) {
		// mCurrentStatIndex = STATS_URLS.length - 1;
		// }
		// loadStatPage ();
	}

	public void rightClicked(View button) {
		SwipeLeft();
		// if(mCurrentStatIndex < STATS_URLS.length - 1) {
		// mCurrentStatIndex++;
		// }
		// else if(mCurrentStatIndex == STATS_URLS.length - 1) {
		// mCurrentStatIndex = 0;
		// }
		// loadStatPage ();
	}

	/*
	@Override
	public boolean onTouchEvent(MotionEvent event) {
	 // TODO Auto-generated method stub
	    return gestureDetector.onTouchEvent(event);
	}


	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener()
	{

	 @Override
	 public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) 
	 {

		  float sensitvity = 50;
		  if((e1.getX() - e2.getX()) > sensitvity){
		   SwipeLeft();
		  }else if((e2.getX() - e1.getX()) > sensitvity){
		   SwipeRight();
		  }

		  return true;
	 }
	};

	GestureDetector gestureDetector = new GestureDetector(simpleOnGestureListener);
	 */
	/**
	 * Sets the current stat's bullet's image to the active bullet image
	 */
	private void setIndicatorBullets() {
		for (int i = firstStatIndex; i < mBullets.length; i++) {
			mBullets[i].setVisibility(View.GONE);
		}

		// see if we need to hide locationAndTimeLayout (at the bottom)
		View v = findViewById(R.id.locationAndTimeLayout);
		if (getResources().getBoolean(R.bool.HIDE_LOCATION_TIME)) {
			v.setVisibility(View.INVISIBLE);
		} else if (getResources().getBoolean(R.bool.HIDE_LOCATION_TIME_DONUT_SCREEN) && STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL)) {
			v.setVisibility(View.INVISIBLE);
		} else {
			v.setVisibility(View.VISIBLE);
		}
		mBullets[mCurrentStatIndex].setVisibility(View.VISIBLE);
		// tell stat view it is being shown, in case it wants to animate
		statView[mCurrentStatIndex].show();
		// setTitle (mCurrentStatIndex); -- title set once to 'Compare' for all charts
		btnArrow[0].startAnimation(alphaDown);
		btnArrow[1].startAnimation(alphaDown);

		trackEvent("Compare", STATS_PAGES[mCurrentStatIndex], "", 0);
	}

	public void backActionClicked(View button) {
		this.finish();
	}

	public void shareClicked(View button) {
		if (mStats != null || (STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL))) {
			try {
				if (button == null)
					return;
				temporarilyDisableButton(button);

				String message = "";
				String subject = "";
				// mShareInfo.setVisibility(View.VISIBLE);
				float density = getResources().getDisplayMetrics().density;
				int width = getResources().getDisplayMetrics().widthPixels;

				if (width > 320 * density)
					// mShareText.setPadding((width-(int)(320*density))/2, 0, 0, 6);

					// mButtons.setVisibility(View.GONE);
					for (int i = 0; i < pages; i++)
						mBullets[i].setVisibility(View.INVISIBLE);

				if (STATS_URLS[mCurrentStatIndex].equals(CALLSTATS_PAGE_URL)) {
					int yourCarrier = 0;
					int allCarriers = 0;// mStats.getJSONObject("allcarriers").getInt(StatsKeys.NORMALLY_ENDED_CALLS);
					try {
						allCarriers = mStats.getJSONObject("0").getInt(StatsKeys.NORMALLY_ENDED_CALLS);
						yourCarrier = mStats.getJSONObject(currentCarrier.OperatorId).getInt(StatsKeys.NORMALLY_ENDED_CALLS);
					} catch (Exception e) {
					}

					if (yourCarrier < allCarriers)
						message = getString(R.string.sharemessage_mystats_calls_less);
					else
						message = getString(R.string.sharemessage_mystats_calls_more);
					subject = getString(R.string.sharemessagesubject_callstats);
				} else if (STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL)) {
					message = getString(R.string.sharemessage_mystats_mycalls);
					subject = getString(R.string.sharemessagesubject_mystats);
				} else if (STATS_URLS[mCurrentStatIndex].equals(RANKING_PAGE_URL)) {
					message = getString(R.string.sharemessage_rankings);
					subject = getString(R.string.sharemessagesubject_rankings);
				} else if (STATS_URLS[mCurrentStatIndex].equals(DOWNLOADSPEED_PAGE_URL)) {
					// int yourCarrier = mStats.getJSONObject("yourcarrier").getInt(SpeedTestKeys.DOWNLOAD_SPEED);
					// int allCarriers = mStats.getJSONObject("allcarriers").getInt(SpeedTestKeys.DOWNLOAD_SPEED);
					int yourCarrier = 0;
					int allCarriers = 0;// mStats.getJSONObject("allcarriers").getInt(StatsKeys.NORMALLY_ENDED_CALLS);
					try {
						allCarriers = mStats.getJSONObject("0").getInt(SpeedTestKeys.DOWNLOAD_SPEED);
						yourCarrier = mStats.getJSONObject(currentCarrier.OperatorId).getInt(SpeedTestKeys.DOWNLOAD_SPEED);
					} catch (Exception e) {
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "shareClicked", "", e);
					}
					subject = getString(R.string.sharemessagesubject_dataspeeds);
					if (yourCarrier < allCarriers)
						message = getString(R.string.sharemessage_mystats_downloadspeed_poor);
					else
						message = getString(R.string.sharemessage_mystats_downloadspeed_good);
				}
				final String fmessage = message;
				final String fsubject = subject;
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						String message;
						TaskHelper.execute(
						new ShareTask(CompareNew.this, fmessage, fsubject, findViewById(R.id.compare_container))); //.execute((Void[]) null);
					}
				}, 400);
			} catch (Exception e) {
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "shareClicked", "", e);
			}
		}
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

				CompareNew.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						button.setEnabled(true);

					}
				});
			}
		}).start();
	}

	public void historyClicked(View button) {
		Intent intent = null;
		if (STATS_URLS[mCurrentStatIndex].equals(DOWNLOADSPEED_PAGE_URL))
		{
			intent = new Intent(this, SpeedTestHistory.class);
			intent.putExtra ("EVENTTYPE", EventType.MAN_SPEEDTEST.getIntValue());
		}
		else
			intent = new Intent(this, EventHistory.class);
		intent.putExtra("fromStats", 1);
		startActivity(intent);
	}

	private void setShareText() {
		String carrier = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName();
		String phone = "";
		if (android.os.Build.BRAND.length() > 0) {
			String brand = android.os.Build.BRAND.substring(0, 1).toUpperCase() + android.os.Build.BRAND.substring(1);
			phone = " - " + brand + " " + android.os.Build.MODEL;
		}
		long timeStamp = System.currentTimeMillis();
		DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
		String dateTime = dateTimeFormat.format(new Date(timeStamp));

		String shareText = carrier + phone + "\n" + dateTime;
		// mShareText.setText(shareText);
	}

	private void getLocalStats() {
		long startTime = System.currentTimeMillis() - 30 * AlarmManager.INTERVAL_DAY;
		long endTime = System.currentTimeMillis();
		JSONObject stats = new JSONObject();

		ReportManager reportManager = ReportManager.getInstance(getApplicationContext());
		yourStats = reportManager.getYourCallStats(startTime, endTime);
		yourSpeedTestAverage = reportManager.getYourSpeedTestAverage(startTime, endTime, getSpeedTier());
		yourStats.putAll(yourSpeedTestAverage);
		try {

			List<Carrier> operators = reportManager.getTopOperators(0, 0, 0, 0, 15, false);
			if (operators != null)
				mStats = reportManager.getTopCarriersStats(operators, 0, 0, 0, 0, getSpeedTier(), false);
			else
				mStats = new JSONObject();
			if (mStats != null)
				mStats.put("yourphone", new JSONObject(yourStats));
		} catch (Exception e) {
			MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
			if (e.getCause() instanceof UnknownHostException) // || e.getCause() instanceof HttpHostConnectException)
				showError(R.string.GenericText_UnknownHost);
		}
	}

	public int getSpeedTier() {
		int networkType = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();

		if (isNetworkWifi())
			speedTier = 10;
		else
			speedTier = MMCPhoneStateListenerOld.getNetworkGeneration(networkType);
		if (speedTier == 0)
			speedTier = 3;
		return speedTier;
	}

	private void showAddress(final Location location) {
		if (location == null || location.getLatitude() == 0.0) {
			radiusTextView.setText("");
			locationTextView.setText(getString(R.string.mystats_unknownlocation));
			return;
		}
		// TODO: get address of location
		mGetAddressTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				return WebReporter.geocode(location);

			}

			@Override
			protected void onPostExecute(String result) {
				long timeStamp = System.currentTimeMillis();
				DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
				dateAndTimeTextView.setText("" + dateTimeFormat.format(new Date(timeStamp)));

				String address = "";
				if (result != null)
					address = result;

				if (address.length() < 3)
					if (nearLocation != null && nearLocation.getLatitude() != 0)
						address = String.format("%.4f, %.4f", nearLocation.getLatitude(), nearLocation.getLongitude());
					else
						address = "unknown";
				int radiuskm = 1000;
				if (mStats != null && mStats.has("radius"))
					try {
						radiuskm = (int) mStats.getDouble("radius");
					} catch (JSONException e) {
					}
				radiuskm = (radiuskm + 500) / 1000;
				mWithinRadius = String.format(getString(R.string.mystats_withinarea), radiuskm);
				mAddress = address;
				if (STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL)) {
					radiusTextView.setText("");
					locationTextView.setText("");
				} else {
					radiusTextView.setText(mWithinRadius);
					locationTextView.setText(mAddress);
				}
			}

		};//.execute((Void[]) null);
		TaskHelper.execute(mGetAddressTask);
	}
	
/*	public String parseAddress(String response) {
		JSONObject json = null;

		String address = null;
		
		if(response == null)
			return null;
		
		try {
			json = new JSONObject(response);
			
			json = json.getJSONObject("address");
			String number = json.getString("house_number");
			String road = json.getString("road");
			address = number + " " + road;
		}
		catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
		return address;
	} */
	
	public String parseAddress(String response) {
		JSONObject json = null;
		JSONArray jsonArray = null;

		if(response == null)
			return null;
		
		try {
			jsonArray = new JSONArray(response);
		} catch (JSONException e) {
			return null;
		}
		try {
			for(int i = 0; i < jsonArray.length(); i++) {
				json = jsonArray.getJSONObject(i);
				if(json.has("error")) {
					String error = json.getString("error");
					return null;
				}
				else {
					json = json.getJSONObject("address");
					String number = "";
					if(json.has("house_number"))
						number = json.getString("house_number");
					String road = json.getString("road");
					String suburb = "";
					//if(json.has("suburb"))
					//	suburb = ", " + json.getString("suburb");
					String address = number + " " + road + suburb;
					return address;
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	public JSONObject getStats() {
		return mStats;
	}

	class GetStatsTask extends AsyncTask<Location, Void, JSONObject> {
		private static final String TAG = "GetStatsTask";

		@Override
		protected JSONObject doInBackground(Location... location) {
			long startTime = System.currentTimeMillis() - 30 * AlarmManager.INTERVAL_DAY;
			long endTime = System.currentTimeMillis();

			if (mStats == null || !mStats.has("yourcarrier")) {
				// mStats = new JSONObject();

				ReportManager reportManager = ReportManager.getInstance(getApplicationContext());
				/*
				HashMap<String, Integer> yourStats = reportManager.getYourCallStats(startTime, endTime);
				HashMap<String, Integer> yourSpeedTestAverage = reportManager.getYourSpeedTestAverage(startTime, endTime);
				yourStats.putAll(yourSpeedTestAverage);
				//get your phone stats
				try {
					mStats.put("yourphone", new JSONObject(yourStats));
				}
				catch (JSONException e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
					//throw new RuntimeException(e);
					MyStats.this.hideLoadingIndicator();
					return null;
				} 
				 */
				double latitude = 0, longitude = 0;
				float radius = 2;

				if (location[0] != null) {
					latitude = location[0].getLatitude();
					longitude = location[0].getLongitude();
					radius = STATS_RADIUS;
				}
				MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "getStats", "get stats for " + latitude + "," + longitude);

				int mcc = reportManager.getMCCMNC()[0];
				List<Carrier> operators = null;

				try {
					operators = reportManager.getTopOperators(latitude, longitude, (int) radius, mcc, 15, true);
					mStats = reportManager.getTopCarriersStats(operators, 0, latitude, longitude, 0f, getSpeedTier(), true);
					if (yourStats != null && mStats != null)
						mStats.put("yourphone", new JSONObject(yourStats));
				} catch (MMCException e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
					if (e.getCause() instanceof UnknownHostException)
						showError(R.string.GenericText_UnknownHost);
					CompareNew.this.hideLoadingIndicators();
					return null;
					// cannot do anything about this exception, the stats will just not get displayed
				} catch (Exception e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
					CompareNew.this.hideLoadingIndicators();
					return null;
					// throw new RuntimeException(e);
				}
				/*
				//get all carriers stats
				try {
					HashMap<String, String> allCarrierStats = reportManager.
							getAllCarriersAverageStats(carrier.get(MMCDevice.KEY_MCC),
									startTime, endTime, latitude, longitude, radius, speedTier);
					if (allCarrierStats != null)
						mStats.put("allcarriers", new JSONObject(allCarrierStats));
				}
				catch (MMCException e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
					MyStats.this.hideLoadingIndicator();
					return null;
					//cannot do anything about this exception, the stats will just not get displayed
				}
				catch (JSONException e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStats", "", e);
					MyStats.this.hideLoadingIndicator();
					return null;
					//throw new RuntimeException(e);
				}
				 */
			}

			return mStats;
		}

		@Override
		protected void onPostExecute(JSONObject stats) {
			if (stats != null) // && !(STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL)))
			{
				for (int i = firstStatIndex; i < pages; i++)
					statView[i].loadStats();

				int radiuskm = 1000;
				if (mStats != null && mStats.has("radius"))
					try {
						radiuskm = (int) mStats.getDouble("radius");
					} catch (JSONException e) {
					}
				radiuskm = (radiuskm + 500) / 1000;
				mWithinRadius = String.format(getString(R.string.mystats_withinarea), radiuskm);
				if (!STATS_URLS[mCurrentStatIndex].equals(MYCALLSTATS_PAGE_URL))
					radiusTextView.setText(mWithinRadius);

			}
			// mWebView.loadUrl("javascript:setStats(" + stats + ")");
		}
	}

	private boolean isNetworkWifi() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivityManager != null) {
			NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
			if (networkInfo != null) {
				int wifiState = networkInfo.getType();
				return (wifiState == ConnectivityManager.TYPE_WIFI);
			}
		}
		return false;
	}

	public void hideLoadingIndicators() {
		for (int i = firstStatIndex; i < pages; i++)
			statView[i].hideLoadingIndicator();

	}

	private void showError(final int stringres) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CompareNew.this, stringres, Toast.LENGTH_LONG).show();
			}
		});
	}

	private void drawArrows() {
		int percent = animatePercentage();
		int i, alpha = 255;
		if (percent > 50 && percent < 100)
			alpha = 255 - (percent - 50) * 5;
		for (i = 0; i < 2; i++) {
			btnArrow[i].setAlpha(alpha);
			btnArrow[i].invalidate();
		}

	}

	protected boolean bAnimating = false;
	protected long animateStart = 0, animateEnd = 0;

	public void beginAnimation(final long delay, final long duration, Handler handler) {
		if (!bAnimating) {
			bAnimating = true;
			animateStart = System.currentTimeMillis() + delay;
			animateEnd = System.currentTimeMillis() + duration + delay;

			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					animateStart = System.currentTimeMillis();
					animateEnd = System.currentTimeMillis() + duration;
					drawArrows();

				}
			}, delay);
		}
	}

	protected int animatePercentage() {
		if (bAnimating) {
			if (System.currentTimeMillis() < animateStart)
				return 0;
			int percent = (int) ((System.currentTimeMillis() - animateStart) * 100 / (animateEnd - animateStart));

			if (System.currentTimeMillis() > animateEnd) {
				bAnimating = false;
				return 100;
			}
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					drawArrows();
				}
			}, 10);
			return percent;
		}
		return 100;
	}

	/**
	 * This class encapsulates the data and the logic for gps management for the {@link MMCServiceOld} service. This
	 * class is intended to be used for turning on the gps when an event takes place.
	 */

//	class GpsListenerForStats extends GpsListenerOld {
//
//		public GpsListenerForStats() {
//			super("GpsListenerForStats");
//		}
//
//		/**
//		 * For events, the chaining property of the GpsManager is not utilised. Instead, we rely on the timeout to stop
//		 * the gps for us. Therefore, after processing the location using both the
//		 * {@link MMCServiceOld#processNewRawLocation(Location)} and
//		 * {@link MMCServiceOld#processNewFilteredLocation(Location)} methods, we simply return true.
//		 */
//		@Override
//		public boolean onLocationUpdate(Location location) {
//			if (location == null)
//				return true;
//			if (location.getAccuracy() < 400) // We'll settle for 400 for statistics
//			{
//				if (statsLocation == null || Math.abs(statsLocation.getLatitude() - location.getLatitude()) > 0.002 || Math.abs(statsLocation.getLongitude() - location.getLongitude()) > 0.002) {
//					// if (bFreshGps == false && (mGetStatsTask == null || mGetStatsTask.getStatus() !=
//					// AsyncTask.Status.RUNNING)) // && mCurrentStatIndex > 0)
//					{
//						statsLocation = location;
//						nearLocation = location;
//						mGetStatsTask = new GetStatsTask();
//						mStats = null;
//						mGetStatsTask.execute(location);
//						showAddress(location);
//						bFreshGps = true;
//						return false;
//					}
//				}
//				return true;
//			}
//			return true;
//		}
//	}


	/**
	 * This class encapsulates the data and the logic for gps management for the {@link MMCServiceOld} service. This
	 * class is intended to be used for turning on the gps when an event takes place.
	 */

//	class LocationListenerForStats extends GpsListenerOld {
//
//		public LocationListenerForStats() {
//			super("LocationListenerForStats");
//		}
//
//		/**
//		 * For events, the chaining property of the GpsManager is not utilised. Instead, we rely on the timeout to stop
//		 * the gps for us. Therefore, after processing the location using both the
//		 * {@link MMCServiceOld#processNewRawLocation(Location)} and
//		 * {@link MMCServiceOld#processNewFilteredLocation(Location)} methods, we simply return true.
//		 */
//		@Override
//		public boolean onLocationUpdate(Location location) {
//			if (location == null)
//				return true;
//			if (statsLocation == null || Math.abs(statsLocation.getLatitude() - location.getLatitude()) > 0.002 || Math.abs(statsLocation.getLongitude() - location.getLongitude()) > 0.002) {
//				// if (bFreshGps == false && (mGetStatsTask == null || mGetStatsTask.getStatus() !=
//				// AsyncTask.Status.RUNNING)) // && mCurrentStatIndex > 0)
//				{
//					statsLocation = location;
//					nearLocation = location;
//					mGetStatsTask = new GetStatsTask();
//					mStats = null;
//					mGetStatsTask.execute(location);
//					showAddress(location);
//					bFreshGps = true;
//					return false;
//				}
//			}
//			return true;
//		}
//	}
	
//	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent){
//			if(intent.getAction().equals(MMCIntentHandlerOld.ACTION_GPS_LOCATION_UPDATE)){
//				Location location = intent.getParcelableExtra("location");
//				if (location == null)
//					return;
//				if (location.getAccuracy() < 400) // We'll settle for 400 for statistics
//				{
//					if (statsLocation == null || Math.abs(statsLocation.getLatitude() - location.getLatitude()) > 0.002 || Math.abs(statsLocation.getLongitude() 
//						- location.getLongitude()) > 0.002) {
//						statsLocation = location;
//						nearLocation = location;
//						mGetStatsTask = new GetStatsTask();
//						mStats = null;
//						mGetStatsTask.execute(location);
//						showAddress(location);
//					}
//				}
//			}
//			else if(intent.getAction().equals(MMCIntentHandlerOld.ACTION_NETWORK_LOCATION_UPDATE)) {
//				Location location = intent.getParcelableExtra("location");
//				if (location == null)
//					return;
//				if (statsLocation == null || Math.abs(statsLocation.getLatitude() - location.getLatitude()) > 0.002 || Math.abs(statsLocation.getLongitude()
//						- location.getLongitude()) > 0.002) {
//					statsLocation = location;
//					nearLocation = location;
//					mGetStatsTask = new GetStatsTask();
//					mStats = null;
//					mGetStatsTask.execute(location);
//					showAddress(location);
//				}
//			}
//		}
//	};

}
