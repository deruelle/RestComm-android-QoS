package com.cortxt.app.MMC.Sampling.Building;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Activities.MMCMapActivity;
import com.cortxt.app.MMC.Activities.MyCoverage.CoverageOverlay;
import com.cortxt.app.MMC.Activities.MyCoverage.MMCMapView;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventsOverlay;
import com.cortxt.app.MMC.ContentProviderOld.ContentValuesGeneratorOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesEnumOld;
import com.cortxt.app.MMC.ContentProviderOld.TablesOld;
import com.cortxt.app.MMC.ContentProviderOld.UriMatchOld;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Sampling.Transit.TransitSamplingMapMath;
import com.cortxt.app.MMC.ServicesOld.Events.EventOld;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.Utils.CustomMyLocationOverlay;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;
import com.cortxt.com.mmcextension.datamonitor.database.DatabaseHandler;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MyLocationOverlay;

public class ManualMapping extends MMCMapActivity implements SensorEventListener {
	
	private MMCMapView mMapView;
	private MappingOverlay mMappingOverlay;
	private CoverageOverlay mCoverageOverlay;
	
	@SuppressWarnings("unused")
	private boolean range = false;
	private CustomMyLocationOverlay  mMyLocationOverlay;
	private SensorManager sensorManager;
	private Sensor accelSensor, stepSensor;
	
	public int mFloorNumber = 0;
	public int mTopNumber = -1;
	private Button actionbarSlinkyIcon;
	private ImageView leftButtonIcon;
	private ImageView rightButtonIcon;
	private Button continueButton;
	private ImageView actionbarline3;
	private ProgressBar busyIndicator;
	private TextView levelTextView;
//	private ScaleBarOverlay scaleBarOverlay;
	private int[] sampled = new int[112];
	private TextView leftButton;
	private TextView rightButton;
	private Button actionbarDeleteButton;
	private ImageView crosshairImageView;
	private ImageView anchorImageView;
	public static final String TAG = ManualMapping.class.getSimpleName();
	private long lastUpdate = 0;
	private float last_x, last_y, last_z;
	private SeekBar rotateSeekBar;
	private SeekBar hiddenSeekBar;
	private TextView scaleTextView;
	private LinearLayout linearLayout2;
	private LinearLayout thumbLinearLayout;
	private LinearLayout topLinearLayout;
	private TextView titleText;
	private Button actionbarHelpButton;
	private long lastIntent = 0;
	private long lastSampleTime = 0;
	//private Location lastSampleLocation = null;
	//private Vibrator vibrator;
	
	// Android API 19 KitKat introduces a Step Detector sensor which could be used 
	//private final int TYPE_STEP_DETECTOR = 18;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.manual_sampling, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
		MMCActivity.customizeTitleBar(this, view, R.string.manualmapping_title, R.string.manualmapping_title);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		mMapView = (MMCMapView) view.findViewById(R.id.my_mapview);
		crosshairImageView = (ImageView) view.findViewById(R.id.crosshairImageView);
		busyIndicator = (ProgressBar) view.findViewById(R.id.busyIndicator);
		mCoverageOverlay = new CoverageOverlay(this, mMapView, busyIndicator, "survey");
		this.mMappingOverlay = new MappingOverlay(this, mMapView, crosshairImageView, busyIndicator, mCoverageOverlay);
		mMapView.setChangeListener(mMappingOverlay);
		mMapView.setZoomLevelChangeListener(mMappingOverlay);
		mMapView.setBuiltInZoomControls(true);
		mMapView.getOverlays().add(mCoverageOverlay);
		mMapView.getOverlays().add(mMappingOverlay);
//		mMapView.setSatellite(false);
			
		leftButton = (TextView) view.findViewById(R.id.leftButton);
		rightButton = (TextView) view.findViewById(R.id.rightButton);		
		actionbarDeleteButton = (Button) view.findViewById(R.id.actionbarDeleteButton);	
		actionbarSlinkyIcon = (Button) view.findViewById(R.id.actionbarSlinkyIcon);
		leftButtonIcon = (ImageView) view.findViewById(R.id.leftButtonIcon);
		rightButtonIcon = (ImageView) view.findViewById(R.id.rightButtonIcon);
		continueButton = (Button) view.findViewById(R.id.continueButton);
		actionbarline3 = (ImageView) view.findViewById(R.id.actionbarline3);
		levelTextView = (TextView) view.findViewById(R.id.levelTextView);
		anchorImageView = (ImageView) view.findViewById(R.id.anchorImageView);	
		rotateSeekBar = (SeekBar) view.findViewById(R.id.rotateSeekBar);	
		hiddenSeekBar = (SeekBar) view.findViewById(R.id.hiddenSeekBar);
		scaleTextView = (TextView) view.findViewById(R.id.scaleTextView);
		linearLayout2 = (LinearLayout) view.findViewById(R.id.linearLayout2);
		thumbLinearLayout = (LinearLayout) view.findViewById(R.id.thumbLinearLayout);
		topLinearLayout = (LinearLayout) view.findViewById(R.id.topLinearLayout);
		actionbarHelpButton = (Button) view.findViewById(R.id.actionbarHelpButton);
		titleText = (TextView) view.findViewById(R.id.actionbartitle);
		
		//Only a visual
		rotateSeekBar.setClickable(false);
		rotateSeekBar.setFocusable(false);
		rotateSeekBar.setEnabled(false);
		//remove progress bar
		Drawable thumb = getResources().getDrawable(R.drawable.ic_stat_notification_icon); 
		rotateSeekBar.setThumb(thumb); 
		
		hiddenSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {       
			@Override       
			public void onStopTrackingTouch(SeekBar seekBar) {
				Drawable thumb = getResources().getDrawable(R.drawable.orientation_pointer); 
				hiddenSeekBar.setThumb(thumb);
			}       
			
			@Override       
			public void onStartTrackingTouch(SeekBar seekBar) {
				Drawable thumb = getResources().getDrawable(R.drawable.orientation_pointer_active); 
				hiddenSeekBar.setThumb(thumb);
			}      
			
			@Override       
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {     
				/*					     start-18
	  			S-180       SE-270         N-0         SW-90      S-180  	 */
				int degree = 0;
				
				if(progress > 18) {
					degree = (progress - 18) * 10; 
				}
				else if(progress < 18) {
					degree = ((progress + 18) * 10);
				}
				mMapView.setRotation(degree);
			}       
		});       
		
		leftButton.setOnClickListener(leftButtonListener);
		rightButton.setOnClickListener(rightButtonListener);
		continueButton.setOnClickListener(continueButtonListener);
		actionbarDeleteButton.setOnClickListener(onDelete);
		
		//mMapView.getController().setZoom(20);
		mMyLocationOverlay = new CustomMyLocationOverlay(this, mMapView);
		mMapView.getOverlays().add(mMyLocationOverlay);  
		
		//Allow two finger rotation		
//		rotationGestureOverlay = new RotationGestureOverlay(this, mMapView);
//		mMapView.getOverlays().add(rotationGestureOverlay); 
		
		//Map scale
//		scaleBarOverlay = new ScaleBarOverlay(this.getBaseContext(), this, mMapView);
//		scaleBarOverlay.setMetric();
//		scaleBarOverlay.drawLongitudeScale(true);
//		scaleBarOverlay.drawLatitudeScale(false);
//		mMapView.getOverlays().add(scaleBarOverlay);
		
		mMapView.getController().setZoom(21);
		centerOnLocation(null);

		//Find mapping type
		int type = -1;
		if(getIntent().hasExtra("type")) 
			type = getIntent().getIntExtra("type", -1);
		
		//Restore previous samples if they exist		
		if(restoreState()) {
			type = MmcConstants.MANUAL_SAMPLING;
		}
		
		mMapView.setMappingType(type);
        MMCActivity.customizeTitleBar(this, view, R.string.manualmapping_title, R.string.manualmapping_title);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); 
		//stepSensor = sensorManager.getDefaultSensor(TYPE_STEP_DETECTOR);
		mMapView.setMappingType(-1);
		onHelp (null);
	}

	@Override
	public void onResume() {
		super.onResume();
		mMyLocationOverlay.enableMyLocation();
		restoreState();
		if (accelSensor != null)
			sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME);//SensorManager.SENSOR_DELAY_GAME);
		
	}	
	
	@Override
	public void onPause() {
		super.onPause();
		mMyLocationOverlay.disableMyLocation();
		sensorManager.unregisterListener(this);
	}
	
	@Override
	protected void onStop() {
		if(mCoverageOverlay != null)
			mCoverageOverlay.clear();
//		mCoverageOverlay = null;
		super.onStop();
	}
	
	private void startSession ()
	{
		//Find mapping type
		int type = -1;
		if(getIntent().hasExtra("type")) 
			type = getIntent().getIntExtra("type", -1);
		
		//Restore previous samples if they exist		
		if(restoreState()) {
			type = MmcConstants.MANUAL_SAMPLING;
		}
		
		mMapView.setMappingType(type);	
		
//				if(type == MmcConstants.MANUAL_SEARCHING) {
//					setWidgetVisibility(false);
//					selectMode();
//				}			
//				if(type == MmcConstants.MANUAL_SAMPLING) {
//					setWidgetVisibility(true);
//					setText();
//				}	
	}

	public boolean restoreState() {
		Cursor cursor = null;
		try {
			EventOld event = MMCIntentHandlerOld.getPlottingEvent();
			if(event == null) 
				return false;
			long startTime = event.getEventTimestamp();		
			ReportManager reportManager = ReportManager.getInstance(ManualMapping.this);			 
			cursor = reportManager.getDBProvider().query(
				UriMatchOld.LOCATIONS.getContentUri(),
				null,
				TablesOld.Locations.TIMESTAMP  + ">=? and accuracy = -1 or accuracy = -2",
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
				
				// TODO: This signalstrength isn't right because it should be queried from the past
				// Maybe use LiveBuffer to get signals and locations instead
				int signalStrength = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
	    		
				mMappingOverlay.addmOverlays(geoPoint, "Sample", signalStrength);
			}
			while (cursor.moveToNext());
			//get polygon, use that last lat/long
			if(mMappingOverlay.returnAsyncTaskStatus() != AsyncTask.Status.RUNNING) {
				mMappingOverlay.requestPolygon(latitude, longitude);
				//put in sampling mode
				mMapView.setMappingType(MmcConstants.MANUAL_SAMPLING);
			}
			else
				Toast.makeText(this, getString(R.string.manualmapping_still_searching), Toast.LENGTH_SHORT).show();
	
		} catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			if(cursor != null)
				cursor.close();
		}
		return true;
	}

	public void setFloorText(String floor) {
		levelTextView.setText(floor);
		levelTextView.setVisibility(View.VISIBLE);
	}
	
	public void setWidgetVisibility() {
		int twoButtons = View.VISIBLE;
		int anchorImage = View.VISIBLE;
		int delete = View.VISIBLE;
		int location = View.VISIBLE;
		int bigButton = View.VISIBLE;
		
		int mapType = mMapView.getMappingType();
		
		linearLayout2.setVisibility(View.VISIBLE);
		thumbLinearLayout.setVisibility(View.VISIBLE);
		topLinearLayout.setVisibility(View.VISIBLE);
		actionbarHelpButton.setVisibility(View.VISIBLE);
		
		switch(mapType) {
		case MmcConstants.MANUAL_MENU:
			twoButtons = View.GONE;
			anchorImage = View.GONE;
			delete = View.GONE;
			location = View.GONE;
			bigButton = View.GONE;
			linearLayout2.setVisibility(View.GONE);
			thumbLinearLayout.setVisibility(View.GONE);
			topLinearLayout.setVisibility(View.GONE);
			actionbarHelpButton.setVisibility(View.GONE);
			break;
		case MmcConstants.MANUAL_SEARCHING:
			twoButtons = View.GONE;
			anchorImage = View.GONE;
			delete = View.GONE;
			location = View.GONE;
			bigButton = View.VISIBLE;
			break;
		case MmcConstants.MANUAL_POLYGON_CREATE:
			twoButtons = View.VISIBLE;
			anchorImage = View.GONE;
			delete = View.VISIBLE;
			location = View.GONE;
			bigButton = View.GONE;
			break;
		case MmcConstants.MANUAL_BROWSE:
			twoButtons = View.GONE;
			anchorImage = View.GONE;
			delete = View.GONE;
			location = View.GONE;
			bigButton = View.VISIBLE;
			break;
		case MmcConstants.MANUAL_POLYGON_DONE:
			twoButtons = View.GONE;
			anchorImage = View.GONE;
			delete = View.VISIBLE;
			location = View.GONE;
			bigButton = View.VISIBLE;
			break;
		case MmcConstants.MANUAL_POLYGON_DELETE_CORNERS:
			twoButtons = View.GONE;
			anchorImage = View.GONE;
			delete = View.VISIBLE;
			location = View.GONE;
			bigButton = View.VISIBLE;
			break;
		case MmcConstants.MANUAL_POLYGON_DELETE_OR_SELECT:
			twoButtons = View.VISIBLE;
			anchorImage = View.GONE;
			delete = View.GONE;
			location = View.GONE;
			bigButton = View.GONE;
			break;
		case MmcConstants.MANUAL_ANCHOR:
			twoButtons = View.GONE;
			anchorImage = View.VISIBLE;
			delete = View.GONE;
			location = View.GONE;
			bigButton = View.VISIBLE;
			break;
		case MmcConstants.MANUAL_SAMPLING:
			twoButtons = View.VISIBLE;
			anchorImage = View.GONE;
			delete = View.VISIBLE;
			location = View.GONE;
			bigButton = View.GONE;
			break;
		}
		
		continueButton.setVisibility(bigButton);
		anchorImageView.setVisibility(anchorImage);
		actionbarDeleteButton.setVisibility(delete);
		actionbarSlinkyIcon.setVisibility(location);
		leftButtonIcon.setVisibility(twoButtons);
		rightButtonIcon.setVisibility(twoButtons);
		leftButton.setVisibility(twoButtons);
		rightButton.setVisibility(twoButtons);
		actionbarline3.setVisibility(twoButtons);
	}
	
	public void setText() {
		int mapType = mMapView.getMappingType();
		
		switch(mapType) {
		case MmcConstants.MANUAL_MENU:
			//Nothing right now
			titleText.setText(R.string.manualmapping_title);
			break;
		case MmcConstants.MANUAL_SEARCHING:
			continueButton.setText(getString(R.string.manualmapping_skip_polygon));
			titleText.setText(R.string.manualmapping_select_polygon);
			break;
		case MmcConstants.MANUAL_POLYGON_CREATE:
			rightButton.setText(getString(R.string.manualmapping_create_corner));
			rightButtonIcon.setBackgroundResource(R.drawable.action_bar_add_marker_white);
			leftButton.setText(getString(R.string.manualmapping_close_polygon));
			leftButtonIcon.setBackgroundResource(R.drawable.submit_icon_white);
			titleText.setText(R.string.manualmapping_polygon_delete_select_title);
			break;
		case MmcConstants.MANUAL_BROWSE:
			titleText.setText(R.string.manualmapping_title);
			continueButton.setText(getString(R.string.manualmapping_done_polygon));  // 'Done'
			break;
		case MmcConstants.MANUAL_POLYGON_DONE:
			continueButton.setText(getString(R.string.manualmapping_done_polygon));
			break;
		case MmcConstants.MANUAL_POLYGON_DELETE_CORNERS:
			continueButton.setText(getString(R.string.manualmapping_delete_lastcorner));
			break;
		case MmcConstants.MANUAL_POLYGON_DELETE_OR_SELECT:
			rightButton.setText(getString(R.string.manualmapping_select_manualpolygon)); 
			rightButtonIcon.setBackgroundResource(R.drawable.action_bar_add_marker_white);
			leftButton.setText(R.string.manualmapping_delete_manualpolygon);
			leftButtonIcon.setBackgroundResource(R.drawable.action_bar_remove_marker_white);
			break;
		case MmcConstants.MANUAL_ANCHOR:
			continueButton.setText(getString(R.string.manualmapping_anchor_sample));
			titleText.setText(R.string.manualmapping_title);
			break;
		case MmcConstants.MANUAL_SAMPLING:
			if(mMappingOverlay.getDelete() == false) { 
				rightButton.setText(getString(R.string.manualmapping_sample)); 
				rightButtonIcon.setBackgroundResource(R.drawable.action_bar_add_marker_white);
				leftButton.setText(R.string.manualmapping_submit);
				leftButtonIcon.setBackgroundResource(R.drawable.submit_icon_white);
			}
			else { //deleting samples
				rightButton.setText(getString(R.string.manualmapping_delete_selection)); 
				leftButton.setText(R.string.manualmapping_undo);
				leftButtonIcon.setBackgroundResource(R.drawable.action_bar_remove_marker_white);
				rightButtonIcon.setBackgroundResource(R.drawable.action_bar_remove_marker_white);	
			}
			titleText.setText(R.string.manualmapping_title);
			break;
		}
	}	

	public void changeMargins(TextView rightTextView, TextView leftTextView, int ratio) {		
		try {
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			
			int size1 = width/ratio;
			int size2 = width - size1;
						
			MarginLayoutParams linearParams = (MarginLayoutParams) rightTextView.getLayoutParams();
			linearParams.width = size1 - 100; //leave room for separator line
			rightTextView.setLayoutParams(linearParams);
			
			MarginLayoutParams linearParams2 = (MarginLayoutParams) leftTextView.getLayoutParams();
			linearParams2.width = size2 ;
			leftTextView.setLayoutParams(linearParams2);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public MappingOverlay getMappingOverlay() {
		return this.mMappingOverlay;
	}

	public void startEvent(GeoPoint geopoint) {		
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_PLOTTING_START);  
		intent.putExtra("type", mMapView.getMappingType()); 
		intent.putExtra("floor", mFloorNumber);
		intent.putExtra("top", mTopNumber);
		
		intent.putExtra("osm_id", mMappingOverlay.osm_id);
		intent.putExtra("latitude", geopoint.getLatitudeE6());
		intent.putExtra("longitude", geopoint.getLongitudeE6());
		if (mMappingOverlay.osm_id == 0)
			intent.putExtra("poly", mMappingOverlay.getPolygonString ());
		sendBroadcast(intent);
	}
	
	public void cancelEvent(final boolean reset) {
		if(mMappingOverlay.size() == 0) {
			mMapView.invalidate();
			mMappingOverlay.setPolygonPoints(new ArrayList<GeoPoint>());
			mMapView.setMappingType(MmcConstants.MANUAL_SEARCHING);
			finish();
		}
		else {
			AlertDialog.Builder builder = new AlertDialog.Builder(ManualMapping.this);
			builder.setMessage(getString(R.string.manualmapping_cancel_text));
			builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(reset){
						Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_PLOTTING_CANCEL);
						sendBroadcast(intent);

						mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
						dialog.dismiss();
					} else {						
						Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_PLOTTING_CANCEL);
						sendBroadcast(intent);
						dialog.dismiss();
						ManualMapping.this.finish();
					}
				}
			});
			builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			AlertDialog alert= builder.create();
		alert.show();
		}
	}
	
	android.view.View.OnClickListener continueButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {	
			if((mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS)) {
				if(mMappingOverlay.getPolygonPoints().size() == 0) {
					Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_delete_error_corners), Toast.LENGTH_SHORT).show();
					//reset buttons
					GeoPoint start = null;
					int size = 0;
					try {
						start = mMappingOverlay.getPolygonPoint(0);
						size = mMappingOverlay.getPolygonPoints().size();
					} catch(Exception e) {
						e.printStackTrace();
					}
					if(start == null) {
						actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);	
						mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);
					}
					else if(size > 3 && start == mMappingOverlay.getPolygonPoint(mMappingOverlay.size() -1) ) { 
						//closed polygon, nothing was deleted
						actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);	
						mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_DONE);
					}
					else {
						actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);	
						mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);
					}
				}
				else
					mMappingOverlay.deleteLastPolygonCorner();
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DONE) {
				//Done polygon draw
				mMappingOverlay.drawPolygon();
				//Save polygon
				savePolygon(mMappingOverlay.polygonToString());
				mMappingOverlay.osm_id = 0;
				//Start next step - anchor/sampling
				mMappingOverlay.addBuilding(mMappingOverlay.getPolygonPoints());
				mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);	
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR) {
				GeoPoint geoPoint = (GeoPoint) mMapView.getMapCenter();
				//if(mMappingOverlay.getPolygonPoints().size() == 0) {
					//check if sample is far away
					int outOfRange = checkAccuracy(geoPoint);
					if(outOfRange == 1) {		
						//is far away, sample anyway?
						outOfRange(geoPoint);
					}
					else if (outOfRange == 2){ //in range, allow
						Toast.makeText(ManualMapping.this, ManualMapping.this.getString(R.string.manualmapping_outofrange_polygon), Toast.LENGTH_SHORT).show();
					}
					else { //in range, allow
						confirmWalking(-1, geoPoint);
					}
//				}
//				else { //have polygon, allow -- polygon has its own check
//					confirmWalking(-1, geoPoint);
//				}
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_SEARCHING) {
				mMappingOverlay.osm_id = 0;
				mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_BROWSE) {
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);
			}
		}
	};
	
	android.view.View.OnClickListener leftButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {				

			//This means user hit 'delete last sample'
			if(mMappingOverlay.getDelete() == true) {
				//delete last sample(overlay)
				
				mMappingOverlay.deleteLastSample();
				//return the text to sampling options
				setText();
				return;
			} 
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_OR_SELECT) {
				deleteManualPolygonAlert();
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE) { 
				if(mMappingOverlay.getPolygonPoints().size() == 0) {
					Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_no_polygon_points), Toast.LENGTH_SHORT).show();
					return;
				}
				else if(mMappingOverlay.getPolygonPoints().size() < 3) {
					Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_not_enough_polygon_points), Toast.LENGTH_SHORT).show();
					return;
				}

				GeoPoint start = mMappingOverlay.getPolygonPoint(0);
				mMappingOverlay.addPolygonPoint(start);
				mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_DONE);
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING) { 
				submit(); 
			}
		}
	};	
	
	android.view.View.OnClickListener rightButtonListener = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View v) {	
			
			if(mMappingOverlay.getDelete() == true) {
				//delete all the tapped icons (with black icons)
				mMappingOverlay.deleteOverlays();
				return;
			} 
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_OR_SELECT) {
				mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE) {
				GeoPoint newCorner = (GeoPoint) mMapView.getMapCenter();
//		    	if(mMappingOverlay.size() == 0) {
//		    		createSample(-1, newCorner); 
////		    		rightButton.setText(getString(R.string.manualmapping_create_corner));
//		    	}
//		    	else {
			    	int size = mMappingOverlay.getPolygonPoints().size();
			    	if(size >= 2) {
			    		GeoPoint previousCorner = mMappingOverlay.getPolygonPoint(size - 1);
			    		int metersBetweenPoints = (int) distanceTo(previousCorner, newCorner);
			    		//Don't allow corners to be the same or very close geopoints
			    		if(metersBetweenPoints > 1) {
				    		mMappingOverlay.addPolygonPoint(newCorner);
				    		mMapView.invalidate();
			    		}
			    	}
			    	else {
			    		mMappingOverlay.addPolygonPoint(newCorner);
			    		mMapView.invalidate();
			    	}
//		    	}
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR || mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING) {		
				GeoPoint geoPoint = (GeoPoint) mMapView.getMapCenter();

				//If we don't have a polygon to limit samples to a building
				//if(mMappingOverlay.getPolygonPoints().size() == 0) {
					//check if sample is far away
					int outofrange = checkAccuracy(geoPoint);
					if(outofrange == 1) {		
						//is far away, sample anyway?
						outOfRange(geoPoint);
					}
					else if (outofrange > 1)
					{
						Toast.makeText(ManualMapping.this, ManualMapping.this.getString(R.string.manualmapping_outofrange_polygon), Toast.LENGTH_SHORT).show();						
					}
					else { //in range, allow
						confirmWalking(-1, geoPoint);
					}
//				}
//				else { //have polygon, allow -- polygon has its own check
//					confirmWalking(-1, geoPoint);
//				}
			}	
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS) {
				mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
				boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this)
						.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR, false);
				if(!dontShow)
					onHelp(null);
				mMappingOverlay.drawPolygon();
			}
		}
	};
	
	public void confirmWalking(final int accuracy, final GeoPoint geoPoint) {
	
		int signalStrength = 0;
		long timestamp = System.currentTimeMillis();
		if (mMappingOverlay.signalArray.size() > 0)
		{
			signalStrength = mMappingOverlay.signalArray.valueAt(mMappingOverlay.signalArray.size()-1); // PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
			timestamp = lastSampleTime + mMappingOverlay.signalArray.keyAt(mMappingOverlay.signalArray.size()-1)* 2000;
		}
		else
		{
			signalStrength = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -255);
		}
			
		final int finalSignal = signalStrength;
		final long finaltime = timestamp;
		if(!wasThereWalking()) {
			//Didn't detect walking - user can still confirm they did
			AlertDialog.Builder builder = new AlertDialog.Builder(ManualMapping.this);
			builder.setMessage(getString(R.string.manualmapping_walking_confirm));
			builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					createSample(accuracy, geoPoint, false, finalSignal, finaltime);
					dialog.dismiss();
					mMapView.setMappingType(MmcConstants.MANUAL_SAMPLING);
				}
			});
			builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			AlertDialog alert= builder.create();
			alert.show();
		}
		else { //was walking
			createSample(accuracy, geoPoint, false, finalSignal, finaltime);
			mMapView.setMappingType(MmcConstants.MANUAL_SAMPLING);
		}
	}
	
	public void deletePolygon() {
		String polygon = mMappingOverlay.polygonToString();
		if(polygon == null)
			return;
		String whereClause = "points = '" + polygon + "'";
		SQLiteDatabase sqlDB = null;
		DatabaseHandler dbHandler = new DatabaseHandler(ManualMapping.this);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			sqlDB.delete(DatabaseHandler.TABLE_MANUAL_POLYGON, whereClause, null);			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close();
		}
	}
	
	public void savePolygon(String points) {
		SQLiteDatabase sqlDB = null;
		DatabaseHandler dbHandler = new DatabaseHandler(ManualMapping.this);
		try {
			sqlDB = dbHandler.getWritableDatabase();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ContentValues values = new ContentValues();
		values.put("points", points);
		try {
			sqlDB.insert(DatabaseHandler.TABLE_MANUAL_POLYGON, null, values);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			sqlDB.close(); 
		}
	}
	
	public void insertLocation (GeoPoint geoPoint, int accuracy, long timestamp)
	{
		ReportManager reportManager = ReportManager.getInstance(ManualMapping.this);
		Location location = new Location("");	
		location.setLatitude(geoPoint.getLatitudeE6()/1000000.0);
		location.setLongitude(geoPoint.getLongitudeE6()/1000000.0);
		location.setAccuracy(accuracy);
		location.setTime(timestamp);
		ContentValues values = ContentValuesGeneratorOld.generateFromLocation(location, 0, 0);
		reportManager.getDBProvider().insert(TablesEnumOld.LOCATIONS.getContentUri(), values);
	}
	
	public void createSample(int accuracy, GeoPoint geoPoint, boolean filler, int signalStrength, long timestamp) {
		String title = "Sample";
		if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE) {
			title = "Anchor";
			startEvent(geoPoint);  // start recording the event when the first sample is added. Then we dont get a bunch of empty samples at the start
		}
		else if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR) {
			title = "Anchor";
			startEvent(geoPoint);  // start recording the event when the first sample is added. Then we dont get a bunch of empty samples at the start
		}
		else if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING && mMappingOverlay.size() == 1 && MMCIntentHandlerOld.getPlottingEvent() == null) {
			//Display the original anchor but do not add it to the table again
			startEvent(geoPoint); 
		}
		
		SparseIntArray signalArray = mMappingOverlay.signalArray;
		if (mMappingOverlay.previousSample != null && signalArray.size() > 1)
		{
			double dx = (geoPoint.getLongitudeE6()-mMappingOverlay.previousSample.getLongitudeE6())/1000000.0;
	    	double dy = (geoPoint.getLatitudeE6()-mMappingOverlay.previousSample.getLatitudeE6())/1000000.0;
	    	double distToSample = Math.sqrt(dx*dx+dy*dy);
	    	float second = 0;
			float accumSeconds = (float)signalArray.size(); // accumulated # of seconds of walking
	    	double distance = 0; // percent of the distance along the line (seconds/accumSeconds)
	    	double prevdistance = 0;
	    	GeoPoint prevPoint = new GeoPoint (mMappingOverlay.previousSample.getLatitudeE6(), mMappingOverlay.previousSample.getLongitudeE6());
	    	if (distToSample > 0.00005)
	    	{
		    	for (int i=0; i<signalArray.size(); i++){
			    	// draw some faded circles to show possible samples
		    		int signal = signalArray.valueAt(i);
		    		int time = signalArray.keyAt(i);
		    		if (signal != 0)
		    		{
		    			second += 1;
				    	
				    	distance = second*distToSample/accumSeconds;
				    	if (distance - prevdistance < 0.00002)
				    		continue;
				    	prevdistance = distance;
				    	GeoPoint point = TransitSamplingMapMath.findNextGeoPoint(prevPoint, geoPoint, distance);
				    	mMappingOverlay.addmOverlays(point, title, signal);
				    	timestamp = lastSampleTime + time* 2000;
						insertLocation (point, accuracy, timestamp);
						MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG,"insertLocation extra", timestamp + "," + point.getLatitudeE6() + "," +  point.getLongitudeE6());
		    		}
		    	}
	    	}
	    }
		mMappingOverlay.addmOverlays(geoPoint, title, signalStrength);
		insertLocation (geoPoint, accuracy, timestamp);
		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG,"insertLocation single", timestamp + "," + geoPoint.getLatitudeE6() + "," +  geoPoint.getLongitudeE6());
    	if (!filler)
		{
			lastSampleTime = System.currentTimeMillis();
			//lastSampleLocation = location;
			//mMappingOverlay.lastSignalArray = mMappingOverlay.signalArray.clone();
			mMappingOverlay.signalArray = new SparseIntArray();
		}
    	
    	// Add a sample too
    	if (title.equals("Anchor"))
    		mMappingOverlay.addmOverlays(geoPoint, "Sample", signalStrength);
    	actionbarSlinkyIcon.setVisibility(View.GONE);
	}
	
	public boolean wasThereWalking() {
		boolean walked = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
			.getBoolean(PreferenceKeys.Miscellaneous.SAMPLING_WALKING, false);
		
		PreferenceManager.getDefaultSharedPreferences(this).edit()
    		.putBoolean(PreferenceKeys.Miscellaneous.SAMPLING_WALKING, false).commit();
		
		return walked;
	}
		
	public void submit() {
		//finish the event
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_PLOTTING_END);
		sendBroadcast(intent);
		
		mMappingOverlay.refreshCoverage (15000);
		
		if(mMappingOverlay.size() == 0) {//No samples
			Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_submit_error), Toast.LENGTH_SHORT).show();
			return;
		}
		
		//Ask if wants to sample more of the building
		AlertDialog.Builder builder = new AlertDialog.Builder(ManualMapping.this);
		builder.setMessage(getString(R.string.manualmapping_continue_sampling));
		builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Need to select a new anchor 
				mMapView.setMappingType(MmcConstants.MANUAL_SAMPLING);
				//clear overlays so we can sample a fresh floor
				newFloor();
				if(-1 == mTopNumber)
					newTopFloor();
				else {
					if(mTopNumber > 100)
						mTopNumber = 100;
					requestFloorLevel(mTopNumber);
				}
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(getString(R.string.manualmapping_exit), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				ManualMapping.this.finish();
			}
		});
		builder.setNeutralButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				//Done!
				dialog.dismiss();
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
	
	android.view.View.OnClickListener onDelete = new android.view.View.OnClickListener() {
		@Override
		public void onClick(View view) {
			
			if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE) {
				if(mMappingOverlay.getPolygonPoints().size() == 0) {
					Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_delete_error_corners), Toast.LENGTH_SHORT).show();
					return;
				}
				else {
					mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_DELETE_CORNERS);
					actionbarDeleteButton.setBackgroundResource(R.drawable.trash_active);
					mMapView.invalidate();
					return;
				}
			}
			else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS) {
				int size = mMappingOverlay.getPolygonPoints().size();
				GeoPoint start = mMappingOverlay.getPolygonPoint(0);
				if(start == null) {
					mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);
				}
				else if(size >= 3 && start == mMappingOverlay.getPolygonPoint(mMappingOverlay.getPolygonPoints().size() -1)) { 
					//closed polygon, nothing was deleted
					actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);	
					mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_DONE);
				}
				else {
					actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);	
					mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);
				}
				return;
			}
			
			if(mMappingOverlay.size() <= 1 && mMappingOverlay.getDelete() == false && mMapView.getMappingType() != MmcConstants.MANUAL_POLYGON_DELETE_CORNERS) {
				Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_delete_error), Toast.LENGTH_SHORT).show();
				return;
			}
			//MappingOverlay onTap() will handle the overlays to delete
			//Right button listener will delete the tapped overlays
			mMappingOverlay.setDelete(); //M.Overlay will now watch the taps
			setText();
			
			if(mMappingOverlay.getDelete() == true) {
				actionbarDeleteButton.setBackgroundResource(R.drawable.trash_active);
				boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
					.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_DELETE, false);
				if(!dontShow)
					onHelp(null);	
			}
			else {
				actionbarDeleteButton.setBackgroundResource(R.drawable.trash_icon_gray);
			}
		}		
	};
			
	//Called from the action bar back button
	public void onExit(View view) {		
		//There are no samples, don't bother prompting user about losing samples
		if(mMappingOverlay.size() == 0) {
			this.finish();
			return;
		}		
		cancelEvent(true);
		retraceSteps();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			retraceSteps();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public void retraceSteps() {
		if(mMapView.getMappingType() == MmcConstants.MANUAL_SEARCHING || mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE_CORNERS || mMapView.getMappingType() == MmcConstants.MANUAL_BROWSE) {
			mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
		}
		if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE && mMappingOverlay.getPolygonPoints().size() == 0)
			mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
		if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE && mMappingOverlay.getPolygonPoints().size() > 0)
			leavePolygonCreateAlert();
		if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR) {
			//No event to cancel yet bc in anchor mode but no anchor sample yet (then would be in sampling mode). So event has not yet been started.
			mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
		}
		if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING) {
			if(mMappingOverlay.size() > 0)  {
				cancelEvent(true);
			}
			else
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
		}
	}
	
	public void discardWalkingSamples (View view)
	{
		mMappingOverlay.signalArray.clear();
		mMapView.invalidate();
		actionbarSlinkyIcon.setVisibility(View.GONE);
	}
	public void centerOnLocation(View view) {
		if(mMyLocationOverlay.getMyLocation() != null) {
			int latitudeE6 = mMyLocationOverlay.getMyLocation().getLatitudeE6();
			int longitudeE6 = mMyLocationOverlay.getMyLocation().getLongitudeE6();

			if(latitudeE6 == 0 && longitudeE6 == 0)
				return;

			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
		}
		else if(mMyLocationOverlay.getLastFix() != null) {
			int latitudeE6 = (int)(mMyLocationOverlay.getLastFix().getLatitude() * 1000000.0);
			int longitudeE6 = (int)(mMyLocationOverlay.getLastFix().getLongitude() * 1000000.0);
			
			if(latitudeE6 == 0 && longitudeE6 == 0)
				return;
			
			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
		}
		else {
			LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if(lastKnownLocation != null) {
				int latitudeE6 = (int)(lastKnownLocation.getLatitude() * 1000000.0);
				int longitudeE6 = (int)(lastKnownLocation.getLongitude() * 1000000.0);
				
				if(latitudeE6 == 0 && longitudeE6 == 0)
					return;
				
				mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
			}
		}
	}	
	
	public void updateLocation() {
		if(mMyLocationOverlay.getMyLocation() == null) {
			if(mMyLocationOverlay.getLastFix() == null) {
				LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
				Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}
	}	
	
	public int checkAccuracy(GeoPoint sample) {		
		updateLocation();
		try {
			float accuracy = mMyLocationOverlay.getLastFix().getAccuracy(); //in meters
			accuracy += 50; // add a little padding
			if(accuracy < 70) 
				accuracy = 70;
			double distance = 0;
			if (mMyLocationOverlay.getMyLocation() != null && sample != null) {
//				distance = mMyLocationOverlay.getMyLocation().distanceTo(sample); //osm version
				distance = distanceTo(sample, mMyLocationOverlay.getMyLocation());
			}
	
			if(distance <= accuracy) {
				return 0;
			}
			if (distance > 200)
				return 2; // don't allow sampling at all
		} catch(Exception e) {
			return 0; //Don't have a lastfix, allow sampling
		}		
		return 1;
	}
		
//	public void sampleWithoutPolygon(boolean bSkip) {
//		
//		boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
//				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_NO_BUILDING, false);
//		if(dontShow && bSkip) {
//			continueButton.setText(getString(R.string.manualmapping_anchor_sample)); 
//			mMapView.getController().setZoom(20);
//			requestFloorLevel(null);
//			mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);	
//			scaleBarOverlay.setEnabled(true);
//			setWidgetVisibility(false);
//			return;
//		}
//	
//		LinearLayout view = new LinearLayout(ManualMapping.this);
//		view.setOrientation(LinearLayout.VERTICAL);
//		
//		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
//		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
//		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
//				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_NO_BUILDING, false));
//		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
//					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_NO_BUILDING, isChecked).commit();
//			}
//		});
//		view.addView(dontPromptAgain);
//		
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		if (bSkip) {
//			builder.setMessage(getString(R.string.manualmapping_polygon_skip_question)); 
//			builder.setTitle(getString(R.string.manualmapping_polygon_skip));
//			builder.setView(view);
//		}
//		else {
//			builder.setMessage(getString(R.string.manualmapping_polygon_notfound_question));
//			builder.setTitle(getString(R.string.manualmapping_polygon_notfound));
//		}
//		builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.dismiss();	
//				continueButton.setText(getString(R.string.manualmapping_anchor_sample)); 
//				mMapView.getController().setZoom(20);
//				requestFloorLevel(null);
//				//Now indoor sampling
//				mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);			
//				setWidgetVisibility(false);
//				scaleBarOverlay.setEnabled(true);
//			}
//		});
//		builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				mMapView.setMappingType(MmcConstants.MANUAL_SEARCHING);
//				dialog.dismiss();
//				resumeSearching();
//			}
//		});
//		AlertDialog alert= builder.create();
//		alert.show();	
//		
//		manualPolygon();
//	}	
	
	public void newBuilding() {
		mMappingOverlay.newBuilding();
		scaleTextView.setText("0.0m");
		mTopNumber = -1;
		sampled = new int[112];
		levelTextView.setVisibility(View.GONE);
		mMappingOverlay.setPolygonPoints(new ArrayList<GeoPoint>());
	}
	
	public void newFloor() {
		mMappingOverlay.newFloor();
		scaleTextView.setText("0.0m");
	}

//	public void manualPolygon() {
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(getString(R.string.manualmapping_polygon_create_title));
//		builder.setMessage(getString(R.string.manualmapping_polygon_create_message));
//		
//		builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.dismiss();	
//				mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);			
//				setManualPolygonText();
////				scaleBarOverlay.setEnabled(true);
//			}
//		});
//		builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				dialog.dismiss();
//				startAnchorMode();
//			}
//		});
//		AlertDialog alert= builder.create();
//		alert.show();
//	}
	
	public void outOfRange(final GeoPoint geoPoint) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.manualmapping_range_msg));
		builder.setTitle(getString(R.string.manualmapping_range_title));
	
		builder.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				range = true;
				confirmWalking(-2, geoPoint);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				range = false;
				dialog.dismiss();		
			}
		});
		AlertDialog alert = builder.create();
		alert.show();		
	}	
	
	public void requestFloorLevel(int topFloor) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setCancelable(true);
		alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				newTopFloor();
			}
		});
		alert.setTitle(this.getString(R.string.manualmapping_floor_title));
		alert.setMessage(this.getString(R.string.manualmapping_floor_message));
		LinearLayout aview = new LinearLayout(this);
		aview.setOrientation(LinearLayout.VERTICAL);
		aview.setPadding(5, 0, 5, 0);
		
		final TextView label = new TextView(this);
		
		label.setText ("    " + getString(R.string.manualmapping_select_floor) + ": "); 
		aview.addView(label);
		
		int index = topFloor; 
		String[] tempFloors = new String[(topFloor+10+1)];			
		int i = topFloor;
		for (int n = 1; n <=  (topFloor); n++) {
			tempFloors[n-1] = getString(R.string.manualmapping_floor) + " " + (i+1);
			i--;
			if(tempFloors[n-1].equals(getString(R.string.manualmapping_floor) + " " + mFloorNumber))
				index = n-1;
		}	
		tempFloors[topFloor] = getString(R.string.manualmapping_ground);
		i = 9;
		for (int n = (topFloor+1); n <  (10+topFloor+1); n++) {
			tempFloors[n] = getString(R.string.manualmapping_floor) + " -" + (10 - i) ;
			i--;
			if(tempFloors[n].equals(getString(R.string.manualmapping_floor) + " " + mFloorNumber))
				index = n;
			}	
		
		final String[] floors = tempFloors;
				
		Spinner tempSpinner = new Spinner(this);
		tempSpinner.setAdapter(new SpinnerAdapter(this, R.layout.custom_spinner, floors));
		tempSpinner.setSelection(index); //default ground
		final Spinner spinner = tempSpinner;
		
        aview.addView(spinner);
		alert.setView(aview);

		alert.setPositiveButton(this.getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				int number = spinner.getSelectedItemPosition();
				sampled[number] = 1;
				String value = floors[number];
		
				if(value.equals(getString(R.string.manualmapping_ground))) {
					mFloorNumber = 0;
					mCoverageOverlay.setFloor(0);
					setFloorText(getString(R.string.manualmapping_ground));
				}
				else {
					value = value.substring(value.indexOf(" ") + 1);
					mFloorNumber = Integer.valueOf(value);
					setFloorText(getString(R.string.manualmapping_floor) + " " + mFloorNumber);
					if (mFloorNumber > 1)
						mFloorNumber = mFloorNumber - 1;  // Floor 2 becomes 1 (0 is main floor)
					mCoverageOverlay.setFloor(mFloorNumber);
				}
				
				String pref = null;
				if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING)
					pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE;
				else
					pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR;
				boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).getBoolean(pref, false);
				if(!dontShow)
					onHelp(null);
			}
		});				

		alert.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				newTopFloor();
			}
		});
				
		alert.show();
	}	
	
	/*	public void requestFloorLevel(String[] inputLevels) {
	
	//Create the child alert to change the levels. It is not shown yet...
	final CharSequence[] items = {" " + getString(R.string.manualmapping_tunnel) + " ", 
		" " + getString(R.string.manualmapping_underpass) + " "};		
	
	final ArrayList<String> seletedItems = new ArrayList<String>();	
			
	AlertDialog.Builder alert = new AlertDialog.Builder(this);
	alert.setCancelable(true);
	alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			dialog.dismiss();
			selectMode();
		}
	});
	alert.setTitle(this.getString(R.string.manualmapping_floor_title));
	alert.setMessage(this.getString(R.string.manualmapping_floor_message));
	LinearLayout aview = new LinearLayout(this);
	aview.setOrientation(LinearLayout.VERTICAL);
	aview.setPadding(5, 0, 5, 0);
	
	final TextView label = new TextView(this);
	
	label.setText ("    " + getString(R.string.manualmapping_select_floor) + ": "); 
	aview.addView(label);
	
	int index = 50; //ground
	String[] tempFloors = null;
	if(inputLevels != null) {
		tempFloors = inputLevels;
	}
	else {	
		tempFloors = new String[71];			
		int i = 50;
		for (int n = 0; n <=  49; n++) {
			tempFloors[n] = getString(R.string.manualmapping_floor) + " " + i;
			i--;
			if(tempFloors[n].equals(getString(R.string.manualmapping_floor) + " " + mFloorNumber))
				index = n;
		}	
		tempFloors[50] = getString(R.string.manualmapping_ground);
		i = 19;
		for (int n = 51; n <=  70; n++) {
			tempFloors[n] = getString(R.string.manualmapping_floor) + " -" + (20 - i) ;
			i--;
			if(tempFloors[n].equals(getString(R.string.manualmapping_floor) + " " + mFloorNumber))
				index = n;
		}	
	}
	final String[] floors = tempFloors;
//	int valueIndex = 0;
//	for(int i = 0; i < floors.length; i++) {
//		if(floors[i].contains(getString(R.string.manualmapping_ground)))
//			valueIndex = i + 1;
//	}
//	numberPicker.setMaxValue(floors.length);
//	numberPicker.setMinValue(1);
//	numberPicker.setValue(valueIndex);			
//	numberPicker.setDisplayedValues(floors);
//	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) //dont allow soft keyboard on number picker
//		numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
			
	Spinner tempSpinner = new Spinner(this);
//	ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
//		android.R.layout.simple_spinner_dropdown_item);
	tempSpinner.setAdapter(new SpinnerAdapter(this, R.layout.custom_spinner, floors));
//	adapter.addAll(floors);
//	tempSpinner.setAdapter(adapter);
	tempSpinner.setSelection(index); //default ground
	final Spinner spinner = tempSpinner;
	
	//Alert dialog 2
	LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.HORIZONTAL);        
    TextView textView = new TextView(this);
    textView.setText(getString(R.string.manualmapping_floor) + " ");
    textView.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
    textView.setTextSize(18);
    textView.setTextColor(Color.BLACK);
    textView.setPadding(40, 0, 210, 20);
    
    LinearLayout lineSeperator = new LinearLayout(this);
    lineSeperator.setOrientation(LinearLayout.HORIZONTAL); 
    lineSeperator.setBackgroundColor(Color.rgb(180, 180, 180));
    lineSeperator.setMinimumHeight(4);
    lineSeperator.setPadding(0, 2, 0, 2);
    
	final EditText newFloor = new EditText(this);
	newFloor.setInputType(InputType.TYPE_CLASS_NUMBER); 
	newFloor.setWidth(245);
	layout.addView(textView);
    layout.addView(newFloor);        
   
    newFloor.setOnFocusChangeListener(new View.OnFocusChangeListener() { //Without this the soft keyboard does not always show
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (hasFocus && alert2 != null) {
            	alert2.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            }
        }
    });
    
    newFloor.addTextChangedListener(new TextWatcher() { 
		@Override
	    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3){
//			if((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {	
	    }

		@Override
		public void afterTextChanged(Editable s) {
			try {
				String selection = newFloor.getText().toString();
				if(!selection.equals("")) {
//					mFloorNumber = Integer.valueOf(newFloor.getText().toString());					
//					setFloorText(getString(R.string.manualmapping_floor) + " " + newFloor.getText());
					if(alert2 != null) {
						Button okButton = alert2.getButton(AlertDialog.BUTTON_NEUTRAL);
						okButton.setEnabled(true);
					}
				}
				else {
					Button okButton = alert2.getButton(AlertDialog.BUTTON_NEUTRAL);
					okButton.setEnabled(false);
				}
			} catch(Exception e) {}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	});	
    
	AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
	alertBuilder.setTitle(getString(R.string.manualmapping_select_level_type)); 
	alertBuilder.setView(lineSeperator);
	alertBuilder.setView(layout);		
	alertBuilder.setCancelable(true);
	alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
		@Override
		public void onCancel(DialogInterface dialog) {
			dialog.dismiss();
			selectMode();
		}
	});
	alertBuilder.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {	
			if (isChecked) {
				switch(indexSelected) {	        	
	        	case 0: 
	        		mFloorNumber = -100;
	        		setFloorText(getString(R.string.manualmapping_tunnel));
	        		dialog.dismiss();
//	        		if(!seletedItems.contains(" Tunnel "))
//	        			seletedItems.add(" Tunnel ");
	        		break;		        	
	        	case 1: 
	        		mFloorNumber = -101;
	        		dialog.dismiss();
	        		setFloorText(getString(R.string.manualmapping_underpass) ); 
//	        		if(!seletedItems.contains(" Under-pass "))
//	        			seletedItems.add(" Under-pass ");
	        		break;	
				}	
				String pref;
				if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING)
					pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE;
				else
					pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR;
				boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).getBoolean(pref, false);
				if(!dontShow)
					onHelp(null);
			}
		}
	});
			
	alertBuilder.setNeutralButton(this.getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			try {
				String selection = newFloor.getText().toString();
//				sampled[spinner.getSelectedItemPosition()] = 1;
				if(!selection.equals("")) {
					mFloorNumber = Integer.valueOf(selection);
					setFloorText(getString(R.string.manualmapping_floor) + " " + newFloor.getText());
//					boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this)
//							.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR, false);
//					if(!dontShow)
//						onHelp(null);
					if(mTopNumber == -1)
						promptForTopFloorOrOther();
					dialog.dismiss();
				}			
				else 
					return;
			} catch(Exception e) {
				e.printStackTrace();
			}					
//			String[] newLevels = null;
//			if(seletedItems.size() > 0) {
//				newLevels = levelOptions(floors, seletedItems);
////				if(newLevels != null)
////					numberPicker.setDisplayedValues(newLevels);
//			}
//			dialog.dismiss();
//			requestFloorLevel(newLevels);
		}
	});
	
	alertBuilder.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {				
			requestFloorLevel(null);
			dialog.dismiss();
		}
	});
//    final AlertDialog alert2 = alertBuilder.create();
    alert2 = alertBuilder.create();
    //end of alert dialog 2
			
//	aview.addView(numberPicker);
    aview.addView(spinner);
	alert.setView(aview);

	alert.setPositiveButton(this.getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			//String value = input.getText().toString();
//			int number = numberPicker.getValue();
			int number = spinner.getSelectedItemPosition();
			sampled[number] = 1;
			String value = floors[number];//floors[number-1];
			if(value.equals(getString(R.string.manualmapping_tunnel))) {
				mFloorNumber = -1;
				setFloorText(getString(R.string.manualmapping_tunnel));
			}
			else if(value.equals(getString(R.string.manualmapping_underpass))) {
				mFloorNumber = -1;
				setFloorText(getString(R.string.manualmapping_underpass));
			}
			else if(value.equals(getString(R.string.manualmapping_ground))) {
				mFloorNumber = 0;
				setFloorText(getString(R.string.manualmapping_ground));
				if(mTopNumber == -1)
					promptForTopFloorOrOther();
			}
			else {
				value = value.substring(value.indexOf(" ") + 1);
				mFloorNumber = Integer.valueOf(value);
				setFloorText(getString(R.string.manualmapping_floor) + " " + mFloorNumber);
				if(mTopNumber == -1)
					promptForTopFloorOrOther();
			}
			String pref = null;
			if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING)
				pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE;
			else
				pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR;
			boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).getBoolean(pref, false);
			if(!dontShow)
				onHelp(null);
		}
	});				
	
	alert.setNeutralButton(getString(R.string.Settings_OtherSettings), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {				
			alert2.show();
			Button okButton = alert2.getButton(AlertDialog.BUTTON_NEUTRAL);
			okButton.setEnabled(false);
			if(seletedItems.size() > 0) {
				String[] newLevels = levelOptions(floors, seletedItems);
				if(newLevels != null) {
//					numberPicker.setDisplayedValues(newLevels);
					Spinner newSpinner = new Spinner(ManualMapping.this);
					ArrayAdapter<String> newAdapter = new ArrayAdapter<String>(ManualMapping.this, 
							android.R.layout.simple_spinner_dropdown_item);
					newAdapter.addAll(newLevels);
					newSpinner.setAdapter(newAdapter);
				}
			}
			dialog.dismiss();
		}
	});

	alert.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			dialog.dismiss();
//			resumeSearching();
			mMappingOverlay.newBuilding();
			scaleTextView.setText("0.0m");
			mTopNumber = -1;
			sampled = new int[71];
			mTopNumber = -1;
			selectMode();
		}
	});
			
	alert.show();
} */

	public void newTopFloor() {
		
		//Alert 2 (Other structures)
		final CharSequence[] items = {" " + getString(R.string.manualmapping_tunnel) + " ", 
			" " + getString(R.string.manualmapping_underpass) + " "};		
		
		LinearLayout layout2 = new LinearLayout(this);
        layout2.setOrientation(LinearLayout.HORIZONTAL);        
        
		final AlertDialog.Builder alertBuilder2 = new AlertDialog.Builder(this);
		alertBuilder2.setTitle(getString(R.string.manualmapping_select_level_type)); 
		alertBuilder2.setView(layout2);		
		alertBuilder2.setCancelable(false);
		alertBuilder2.setMultiChoiceItems(items, null, new DialogInterface.OnMultiChoiceClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int indexSelected, boolean isChecked) {	
				if (isChecked) {
					switch(indexSelected) {	        	
		        	case 0: 
		        		mFloorNumber = -100;
		        		mCoverageOverlay.setFloor(mFloorNumber);
		        		//So we dont ask for top floor again
		        		mTopNumber = -2;
		        		setFloorText(getString(R.string.manualmapping_tunnel));
		        		dialog.dismiss();
		        		break;		        	
		        	case 1: 
		        		mFloorNumber = -101;
		        		mCoverageOverlay.setFloor(mFloorNumber);
		        		//So we dont ask for top floor again
		        		mTopNumber = -2;
		        		dialog.dismiss();
		        		setFloorText(getString(R.string.manualmapping_underpass) ); 
		        		break;	
					}	
					String pref;
					if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING)
						pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE;
					else
						pref = PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR;
					boolean dontShow = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).getBoolean(pref, false);
					if(!dontShow)
						onHelp(null);
				}
			}
		});
		
		alertBuilder2.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {				
				newTopFloor();
				dialog.dismiss();
			}
		});
		
		alertBuilder2.create();
		
		//Alert dialog 1 (Top floor)
		final AlertDialog.Builder alert1 = new AlertDialog.Builder(this);
		alert1.setCancelable(false);
	    alert1.setMessage(getString(R.string.manualmapping_polygon_top_floor));

		LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);        
        TextView textView = new TextView(this);
        textView.setText(getString(R.string.manualmapping_floor) + " ");
        textView.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.CENTER_VERTICAL);
        textView.setTextSize(18);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(40, 0, 210, 20);
        
	    final EditText newFloor = new EditText(this);
		newFloor.setInputType(InputType.TYPE_CLASS_NUMBER); 
		newFloor.setWidth(120);
//		newFloor.setHint(getString(R.string.manualmapping_ground));
		newFloor.setHint("1");
		newFloor.setFilters(new InputFilter[] {
		    // Maximum 3 characters.
			new InputFilter.LengthFilter(3),
		    // Digits only.
			DigitsKeyListener.getInstance(),  // Not strictly needed, IMHO.
		});
		// Digits only & use numeric soft-keyboard.
		newFloor.setKeyListener(DigitsKeyListener.getInstance());
		layout.addView(textView);
        layout.addView(newFloor);        
        alert1.setView(layout);
	  
	    alert1.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int whichButton) {
	    		String level = newFloor.getText().toString();
	    		if(level.equals("") || level.equals("1")) {
	    			mTopNumber = 0;
	    		}
	    		else {
	    			try {
	    				int n = Integer.valueOf(level);
	    				if(n > 100) {
	    					Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_topfloor_toobig), Toast.LENGTH_SHORT).show();
	    					n = 100;
	    				}
	    				mTopNumber =  n-1;
	    			} catch(Exception e) {
	    				Toast.makeText(ManualMapping.this, getString(R.string.manualmapping_invalid_topfloor), Toast.LENGTH_SHORT).show();
	    			}
	    		}
	    		requestFloorLevel(mTopNumber);
	    	}
	    });

	    alert1.setNeutralButton(getString(R.string.manualmapping_other_structure), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int whichButton) {
	        	alertBuilder2.show();
				dialog.dismiss();
	        }
	    });
	    
	    alert1.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
			}
		});
	    
	    alert1.show();
	} 
	
/*	public void promptForTopFloor() {
		String floors[] = new String[50];
		for(int i = 0; i < floors.length; i++) {
			floors[i] = getString(R.string.manualmapping_floor) + " " + (i+1);
		}

		LinearLayout view = new LinearLayout(this);
		view.setOrientation(LinearLayout.VERTICAL);
		view.setPadding(5, 0, 5, 0);
		
		Spinner tempSpinner = new Spinner(this);
		tempSpinner.setAdapter(new SpinnerAdapter(this, R.layout.custom_spinner, floors));
		if(mFloorNumber > 0 || mFloorNumber <=49) {
			tempSpinner.setSelection(mFloorNumber -1); 
		}
		else {
			tempSpinner.setSelection(0); 
		}
		final Spinner spinner = tempSpinner;
		view.addView(spinner);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(ManualMapping.this);
		alert.setMessage(getString(R.string.manualmapping_polygon_top_floor));
		alert.setCancelable(false);
		alert.setView(view);
		alert.setPositiveButton(this.getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				mTopNumber = spinner.getSelectedItemPosition();
			}		
		});
		alert.show();
	} */
	
	public String[] levelOptions(String[] floors, ArrayList<String> seletedItems) {

		final CharSequence[] items = {" " + getString(R.string.manualmapping_tunnel) + " ", 
				" " + getString(R.string.manualmapping_underpass) + " "};

		List<String> list = Arrays.asList(floors);  
			
        ArrayList<String> temp = new ArrayList<String>();
        //Loop through each selected option and add those options to the spinner arraylist
        for(int i = items.length - 1; i >= 1; i--) {      	 
        	if(seletedItems.contains(items[i])) { //check for all but floors, floors need to stay in the right order
        		String tempString = items[i].toString();
        		if(!list.contains(tempString))
        			temp.add(tempString.trim());
        	}
        }
        
        //Add previous floors
        for (int n = 0; n <= floors.length -1; n++)
        	temp.add(floors[n]);
        
		if(seletedItems.contains(items[items.length - 1])) { //new floors
       		//Add 50 new upper floors
			String value = floors[floors.length - 1];
    		int f = Integer.valueOf(value.substring(value.indexOf(" ")+1));
    		for (int n = floors.length; n <= (floors.length + 50); n++) {
    			temp.add(getString(R.string.manualmapping_floor) + " " + f);
    			f++;
    		}
    	}        
        
        //Convert it to String[]
        String[] levels = new String[temp.size()];
        for(int i = 0; i < temp.size(); i++) {
        	levels[i] = temp.get(i).toString();
        }        
        
		return levels;
	}
	
	public void onHelp(View button) {		
		if(button != null)
				temporarilyDisableButton(button);
		
		if(mMapView.getMappingType() == MmcConstants.MANUAL_SEARCHING)
			showSearchingAlert();
		else if(mMappingOverlay.getDelete() == true)
			showDeleteAlert(); 
		else if(mMapView.getMappingType() == MmcConstants.MANUAL_ANCHOR)
			showAnchorAlert();
		else if(mMapView.getMappingType() == MmcConstants.MANUAL_SAMPLING)
			showSamplingAlert();	
		else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_CREATE)
			showPolygonAlert();	
		else if(mMapView.getMappingType() == -1)
			introAlert();	
		//else if(mMapView.getMappingType() == MmcConstants.MANUAL_POLYGON_DELETE)
		//	showPolygonDeleteSelectAlert();
	}
	
	public void selectMode() {

		final CharSequence[] items = { getString(R.string.manualmapping_polygon_option_saved), 
			getString(R.string.manualmapping_polygon_option_create), 
			getString(R.string.manualmapping_polygon_option_none),
			getString(R.string.manualmapping_polygon_option_browse)};			

		AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
		alertBuilder.setTitle(getString(R.string.manualmapping_polygon_option_title)); 		
		alertBuilder.setCancelable(true);
		alertBuilder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				ManualMapping.this.finish();
			}
		});
		alertBuilder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int indexSelected) {	
				//if (isChecked) {
				switch(indexSelected) {	        	
	        	case 0: 
	        		mMapView.setMappingType(MmcConstants.MANUAL_SEARCHING);
					break;
	        	case 1: 
	        		mMapView.setMappingType(MmcConstants.MANUAL_POLYGON_CREATE);
					break;
	        	case 2: 
	        		mMapView.setMappingType(MmcConstants.MANUAL_ANCHOR);
	        		break;
	        	case 3: 
	        		mMapView.setMappingType(MmcConstants.MANUAL_BROWSE);
	        		break;
				}
				dialog.dismiss();				
			}
		});
		alertBuilder.show();
	}
	
	
	public void leavePolygonCreateAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(getString(R.string.manualmapping_polygon_cancel_msg));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.GenericText_Yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
			}
		});
		alert.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);	
			}
		});
	
		alert.show();
	}
	
	public void deleteManualPolygonAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);

		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(getString(R.string.manualmapping_polygon_delete));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.manualmapping_delete), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				deletePolygon();
				mMapView.setMappingType(MmcConstants.MANUAL_MENU);
				mMappingOverlay.removeBuilding(mMappingOverlay.getPolygonPoints());
				dialog.dismiss();
			}
		});
		alert.setNegativeButton(getString(R.string.GenericText_Cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});
	
		alert.show();
	}
	
	public void showAnchorAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		TextView textView1 = new TextView(this); 
		textView1.setText(getString(R.string.manualmapping_instruct_anchor1));	
		textView1.setPadding(10, 2, 10, 2);
		textView1.setGravity(Gravity.CENTER);
		view.addView(textView1);
		
		ImageView imageView = new ImageView(this); 
		imageView.setImageDrawable(getResources().getDrawable(R.drawable.anchor_point_illustrator));		
		view.addView(imageView);
		
		TextView textView2 = new TextView(this); 
		textView2.setText(getString(R.string.manualmapping_instruct_anchor2));	
		textView2.setPadding(10, 2, 10, 2);
		textView2.setGravity(Gravity.CENTER);
		view.addView(textView2);
		
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR, false));
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ANCHOR, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);		
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_instruct_anchor_title));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	
	public void showSearchingAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SEARCH, false));
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SEARCH, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_instruct_search_title));
		alert.setMessage(getString(R.string.manualmapping_instruct_search1) + "\n" 
		+ getString(R.string.manualmapping_instruct_search2));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	
	public void showSamplingAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		TextView textView1 = new TextView(this); 
		textView1.setText(getString(R.string.manualmapping_instruct_sample1));	
		textView1.setPadding(10, 2, 10, 2);
		textView1.setGravity(Gravity.CENTER);
		view.addView(textView1);
		
		ImageView imageView = new ImageView(this); 
		imageView.setImageDrawable(getResources().getDrawable(R.drawable.create_sample_illustrator));		
		view.addView(imageView);
		
		TextView textView2 = new TextView(this); 
		textView2.setText(getString(R.string.manualmapping_instruct_sample2));	
		textView2.setPadding(10, 2, 10, 2);
		textView2.setGravity(Gravity.CENTER);
		view.addView(textView2);
		
		boolean dontAsk = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE, false);
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(dontAsk);
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_SAMPLE, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_instruct_sample_title));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	public void introAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		TextView textView1 = new TextView(this); 
		textView1.setText(getString(R.string.manualmapping_about1));	
		textView1.setPadding(10, 2, 10, 2);
		textView1.setGravity(Gravity.CENTER);
		view.addView(textView1);
		
		ImageView imageView = new ImageView(this); 
		imageView.setImageDrawable(getResources().getDrawable(R.drawable.create_sample_illustrator));		
		view.addView(imageView);
		
		TextView textView2 = new TextView(this); 
		textView2.setText(getString(R.string.manualmapping_about2));	
		textView2.setPadding(10, 2, 10, 2);
		textView2.setGravity(Gravity.CENTER);
		view.addView(textView2);
		
		boolean dontAsk = PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ABOUT, false);
		if (dontAsk)
		{
			startSession ();
			return;
		}
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(dontAsk);
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_ABOUT, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_about_title));
		//alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
				startSession ();
			}
		});				
	
		alert.show();
	}
	
	public void showPolygonDeleteSelectAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_POLYGON_DELETE, false));
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_POLYGON_DELETE, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_polygon_delete_select_title));
		alert.setMessage(getString(R.string.manualmapping_polygon_delete_select_msg));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	
	public void showDeleteAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_DELETE, false));
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_DELETE, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_delete_title));
		alert.setMessage(getString(R.string.manualmapping_delete_msg));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	
	public void showPolygonAlert() {
		LinearLayout view = new LinearLayout(ManualMapping.this);
		view.setOrientation(LinearLayout.VERTICAL);
		
		CheckBox dontPromptAgain = new CheckBox(ManualMapping.this);
		dontPromptAgain.setText(R.string.manualmapping_no_prompt);
		dontPromptAgain.setChecked(PreferenceManager.getDefaultSharedPreferences(ManualMapping.this) 
				.getBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_POLYGON_CREATE, false));
		dontPromptAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				PreferenceManager.getDefaultSharedPreferences(ManualMapping.this).edit()
					.putBoolean(PreferenceKeys.Miscellaneous.DONT_ASK_FOR_MAPPING_INSTRUCT_POLYGON_CREATE, isChecked).commit();
			}
		});
		view.addView(dontPromptAgain);
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(getString(R.string.manualmapping_polygon_help_title));
		alert.setMessage(getString(R.string.manualmapping_polygon_help_message));
		alert.setCancelable(true);
		alert.setView(view);
		
		alert.setPositiveButton(getString(R.string.getstarted_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});				
	
		alert.show();
	}
	
	public void temporarilyDisableButton(final View button) {
		if(button == null)
			return;
		
		button.setEnabled(false);		
		
		new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000);  
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ManualMapping.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        button.setEnabled(true);
                    }
                });
            }
        }).start();
	}
	
	private void stepDetected ()
	{
		// Look for 2 consecutive steps of similar duration
//		long prevStepDuration = System.currentTimeMillis() - previousStep1;
//		long prevStepDuration2 = previousStep1 - previousStep2;
//		boolean bValid = false;
//		int diff = (int)Math.abs(prevStepDuration2-prevStepDuration);
//		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "stepDetected", "prevStepDuration " + prevStepDuration + " prevStepDuration2 " + prevStepDuration2 + "  diff" + diff );
//		
//		// both steps between 1/3 and 1 second?
//		if (prevStepDuration > 300 && prevStepDuration < 1100 && prevStepDuration2 > 300 && prevStepDuration2 < 1100 )
//		{
//			if (diff < 400)
//				bValid = true;
//		}
//		previousStep2 = previousStep1;
//		previousStep1 = System.currentTimeMillis();
//		
//		if (bValid == false)
//			return;
//		MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "stepDetected", "STEP DETECTED" );
		
//		titleText.setText("STEP");
//		new Handler().postDelayed(new Runnable() {
//		      @Override
//		      public void run() {
//		    	  titleText.setText("");
//		      }
//		    }, 150);
		if (mMapView.getMappingType() != MmcConstants.MANUAL_SAMPLING)
			return;
		
		//walked
//    	System.out.println("Walking - accelerometer: " + speed);
    	PreferenceManager.getDefaultSharedPreferences(this).edit()
			.putBoolean(PreferenceKeys.Miscellaneous.SAMPLING_WALKING, true).commit();
    	
    	int secondsFromSample = (int)((System.currentTimeMillis() - lastSampleTime)/2000);
    	// While walking store 1 signal sample for each distinct second where walking occurs
    	if (mMappingOverlay.signalArray.indexOfKey(secondsFromSample) < 0)
    	{
    		int signalStrength = PreferenceManager.getDefaultSharedPreferences(this).getInt(PreferenceKeys.Miscellaneous.SIGNAL_STRENGTH, -121);
    		mMappingOverlay.signalArray.put(secondsFromSample, signalStrength);
    		mMapView.invalidate();
    	}
    	if (mMappingOverlay.signalArray.size() == 2)
    	{
    		actionbarSlinkyIcon.setVisibility(View.VISIBLE);
    	}
	}
	
	// To detect a step, we want the force to exceed 10.5 and drop below 9.5 quickly
	private long forceAbove105 = 0;
	private long firstStepTime = 0;
	private double rHigh = 0, rLow = 0;
	private long previousStep1 = 0, previousStep2 = 0;
	private long startAveraging = 0;
	private double averageR = 0;
	int averageCount = 0;
	@Override
	public void onSensorChanged(SensorEvent event) {
		Sensor mySensor = event.sensor;
		
		if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	    	if (stepSensor != null)
	    		return;
	        float x = event.values[0];
	        float y = event.values[1];
	        float z = event.values[2];
	        double resultant = Math.sqrt(x*x+y*y+z*z);
	        if (startAveraging == 0)
	        	startAveraging = System.currentTimeMillis();
	        if (startAveraging > 0 && startAveraging + 8000 > System.currentTimeMillis())	
	        {
	        	averageR = (averageR * averageCount + resultant) / (averageCount + 1);
	        	averageCount ++;
	        }
	        else if (forceAbove105 == 0 && resultant > averageR + 0.4) // resultant > 10.4)
	        {	
	        	//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "forceAbove105 " + resultant + " > " + averageR);
	        	forceAbove105 = System.currentTimeMillis();
	        	rHigh = resultant;
	        	//titleText.setText(Double.toString(resultant));
	        }
	        else if (forceAbove105 > 0 && resultant < averageR - 0.4) // && resultant < 9.1)
	        {
	        	int diff = (int)(System.currentTimeMillis() - forceAbove105);
	        	//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "forceBelow95 " + resultant + "  diff " + diff + " < " + averageR);
	        	
//	        	long prevStepDuration = System.currentTimeMillis() - previousStep1;
//	    		long prevStepDuration2 = previousStep1 - previousStep2;
//	    		boolean bValid = false;
//	    		if ( (int)Math.abs(prevStepDuration2-prevStepDuration) < 300)
//	    			diff = 199;
	        	//vibrator.vibrate(200);
	        	if (diff > 120 && diff < 400)
	        	{
	        		rLow = resultant;
//	        		titleText.setText(Long.toString(System.currentTimeMillis()-forceAbove105) + "  " + rHigh + "  " + rLow);
	        		
//	        		titleText.setText("step?");
//	        		new Handler().postDelayed(new Runnable() {
//	        		      @Override
//	        		      public void run() {
//	        		    	  if (!titleText.getText().equals("STEP"))
//	        		    		  titleText.setText("");
//	        		      }
//	        		    }, 500);
	        		
	        		// step detected. look for 2 in a row, .3 to .9 seconds apart
	        		if (firstStepTime == 0)
	        			firstStepTime = System.currentTimeMillis();
	        		else 
	        		{
	        			long delay = System.currentTimeMillis() - firstStepTime;
	        			//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "firstStep Diff " + delay);
	        			if (delay > 300 && delay < 1100)
	        			{
	        				//MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "STEP DETECTED");
		        			
	        				stepDetected ();
	        			}
	        			firstStepTime = System.currentTimeMillis();
	        			//else
	        			//	firstStepTime = 0;
	        		}
	        	}
	        	//else
	        	//	MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "not step Diff " + diff);
	        	firstStepTime = System.currentTimeMillis();
	        	forceAbove105 = 0;
	        }
//	        else if (forceAbove105 > 0 && resultant < 10.7 && (forceAbove105 + 440 < System.currentTimeMillis() || forceAbove105 + 100 > System.currentTimeMillis()))
//	        {
//	        	forceAbove105 = 0;
//	        	MMCLogger.logToFile(MMCLogger.Level.DEBUG, TAG, (System.currentTimeMillis()%1000000) + "onSensorChanged", "Cancel forceAbove105 " + resultant);
//	        	firstStepTime = System.currentTimeMillis();
//	        }
 
//	        long curTime = System.currentTimeMillis();
//	 
//	        //if ((curTime - lastUpdate) > 100) 
//	        {
//	            long diffTime = (curTime - lastUpdate);
//	            lastUpdate = curTime;
//	 
//	            float speed = Math.abs(x + y + z - (last_x + last_y + last_z));/// diffTime * 10000;
//	 
//	            if (speed > 0.8 && speed < 4) {
//	                stepDetected ();
//	            }
//	 
//	            last_x = x;
//	            last_y = y;
//	            last_z = z;
//	        }
	    }
	    if (lastIntent + 30000 < System.currentTimeMillis())
	    {
	    	lastIntent = System.currentTimeMillis();
		    Intent intent = new Intent(MMCIntentHandlerOld.VIEWING_SIGNAL);
			sendBroadcast(intent);
	    }
	} 	
	
	public class SpinnerAdapter extends ArrayAdapter<String> {
		String[] floors;
		 
        public SpinnerAdapter(Context context, int txtViewResourceId, String[] objects) {
            super(context, txtViewResourceId, objects);
            floors = objects;
        }
 
        @Override
        public View getDropDownView(int position, View view, ViewGroup viewGroup) {
            return getCustomView(position, view, viewGroup);
        }
        
        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            return getCustomView(position, view, viewGroup);
        }
        
        public View getCustomView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View spinner = inflater.inflate(R.layout.custom_spinner, parent, false);
         
            ImageView spinnerImageView = (ImageView) spinner.findViewById(R.id.spinnerImageView);
//            int test = 0;
//            if(position >= 100)
//            	test = 5;
            if(sampled[position] == 1)
            	spinnerImageView.setImageResource(R.drawable.checked_floor_icon);
            else
            	spinnerImageView.setImageResource(android.R.color.transparent);
            
            TextView floorTextView = (TextView) spinner.findViewById(R.id.floorTextView);
            floorTextView.setText(floors[position]);
 
            return spinner;
        }
    }

	public float distanceTo(GeoPoint StartP, GeoPoint EndP) {
	    int Radius = 6371;//radius of earth in Km         
	    double lat1 = StartP.getLatitudeE6()/1E6;
	    double lat2 = EndP.getLatitudeE6()/1E6;
	    double lon1 = StartP.getLongitudeE6()/1E6;
	    double lon2 = EndP.getLongitudeE6()/1E6;
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLon = Math.toRadians(lon2-lon1);
	    double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
	    Math.sin(dLon/2) * Math.sin(dLon/2);
	    double c = 2 * Math.asin(Math.sqrt(a));
	    double valueResult= Radius*c;
	    double km = valueResult/1;
//	    DecimalFormat newFormat = new DecimalFormat("####");
//	    float kmInDec =  Float.valueOf(newFormat.format(km));
//	    float meter = kmInDec%1000;
	    float meter = (float) (km%1000) * 1000;
//	    int meterInDec = Integer.valueOf(newFormat.format(meter));
//	    System.out.println("Radius Value " + valueResult + " KM " + kmInDec + " Meter " + meter);

	    return meter;
	 }

	public void setScaleText(String newScale) {
		scaleTextView.setText(newScale);
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}