package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.HashSet;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Activities.MyCoverage.MMCMapView;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventOverlayItem;
import com.cortxt.app.MMC.Activities.MyCoverage.EventsOverlay.EventsOverlay;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedMapActivityOld;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.EventType;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;

public class TransitSamplingOverview extends MMCTrackedMapActivityOld implements OnClickListener{

	//Editable fields
	private TextView stationsTextView;
	private TextView timeTextView;
	private TextView distanceTextView;
	private TextView collectedTextView;
	private TextView speedtestsTextView;
	private TextView downloadTextView;
	private TextView uploadTextView;
	private TextView issuesTextView;
	private TextView usageTextView;
	
	//Labels for the above
	private TextView stationsLabelTextView;
	private TextView timeLabelTextView;
	private TextView distanceLabelTextView;
	private TextView collectedLabelTextView;
	private TextView speedtestsLabelTextView;
	private TextView downloadLabelTextView;
	private TextView uploadLabelTextView;
	private TextView issuesLabelTextView;
	private TextView usageLabelTextView;
	
	private ViewFlipper flipper;
	private ImageView statIndicatorOverview;
	private ImageView statIndicatorMap;
	private boolean SampleOnOFf = true;
	
	private TransitStats stats = null;
	private MMCMapView mMapView;
	private EventsOverlay mEventsOverlay;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_overview_new, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);

        MMCActivity.customizeTitleBar(this, view, R.string.transitsampling_overview_title, R.string.transitcustom_overview_title);
		
		//Editable fields
		stationsTextView = (TextView) view.findViewById(R.id.stationsTextView);
		timeTextView = (TextView) view.findViewById(R.id.timeTextView);
		distanceTextView = (TextView) view.findViewById(R.id.distanceTextView);
		collectedTextView = (TextView) view.findViewById(R.id.collectedTextView);
		speedtestsTextView = (TextView) view.findViewById(R.id.speedtestsTextView);
		downloadTextView = (TextView) view.findViewById(R.id.downloadTextView);
		uploadTextView = (TextView) view.findViewById(R.id.uploadTextView);
		issuesTextView = (TextView) view.findViewById(R.id.issuesTextView);
		usageTextView = (TextView) view.findViewById(R.id.usageTextView);
		
		//Labels
		stationsLabelTextView = (TextView) view.findViewById(R.id.stationsLabelTexView);
		timeLabelTextView = (TextView) view.findViewById(R.id.timeLabelTextView);
		distanceLabelTextView = (TextView) view.findViewById(R.id.distanceLabelTextView);
		collectedLabelTextView = (TextView) view.findViewById(R.id.collectedLabelTextView);
		speedtestsLabelTextView = (TextView) view.findViewById(R.id.speedtestsLabelTextView);
		downloadLabelTextView = (TextView) view.findViewById(R.id.downloadLabelTextView);
		uploadLabelTextView = (TextView) view.findViewById(R.id.uploadLabelTextView);
		issuesLabelTextView = (TextView) view.findViewById(R.id.issuesLabelTextView);
		usageLabelTextView = (TextView) view.findViewById(R.id.usageLabelTextView);
		
		TextView overviewTitle = (TextView) view.findViewById(R.id.actionbartitle);
		overviewTitle.setText((Html.fromHtml(getString(R.string.transitsampling_overview_title))));
		
		flipper = (ViewFlipper) view.findViewById(R.id.flipper);
		flipper.setOnClickListener(this);
		statIndicatorOverview = (ImageView) view.findViewById(R.id.statIndicatorOverview);
		statIndicatorMap = (ImageView) view.findViewById(R.id.statIndicatorMap);
		
		setFonts();
		
		Intent intent = this.getIntent();
		if(intent.hasExtra("stats")) {
			stats =  (TransitStats) intent.getSerializableExtra("stats");
		}
		updateTextViews();
		
		mMapView = (MMCMapView) findViewById(R.id.transitoverview_mapview);
//		transitSamplingOverlay = new TransitSamplingOverlay(this, mMapView, resourceProxy);
//		mMapView.getOverlays().add(transitSamplingOverlay);
		mMapView.setBuiltInZoomControls(false);
		mMapView.getController().setZoom(20);
		mMapView.setClickable(false);
		centerOnLocation((LocationManager) getSystemService(Context.LOCATION_SERVICE));
		
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
		mMapView.setZoomLevelChangeListener(mEventsOverlay);
	}
	
	public void onClick(View view) {
		flipper.showNext();
		SampleOnOFf = !SampleOnOFf;
		if(SampleOnOFf) {
			statIndicatorOverview.setVisibility(View.VISIBLE);
			statIndicatorMap.setVisibility(View.GONE);
		}
		else {
			statIndicatorOverview.setVisibility(View.GONE);
			statIndicatorMap.setVisibility(View.VISIBLE);
		}
	}
	
	public void setFonts() {
		//Labels
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, stationsLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, timeLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, distanceLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, collectedLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, speedtestsLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, downloadLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, uploadLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, issuesLabelTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, usageLabelTextView, this);
		
		//Editable fields
		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, stationsTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, timeTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, distanceTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, collectedTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, speedtestsTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, downloadTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, uploadTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, issuesTextView, this);
		FontsUtil.applyFontToTextView(MmcConstants.font_Regular, usageTextView, this);
	}
	
	public void onExitClicked (View view) {
		this.finish();
		//TODO Might have to alter this to take you back to DashBoard or transit main
	}
	
	public void updateTextViews() {
		if(stats == null)
			return;
		try {
			stationsTextView.setText(stats.getStationCount());
			timeTextView.setText(stats.getMinutes() + "mins");
			distanceTextView.setText(stats.getDistance());
			collectedTextView.setText(stats.getSamplesCollected());
			speedtestsTextView.setText(stats.getSpeedTests());
			downloadTextView.setText(stats.getTopDownload() + "Mb/s");
			uploadTextView.setText(stats.getTopUpload() + "Mb/s");
			issuesTextView.setText(stats.getIssues());
			usageTextView.setText(stats.getUsage());
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void centerOnLocation(LocationManager locationManager) {
		if(locationManager == null)
			return;
		Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		if(lastKnownLocation != null) {
			int latitudeE6 = (int)(lastKnownLocation.getLatitude() * 1000000.0);
			int longitudeE6 = (int)(lastKnownLocation.getLongitude() * 1000000.0);
			if(latitudeE6 == 0 && longitudeE6 == 0)
				return;
			mMapView.getController().setCenter(new GeoPoint(latitudeE6, longitudeE6));
		}
	}

	public void leftClicked(View button) {
		onClick(null);
	}

	public void rightClicked(View button) {
		onClick(null);
	}
	
	public void newItinerary(View view) {
		Intent overviewIntent = new Intent(TransitSamplingOverview.this, TransitSamplingMain.class);
		startActivity(overviewIntent);
		this.finish();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
