package com.cortxt.app.MMC.Sampling.Transit;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Activities.EventHistory;
import com.cortxt.app.MMC.Activities.MyCoverage.CoverageOverlay;
import com.cortxt.app.MMC.Activities.MyCoverage.MMCMapView;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventOverlayItem;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventsOverlay;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedMapActivityOld;
import com.cortxt.app.MMC.ContentProviderOld.ContentValuesGeneratorOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesEnumOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.Utils.CustomMyLocationOverlay;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.CommonIntentBundleKeysOld;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;

public class TransitSamplingMap extends MMCTrackedMapActivityOld implements SensorEventListener /*, AnimationListener*/ {

	//Widgets
	private Button startPauseButton;
	private Button stopButton;
	private TextView stationTextView;
	private ImageView speedImageView;
	private TextView speedTextView;
//	private ImageView menuImageView;
	private TextView latLongTextView;
	private RelativeLayout topactionbarLayout;
	private ImageView informationImageView;
	private ImageView troubleImageView;
	private ImageView seperator;
	private View informationLayout;
	
	private MMCMapView mMapView;
	private TransitSamplingOverlay transitSamplingOverlay;
	private TransitStats transitStats = null;
	private AccelerometerHistory history;
	private ArrayList<TransitInfo> stops = null;
//	private static CountDownTimer timer = null;
	private TimerTask timerTask;
	private SensorManager sensorManager;
	private Sensor magnetSensor; 
	private Sensor gravitySensor; 
	private Sensor accelSensor;
	private ReportManager reportManager;
	private LocationManager locationManager;
	private WifiManager wifiManager;
	private CustomMyLocationOverlay  myLocationOverlay;
	private TransitDatabaseReadWriteNew transitDB;
	private EventsOverlay mEventsOverlay;
	private CoverageOverlay mCoverageOverlay;
	private ProgressBar busyIndicator;
	private PopupWindow popupWindowStations;
	private PopupWindow popupWindowProblems;
	private TransitSamplingMapMath transitMath;
	
	private int startStationIndex = 0;
	private int stopStationIndex = 0;
	private int stationIdFrom = 0;
	private int stationIdTo = 0;
	private long startTime = 0;  // start of 1st transit event
	private long eventStartTime = 0; // start time of current transit event
	private int count = 0;
	private int transportId = 0;
	private boolean first = true;
	private int polyIndex;
	private GeoPoint previousSample = null;
	private boolean firstOrLastPolyPoint = true;
	private int timerSeconds = 0, lastCorrectedSec = 0;
	private long departureTime = 0;
	private double accelTime = 12, decelTime = 12, duration, stationStopTime = 20;
	private double distanceInDegrees, avgSpeedInDegrees = 0; //generated line on polyline d distance from last sample 
	private double distanceAlong = 0, stationDist = 0;
	private double period = 2; // period of each sample, in seconds
	private Map<Integer, Integer> signalArray = new HashMap<Integer, Integer>();
	private Map<Integer, OverlayItem> osamples = new HashMap<Integer, OverlayItem>();
	private boolean bPaused = false;
	public GeoPoint nextSample = null;
	
	private long accelUpdate = 0, accelUpdate1000 = 0;
//	private DrawerListAdapter adapter;
	private List<GeoPoint> points;
	private List<Integer> correction_points;
	private List<Double> correction_dists;
	private List<Integer> correction_times;
//	private	TransitDrawerLayout dLayout;
//	private	DrawerLayout dLayout;
//	private	ListView dList;
//	private ArrayList<DrawerItem> drawerItems;
//	private ActionBarDrawerToggle mDrawerToggle;
	
    public static final String TAG = TransitSamplingMap.class.getSimpleName();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_map_new, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);

        MMCActivity.customizeTitleBar(this, view, R.string.transitsampling_map_title, R.string.transitcustom_map_title);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		transitDB = new	TransitDatabaseReadWriteNew(this);
		transitStats = new TransitStats();
		
		Intent intent = this.getIntent();
		if(intent.hasExtra("transport_id")) {
			transportId = intent.getIntExtra("transport_id", 0);
			count = transitDB.getTransportCount(transportId);
		}
		stops = transitDB.getTransportStopsFromDB(transportId);
		if(intent.hasExtra("start"))
			stationIdFrom = intent.getIntExtra("start",-1);
		if(intent.hasExtra("stop"))
			stationIdTo = intent.getIntExtra("stop", -1);
		startStationIndex = indexOfStation (stationIdFrom);
		stopStationIndex = indexOfStation (stationIdTo);
		if(startStationIndex == -1 || stopStationIndex == -1) {
			MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "onCreate", "missing startStation or stopStation, exiting map");
			TransitSamplingMap.this.finish();
		}
		
		stationTextView = (TextView) view.findViewById(R.id.stationTextView);
		stationTextView.setText(stops.get(startStationIndex).getName());
		speedImageView  = (ImageView) view.findViewById(R.id.speedImageView);
		speedTextView  = (TextView) view.findViewById(R.id.speedTextView);
		startPauseButton = (Button) view.findViewById(R.id.stopStartButton);
		stopButton = (Button) view.findViewById(R.id.pauseButton);
		latLongTextView = (TextView) view.findViewById(R.id.latLongTextView);
		topactionbarLayout = (RelativeLayout) view.findViewById(R.id.topactionbarLayout);
		informationImageView = (ImageView) view.findViewById(R.id.informationImageView);
		troubleImageView = (ImageView) view.findViewById(R.id.troubleImageView);
		seperator = (ImageView) view.findViewById(R.id.seperator);
		informationLayout = view.findViewById(R.id.informationLayout);
		busyIndicator = (ProgressBar) view.findViewById(R.id.busyIndicator);
		
		reportManager = ReportManager.getInstance(TransitSamplingMap.this);
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		wifiManager =  (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
//		CoverageMapFragment mapFrag = (CoverageMapFragment) getSupportFragmentManager().findFragmentById(R.id.transit_map_fragment);
//		mMapView = ((MyCoverageMapActivity) mapFrag.getHostedActivity()).mMapView;
		mMapView = (MMCMapView) findViewById(R.id.transit_mapview);
		mCoverageOverlay = new CoverageOverlay(this, mMapView, busyIndicator, "transit");
		transitSamplingOverlay = new TransitSamplingOverlay(this, mMapView, busyIndicator, mCoverageOverlay);
		mMapView.getOverlays().add(transitSamplingOverlay);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setChangeListener(transitSamplingOverlay);
		mMapView.getController().setZoom(18);
		myLocationOverlay = new CustomMyLocationOverlay(this, mMapView);
		mMapView.getOverlays().add(myLocationOverlay);  
		mMapView.getOverlays().add(mCoverageOverlay);
//		centerOnLocation(null);
		centerOnStation(startStationIndex);

		transitMath = new TransitSamplingMapMath();
		
		IntentFilter intentFilter = new IntentFilter(CommonIntentBundleKeysOld.ACTION_UPDATE);
		intentFilter.addAction(CommonIntentBundleKeysOld.ACTION_EVENT_UPDATE);
		registerReceiver(broadcastReceiver, intentFilter);
		
		HashSet<Integer> eventsToDisplay = new HashSet<Integer>();
		eventsToDisplay.add(EventType.EVT_DROP.getIntValue());
		eventsToDisplay.add(EventType.EVT_CALLFAIL.getIntValue()); 
//		eventsToDisplay.add(EventType.COV_VOD_NO.getIntValue());
//		eventsToDisplay.add(EventType.COV_VOD_YES.getIntValue());
//		eventsToDisplay.add(EventType.COV_DATA_NO.getIntValue());
//		eventsToDisplay.add(EventType.COV_3G_NO.getIntValue());
//		eventsToDisplay.add(EventType.COV_4G_NO.getIntValue());
		eventsToDisplay.add(EventType.MAN_SPEEDTEST.getIntValue());
		eventsToDisplay.add(EventType.MAN_TRANSIT.getIntValue());

		ArrayList<EventOverlayItem> eventList = new ArrayList<EventOverlayItem>();
		mEventsOverlay = new EventsOverlay(this, mMapView, eventsToDisplay, eventList);
		mEventsOverlay.setTransit(true);
		mMapView.setZoomLevelChangeListener(getEventsOverlay());
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD); 
		gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY); 
		accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//.TYPE_ACCELEROMETER); 
		
		history = new AccelerometerHistory();
		
		//Sliding menu
		LinearLayout menuView = new LinearLayout(TransitSamplingMap.this);
		menuView.setOrientation(LinearLayout.VERTICAL);
		
		ImageView imageView = new ImageView(this); 
		imageView.setImageDrawable(getResources().getDrawable(R.drawable.create_sample_illustrator));		
		menuView.addView(imageView);
		
		TextView textView1 = new TextView(this); 
		textView1.setText(getString(R.string.manualmapping_instruct_sample1));	
		textView1.setPadding(10, 2, 10, 2);
		textView1.setGravity(Gravity.CENTER);
		menuView.addView(textView1);
		addStations ();
		
        //isNextStopFinalStation();
        //getDirection();
        
//    	dLayout = (TransitDrawerLayout) view.findViewById(R.id.drawer_layout);
//        dLayout = (DrawerLayout) view.findViewById(R.id.drawer_layout);
//		dList = (ListView) view.findViewById(R.id.left_drawer);
//		dList.setAdapter(adapter);
//		
//		drawerItems = new ArrayList<DrawerItem>();
//        addDrawerItems();
//        adapter = new DrawerListAdapter(TransitSamplingMap.this, drawerItems);
//        dList.setAdapter(adapter);
//        
//    	// enabling action bar app icon and behaving it as toggle button
////		getActionBar().setDisplayHomeAsUpEnabled(true);
////		getActionBar().setHomeButtonEnabled(true);
//	
//        mDrawerToggle = new ActionBarDrawerToggle(this, dLayout,
//                R.drawable.drawer_icon, //menu toggle icon
//                R.string.transitsampling_map_title, //drawer open - description for accessibility
//                R.string.transitsampling_map_title //drawer close - description for accessibility
//        ){
//            public void onDrawerClosed(View view) {
////                getActionBar().setTitle(mTitle);
//                // calling onPrepareOptionsMenu() to show action bar icons
//                invalidateOptionsMenu();
//            }
// 
//            public void onDrawerOpened(View drawerView) {
////                getActionBar().setTitle(mDrawerTitle);
//                // calling onPrepareOptionsMenu() to hide action bar icons
//                invalidateOptionsMenu();
//            }
//        };
//        dLayout.setDrawerListener(mDrawerToggle);
        
    	setStartStationAndTimeText();
    	popupWindowProblems = popupWindowProblems();
    	// this will also draw the polyline
    	List<GeoPoint> points = getStationlineFromDB(stops.get(startStationIndex).getStationId());
    	
    	setStartResumeButton();
	}
	
//	@Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//		System.out.println("onCreateOptionsMenu");
//        return true;
//    }
//
	public void onMenuClick(View view) {
//		try {
////			onOptionsItemSelected(null);
////			Menu menu = (Menu) this.getMenuInflater();
////			MenuItem item = menu.getItem(menu.size()-1);
////			mDrawerToggle.onOptionsItemSelected(item);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
		showHistory(view);
	}
// 
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//    	System.out.println("onOptionsItemSelected");
//        // toggle drawer on selecting action bar app icon/title
//        if (mDrawerToggle.onOptionsItemSelected(item)) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
// 
//    /***
//     * Called when invalidateOptionsMenu() is triggered
//     */
//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//    	System.out.println("onPrepareOptionsMenu");
//        return super.onPrepareOptionsMenu(menu);
//    }
// 
//    /**
//     * When using the ActionBarDrawerToggle, you must call it during
//     * onPostCreate() and onConfigurationChanged()...
//     */
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        System.out.println("onPostCreate");
//        // Sync the toggle state after onRestoreInstanceState has occurred.
//        mDrawerToggle.syncState();
//    }
// 
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        System.out.println("onConfigurationChanged");
//        // Pass any configuration change to the drawer toggls
//        mDrawerToggle.onConfigurationChanged(newConfig);
//    }
//	
//	public void addDrawerItems() {
//		int size = stops.size();
//        for(int i = 0; i < size; i++) {
//        	TransitInfo stop = stops.get(i);
//        	String name = stop.getName();
//        	drawerItems.add(new DrawerItem(name));
//        }
//	}
	
	public void showHistory(View v) {
//		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
//		if(findViewById(R.id.fragment_container) == null) {
//			// its portrait
////			if(coverageMenu!=null){
////				coverageMenu.dismissWindow();
////			}
////			Intent intent = new Intent(MyCoverage.this, EventHistory.class);
////			startActivity(intent);
//			return;
//		}
//		// its landscape
//		Fragment f = getSupportFragmentManager().findFragmentByTag("eventHistory");
//		if(f == null){
//			f = new EventHistoryFragment();
//			ft.add(R.id.fragment_container, f, "eventHistory").commit();
//			v.setBackgroundColor(0xff3399cc);
//		} else if(f.getView().getVisibility() == View.VISIBLE){
//			// Toast.makeText(this, "Visible", Toast.LENGTH_SHORT).show();
//			ft.hide(f).commit();
//			v.setBackgroundColor(Color.TRANSPARENT);
//		}else{
//			// Toast.makeText(this, "Not Visible", Toast.LENGTH_SHORT).show();
//			ft.show(f).commit();
//			v.setBackgroundColor(0xff3399cc);
//		}
	}

	@Override
	public void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
		//sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_NORMAL);
		//sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		//locationTimeSeries crashes if this is not called
		getEventsOverlay().buffer.updateActivityFromDB(3*3600*1000);
		
		IntentFilter intentFilter = new IntentFilter(CommonIntentBundleKeysOld.ACTION_UPDATE);
		intentFilter.addAction(CommonIntentBundleKeysOld.ACTION_EVENT_UPDATE);
		registerReceiver(broadcastReceiver, intentFilter);
		
//		restoreState(); 
	}
	
	@Override
	public void onPause() {
		super.onPause();
		myLocationOverlay.disableMyLocation();
		sensorManager.unregisterListener(this);
		unregisterReceiver(broadcastReceiver);
		
		saveState();
		
		//Want to be able to sample when screen is off or doing another task such as a phone call or SMS
//		if(timer != null) {
//			timer.cancel();
//			timer.purge();
//			timer = null;
//		}
	}
	
	@Override 
	public void onStop() {
		super.onStop();
		if(timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		if(mCoverageOverlay != null)
			mCoverageOverlay.clear();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			confirmExitPrompt();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		transitSamplingOverlay.deleteOverlays();
		if(timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		this.finish();
	}

	public void saveState() {
		//Samples are already saved so at this point this doesn't need to do anything
	}
	
	public boolean restoreState() {
		Cursor cursor = null;
		try {
			EventOld event = MMCIntentHandlerOld.getTransitEvent();
			if(event == null) 
				return false;
			long startTime = event.getEventTimestamp();		
			ReportManager reportManager = ReportManager.getInstance(TransitSamplingMap.this);			 
			cursor = reportManager.getDBProvider().query(
				UriMatchOld.LOCATIONS.getContentUri(),
				null,
				TablesOld.Locations.TIMESTAMP  + ">=? and (accuracy = -3 or accuracy = -4) ",
				new String[]{ String.valueOf(startTime)},
				TablesOld.Locations.TIMESTAMP + " ASC"
			);
			cursor.moveToFirst();
			int latitude;
			int longitude;
			do {
				//get samples
				latitude = cursor.getColumnIndex(TablesOld.Locations.LATITUDE);
				longitude = cursor.getColumnIndex(TablesOld.Locations.LONGITUDE);
				GeoPoint geoPoint = new GeoPoint(latitude, longitude);
				int signal = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
				
				transitSamplingOverlay.addSample(geoPoint, signal);
			}
			while (cursor.moveToNext());
	
		} catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if(cursor != null)
				cursor.close();
		}
		return true;
	}
	
	public EventsOverlay getEventsOverlay() {
		return mEventsOverlay;
	}

	public void setStartStationAndTimeText() {
		speedImageView.setVisibility(View.GONE);
    	//speedTextView.setText(getString(R.string.transitsampling_started) + " @ "+ startStation);
    	speedTextView.setText(stops.get(startStationIndex).getName());
    	Calendar cal = Calendar.getInstance(); 
	    int hour = cal.get(Calendar.HOUR_OF_DAY); 
	    int min = cal.get(Calendar.MINUTE);
	    int timeOfDay = cal.get(Calendar.AM_PM);
	    String time = "AM";
	    if(timeOfDay == 0) { 
	    	time = "PM";
	    }
	    String minute = String.valueOf(min);
	    if(min < 10) {
	    	minute = "0" + minute;
	    }
    	latLongTextView.setText(hour + ":" + minute + " " + time);	
	}
	
//	public int getTrainSpeed(int stationId) {
//		double meters = transitDB.getDistance(stationId);
//		int minutes = transitDB.getDuration(stationId);
//		double metersPerSecond = meters/(minutes*60);
//		double kmPerHour = metersPerSecond * 3.6;
//		
//		return (int) kmPerHour;
//	}
	
/*	public int getTimeBetweenSamples(int stationId) {
		int minutes = transitDB.getDuration(stationId);
		int seconds = minutes * 60;
		int time = 0;
		if(seconds > 0)
			time = (int) (seconds/SAMPLES_PER_EVENT);
		return time;
	} */
	
//	public double getDistancePerSecond(int stationId, double seconds) {
//		// distance in degrees
//		double distance = transitDB.getDistance(stationId);
//		
//		double distanceBetweenSamples = distance/seconds;
//		
//		return distanceBetweenSamples;
//	}
	
	public void missedStop(View view) {
//		Cancel the event leading up to the stop,
//		and disable the event for the current leg too. 
//		But resume events at the next stop.
		//if(direction == 0)
		//	initSampling();
		cancelEvent();
		updateStops();
		transitSamplingOverlay.deleteOverlays();
		int stationId = stops.get(startStationIndex).getStationId();
		centerOnStation(startStationIndex);//stationTextView.getText().toString());
		points = getStationlineFromDB(stationId); //transitDB.getStationLine(stationFrom); 
		
//		transitSamplingOverlay.makeSamplesInvalid();
		//initSampling();
		transitSamplingOverlay.deleteOverlays();
//		startPauseButton.setText(getString(R.string.transitsampling_start));
//		startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.depart_station_white, 0, 0, 0);
		setStartResumeButton();
	}
	
//	public int getDirection() {
//		first = false;
//		if(stationIdFrom == 0 || stationIdTo == 0) {
//			first = true;
//			stationFrom = transitDB.getStationId(startStation, transportId);
//			stationTo = transitDB.getStationId(stopStation, transportId);
//		}
//		return MmcConstants.DIRECTION_ORIGINAL;
//		int fromSequence = transitDB.getStopSequence(stationFrom, transportId);
//		int toSequence = transitDB.getStopSequence(stationTo, transportId);
//		
//		if(fromSequence < toSequence)
//			return MmcConstants.DIRECTION_ORIGINAL;
//		else if(fromSequence == 0 && toSequence == 0) {
//			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getDirection", "From and to stop_sequence == 0");
//			return -1;
//		}
//		else
//		{
//			if (first) // for first stop in reverse direction, use the station_line from 1 station back
//			{
//				TransitInfo info = stops.get(fromSequence-1);
//				stationFrom = info.getStationId();
//			}
//			return MmcConstants.DIRECTION_REVERSE;
//		}
//	}
	
	public void onExit(View view) { 
		confirmExitPrompt();
		//this.finish();
	}
	
	public void onSettings(View view) {
		Intent intent = new Intent(this, TransitSamplingMap.class);
		startActivityForResult(intent, RESULT_OK);
	}
	
	public void onHistory(View view) {
		Intent intent = new Intent(this, EventHistory.class);
		intent.putExtra("fromTransit", 1);
		startActivity(intent);
	}
	
	public void onPauseClick(View view) {
		bPaused = !bPaused;
//		if (bPaused == true)
//			stopButton.setText("Resume");
//		else
//			stopButton.setText("Pause");
		setStartResumeButton();
	}
	
	public void confirmExitPrompt() {
		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TransitSamplingMap.this);
//		alertBuilder.setTitle(getString(R.string.)); 		
		alertBuilder.setMessage(getString(R.string.transitsampling_confirm_exit));
		alertBuilder.setCancelable(true);
		alertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
//				TransitSamplingMap.this.finish();
				dialog.dismiss();
			}
		});
		alertBuilder.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				
				dialog.dismiss();
        	}
        });
		alertBuilder.setPositiveButton(getString(R.string.GenericText_Yes),  new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				if(timerTask != null) {
					timerTask.cancel();
					timerTask = null;
				}
//				goBackToPickingItinerary();
				cancelEvent();
				showOverview();
				TransitSamplingMap.this.finish();
        	}
		});
		alertBuilder.show();
	}
	
	public void onTrouble(View view) {
//		if(trouble == false) { //TODO put back
//			Toast.makeText(TransitSamplingMap.this, "No problems detected", Toast.LENGTH_SHORT).show();
//			return;
//		}
		showProblems(view);
//		showTroubleAlert();
		temporarilyDisableButton(view);
		troubleImageView.setBackgroundColor(Color.rgb(0,0,0));
	}
	
//	public void showTroubleAlert() {
//		final CharSequence[] items = { getString(R.string.transitsampling_trouble_wrong_way), 
//				getString(R.string.transitsampling_trouble_unscheduled_stop), getString(R.string.transitsampling_trouble_skip),
//				getString(R.string.transitsampling_trouble_abort) };			
//
//		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(TransitSamplingMap.this);
//		alertBuilder.setTitle(getString(R.string.transitsampling_trouble_title)); 		
//		alertBuilder.setCancelable(true);
//		alertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
//			@Override
//			public void onCancel(DialogInterface dialog) {
//				TransitSamplingMap.this.finish();
//			}
//		});
//		alertBuilder.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.dismiss();
//        	}
//        });
//		alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int indexSelected) {	
//				transitStats.incrementIssues();
//				trouble = false;
//				troubleImageView.setBackgroundResource(R.drawable.alert_sample_active);
//				switch(indexSelected) {	        	
//				case 0: //wrong way
//					goBackToPickingItinerary(); //For now these all (almsot all) take back to itinerary page
//					break;
//				case 1: //unscheduled stop
//					goBackToPickingItinerary();
//					break;
//				case 2: //skip
//					missedStop(null);
//					break;
//				case 3: //abort
//					goBackToPickingItinerary();
//					break;
//				}
//				
//				dialog.dismiss();				
//			}
//		});
//		alertBuilder.show();
//	}
	
	public void goBackToPickingItinerary() {
		Intent intent = new Intent(TransitSamplingMap.this, TransitSamplingMain.class);
		startActivity(intent);
		TransitSamplingMap.this.finish(); 
	}
	
	public void showStations(View view) {
		popupWindowStations.showAsDropDown(informationLayout, -5, 0);
	}
	
	public void showProblems(View view) {
		 popupWindowProblems.showAsDropDown(view, -5, 0);
	}
	
	// When the actual travel time between stations is known, re-calculate those locations and save them to the DB
	private boolean finalizeSamples (int iDuration)
	{
		duration = iDuration;
		initSampling ();
		int s;
		GeoPoint loc;
		if (timerTask != null)
		 {
			 timerTask.cancel ();
			 timerTask = null;
		 }
		transitSamplingOverlay.deleteOverlays();
		for (s=0; s<iDuration; s+=period)
		{
			loc = locationAtTime (s, false);
			if (loc == null)  // invalid sampling
				return false;
			placeSamples(loc, true, s);
			//mHandler.obtainMessage(2).sendToTarget();
			//if (loc == null)
			//	return;
		}
		return true;
	}
	
	// Sampling can be initialized either for realtime sampling, or after-correction
	private void initSampling ()
	{
		points = getStationlineFromDB(stationIdFrom); //transitDB.getStationLine(stationFrom); 
		
		int countDownInterval = 2; //Brad asked for a sample to be placed every 2 seconds
		
		distanceInDegrees = transitDB.getDistance(stationIdFrom, transportId);
		avgSpeedInDegrees = distanceInDegrees/duration; //  getDistancePerSecond(stationFrom, duration); //generated line on polyline d distance from last sample 
		distanceAlong = 0;
		timerSeconds = 0;
		polyIndex = 0;
		
		MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "sampleTimer", "Time between samples: " + countDownInterval);
		
//		direction = getDirection();
//	
//		if(direction == MmcConstants.DIRECTION_REVERSE) {
//			polyIndex = points.size()-1;
//		}
//		else {
//			polyIndex = 0;
//		}
		
//		GeoPoint station = transitDB.getStationLocation(stationFrom, transportId);
//		 if(station != null)
//			 transitSamplingOverlay.addStation(station);
//		 GeoPoint intersect = transitDB.getStationIntersect(stationFrom, transportId);
//		 if(intersect != null)
//			 transitSamplingOverlay.addIntersect(intersect);
	}
	private int lastCorrection = 0;
	protected Double correctLocation (Double estimatedDist, int seconds, boolean live)
	{
		Double correctedDistance = 0d;
		Double totalDistance = 0d;
		Double gapDistance = 0d;
		if (live)
		{
			int pts = correction_times.size();
			
			// correct GPS location every 5 seconds, if needed
			if (lastCorrection + 6 < seconds) // && seconds < 17)
			{
				
				Location myloc = myLocationOverlay.getLastFix();
				//if (seconds > 20)
				//	myloc.setLongitude(myloc.getLongitude() + 0.0025);
				if (myloc.getProvider().equals(LocationManager.GPS_PROVIDER) && myloc.getAccuracy() < 30)// && myloc.getTime()+5000 > System.currentTimeMillis())
				{
					// Snap to track
					
					int latitudeE6 = (int)(myloc.getLatitude()*1000000);//myLocationOverlay.getMyLocation().getLatitudeE6();
					int longitudeE6 = (int)(myloc.getLongitude()*1000000);//myLocationOverlay.getMyLocation().getLongitudeE6();
		
					if(latitudeE6 == 0 && longitudeE6 == 0)
						return null;
					GeoPoint mypoint = new GeoPoint(latitudeE6, longitudeE6);
					
					for(int index = 0; index < points.size()-1; index++) {
						 GeoPoint point = points.get(index);
						 GeoPoint nextPoint = points.get(index+1);
						 double distanceToNextPoint = TransitSamplingMapMath.distanceTo(point, nextPoint);
						 totalDistance += distanceToNextPoint;
						 if (index > correction_points.get(pts-1))
							 gapDistance += distanceToNextPoint;
						 else if (index == correction_points.get(pts-1))
							 gapDistance += (distanceToNextPoint - correction_dists.get(pts-1));
						 //points.remove(point);
						 //index --;
					
						 double lAl = TransitSamplingMapMath.distanceTo(point, mypoint);				
						 double lBl = TransitSamplingMapMath.distanceTo(point, nextPoint);
					
						 if(lBl == 0)
						 	 continue;
						 
						 if(lAl < lBl + 0.001) {
							 double lCl = transitMath.findlCl(point, nextPoint, mypoint, lBl);
							 //System.out.println("lAl = " + lAl + ", lBl = " + lBl + ", lCl = " + lCl);
							 if(lCl <= lBl && lCl >= 0) //then not a right triangle
							 {
								 double lDl = transitMath.findlDl(lCl, lAl);
								 //GeoPoint intersect = transitMath.findNextGeoPoint(point, nextPoint, lCl, lBl);
								 
								 if(lDl <= 0.0005)
								 {
									 totalDistance -= distanceToNextPoint;
									 totalDistance += lCl;
									 gapDistance -= distanceToNextPoint;
									 gapDistance += lCl;
									 
									 correctedDistance = lCl;
									 MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "GPS CORRECTION", "fix: " + mypoint.getLatitudeE6() + "," + mypoint.getLongitudeE6() +
											 		" between point0: " + point.getLatitudeE6() + ", " + point.getLongitudeE6() +
											 		"point1: " + nextPoint.getLatitudeE6() + "," + nextPoint.getLongitudeE6());
									 MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "CORRECTED DISTANCE", "lcl: " + lCl + ", corrected: " + correctedDistance + ", esitmate:" + estimatedDist);
										 		
									 double diff = Math.abs(correctedDistance-estimatedDist);
									 
									 if (diff > 0.0008 || polyIndex != index)
									 {
										 double dis = correctedDistance - correction_dists.get(pts-1);
										 if (dis > 0.0008 || correction_points.get(pts-1) != index)
										 {
											 // correct the samples since the last correction
//											 for (int s=lastCorrection; s< seconds; s+= period)
//											 {
//												 correction_times.add(s);
//												 correction_dists.add(correctedDistance);
//												 correction_points.add(index);
//											 }
											 double speed = speedAtTime(seconds, gapDistance, seconds-lastCorrection, false);
											 // is speed realistic (20-120kph)
											 if (gapDistance/(seconds-lastCorrection) < 0.0008)
												 fillCorrectionGap ((int)lastCorrection, seconds, correction_points.get(pts-1), index, correction_dists.get(pts-1), gapDistance);
											 else
												 speed = stationDist/duration; // use original speed if speed was excessive (this could mean user departed late)
											 lastCorrection = seconds;
											 //double speed = totalDistance / seconds;//dis/(seconds - correction_times.get(pts-1));
											 // calculate how long to reach next station at this speed
											 
											 //if (pts > 1)
											 {
												 // this should adjust the remaining speed
												 double remaining = stationDist - totalDistance;
												 double timeremaining = remaining / speed + decelTime/3;
												 duration = seconds + timeremaining;
												 
												 MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, "CORRECTED DURATION", "seconds: " + seconds + ", timeremaining: " + timeremaining + ", duration:" + duration);
													
											 }
											 correction_times.add(seconds);
											 correction_dists.add(correctedDistance);
											 correction_points.add(index);
											 polyIndex = index;
											 transitSamplingOverlay.addGpsLocation(mypoint); // show correction point on map
											 return correctedDistance;
										 }
									 }
									 else
										 break;
								 }
							 }
						 }
					}
				}
			}
		}
		else
		{
			for (int i = 0; i< correction_times.size(); i++)
			{
				if (correction_times.get(i) == seconds)
				{
					polyIndex = correction_points.get(i);
					return correction_dists.get(i);
				}
				else if (i < correction_times.size()-1 && correction_times.get(i+1) > seconds)
				{
//					double dis0 = correction_dists.get(i);
//					double dis1 = correction_dists.get(i+1) - correction_dists.get(i);
//					int sec0 = seconds - correction_times.get(i);
//					int sec1 = correction_times.get(i+1) - correction_times.get(i);
//					double dis = dis0 + dis1 * sec0/sec1;
//					return dis;
					//if ()
				}
				else if (correction_times.get(i) > seconds)
				{
					break;
				}
			}
		}
		return -1d;
	}
	protected GeoPoint locationAtTime (double seconds, boolean live)
	{
		GeoPoint end = points.get(points.size()-1);
//		 if(direction == MmcConstants.DIRECTION_ORIGINAL) {
//			 end = points.get(points.size()-1);
//		 }
//		 else {
//			 end = points.get(0);
//		 }
		 
		 //If first or last point in line, or last or first in line (depending on direction)
//		 if((polyIndex == 0  && firstOrLastPolyPoint) 
//				 || ((polyIndex == points.size() -1) && firstOrLastPolyPoint)) {
//			 nextSample = points.get(polyIndex);
//			 firstOrLastPolyPoint = false;
//		 }
//		 else if (!firstOrLastPolyPoint)
		 if (polyIndex < points.size()-1) {	 
			 GeoPoint lineStart = points.get(polyIndex);
			 GeoPoint lineEnd = points.get(polyIndex + 1);
//			 try {
//				 lineEnd = points.get(polyIndex + direction);
//			 } catch(Exception e) {
////				 if (timertask != null)
////				 {
////					 timertask.cancel ();
////					 timertask = null;
////				 }
//				 MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "sampleTimer", "error getting next polypoint " + e.getMessage());
//				 return null;
//			 }
			 double distanceToNextPoint = TransitSamplingMapMath.distanceTo(lineStart, lineEnd);
			 //MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "sampleTimer", "distance to next point " + distanceToNextPoint);
			 while (distanceToNextPoint <= distanceAlong) {
				 //if the next sample should be on the new "curve", change the slope
				 polyIndex += 1;//direction;
				 if(points.get(polyIndex) == end)
				 {
//					 firstOrLastPolyPoint = true;
//					 if (timertask != null)
//					 {
//						 timertask.cancel ();
//						 timertask = null;
//					 }
					 nextSample = points.get(polyIndex);
					 mHandler.obtainMessage(1).sendToTarget();
					 return nextSample;
				 }
				 lineStart = points.get(polyIndex);
				 lineEnd = points.get(polyIndex + 1);
				 distanceAlong = distanceAlong - distanceToNextPoint;
				 distanceToNextPoint = TransitSamplingMapMath.distanceTo(lineStart, lineEnd);
				 
				 MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "sampleTimer", "polyIndex " + polyIndex);
			 }
			 double speed = speedAtTime(seconds, distanceInDegrees, duration, true);
			 distanceAlong += speed*period;
			 if (live == false && speed > 0.0008)
				 return null;
			 double correctDistance = correctLocation (distanceAlong, (int)seconds, live);
			 if (correctDistance >= 0)
			 {
				 distanceAlong = correctDistance;
				 lineStart = points.get(polyIndex);
				 lineEnd = points.get(polyIndex + 1);
				 lastCorrectedSec = (int)seconds;
				 //distanceToNextPoint = TransitSamplingMapMath.distanceTo(lineStart, lineEnd);
				 //nextSample = transitMath.findNextGeoPoint(lineStart, lineEnd, distanceAlong, distanceToNextPoint);
			 }
			 nextSample = transitMath.findNextGeoPoint(lineStart, lineEnd, distanceAlong);//, distanceToNextPoint);
			 
		 }
		 return nextSample;
	}
	
	protected void fillCorrectionGap (int seconds0, int seconds1, int pointIndex0, int pointIndex1, double dist0, double distGap)
	{
		GeoPoint end = null;
		end = points.get(pointIndex1);
		 
		int pointIndex = pointIndex0;
		double dist = dist0;
		
		for (int s=seconds0+(int)period; s<seconds1; s+= period)
		{
			 GeoPoint lineStart = points.get(pointIndex);
			 GeoPoint lineEnd = points.get(pointIndex + 1);
			 
			 double distanceToNextPoint = TransitSamplingMapMath.distanceTo(lineStart, lineEnd);
			 //MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "sampleTimer", "distance to next point " + distanceToNextPoint);
			 while (distanceToNextPoint <= dist) {
				 //if the next sample should be on the new "curve", change the slope
				 pointIndex += 1;
//				 if(pointIndex == pointIndex1)
//				 {
//					 firstOrLastPolyPoint = true;
//
//					 nextSample = points.get(polyIndex);
//					 mHandler.obtainMessage(1).sendToTarget();
//					 return nextSample;
//				 }
				 lineStart = points.get(pointIndex);
				 if (pointIndex > points.size()-2)
					 return;
				 lineEnd = points.get(pointIndex + 1);
				 dist = dist - distanceToNextPoint;
				 distanceToNextPoint = TransitSamplingMapMath.distanceTo(lineStart, lineEnd);
			 }
			 
			 //nextSample = transitMath.findNextGeoPoint(lineStart, lineEnd, distanceAlong, distanceToNextPoint);
			 correction_times.add(s);
			 correction_dists.add(dist);
			 correction_points.add(pointIndex);
			 //
			 //if (s>seconds0+(int)period)
			 {
				 OverlayItem sample = osamples.get(s);
				 GeoPoint point = transitMath.findNextGeoPoint(lineStart, lineEnd, dist);
				 Integer signal = signalArray.get(s);
				 transitSamplingOverlay.moveSample(sample, point, signal); 
			 }
			 if (seconds0 < 3) // from a stop
				 dist += speedAtTime(s, distGap, seconds1-seconds0, false)*period;
			 else
				 dist += distGap/(seconds1-seconds0)*period;//speedAtTime(s, distGap, seconds1-seconds0, false)*period;
			 
		 }
		 
	}
	
	private double speedAtTime (double seconds, double dist, double dur, boolean bDecel)
	{
		if (bPaused)
			return 0;
		double aTime = accelTime; // time in seconds to accelerate to top speed
		double dTime = decelTime; // time in seconds to decelerate to top speed
		
		if ((accelTime + decelTime) > dur)
		{   // kep same ratio of accel to decel
			aTime = accelTime * dur / (accelTime + decelTime);
			dTime = dur - accelTime;
		}
		double maxTime = dur - aTime - dTime; // time at max speed ( t2 )
		// d = 1/2 a1 t1^2 + v2 t2 + v2 t3 + 1/2 a3 t3^2 
		// a1 = accelation, v2 = max speed, a3 = deceleration
		// t1 = accelTime, t3 = decelTime, t2 = maxTime (time at max speed)
		// leads to this VMax equation (solve for v2, knowing v2 = a1*t1 = -a3*t3)
		double Vmax = dist / (aTime/2 + dTime/2 + maxTime);
		
		if (bDecel == false)
		{
			if (seconds == dur )
			{
				maxTime = dur - aTime;
				Vmax = dist / (aTime/2 + maxTime);
				aTime = dur;
			}
			else
			{
				aTime = accelTime;
				maxTime = seconds - accelTime;
				Vmax = dist / dur;//(accelTime/2 + maxTime);
			}
		}
		double v;
		
		if (seconds < aTime)
		{
			v = Vmax * seconds / aTime;
		}
		else if (seconds < dur - dTime || bDecel == false)
		{
			v = Vmax;
		}
		else if (seconds < dur)
		{
			v = Vmax * (dur - seconds) / (dTime);
		}
		else
			v = Vmax / 3; // just in case the location is a little short of the finish
		return v;
	}

	public void placeSamples(GeoPoint mapCenter, boolean save, int sec) {
		//GeoPoint geoPoint = (GeoPoint) mMapView.getMapCenter();
		//Draw this sample
		Integer signal = 0;
		if (mapCenter == null)
			return;
		
		//Save this sample in the table
		if (save)
		{	
			signal = signalArray.get(sec);
			
			//transitSamplingOverlay.addSample(mapCenter, signal); 
			saveSample(mapCenter, sec);
			//Keep a count of samples to show in Overview should always be equal to SAMPLES_PER_EVENT
			transitStats.increaseSamplesCollected();
		}
		else
		{	try {
				signal = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
				signalArray.put(sec, signal);
			} catch(Exception e) {
				MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "placeSamples", e.getMessage());
			}
		}
		osamples.put(sec, transitSamplingOverlay.addSample(mapCenter, signal)); 
	}
	
	public void buttonControls(View view) {
		if(first == true) {
			//startEvent();
			informationImageView.setBackgroundResource(R.drawable.depart_station_white);
		}
		
		if(startTime == 0) {
			startTime = System.currentTimeMillis();
			transitStats.setStartTime(startTime);
		}
		
		
		if(view.getId() == startPauseButton.getId()) {
			//Start sampling
			if(startPauseButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_start_title)).toString())) { 
				setStartTime();
				startEvent();
				if(isNextStopFinalStation()) 
					setPauseStopButtons(true);
				else
					setPauseStopButtons(false);
				departureTime = System.currentTimeMillis();
				sampleTimer();
			} 
			//Pause hit
			else if(startPauseButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_pause_title)).toString())) {
				onPauseClick(null);
				setStartResumeButton();
			}
			//Overview hit
			else if(startPauseButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_overview_title)).toString())) {
				//go to overview
				Intent overviewIntent = new Intent(TransitSamplingMap.this, TransitSamplingOverview.class);
				overviewIntent.putExtra("stats", transitStats);
				startActivity(overviewIntent);
				this.finish();
			}
			//Resume hit
			else if(startPauseButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_resume_title)).toString())) {
				onPauseClick(null);
				if(isNextStopFinalStation()) {
					setPauseStopButtons(true);
				}
				setPauseStopButtons(false);
			}
		}
		else if(view.getId() == stopButton.getId()) {
			//Stop hit
			if(stopButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_stop_title)).toString())) {
				boolean valid = true;//validateTravelTime();
				setStartResumeButton();
				
				if(timerTask != null) {
					timerTask.cancel();
					timerTask = null;
				}
				valid = finalizeSamples (timerSeconds);
				if (valid == false)
					cancelEvent ();
				updateStops();
				centerOnStation(startStationIndex);
				points = getStationlineFromDB(stationIdFrom); //transitDB.getStationLine(stationFrom); 
				
			}
			//final stop
			else if(stopButton.getText().toString().equals(Html.fromHtml(getString(R.string.transitsampling_stop_title_final)).toString())) {
				finalizeSamples (timerSeconds);
				centerOnStation(stopStationIndex);
				finalStop();
				//validateTravelTime();
				
				if(timerTask != null) {
					timerTask.cancel();
					timerTask = null;
				}
				setOverviewButton();
			}
		}
		
		
//		if(startPauseButton.getText().equals(Html.fromHtml(getString(R.string.transitsampling_pause_title))) &&
//				view.getId() == startPauseButton.getId()) {
//			onPauseClick(null);
//			setStartResumeButton();
//			return;
//		}
//		else if(stopButton.getText().equals(Html.fromHtml(getString(R.string.transitsampling_stop_title))) &&
//				view.getId() == stopButton.getId()) {
//			
//			boolean valid = true;//validateTravelTime();
//			setStartResumeButton();
//			
//			if(timerTask != null) {
//				timerTask.cancel();
//				timerTask = null;
//			}
//			//int dur = (int)((System.currentTimeMillis() - departureTime) / 1000);
//			int dur = timerSeconds;
//			finalizeSamples (dur);
//			updateStops();
//			if(valid) {
//				centerOnStation(stationTextView.getText().toString());
//				points = getStationlineFromDB(stationFrom); //transitDB.getStationLine(stationFrom); 
//				
//			}
//		}
//		
//		//Start sampling
//		if(startPauseButton.getText().equals(Html.fromHtml(getString(R.string.transitsampling_start_title)))) { 
//			setStartTime();
//			startEvent();
////			startPauseButton.setText(getString(R.string.transitsampling_stop));
////			startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.trip_pause_active, 0, 0, 0);
//			if(isNextStopFinalStation()) {
////				startPauseButton.setText(getString(R.string.transitsampling_map_laststation));
////				startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.depart_station_white, 0, 0, 0);
//				setPauseStopButtons(true);
//			}
//			setPauseStopButtons(false);
//			departureTime = System.currentTimeMillis();
//			sampleTimer();
//		} 
//		//Stop sampling -- reached next station
//		else if(stopButton.getText().equals(Html.fromHtml(getString(R.string.transitsampling_stop_title)))) {
//			boolean valid = true;//validateTravelTime();
//			
////			startPauseButton.setText(getString(R.string.transitsampling_start));
////			startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.depart_station_white, 0, 0, 0);
//			setStartResumeButton();
//			
//			if(timerTask != null) {
//				timerTask.cancel();
//				timerTask = null;
//			}
//			//int dur = (int)((System.currentTimeMillis() - departureTime) / 1000);
//			int dur = timerSeconds;
//			finalizeSamples (dur);
//			updateStops();
//			if(valid) {
//				centerOnStation(stationTextView.getText().toString());
//				points = getStationlineFromDB(stationFrom); //transitDB.getStationLine(stationFrom); 
//				
//			}
//		}
//		//Reached last station
//		else if(stopButton.getText().equals(Html.fromHtml(getString(R.string.transitsampling_stop_title_final)))) {
//			finalStop();
//			validateTravelTime();
//			//Stop sampling, setup button for overview selection
//			if(timerTask != null) {
//				timerTask.cancel();
//				timerTask = null;
//			}
////			startPauseButton.setText(getString(R.string.transitsampling_map_seeoverview));
////			//There is no longer an icon on the button
////			startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
//			setOverviewButton();
//		}
//		else if(startPauseButton.getText().equals(getString(R.string.transitsampling_map_seeoverview))) {
//			//go to overview
//			Intent overviewIntent = new Intent(TransitSamplingMap.this, TransitSamplingOverview.class);
//			overviewIntent.putExtra("stats", transitStats);
//			startActivity(overviewIntent);
//			this.finish();
//		}
	}
	
	public boolean isNextStopFinalStation() {
		return (startStationIndex == stopStationIndex-1);
	
//		int currentStationIndex = -2;
//		int endIndex = -2;
//		
//		for(int i = 0; i < stops.size(); i++) {
//			TransitInfo info = stops.get(i);
//			
//			String infoName = info.getName();
//			String currentName = (String) stationTextView.getText();
//			if(infoName.equals(currentName)) {
//				//Found the station we're on right now
//				currentStationIndex = i;
//			}
//			
//			if(info.getName().equals(stopStation)) {
//				endIndex = i;
//			}
//		}
//		
//		for(int i = 0; i < stops.size(); i++) {
//			//int direction = getDirection();
//			if(direction == 0) {
//				MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "isNextStopFinalStation", "Error finding direction");
//				break;
//			}
//			
//			if(i == currentStationIndex + direction && i == endIndex) {
//				return true;
//			}
//		}
//		return false;
	}
	
	public void updateLocation() {
		GeoPoint geoPoint = (GeoPoint) mMapView.getMapCenter();
		
		int latitudeE6 = geoPoint.getLatitudeE6();
		int longitudeE6 = geoPoint.getLongitudeE6();
		double latitude = (double) latitudeE6/1000000;
		double longitude = (double) longitudeE6/1000000;
		DecimalFormat decimalFormat = new DecimalFormat("#.######");
		String result = "@ " + decimalFormat.format(latitude) + ", ";
		result +=  decimalFormat.format(longitude);
		
		//latLongTextView.setText(result);
	}
	
	public void startEvent() {		
		eventStartTime = System.currentTimeMillis();
		GeoPoint geoPoint = mMapView.getMapCenter();
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_TRANSIT_START);  
		intent.putExtra("latitude", geoPoint.getLatitudeE6());
		intent.putExtra("longitude", geoPoint.getLongitudeE6());
		sendBroadcast(intent);
	}
	
	public void cancelEvent() {
		if(timerTask != null) {
			try {
				timerTask.cancel();
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			timerTask = null;
		}
		
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_TRANSIT_CANCEL);
		sendBroadcast(intent);
	}
	
	public void stopEvent() {
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_TRANSIT_END);
		
		int stationTo = 0;
		if (startStationIndex < stops.size()-1)
			stationTo = stops.get(startStationIndex+1).getStationId();
		intent.putExtra("stationTo", stationTo);
		intent.putExtra("stationFrom", stationIdFrom);
		intent.putExtra("duration", duration);
		int corrected = 0;
		if (lastCorrectedSec > 0)
			corrected = 1;
		intent.putExtra("corrected", corrected);
		String strAccel = history.toString(eventStartTime, System.currentTimeMillis());
		strAccel = "4,time,x,y,r\r\n" + strAccel;
		intent.putExtra("accelerometer", strAccel);
		sendBroadcast(intent);
	}
	
	public void addStations ()
	{
		for(int i = 0; i < stops.size(); i++) 
		{
			TransitInfo info = stops.get(i);
			GeoPoint station = new GeoPoint(info.getGeoPoint().getLatitudeE6(), info.getGeoPoint().getLongitudeE6());
			transitSamplingOverlay.addStation(station);
		}
	}
	public void updateStops() {
		int currentStationIndex = -2;
		stopEvent();
		//direction = 1;
		startStationIndex ++;
		if (startStationIndex < stops.size())
		stationIdFrom = stops.get(startStationIndex).getStationId();
		stationTextView.setText(stops.get(startStationIndex).getName());
		updateStats(1);
		
		
//		for(int i = 0; i < stops.size(); i++) {
//			TransitInfo info = stops.get(i);
//			
//			String infoName = info.getName();
//			String currentName = (String) stationTextView.getText();
//			if(infoName.equals(currentName)) {
//				//Found the station we're on right now
//				currentStationIndex = i;
////				if(adapter != null)
////					adapter.setCurrent(i);
//			}
//		}
//		
//		for(int i = 0; i < stops.size(); i++) {
//			TransitInfo info = stops.get(i);
//			
//			//int direction = getDirection();
////			if(direction == 0) {
////				MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "updateStops", "Error finding direction");
////				break;
////			}
//			
//			if(i == (currentStationIndex + direction) //If we found the next station. Direction will equal -1 or 1
//					&& currentStationIndex >= 0) //If we found the current station
//			{ 
//				//Update text to show new station name
//				stationTextView.setText(info.getName());
//				
//				//Update station IDs
//				stationFrom = 0;//info.getStationId();
//				stationTo = 0;//stops.get(i + direction).getStationId();
//				
//				startStation = info.getName();
//				getDirection();
//				//List<GeoPoint> points = getStationlineFromDB(stationFrom);
//
//				//Put completed icon with station in sliding menu
////				adapter.setFinished(currentStationIndex);
//				
//				//startEvent();
//				updateStats(direction);
//				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "updateStops", "New stationFrom: " + info.getName());
//				break;
//			}
//		}
		
		//cleanup accelerometer data - previous data gets sent with event
		history.clearAccelerometerHistory();
	}
	
	public void setStartTime() {
		stops.get(startStationIndex).setStartTime(System.currentTimeMillis());
//		for(int i = 0; i < stops.size(); i++) {
//			TransitInfo info = stops.get(i);
//			
//			String infoName = info.getName();
//			String currentName = (String) stationTextView.getText();
//			if(infoName.equals(currentName)) {
//				//update end time for the previous stationline
//				stops.get(i).setStartTime(System.currentTimeMillis());
//			}
//		}
	}
	
	public void finalStop() {
		stopEvent();
		stationTextView.setText(stops.get(stopStationIndex).getName());
		transitDB.increaseTransportCount(transportId, count);
		transitStats.setUsage(count+1);
		updateStats(1);
		stationIdFrom = stationIdTo;
		history.clearAccelerometerHistory();
		
//		for(int i = 0; i < stops.size(); i++) {
//			//int direction = getDirection();
//			if(direction == 0) {
//				MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "finalStop", "Error finding direction");
//				break;
//			}
//			
//			TransitInfo info = stops.get(i);
//			if(info.getName().equals(stopStation)) {
//				count += 1;
//				stationTextView.setText(info.getName());
//				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "finalStop", "Reached final station: " + info.getName() + ", route count = " + count);
//				
//				transitDB.increaseTransportCount(transportId, count);
//				transitStats.setUsage(count);
//				//Put finished icon with station in sliding menu 
////				adapter.setFinished(i);
//				updateStats(direction);
//				//Done to and from are the same
//				stationFrom = stationTo;
//				history.clearAccelerometerHistory();
//				break;
//			}
//		}
	}
		
	public void updateStats(int direction) {
//		double increase = 0;
//		if(direction == MmcConstants.DIRECTION_REVERSE) {
//			increase = transitDB.getDistance(stationTo, transportId);
//		}
//		else {
//			increase = transitDB.getDistance(stationFrom, transportId);
//		}
//		
//		if(stationFrom != stationTo)
		transitStats.increaseDistance(distanceInDegrees);
		transitStats.increaseTime(System.currentTimeMillis());
		transitStats.incrementStationCount();
	}

	public void calculateWifiSpeed() {
//		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//		if (wifiInfo != null) {
//		    Integer linkSpeed = wifiInfo.getLinkSpeed(); //measured using WifiInfo.LINK_SPEED_UNITS - Mbps
//		    if(linkSpeed < 0)
//		    	linkSpeed = 0;
//		    speedTextView.setText(String.valueOf(linkSpeed) + " Mb/s");
//		    speedImageView.setVisibility(View.VISIBLE);
//		    speedImageView.setBackgroundResource(signalStrengthIcon());
//		}
	}
	
//	public boolean validateTravelTime() {
//		int index = -1;
//		for(int i = 0; i < stops.size(); i++) {
//			TransitInfo info = stops.get(i);
//			
//			String infoName = info.getName();
//			String currentName = (String) stationTextView.getText();
//			if(infoName.equals(currentName)) {
//				index = i;
//			}
//		}
//		
//		if(index == -1)
//			return false;
//		
//		TransitInfo info = stops.get(index);
//		long startTime = info.getStartTime();
//		int expectedMinutes = transitDB.getDuration(stationFrom, transportId);
//		long endTime = System.currentTimeMillis();
//		
//		double difference = endTime - startTime;
//		difference = Math.floor(difference / 1000);
//		//Seconds
//		difference = Math.floor(difference / 60);
//		//Minutes
//		int actualMinutes = (int) difference % 60;
//		
//		if (actualMinutes <= (expectedMinutes + 1) && actualMinutes >= (expectedMinutes -1)) {
//			return true;
//		}
//		else {
//			createProblem(index);
//			return false;
//		}
//	}
	
	public void createProblem(int index) {
		//Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		//vibrator.vibrate(500);
		troubleImageView.setBackgroundColor(Color.rgb(54,180,227));
	}
	
//	public int signalStrengthIcon() {		
//		int signalStrength = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
//		
//		if(signalStrength <= -121 || signalStrength == 0)
//			return R.drawable.mapping_marker_grey_default;
//		
//		TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
//		
//		int percent = -1;
//		if(telephonyManager.getNetworkType() == TelephonyManager.NETWORK_TYPE_EDGE) { //2g
//			percent = Math.abs(getPercentageFromDbm(signalStrength, -50));
//		}
//		else
//			percent = Math.abs(getPercentageFromDbm(signalStrength, -70));
//		if(percent <= 0)
//			return R.drawable.mapping_marker_grey_default;
//		else if(percent >= 1 && percent <= 19) 
//			return R.drawable.mapping_marker_red;
//		else if(percent >= 20 && percent <= 39) 
//			return R.drawable.mapping_marker_orange;
//		else if(percent >= 40 && percent <= 59) 
//			return R.drawable.mapping_marker_yellow;
//		else if(percent >= 60 && percent <= 79) 
//			return R.drawable.mapping_marker_light_green;		
//		else if(percent >= 80) 
//			return R.drawable.mapping_marker_dark_green;	
//		
//		return R.drawable.mapping_marker_grey_default;
//	}
//	
//	public int getPercentageFromDbm(int dbm, int MAX_SIG) {
//		int MIN_SIG = -120;
//		
//		if(dbm == -1){
//			return 0;
//		}
//		
//		return 100 * (dbm - MIN_SIG) / (MAX_SIG - MIN_SIG);
//	}
	
	public void centerOnLocation(View view) {
		Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(lastKnownLocation != null) {
			int latitudeE6 = (int)(lastKnownLocation.getLatitude() * 1000000.0);
			int longitudeE6 = (int)(lastKnownLocation.getLongitude() * 1000000.0);
			if(latitudeE6 == 0 && longitudeE6 == 0)
				return;
			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "centerOnLocation", "new location " + latitudeE6 + ", " + longitudeE6);
		}
	}	
	
//	public GeoPoint newGPSLocation() {
//		/*if(myLocationOverlay.getMyLocation() != null) {
//			int latitudeE6 = myLocationOverlay.getMyLocation().getLatitudeE6();
//			int longitudeE6 = myLocationOverlay.getMyLocation().getLongitudeE6();
//
//			if(latitudeE6 == 0 && longitudeE6 == 0)
//				return null;
//			return new GeoPoint(latitudeE6, longitudeE6);
//		}
//		else if(myLocationOverlay.getLastFix() != null) {
//			int latitudeE6 = (int)(myLocationOverlay.getLastFix().getLatitude() * 1000000.0);
//			int longitudeE6 = (int)(myLocationOverlay.getLastFix().getLongitude() * 1000000.0);
//			
//			if(latitudeE6 == 0 && longitudeE6 == 0)
//				return null;
//			return new GeoPoint(latitudeE6, longitudeE6);
//		}
//		else { */
//			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//			if(lastKnownLocation != null) {
//				int latitudeE6 = (int)(lastKnownLocation.getLatitude() * 1000000.0);
//				int longitudeE6 = (int)(lastKnownLocation.getLongitude() * 1000000.0);
//				
//				if(latitudeE6 == 0 && longitudeE6 == 0)
//					return null;
//				GeoPoint point = new GeoPoint(latitudeE6, longitudeE6);
//				saveGPSSample(point);
//				return point;
//			}
////		}
//		return null;
//	}	

	public void centerOnStation(int stationIndex) {
		//if(stops == null || stops.size() == 0)
		//	centerOnLocation(null);
		if (stops == null || stationIndex >= stops.size())
			return;
		TransitInfo info = stops.get(stationIndex);
		int latitudeE6 = info.getGeoPoint().getLatitudeE6();
		int longitudeE6 = info.getGeoPoint().getLongitudeE6();
		mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
		//MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "centerOnFirstStation", "station location " + latitudeE6 + ", " + longitudeE6);
		popupWindowStations = popupWindowStations();
	}
	
	public Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			if(nextSample != null) {
				previousSample = nextSample;
				mMapView.getController().setCenter(nextSample);
				placeSamples(nextSample, false, msg.what);
			}
			else {
				//TODO: do handling
			}
			 
			calculateWifiSpeed();
			//sliding menu closes itself when map animates, this tries to keep it open if it is open
//			reShowMenu();
			//This updates location textview
			updateLocation();
			
			// hi-lite problem button if timer ran too long before pressing Arrived
			if (timerSeconds - 60 > duration)
			{
				timerTask.cancel();
				 createProblem (1);
			}
		}
	};
	
	public void setStartResumeButton() {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT, 5.0f);
		startPauseButton.setLayoutParams(params);
		stopButton.setVisibility(View.GONE);
		startPauseButton.setBackgroundColor(Color.rgb(15, 148, 35));
		startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable._train_start_sampling, 0, 0, 0);
		
		String title = getString(R.string.transitsampling_start_title);
		if(bPaused) {
			title = getString(R.string.transitsampling_resume_title);
			startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable._train_green_light, 0, 0, 0);
		}
		else if (startStationIndex == stops.size()-1)
		{
			setOverviewButton();
			return;
		}
		startPauseButton.setText(Html.fromHtml(title));
	}
	
	public void setPauseStopButtons(boolean finalStop) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			0,
//            LayoutParams.MATCH_PARENT, 0.45f);
		 LayoutParams.MATCH_PARENT, 2.4f);
		startPauseButton.setLayoutParams(params);
		stopButton.setLayoutParams(params);
		stopButton.setVisibility(View.VISIBLE);
		startPauseButton.setBackgroundColor(Color.rgb(51, 51, 51));
		stopButton.setBackgroundColor(Color.rgb(51, 51, 51));
		
		String title = getString(R.string.transitsampling_pause_title);
		startPauseButton.setText(Html.fromHtml(title));
		title = getString(R.string.transitsampling_stop_title);
		startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable._train_red_light, 0, 0, 0);
		stopButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.train_stop_at_station, 0, 0, 0);
		if(finalStop) {
			title = getString(R.string.transitsampling_stop_title_final);
			//stopButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.train_icon_gray, 0, 0, 0);
		}
			
		stopButton.setText(Html.fromHtml(title));
		
		LinearLayout.LayoutParams seperatorParams = new LinearLayout.LayoutParams(0,
			 LayoutParams.FILL_PARENT, 0.04f); //w h weight
		seperator.setLayoutParams(seperatorParams);
	}
	
	public void setOverviewButton() {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
	            LayoutParams.MATCH_PARENT, 1.0f);
		startPauseButton.setLayoutParams(params);
		stopButton.setVisibility(View.GONE);
		startPauseButton.setBackgroundColor(Color.rgb(51, 153, 204)); //#3399cc
		
		String title = getString(R.string.transitsampling_overview_title);
		startPauseButton.setText(Html.fromHtml(title));
		startPauseButton.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.history_white, 0, 0, 0);
	}
	
	public void sampleTimer() {
		duration = transitDB.getDuration(stationIdFrom, transportId) - stationStopTime;
		bPaused = false;
		correction_points = new ArrayList<Integer>();
		correction_dists = new ArrayList<Double>();
		correction_times = new ArrayList<Integer>();
		correction_points.add (0);
		correction_dists.add (0d);
		correction_times.add(0);
		lastCorrection = 0;
		lastCorrectedSec = 0;
		Integer signal = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
		signalArray.put(0, signal);
		
		stationDist = transitDB.getDistance(stationIdFrom, transportId);
		// Adjust duration based on actual time of previous leg, if speed was realistic (30 - 120 kph)
		if (avgSpeedInDegrees > 0.00003 && avgSpeedInDegrees < 0.0003)
		{
			double projectedDuration = stationDist / avgSpeedInDegrees;
			if ((stationDist < 0.01 && distanceInDegrees < 0.01) || (stationDist > 0.01 && distanceInDegrees > 0.01))
			{
				// split the difference
				duration = (duration+projectedDuration*2)/3;
			}
		}
		initSampling ();
		if(points == null || points.size() == 0) {
			return;
		}
		int t = 0;

		if(timerTask == null) {
			 new Timer().scheduleAtFixedRate(timerTask = new TimerTask() {

				 @Override
				 public void run (){// onTick(long millisUntilFinished) {	
					
					 TransitSamplingMap.this.locationAtTime (timerSeconds, true);
					 if (!bPaused)
						 timerSeconds += period;
					 
					 mHandler.obtainMessage((int)(timerSeconds)).sendToTarget();

				 }
			 }, 0, (int)(1000 * period));
		}
	}

	public void saveSample(GeoPoint geoPoint, int sec) {
		Location location = new Location("");	
		location.setLatitude(geoPoint.getLatitudeE6()/1000000.0);
		location.setLongitude(geoPoint.getLongitudeE6()/1000000.0);
		location.setAccuracy(-3); //to indicate transit
		if (lastCorrectedSec == sec)
			location.setAccuracy(-4);//to indicate gps corrected transit
		location.setTime(eventStartTime+sec*1000);
		ContentValues values = ContentValuesGeneratorOld.generateFromLocation(location, 0, 0);
		reportManager.getDBProvider().insert(TablesEnumOld.LOCATIONS.getContentUri(), values);
	}

	public void saveGPSSample(GeoPoint geoPoint) {
		Location location = new Location("");	
		location.setLatitude(geoPoint.getLatitudeE6()/1000000.0);
		location.setLongitude(geoPoint.getLongitudeE6()/1000000.0);
		location.setAccuracy(4); //to indicate transit
		ContentValues values = ContentValuesGeneratorOld.generateFromLocation(location, 0, 0);
		reportManager.getDBProvider().insert(TablesEnumOld.LOCATIONS.getContentUri(), values);
	}
	
	public List<GeoPoint> getStationlineFromDB(int stationId) {
		List<GeoPoint> points = transitDB.getStationLine(stationId, transportId);

		if(points == null || points.size() == 0) {
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "addPolygon", "addPolygon failed, points size = 0");
			return null;
		}
		else {
			transitSamplingOverlay.setPolygon(points);
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "addPolygon", "addPolygon successful, points size = " + points.size());
			return points;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}
	
	// Constants for the low-pass filters
	private float timeConstant = 0.8f;//0.518f;
	private float alpha = 0.5f;
	private float dt = 0;
	private int countLP = 0;
	// Gravity and linear accelerations components for the
	// Wikipedia low-pass filter
	private float[] gravityLP = new float[]
	{ 0, 0, 0 };
	 
	private float[] linearAccelerationLP = new float[]
	{ 0, 0, 0 };
	 
	// Raw accelerometer data
	private float[] inputLP = new float[]
	{ 0, 0, 0 };
	 
	// Timestamps for the low-pass filters
	private float timestampLP = System.nanoTime();
	private float timestampOldLP = System.nanoTime();
	/**
	* Add a sample.
	* 
	* @param acceleration
	*            The acceleration data.
	* @return Returns the output of the filter.
	*/
	private float[] lowpassFilter (float[] acceleration)
	{
		// Get a local copy of the sensor values
		System.arraycopy(acceleration, 0, this.inputLP, 0, acceleration.length);
		 
		timestampLP = System.nanoTime();
		 
		// Find the sample period (between updates).
		// Convert from nanoseconds to seconds
		dt = 1 / (countLP / ((timestampLP - timestampOldLP) / 1000000000.0f));
		 
		countLP++;
		         
		alpha = timeConstant / (timeConstant + dt);
		alpha = 0.1f;
		 
		gravityLP[0] = alpha * gravityLP[0] + (1 - alpha) * inputLP[0];
		gravityLP[1] = alpha * gravityLP[1] + (1 - alpha) * inputLP[1];
		gravityLP[2] = alpha * gravityLP[2] + (1 - alpha) * inputLP[2];
		 
		linearAccelerationLP[0] = inputLP[0] - gravityLP[0];
		linearAccelerationLP[1] = inputLP[1] - gravityLP[1];
		linearAccelerationLP[2] = inputLP[2] - gravityLP[2];
		
		return gravityLP;
	}
	
	//@Override
	public void onSensorChanged2(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float[] force = lowpassFilter (event.values);
			float x = force[0];
	        float y = force[1];
	        float z = force[2];
	        
	        double resultant = Math.sqrt(x*x+y*y+z*z);
	        double acc = Math.sqrt(resultant*resultant-10*10);
			String txt= "";
	        txt += "Rt= " + (int)(resultant*1000);
			txt += " acc= " + (int)(acc*1000);
			speedTextView.setText(txt);
			
			//txt = "x= " + (int)(event.values[0]*1000);
			//txt += " y= " + (int)(event.values[1]*1000);
			//txt += " z= " + (int)(event.values[2]*1000);
			//latLongTextView.setText(txt);
			
			if (accelUpdate1000 + 1000 < System.currentTimeMillis()) {
				accelUpdate1000 = System.currentTimeMillis();
				
				latLongTextView.setText(txt);	
				history.updateAccelerometerHistory((int)(resultant*100), (int)(acc*100), 0);
			}
		}
	}

	private float[] magnetic = new float[4];
	private float[] gravity = new float[4];
	private float[] gravityAtRest = new float[4];
	
	float [] linearAcceleration = new float[4];
	private List<Integer> samples = new ArrayList<Integer>();
	
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor sensor = event.sensor;
		int i;
		
//    	if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//    	{
//    		for (i=0;i<3;i++)
//    			magnetic[i] = event.values[i];
//    	}
//    	else if (sensor.getType() == Sensor.TYPE_GRAVITY) 
//		{
//    		for (i=0;i<3;i++)
//    			gravity[i] = event.values[i];
//    		if (timerTask == null)
//    		{
//    			for (i=0;i<3;i++)
//        			gravityAtRest[i] = event.values[i];
//    		}
//		}
		//else if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
		//else 
		if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {// && gravity != null && magnetic != null) {
			//if (accelUpdate + 100 < System.currentTimeMillis()) 
			{
				accelUpdate = System.currentTimeMillis();
				
				float[] force = lowpassFilter (event.values);
				for (i=0;i<3;i++)
					linearAcceleration[i] = force[i];
//				if (timerTask == null)
//	    		{
//	    			for (i=0;i<3;i++)
//	        			gravityAtRest[i] = event.values[i];
//	    		}
		        float x = event.values[0];
		        float y = event.values[1];
		        float z = event.values[2];
		        
		        samples.add((int)(x*100));
	            samples.add((int)(y*100));
	            samples.add((int)(z*100));
		        //history.updateAccelerometerHistory(x, y, z);
		        
		        String txt= "";
		        txt += "y= " + (int)(y*100);
				txt += " x= " + (int)(x*100);
				txt += " z= " + (int)(z*100);
				
				if (accelUpdate1000 + 1000 < System.currentTimeMillis()) {
					accelUpdate1000 = System.currentTimeMillis();
					int n=0, X=0, Y=0, Z = 0, Rt = 0, count = samples.size();
					if (count > 2)
					{
						for (n=0; n<count; n+=3)
						{
							X += samples.get(n);
							Y += samples.get(n+1);
							Z += samples.get(n+2);
						}
						// average, count is 3 times too high
						X = X*3 / count;
						Y = Y*3 / count;
						Z = Z*3 / count;
						Rt = (int)Math.sqrt(X*X+Y*Y+Z*Z);
						
						//latLongTextView.setText("X=" +X + " Y=" + Y + " R" + Rt);	
						history.updateAccelerometerHistory(X, Y, Z);
						samples.clear();
					}
				}
				
				
//				speedTextView.setText(txt);
//				
////				float[] R = new float[9];
////	            float[] I = new float[9];
////	            
////	            boolean b = SensorManager.getRotationMatrix(R, I, gravity, magnetic);
//	            
//	            float[] A_W = new float[4];
//	            float[] R = new float[16];
//	            float[] I = new float[16];
//	            float[] RINV = new float[16];    
//	            
//	            double gravMag = Math.sqrt(gravity[0]*gravity[0]+gravity[1]*gravity[1]+gravity[2]*gravity[2]);
//	            double gravMagAtRest = Math.sqrt(gravityAtRest[0]*gravityAtRest[0]+gravityAtRest[1]*gravityAtRest[1]+gravityAtRest[2]*gravityAtRest[2]);
//	            float[] gravityCorrect = new float[4];
//	            for (i=0; i<3; i++)
//	            {
//	            	gravityCorrect[i] = (float)(gravity[i] * gravMagAtRest / gravMag);
//	            }
//	            //float [] linearAcceleration = event.values.clone();
//
//	            boolean b = SensorManager.getRotationMatrix(R, I, gravityCorrect, magnetic);
//	            Matrix.invertM(RINV, 0, R, 0);          
//	            Matrix.multiplyMV(A_W, 0, RINV, 0, linearAcceleration, 0);
//	            
//	            
//	            if (b == true)
//	            {
//		            //float [] A_D = event.values.clone();
////		            float [] A_W = new float[3];
////		            A_W[0] = R[0] * A_D[0] + R[1] * A_D[1] + R[2] * A_D[2];
////		            A_W[1] = R[3] * A_D[0] + R[4] * A_D[1] + R[5] * A_D[2];
////		            A_W[2] = R[6] * A_D[0] + R[7] * A_D[1] + R[8] * A_D[2];
////		            
//		            samples.add((int)(A_W[0]*100));
//		            samples.add((int)(A_W[1]*100));
//		            samples.add((int)(A_W[2]*100));
//		            
////		            txt += "    E= " + (int)(A_W[0]*100);
////					txt += " N= " + (int)(A_W[1]*100);
////					txt += " D= " + (int)(A_W[2]*100);
//		            
//		            txt += "    Gmag= " + (int)(gravMag*100);
//		            txt += " GmagR= " + (int)(gravMagAtRest*100);
//					
//					if (accelUpdate1000 + 1000 < System.currentTimeMillis()) {
//						accelUpdate1000 = System.currentTimeMillis();
//						int n=0, N=0, E=0, D = 0, Rt = 0, count = samples.size();
//						if (count > 2)
//						{
//							for (n=0; n<count; n+=3)
//							{
//								N += samples.get(n);
//								E += samples.get(n+1);
//								D += samples.get(n+2);
//							}
//							// average, count is 3 times too high
//							N = N*3 / count;
//							E = E*3 / count;
//							D = D*3 / count;
//							Rt = (int)Math.sqrt(N*N+E*E);
//							
//							latLongTextView.setText("E=" + E + " N=" + N + " D= " + D + " Rt=" + Rt);	
//							history.updateAccelerometerHistory(N, E, Rt);
//							samples.clear();
//						}
//					}
//	            }
//	            else
//	            	txt += "    E=N=Up=unknown";
	            //speedTextView.setText(txt);
	            
		    }
	    }
	}
	
	public void showOverview() {
		Intent intent = new Intent(this, TransitSamplingOverview.class);
		intent.putExtra("stats", transitStats);
		startActivity(intent);
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
				TransitSamplingMap.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						button.setEnabled(true);
					}
				});
			}
		}).start();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
		Bundle extras;
		@Override
		public void onReceive(Context context, Intent intent){
			extras = intent.getExtras();

			if (intent.getAction().equals(CommonIntentBundleKeysOld.ACTION_EVENT_UPDATE)){
				manageEventUpdate();
			}
		}
		private void manageEventUpdate() {
			if (extras != null){
				if (extras.containsKey(CommonIntentBundleKeysOld.KEY_UPDATE_EVENT)){
					EventType event = (EventType) extras.getSerializable(CommonIntentBundleKeysOld.KEY_UPDATE_EVENT);
					getEventsOverlay().buffer.addEvent(event);
					getEventsOverlay().show(true);
				}
			}
		}
	};
	
//	public void changeDirection(String newCurrentStation) {
//		String temp = startStation;
//		startStation = stopStation;
//		stopStation = temp;
//		int temp2 = stationFrom;
//		stationFrom = stationTo;
//		stationTo = temp2;
//		
//		stationTextView.setText(newCurrentStation);
//	}

	public PopupWindow popupWindowStations() {
		 PopupWindow popupWindow = new PopupWindow(this);
		 final ListView listViewStations = new ListView(this);
	         
		 int size = stops.size(), n=0;
		 int listsize = 7;
		 if (startStationIndex >= size - 3)
			 listsize -= (startStationIndex+4-size);
		 else if (startStationIndex < 3)
			 listsize -= (3-startStationIndex);
		 TransitInfo[] info = new TransitInfo[listsize];
		 for(int i = 0; i < size; i++) {
			 if (i >= startStationIndex -3 && i <= startStationIndex + 3)
			 {
				 //TransitInfo stop = stops.get(i);
				 info[n++] = stops.get(i);
			 }
		 }
		 
	        
		 listViewStations.setAdapter(stationsAdapter(info));
		 popupWindow.setFocusable(true);
		 float screenDensityScale = this.getResources().getDisplayMetrics().density;	
		 popupWindow.setWidth((int)(200*screenDensityScale));
		 popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		 popupWindow.setContentView(listViewStations);
		 
		 listViewStations.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					TransitInfo station = (TransitInfo) listViewStations.getItemAtPosition(position);
					PopupWindow oldPopup = popupWindowStations;
					changeDepartStation(station);
					oldPopup.dismiss();
				}
		 });
	 
		 return popupWindow;
	 }
	
	private int indexOfStation (Integer stationId)
	{
		int i;
		for (i=0; i<stops.size(); i++)
			if (stops.get(i).getStationId() == stationId)
				return i;
		return -1;
	}
	
	public void changeDepartStation(TransitInfo newStation) {
		cancelEvent();
		if(timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		transitSamplingOverlay.deleteOverlays();
		stationIdFrom = newStation.getStationId();
		startStationIndex = indexOfStation (stationIdFrom);
		
		transitSamplingOverlay.deleteOverlays();
		
		stationTextView.setText(newStation.getName());
		centerOnStation(startStationIndex);
		setStartResumeButton();
		//getDirection();
		points = getStationlineFromDB(stationIdFrom); 
	}
	
	public PopupWindow popupWindowProblems() {
		 PopupWindow popupWindow = new PopupWindow(this);
		 final ListView listViewProblems = new ListView(this);
	 
		 String[] items = { getString(R.string.transitsampling_trouble_skip),
				 getString(R.string.transitsampling_trouble_abort) };			

		 
		 listViewProblems.setAdapter(problemsAdapter(items));
		 popupWindow.setFocusable(true);
		 float screenDensityScale = this.getResources().getDisplayMetrics().density;	
		 popupWindow.setWidth((int)(200*screenDensityScale));
		 popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		 popupWindow.setContentView(listViewProblems);
		 
		 listViewProblems.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					if(listViewProblems != null) {
						String problem = (String) listViewProblems.getItemAtPosition(position);
//						if(problem.equals(getString(R.string.transitsampling_trouble_wrong_way))) {
//							goBackToPickingItinerary();
//						}
//						else if(problem.equals(getString(R.string.transitsampling_trouble_unscheduled_stop))) {
//							goBackToPickingItinerary();
//						}
//						else 
						if(problem.equals(getString(R.string.transitsampling_trouble_skip))) {
							missedStop(null);
						}
						else if(problem.equals(getString(R.string.transitsampling_trouble_abort))) {
							goBackToPickingItinerary();
						}
						popupWindowProblems.dismiss();
					}
				}
		 });
	 
		 return popupWindow;
	}
	 
	private ArrayAdapter<String> problemsAdapter(String stationsArray[]) {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, stationsArray) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				String text = getItem(position);
				
				TextView listItem = new TextView(TransitSamplingMap.this);
				
				if(text.equals(stationTextView.getText().toString())) {
					listItem.setBackgroundColor(Color.rgb(51, 153, 204));
				}
				else {
					listItem.setBackgroundColor(Color.rgb(51, 51, 51));
				}
				listItem.setText(text);
				listItem.setTextSize(16);
				listItem.setPadding(10, 10, 10, 10);
				listItem.setTextColor(Color.WHITE);
				return listItem;
			}
		};
		return adapter;
	}
	private ArrayAdapter<TransitInfo> stationsAdapter(TransitInfo stationsArray[]) {
		ArrayAdapter<TransitInfo> adapter = new ArrayAdapter<TransitInfo>(this, android.R.layout.simple_list_item_1, stationsArray) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TransitInfo station = getItem(position);
				if (station == null)
					return null;
				TextView listItem = new TextView(TransitSamplingMap.this);
				
				if(station.getStationId() == stationIdFrom){ //.equals(stationTextView.getText().toString())) {
					listItem.setBackgroundColor(Color.rgb(51, 153, 204));
				}
				else {
					listItem.setBackgroundColor(Color.rgb(51, 51, 51));
				}
				listItem.setText(station.getName());
				listItem.setTextSize(16);
				listItem.setPadding(10, 10, 10, 10);
				listItem.setTextColor(Color.WHITE);
				return listItem;
			}
		};
		return adapter;
	}
}
