package com.cortxt.app.MMC.Sampling.Transit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.MMCService;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.EventHistory;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.Reporters.WebReporter.WebReporter;
import com.cortxt.app.MMC.ServicesOld.Intents.MMCIntentHandlerOld;
import com.cortxt.app.MMC.ServicesOld.Location.LocationRequest;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TransitSampling extends MMCTrackedActivityOld {
	//new_dashboard_icons3
	private Spinner citySpinner;
	private Spinner lineSpinner;
	private LinearLayout linearLayout1;
	private LinearLayout linearLayout2;
	private Spinner fromSpinner;
	private LinearLayout linearLayout3;
	private Spinner endSpinner;
	private CheckBox roundTripCheckBox;
	private ProgressBar busyIndicator;
	private Button startStationButton;
//	private ProgressDialog barProgressDialog;
	private LocationManager locationManager ;
	
	private final int RESULT_OK = 1;
	private final int RESULT_ERROR = 2;
	private final int START_STATION = 3;
	private final int STOP_STATION = 4;
	
	public static final String TAG = TransitSampling.class.getSimpleName();
	private TransitDatabaseReadWriteNew transitDB;
	private ArrayList<TransitInfo> stations = new ArrayList<TransitInfo>();
	
	private String startStation = null;
	private String stopStation = null;
	private String[][] transports; //transports is [transport_id][name]
//	private String[] transports;
	private int transportId = 0;	
	private int departId = 0, arrivalId = 0;
	private int areaId = 0;
	private boolean prefilling = true, untouched = true;
	
	private String previousCity = null;
	private String previousLine = null;
	private double[][] polyline = null;
	
	private AsyncTask<Void, Void, String> cityAsyncTask = null;
	private AsyncTask<Void, Void, String> transportAsyncTask = null; 
	private AsyncTask<Void, Void, String> stationAsyncTask = null;
	private AsyncTask<Void, Void, String> polylineAsyncTask = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_new, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);

		//White labeling
        MMCActivity.customizeTitleBar(this, view, R.string.transitsampling_title, R.string.transitcustom_sampling_title);
//		Dashboard.customizeHeadings(this, view, new int[]{R.id.ResultHead,R.id.YourDeviceHead,R.id.speedtest_carrieravg} );
//		String labelColor = (getResources().getString(R.string.SPEED_CUSTOM_LABEL_COLOR));
//		Dashboard.customizeSimpleLabelsColor(view,new int[]{R.id.latencyHead,R.id.DownloadHead,R.id.UploadHead,R.id.speedtest_latency,R.id.speedtest_download,
//			R.id.speedtest_upload,R.id.speedtest_carrier_latency,R.id.speedtest_carrier_download,R.id.speedtest_carrier_upload},labelColor);		
		
		roundTripCheckBox = (CheckBox) view.findViewById(R.id.roundTripCheckBox);
		citySpinner = (Spinner) view.findViewById(R.id.citySpinner);
		lineSpinner = (Spinner) view.findViewById(R.id.lineSpinner);
		linearLayout1 = (LinearLayout) view.findViewById(R.id.linearLayout1);
		linearLayout2 = (LinearLayout) view.findViewById(R.id.linearLayout2);
		fromSpinner = (Spinner) view.findViewById(R.id.fromSpinner);
		linearLayout3 = (LinearLayout) view.findViewById(R.id.linearLayout3);
		endSpinner = (Spinner) view.findViewById(R.id.endSpinner);
		busyIndicator = (ProgressBar) view.findViewById(R.id.busyIndicator);
		startStationButton = (Button) view.findViewById(R.id.startStationButton);
		
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//		if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
//			useLocationAlert();
//		}

		citySpinner.setOnTouchListener(cityOnTouchListener);
		citySpinner.setOnItemSelectedListener(cityOnItemSelectedListener);
		
		fromSpinner.setOnTouchListener(fromOnTouchListener);
		fromSpinner.setOnItemSelectedListener(fromOnItemSelectedListener);

		endSpinner.setOnTouchListener(endOnTouchListener);
		endSpinner.setOnItemSelectedListener(endOnItemSelectedListener);
		
		lineSpinner.setOnTouchListener(lineOnTouchListener);
		lineSpinner.setOnItemSelectedListener(lineOnItemSelectedListener);
		
		transitDB = new	TransitDatabaseReadWriteNew(this);
		startLocation();
		
//		getAreaInformation();
//		getEstimatedPrefillInfo();
		
		
//		Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//		double latitude = 0;
//		double longitude = 0;
//		if(lastKnownLocation != null) {
//			latitude = (lastKnownLocation.getLatitude());
//			longitude = (lastKnownLocation.getLongitude());
//		}
	}
	
	private LocationRequest locationRequest;
	public void startLocation() {
		locationRequest = new LocationRequest (this, 400);
		locationRequest.setUpdateUI(true);
		locationRequest.setOnNewLocationListener(new LocationRequest.OnLocationListener() {
			@Override
			public void onLocation(LocationRequest locationRequest) {
				if (locationRequest.bLocationChanged == false)
					return;
				Location location = locationRequest.getLocation();
				if (untouched == true)
					getEstimatedPrefillInfo();
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
			if (untouched == true)
				getEstimatedPrefillInfo();
//			getCarrierSpeed(location);
//			statsLocation = location;
//			nearLocation = location;
//			showAddress (location);
//			bFreshGps = true;
//			lastLocationTime = System.currentTimeMillis();
		}
		
		
	};

	public void onSettingsClicked(View view) {
		Intent intent = new Intent(this, TransitSamplingSettings.class);
		startActivity(intent);
	}

	public void onHistoryClicked(View view) {
		Intent intent = new Intent(TransitSampling.this, EventHistory.class);
		intent.putExtra("fromTransit", 1);
		startActivity(intent);
	}

	public void onExitClicked(View view) {
		Intent returnIntent = new Intent();
		setResult(RESULT_OK, returnIntent);
		this.finish();
	}

	public void onCreateClicked(View view) {
		
//		if(busyIndicator.isActivated()) {
//			//This is a simple way to show that an api call is in progress
//			return;
//		}
		
		if(isAPICallInProgress()) { //this returns true if some are running
			return;
		}
		String current = fromSpinner.getSelectedItem().toString();
		startStation = current;
		current = endSpinner.getSelectedItem().toString();
		stopStation = current;
		
		boolean hasStations = transitDB.getHasStations(transportId);
		if(hasStations) {
//			System.out.println(transportId + " has stations already in polyline");
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "onCreateClicked", transportId + " has stations already in polyline");
			//return; 
		}
		else {
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "onCreateClicked", transportId + " does not have polyline or stations yet, starting download");
			getPolyline(); 
		}
		
		if(validate()) {
			startStationButton.setEnabled(false);
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "onCreateClicked",  "valid itinerary: startStation=" +  startStation + ", stopStation=" + stopStation);
			//int arrivalId = 0, departId = 0;
			for(int i = 0; i < stations.size(); i ++) { 
				TransitInfo station = stations.get(i);
				if(station.getName().equals(stopStation)) {
					arrivalId = station.getStationId();//transitDB.getStopId(stopStation, station.getStationId(), transportId);
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "onCreateClicked",  "arrivalId = " + arrivalId);
					if (departId == 0)
					{
						Toast.makeText(this, this.getString(R.string.transit_final_station_error), Toast.LENGTH_LONG).show();
						return;
					}	
				}
				if(station.getName().equals(startStation)) {
					departId = station.getStationId();// transitDB.getStopId(startStation, station.getStationId(), transportId);
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "onCreateClicked",  "departId = " + departId);
				}
				
			}
			if(departId == 0 || arrivalId == 0)
			{
				Toast.makeText(this, "Error saving itinerary: departId=" + departId + ", arrivalId=" + arrivalId, Toast.LENGTH_LONG).show();
				return;
			}
			
			transitDB.saveItinerary(transportId, departId, arrivalId);
			
			//transitDB.saveItinerary(transportId, departId, arrivalId);
//			if(roundTripCheckBox.isChecked()) {
//				//Save trip in reverse
//				transitDB.saveItinerary(transportId, arrivalId, departId);
//			}

			//if(hasStations) 
//			{
//				Intent intent = new Intent(this, TransitSamplingMap.class);
////				intent.putExtra("stops", wrapper);
//				intent.putExtra("start", departId);
//				intent.putExtra("stop", arrivalId);
//				intent.putExtra("transport_id", transportId);
//				startActivity(intent);
//				this.finish();
//			}
		}
	}

	public void cancelEvent() {
		Intent intent = new Intent(MMCIntentHandlerOld.MANUAL_TRANSIT_CANCEL);
		sendBroadcast(intent);
	}

	public void useLocationAlert() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(getString(R.string.transitsampling_location_alert));
		alert.setCancelable(false);
		alert.setPositiveButton(getString(R.string.GenericText_OK), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				setLocation();
				dialog.dismiss();
			}
		});
		alert.setNegativeButton(getString(R.string.GenericText_No), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alert.show();
	}

	android.widget.AdapterView.OnItemSelectedListener cityOnItemSelectedListener = new android.widget.AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
			untouched = false;
			if(!prefilling) {
				try {
					int citySpinnerSize = citySpinner.getAdapter().getCount();
					String cityName = citySpinner.getSelectedItem().toString();
					if(citySpinnerSize > 1 && !previousCity.equals(cityName)) {
						previousCity = cityName;
						int cityId = transitDB.getCityId(cityName);
						if(cityId != 0) {
							//City was already saved in the DB
							getTransports(0,0,0,false, cityId);
						}
						else {
							//Search for city by name
							getCity(0, 0, 1, false, cityName);
						}
					}
				} catch(Exception e) {
					e.printStackTrace(); //saw 1 error were city name didn't populate in prefill but rest did? Haven't found reason yet
				}
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			//Do nothing
		}
	};
	
	android.widget.AdapterView.OnItemSelectedListener lineOnItemSelectedListener = new android.widget.AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
			linearLayout1.setBackgroundColor(Color.rgb(54, 180, 227));
			linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
			untouched = false;
			
			try {
				if(!prefilling && transportId != 0) { 
					int lineSpinnerSize = lineSpinner.getAdapter().getCount();
					String lineName = lineSpinner.getSelectedItem().toString();
	//				int tempId = transitDB.getTransportIdFromName(lineName);;
	//				if(tempId != 0)
	//					transportId = transitDB.getTransportIdFromName(lineName);
					for(int i = 0; i < transports.length; i++) {
						if(transports[i][0].equals(lineName))
							transportId = Integer.valueOf(transports[i][1]);
					}
					//Location lastKnownLocation = ReportManager.getInstance(getApplicationContext()).getLastKnownLocation();
					Location lastKnownLocation = locationRequest.getLocation();
					double latitude = 0;
					double longitude = 0;
					if(lastKnownLocation != null) {
						latitude = (lastKnownLocation.getLatitude());
						longitude = (lastKnownLocation.getLongitude());
					}
					if(lineSpinnerSize > 1 &&  !previousLine.equals(lineName)) {
						previousLine = lineName;
						getStations(latitude, longitude, 0, false); //need to flag this to set start and end stations lists 
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			//Do nothing
		}
	};
	
	android.widget.AdapterView.OnItemSelectedListener endOnItemSelectedListener = new android.widget.AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
			//linearLayout3.setBackgroundColor(Color.rgb(54, 180, 227));
			//linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			//linearLayout3.setBackgroundColor(Color.rgb(54, 180, 227));
			//linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
			showList(STOP_STATION);
		}
	};
	
	android.widget.AdapterView.OnItemSelectedListener fromOnItemSelectedListener = new android.widget.AdapterView.OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
			//linearLayout2.setBackgroundColor(Color.rgb(54, 180, 227));
			//linearLayout1.setBackgroundColor(Color.rgb(204, 204, 204));
			untouched = false;
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			//linearLayout2.setBackgroundColor(Color.rgb(54, 180, 227));
			//linearLayout1.setBackgroundColor(Color.rgb(204, 204, 204));
		}
	};
	
	android.view.View.OnTouchListener cityOnTouchListener = new android.view.View.OnTouchListener()  {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			//Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			//Location lastKnownLocation = ReportManager.getInstance(getApplicationContext()).getLastKnownLocation();
			Location lastKnownLocation = locationRequest.getLocation();
			double latitude = 0;
			double longitude = 0;
			if(lastKnownLocation != null) {
				latitude = (lastKnownLocation.getLatitude());
				longitude = (lastKnownLocation.getLongitude());
			}
			
			linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
			linearLayout1.setBackgroundColor(Color.rgb(204, 204, 204));
			
			//Search for 3 closest cities
			getCity(latitude, longitude, 3, false, null);
			return false;
		}
	};
	
	
	android.view.View.OnTouchListener lineOnTouchListener = new android.view.View.OnTouchListener()  {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
//			Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//			double latitude = 0;
//			double longitude = 0;
//			if(lastKnownLocation != null) {
//				latitude = (lastKnownLocation.getLatitude());
//				longitude = (lastKnownLocation.getLongitude());
//			}
//			getTransports(latitude, longitude, 5, false, 0);
			
//			String cityName = citySpinner.getSelectedItem().toString();
//			int cityId = transitDB.getCityId(cityName);
//			if(cityId != 0) {
//				//City was already saved in the DB
//				getTransports(0,0,0,false, cityId);
//			}
//			else {
//				getTransports(latitude, longitude, 5, false, 0);
//			}
			
			linearLayout1.setBackgroundColor(Color.rgb(54, 180, 227));
			linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
			
			return false;
		}
	};
	
	android.view.View.OnTouchListener endOnTouchListener = new android.view.View.OnTouchListener()  {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			//linearLayout3.setBackgroundColor(Color.rgb(54, 180, 227));
			//linearLayout2.setBackgroundColor(Color.rgb(204, 204, 204));
			String current = endSpinner.getSelectedItem().toString();
			int index = 0;
			String[] info = new String[stations.size()-1];
	    	//Not allowing user to choose the last station in the line bc then there is nothing to sample and also 
	    	//TransitSamplingMap will have issues with it
	    	for(int i = 0; i < stations.size()-1; i++) {
	    		String station = stations.get(i).getName();
	    		info[i] = station;
	    		if(station.equals(current)) {
	    			index = i;
	    		}
	    	}
			//showList(STOP_STATION);
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
	    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	endSpinner.setAdapter(null);
	    	endSpinner.setAdapter(adapter);
	    	endSpinner.setSelection(index);
	    	
			//temporarilyDisableButton(endSpinner);
			return false;
		}
	};
	
	android.view.View.OnTouchListener fromOnTouchListener = new android.view.View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent event) {
			linearLayout2.setBackgroundColor(Color.rgb(54, 180, 227));
			if (fromSpinner.getSelectedItem() == null)
				return false;
//			getStations(0, 0, 1, true);

			String current = fromSpinner.getSelectedItem().toString();
			int index = 0;
			if(stations.size() == 1) {
				MMCLogger.logToTransitFile(MMCLogger.Level.ERROR, TAG, "fromOnTouchListener", "line has only 1 station");
				return false;
			}
	    	String[] info = new String[stations.size()-1];
	    	//Not allowing user to choose the last station in the line bc then there is nothing to sample and also 
	    	//TransitSamplingMap will have issues with it
	    	for(int i = 0; i < stations.size()-1; i++) {
	    		String station = stations.get(i).getName();
	    		info[i] = station;
	    		if(station.equals(current)) {
	    			index = i;
	    		}
	    	}
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
	    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	fromSpinner.setAdapter(null);
	    	fromSpinner.setAdapter(adapter);
	    	fromSpinner.setSelection(index);
	    	
//			linearLayout3.setBackgroundColor(Color.rgb(204, 204, 204));
//			showList(START_STATION);
//			temporarilyDisableButton(fromSpinner);
			return false;
		}
	};
	
	public void lookForCity() {
		String name = citySpinner.getSelectedItem().toString();
		int cityId = transitDB.getCityId(name);
		if(cityId == 0) {
			//Maybe area name is needed not city name
			cityId = transitDB.getAreaIdFromAreaName(name);
			if(cityId != 0)
				areaId = cityId;
		}
		if(areaId == 0)
			areaId = transitDB.getAreaIdFromCity(name, cityId);
		
		//If area id is still null, this area does not exist in the database
		if(areaId == 0) {
			Toast.makeText(TransitSampling.this, getString(R.string.transitsampling_city_invalid), Toast.LENGTH_LONG).show();
			return;
		}
		
//		getTransports();
			
//		if(!citySpinner.enoughToFilter())
//			citySpinner.showDropDown();

//		lineSpinner.dismissDropDown();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);

	    if(resultCode == START_STATION) {
	    	int index = 0;
	    	String selection  = data.getStringExtra("stop");
	    	startStation = selection;
	    	String[] info = new String[stations.size()];
	    	for(int i = 0; i < stations.size(); i++) {
	    		String station = stations.get(i).getName();
	    		if(station.equals(selection)) {
	    			index = i;
	    			info[i] = station;
//	    			break;
	    		}
	    	}
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
	    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	fromSpinner.setAdapter(null);
	    	fromSpinner.setAdapter(adapter);
	    	fromSpinner.setSelection(index);
	    	previousLine = info[index]; 
	    }
	    else if (resultCode == STOP_STATION) {
	    	int index = 0;
	    	String selection  = data.getStringExtra("stop");
	    	stopStation = selection;
	    	String[] info = new String[stations.size()];
	    	for(int i = 0; i < stations.size(); i++) {
	    		String station = stations.get(i).getName();
	    		if(station.equals(selection)) {
	    			index = i;
	    			info[i] = station;
	    			break;
	    		}
	    	}
	    	ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
	    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	endSpinner.setAdapter(null);
	    	endSpinner.setAdapter(adapter);
	    	endSpinner.setSelection(index);
	    }
	    else if(resultCode == RESULT_ERROR) {
	    	int type = 0;
	    	if(data.hasExtra("type"))
	    		type = data.getIntExtra("type", 0);
	    	if(type == START_STATION && startStation == null)//if not null keeping previous selection
	    		Toast.makeText(this, "No start station selected", Toast.LENGTH_SHORT).show();
	    	if(type == STOP_STATION && stopStation == null) //if not null keeping previous selection
	    		Toast.makeText(this, "No stop station selected", Toast.LENGTH_SHORT).show();
	    }
	}

	public void showList(int type) {
		Intent intent = new Intent(TransitSampling.this, TransitSamplingListView.class);
		int size = stations.size();
		String[] stops = new String[size];
		
		for(int i = 0; i < size; i++) {
			TransitInfo station = stations.get(i);
			stops[i] = station.getName();
		}
		
		//starting or stopping station
		intent.putExtra("stationtype", type);
		intent.putExtra("stops", stops);
		startActivityForResult(intent, type);
	}

	public void setLocation() {
		try {
			//Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			//Location lastKnownLocation = ReportManager.getInstance(getApplicationContext()).getLastKnownLocation();
			Location lastKnownLocation = locationRequest.getLocation();
			if(lastKnownLocation != null) {
				double latitude = (lastKnownLocation.getLatitude());
				double longitude = (lastKnownLocation.getLongitude());

				Geocoder geocoder = new Geocoder(TransitSampling.this);
				List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
				String addressString = null;
				Address address = null;
				if(addresses != null && addresses.size() > 0) {
					address = addresses.get(0);
					if(address.getLocality() != null)
						addressString = address.getLocality();
					//Some areas locality returns null but have a sub-admin part of the geolocation
					else if(address.getSubAdminArea() != null)
						addressString = address.getSubAdminArea();
				}
				if(addressString != null) {
					String[] city = {addressString};
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, city);
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					citySpinner.setAdapter(null);
					citySpinner.setAdapter(adapter);
					citySpinner.setSelection(0);
					previousCity = addressString;
					
//					citySpinner.setText(addressString);
					areaId = transitDB.getAreaIdFromAreaName(addressString.toString());
				}
				else {
					Toast.makeText(this, getString(R.string.transitsampling_error_geocoder), Toast.LENGTH_SHORT).show();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void getLines() {
//		if(transports == null)
//			return;
//
//		String[] lines = new String[transports.length];
//		for(int i = 0; i < transports.length; i++) {
//			lines[i] = transports[i][1];
//		}
//		lines = removeDuplications(lines);
//		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, lines);
//		lineSpinner.setAdapter(adapter2);
//		lineSpinner.setEnabled(true);
	}

//	public String[] removeDuplications(String[] lines) {
//		Set<String> uniqueWords = new HashSet<String>(Arrays.asList(lines));
//		Object[] array = uniqueWords.toArray();
//		String[] uniqueLines = new String[array.length];
//		int i = 0;
//		for(Object obj: array) {
//			if(obj != null) {
//				uniqueLines[i] = (String) obj;
//				i++;
//			}
//		}
//		return uniqueLines;
//	}
	
	public boolean validate() {
		if(startStation == null) {
			Toast.makeText(this, getString(R.string.transitsampling_error_startstation), Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(stopStation == null) {
			Toast.makeText(this, getString(R.string.transitsampling_error_stopstation), Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	public void temporarilyDisableButton(final View view) {
		if(view == null)
			return;
		
		view.setEnabled(false);		
		
		new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);  
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                TransitSampling.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setEnabled(true);
                    }
                });
            }
        }).start();
	}
	
	public void populateCityDropDown(String[] cities) {	
		if(cities == null)
			cities = transitDB.getCities();
		//cities = removeDuplications(cities);
		ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, cities);
		citySpinner.setAdapter(null);
		citySpinner.setAdapter(adapter2);
	}
	
/*	public void populateRoutesFromDB(int areaId) {
		List<CityInfo> lines = transitDB.getTransportsAndIds(areaId);
		transports = new String[lines.size()][2];
		for(int i = 0; i < lines.size(); i++) {
			CityInfo info = lines.get(i);
			transports[i][0] = String.valueOf(info.getId());
			transports[i][1] = info.getName();
		}
	} */
	
	public void getTransports(final double latitude, final double longitude, final int limit, final boolean prefill, final int cityId) {
		busyIndicator.setVisibility(View.VISIBLE);
		transportAsyncTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {	
				try {
					String location = "";
					if(latitude != 0 && longitude != 0) {
						//if lat and long are 0 then an empty location will return all cities in database
						location = "lat=" + latitude + "&lng=" + longitude;
					}
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getTransports", "requesting with lat/lon = " + location);

//						HttpParams httpParameters = new BasicHttpParams();
//						int timeoutConnection = 10000;
//						HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//						int timeoutSocket = 10000;
//						HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//
//						DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
					//String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";

//https://dev.mymobilecoverage.com/api/transit/transports?lat=50.6731517&lng=-120.316971&limit=1&apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc&fields=id,sname,url,city_id,agency_id,route_type,area_id,lname,route_id
					String apiKey = MMCService.getApiKey(TransitSampling.this);
					String URL = getString(R.string.MMC_URL_LIN);
					String api = "/api/transit/";
					String fields = "&fields=id,sname,url,city_id,agency_id,route_type,area_id,trip_headsign,lname,route_id&transittype=0,1,2";
					if(!prefill && cityId != 0) {
						location += "transitarea=" + cityId;

					}
					//https://dev.mymobilecoverage.com/api/transit/transports?&limit=1&apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc&fields=id,sname,url,city_id,agency_id,route_type,area_id,lname,route_id
//						HttpGet request = new HttpGet(URL + api + "transports?" + location + "&limit=" + limit + "&apiKey=" + apiKey + fields);

					//ie: Tue, 22 Nov 2011 20:56:21 GMT
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");
					Calendar calendar = new GregorianCalendar();
					String date = simpleDateFormat.format(calendar.getTime());
//						request.setHeader("Content-Type", "application/json; charset=utf-8");
//						request.setHeader("Date:", date);
//
//						HttpResponse response = httpClient.execute(request);
//						String responseContents = "";
//						try {
//							responseContents = EntityUtils.toString(response.getEntity());
//						} catch(Exception e) {
//							MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getEstimatedPrefillInfo", "Error responseContents " + e.getMessage());
//						}

					URL url = new URL(URL + api + "transports?" + location + "&limit=" + limit + "&apiKey=" + apiKey + fields);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(10000);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conn.setRequestProperty("Date:", date);
					conn.connect();
					String responseContents = WebReporter.readString(conn);

					return readTransportFields(responseContents);

				} catch (Exception e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getTransports", "Exception ", e);
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(String line) {
				if(line != null) {
					int index = 0;
					String[] info = new String[transports.length];
					for(int i = 0; i < transports.length; i++) {
						info[i] = transports[i][0];
						if(transports[i][0] != null && transports[i][0].equals(line)) {
							index = i;
						}
					}
					//info = removeDuplications(info);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			    	lineSpinner.setAdapter(null);
			    	lineSpinner.setAdapter(adapter);
			    	if(prefill) {
			    		lineSpinner.setSelection(index);
			    		previousLine = info[index];
			    	}
					busyIndicator.setVisibility(View.GONE);
					//Populate start station with closest station
					if(prefill)
						getStations(latitude, longitude, 0, false);
					else if(cityId != 0) {
						getStations(0, 0, 1, false); 
					}
				}
				busyIndicator.setVisibility(View.GONE);
				transportAsyncTask = null;
			}
		}.execute((Void[])null);
	} 
	
	public String readStationJson(InputStream in, int limit, int transportId) throws IOException {
		JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
		jsonReader.setLenient(true);
		jsonReader.beginObject();
		Gson gson = new Gson();
		stations = new ArrayList<TransitInfo>();
		boolean firstStation = true;
		String stationName = null;
		
		while (jsonReader.hasNext()) {
			String name = "";
			try {
				name = jsonReader.nextName();
				System.out.println("name: " + name);
			} catch(Exception e) {
				jsonReader.skipValue();
			}
			
			if (name.equals("success")) {
				boolean success = jsonReader.nextBoolean();
				if(success == false) {
					jsonReader.endObject();
					jsonReader.close();
					return null;
				}
			}
			else if(name.equals("values")) {
				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Starting to process stations");
				jsonReader.beginArray();
				//int i = 0;
				while (jsonReader.hasNext()) {
					Stations station = gson.fromJson(jsonReader, Stations.class);
					
					int stationId = station.getId();
					String stopName = station.getStopName();
					String stopId = station.getStopId();
					int stopSequence = station.getStopSequence()-1;
					GeomPoint geom = station.getGeomPoint();
					double[] point = geom.getCoordinates();
					int longitude = (int) (point[0] * 1000000);
					int latitude = (int) (point[1] * 1000000);
					double distance = station.getDistance();
					Duration duration = station.getDuration();
					int minutes = duration.getMinutes();
					int seconds = minutes * 60;
					if (limit != 1)
					{
						stations.add(new TransitInfo(stopName, stationId, new SerializableGeoPoint(latitude, longitude)));
						if(!transitDB.doesStationAlreadyExist(stationId, String.valueOf(transportId)))
							transitDB.saveStation(stationId, stopName, areaId, latitude, longitude, distance, seconds, stopSequence, transportId);
						//System.out.println("station process = " + ++i + " stop name " + stopName + ", transportId: " + transportId);
					}
					if(firstStation == true) {
						stationName = stopName;
						firstStation = false;
					}
				}
				jsonReader.endArray();
			}
		}
		jsonReader.close();
		return stationName;
	}
	
/*	public void getAreaInformation() {
		busyIndicator.setVisibility(View.VISIBLE);
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
//				final String apiKey = PreferenceManager.getDefaultSharedPreferences(this).getString(PreferenceKeys.User.API_KEY, null);
				String url = getString(R.string.MMC_URL_LIN) + "/api/transit?areas";
				HttpClient client = new DefaultHttpClient();
				String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
//				https://dev.mymobilecoverage.com/api/transit/areas?apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc
				HttpGet get = new HttpGet("https://dev.mymobilecoverage.com/api/transit/areas?apiKey=" + apiKey);
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");	//ie: Tue, 22 Nov 2011 20:56:21 GMT
				Calendar calendar = new GregorianCalendar();
				String date = simpleDateFormat.format(calendar.getTime());
				get.setHeader("Content-Type", "application/json; charset=utf-8");
				get.setHeader("Date:", date);
				HttpResponse response = null;
				
				try {
					response = client.execute(get);
				} catch(Exception e) {
					System.out.println(e);
				}
				String responseContents = "";
				try {
					responseContents = EntityUtils.toString(response.getEntity());
				} catch(Exception e) {
					System.out.println(e);
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getAreaInformation", "Error requesting areas: " + e.getMessage());
				}
				
				return responseContents;
			}
				
			@Override
			protected void onPostExecute(String responseContents) {
				saveAreas(responseContents);
				busyIndicator.setVisibility(View.GONE);
			}			
		}.execute((Void[])null);
	} */
	
/*	public void saveAreas(String areas) {
		JSONObject json = null;
		JSONArray jsonArray;	
		String areaName = null;
		int areaId = 0;
		List<String> citiesToDisplay = new ArrayList<String>();
		
		if(areas == null)
			return;
		
		try {
			json = new JSONObject(areas);	
			String success = json.getString("success");
			
			if(success.equals("false")) {
				//Server request failed, but see if there are areas in the database 
				populateCityDropDown(null);
				return;
			}
			
			jsonArray = json.getJSONArray("values");
			int total = json.getInt("count");

			for(int i = 0; i < total; i++) {
				json = jsonArray.getJSONObject(i);
				
				areaName = json.getString("area_name");
				areaId = json.getInt("area_id");
				
				boolean newArea = transitDB.doesAreaExist(areaId);
				
				//if area doesnt have data yet, or area is out of date clean up data
				if(!transitDB.doesAreaAlreadyHaveData(areaId) || 
						transitDB.isAreaCurrent(areaId) == false || newArea) {
					transitDB.deleteStops(areaId);
					transitDB.deleteTransports(areaId);
					transitDB.deleteShapes(areaId);
					transitDB.deleteArea(areaId);
					transitDB.deleteCities(areaId);
					
					newArea = true;
					transitDB.saveArea(areaName,areaId);
				}
			
				String cities = json.getString("cities");			
				JSONArray citiesArray = new JSONArray(cities); 
					
				for(int k = 0; k < citiesArray.length(); k++) {
					JSONObject cityJson =  citiesArray.getJSONObject(k);
					String name = cityJson.getString("city_name");
						if(newArea) {
							int id = cityJson.getInt("city_id");
							transitDB.saveCity(name, id, areaId);
						}
					citiesToDisplay.add(areaName);
				}
			}
			
			String[] citiesToStringArray = (String[]) citiesToDisplay.toArray(new  String[citiesToDisplay.size()]);
			populateCityDropDown(citiesToStringArray);
		}
		catch (JSONException e) {
			e.printStackTrace();
			return;
		}
	} */

	public void saveStationsToPolyline() {
//		busyIndicator.setVisibility(View.VISIBLE);
//		new AsyncTask<Void, Void, String>() {
//			@Override
//			protected String doInBackground(Void... params) {	
				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "saveStationsToPolyline", "starting transit math");
				 TransitSamplingMapMath transitMath = new TransitSamplingMapMath();

				 List<GeoPoint> points = new ArrayList<GeoPoint>();
				 for(int i = 0; i < polyline.length; i++) {
					 int lat = (int) (polyline[i][1] * 1000000.0);
					 int lon = (int) (polyline[i][0] * 1000000.0);
					 GeoPoint point = new GeoPoint(lat, lon);
					 points.add(point);
				 }
			
				 //boolean firstStationLine = true;
				 //List<GeoPoint> endStationLine = new ArrayList<GeoPoint>();
				 List<GeoPoint> stationLine = new ArrayList<GeoPoint>();
				 stationLine.add (points.get(0));
				 //GeoPoint lastPointInPrevousLine = points.get(0);
				 
				 int size = stations.size(), h = 0;
				 if(size == 0)
					 return;
		 
				 //Loop for every station but first and last
				 for(int i = 0; i < size; i++) { //First and last stations don't have a line, just snap to first or last point on polyline
					 TransitInfo station = stations.get(i);
					 int stationId = station.getStationId();
					 if (i == size-1) // last station
					 {
						//Add station line for last station, with just the snap-point of the last station
						 transitDB.saveIntersect(stationId, stationLine.get(0), transportId);
						 transitDB.saveStationLine(stationId, stationLine, transportId);
						 break;
					 }
					 TransitInfo nextstation = stations.get(i+1);
					 
					 if (stationId == 51160)
						 h = 51160;
					 //int lat = nextstation.getGeoPoint().getLatitudeE6();
					 //int lon = nextstation.getGeoPoint().getLongitudeE6();
					 GeoPoint stationLocation = nextstation.getGeoPoint().toGeoPoint();// new GeoPoint(lat, lon);
			
//					 if (h != 51160)
//						 continue;
					 for(int index = 0; index < points.size()-1; index++) {
						 GeoPoint point = points.get(index);
						 GeoPoint nextPoint = points.get(index+1);
						 points.remove(point);
						 index --;
					
						 double lAl = TransitSamplingMapMath.distanceTo(point, stationLocation);				
						 double lBl = TransitSamplingMapMath.distanceTo(point, nextPoint);
					
						 if(lBl == 0)
						 	 continue;
						 
//						 if(firstStationLine) {
//							 //Accumulate points until the second station intersects the polyline
//							 endStationLine.add(point);
//						 }
//					
//						 if(lastPointInPrevousLine != null)
//							 stationLine.add(lastPointInPrevousLine);
//						 lastPointInPrevousLine = null;
						 
						 if(lAl < lBl + 0.001) {
							 	
							 double lCl = transitMath.findlCl(point, nextPoint, stationLocation, lBl);
							 //System.out.println("lAl = " + lAl + ", lBl = " + lBl + ", lCl = " + lCl);
							 if(lCl <= lBl || index == points.size()-2) //then not a right triangle
							 {
		
								 double lDl = transitMath.findlDl(lCl, lAl);
								 //System.out.println("lDl = " + lDl);
									
								 GeoPoint intersect = transitMath.findNextGeoPoint(point, nextPoint, lCl);
								 if(lDl <= 0.0005) { //20 meters
										
//									 if(firstStationLine) {
//										 firstStationLine = false;
//										 int id = stations.get(0).getStationId();
//										 transitDB.saveIntersect(id, points.get(0));
//										 transitDB.saveStationLine(id, endStationLine);
//										 System.out.println("saved start station line");
//										 endStationLine = new ArrayList<GeoPoint>();
//									 }
									 
									 //This is where the station intersects the line
									 //To know later that this isn't just another point in the polyline, save this point to its station in the DB
									 transitDB.saveIntersect(stationId, intersect, transportId);
									 stationLine.add (intersect);
									 transitDB.saveStationLine(stationId, stationLine, transportId);
									 //System.out.println("saved");
									 //points.add(index+1, intersect); //add station intersect point in and shift the rest over
									 transitDB.updateHasStations(transportId, true);
									 stationLine.clear();
									 stationLine.add (intersect);
									 //lastPointInPrevousLine = intersect;
										break;
								 }
							 }
						 }
						 stationLine.add(point);
						 
					 }
					 //Start over for next station line
					 //stationLine.removeAll(stationLine);
				 }
				 
				 //Add station line for last station
//				 if(lastPointInPrevousLine != null) {
//					 int id = stations.get(size-1).getStationId();
//					 transitDB.saveIntersect(id, points.get(points.size()-1));
//					 transitDB.saveStationLine(id, points); //What is left after the last station intersect
//					 System.out.println("saved end station line");
//				 }
//				return null;
			}
			
//			@Override
//			protected void onPostExecute(String station) {
//				 busyIndicator.setVisibility(View.GONE);
//				 Intent intent = new Intent(TransitSampling.this, TransitSamplingMain.class);
//				 startActivity(intent);
//				 TransitSampling.this.finish();
//			}
//      }.execute((Void[])null);
//	}

	public void getStations(final double latitude, final double longitude, final int limit, final boolean arrivalField) {
		busyIndicator.setVisibility(View.VISIBLE);
		stationAsyncTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {	
				String responseContents = null;
				try {
					String location = "";
					if(latitude != 0 && longitude != 0) {
						//if lat and long are 0 then an empty location will return all cities in database
						location = "lat=" + latitude + "&lng=" + longitude;
					}
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getStations", "requesting with lat/lon = " + location);
//					https://dev.mymobilecoverage.com/api/transit/stations?&limit=0&apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc&transitroute=760
//					HttpParams httpParameters = new BasicHttpParams();
//					int timeoutConnection = 10000;
//					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//					int timeoutSocket = 10000;
//					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//
//					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
//					//String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
//					//https://dev.mymobilecoverage.com/api/transit/stations?lat=50.6730069&lng=-120.3169415&limit=1&apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc&transitroute=451
//
					//String test = "https://dev.mymobilecoverage.com/api/transit/stations?transitarea=1&route_type=2&apiKey=16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
					String apiKey = MMCService.getApiKey(TransitSampling.this);
					String URL = getString(R.string.MMC_URL_LIN);
					String api = "/api/transit/stations";
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");
					Calendar calendar = new GregorianCalendar();
					String date = simpleDateFormat.format(calendar.getTime());


//					HttpGet request = new HttpGet(URL + api + "?" + location + "&transitroute=" + transportId + "&route_type=0&apiKey=" + apiKey);
//					HttpGet request = new HttpGet(URL + api + "?" + location + "&limit=" + limit + "&apiKey=" + apiKey + "&transitroute=" + transportId);
//
//					request.setHeader("Content-Type", "application/json; charset=utf-8");
//					request.setHeader("Date:", date);
//
//					HttpResponse response = httpClient.execute(request);
//
//					HttpEntity responseEntity = response.getEntity();
//					InputStream in = responseEntity.getContent();

					URL url = new URL(URL + api + "?" + location + "&limit=" + limit + "&apiKey=" + apiKey + "&transitroute=" + transportId);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(10000);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conn.setRequestProperty("Date:", date);
					conn.connect();

					return readStationJson(conn.getInputStream(), limit, transportId);

				} catch (Exception e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getStations", "Exception ", e);
				}
				return responseContents;
			}
			
			@Override
			protected void onPostExecute(String station) {
				busyIndicator.setVisibility(View.GONE);
				
				if(arrivalField && stations != null) {
					if(stations.size() == 0) {
						MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getStations, onPostExecute", "stations = 0");
						return; 
					}
					String[] info = new String[stations.size()];
					for(int i = 0; i < stations.size(); i++) {
						info[i] = stations.get(i).getName();
					}
					//info = removeDuplications(info);
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			    	endSpinner.setAdapter(null);
			    	endSpinner.setAdapter(adapter);
//			    	if(latitude != 0 && longitude != 0) {
//			    		endSpinner.setSelection(0); 
//			    		stopStation = info[0];
//			    	}
//			    	else 
			    	{
			    		endSpinner.setSelection(info.length-1);
			    		stopStation = info[info.length-1];
			    	}
			    	prefilling = false;
				}
				else { //start field
					if(station == null)
						return;
					String[] info;
					if (stations.size() == 0 )
					{
						info = new String[1];
						info[0] = station;
					}
					else
					{
						info = new String[stations.size()];
						for(int i = 0; i < stations.size(); i++) 
						{
							info[i] = stations.get(i).getName();
						}
					}
					prefilling = false;
					
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(TransitSampling.this, android.R.layout.simple_spinner_item, info);
			    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			    	fromSpinner.setAdapter(null);
			    	fromSpinner.setAdapter(adapter);
			    	fromSpinner.setSelection(0);
			    	startStation = info[0];
			    	//stopStation = info[info];
			    	//Populate End station with first and last stations (for now show all until approved)
					getStations(0, 0, 0, true);
				}
				stationAsyncTask = null;
			}			
		}.execute((Void[])null);
	} 
	
/*	public String readTransportJson(InputStream in) throws IOException {
		JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
		jsonReader.setLenient(true);
		jsonReader.beginObject();
		Gson gson = new Gson();
		int count = 0;
		String line = null;
		boolean firstTransport = true;
		
		while (jsonReader.hasNext()) {
			String name = "";
			try {
				name = jsonReader.nextName();
				System.out.println("name: " + name);
			} catch(Exception e) {
				jsonReader.skipValue();
			}
			
			if (name.equals("success")) {
				boolean success = jsonReader.nextBoolean();
				if(success == false) {
					jsonReader.endObject();
					jsonReader.close();
					return null;
				}
			}
			else if (name.equals("count")) {
				count = jsonReader.nextInt();
			}
			else if(name.equals("values")) {
				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Starting to process transports");
				jsonReader.beginArray();
				int i = 0;
//				transports = new String[count][2];
				while (jsonReader.hasNext()) {
					Transports transport = gson.fromJson(jsonReader, Transports.class);
					
					String shortName = transport.getShortName().trim();
					String longName = transport.getLongName().trim();
					int transportId = transport.getTransportId(); 
					int cityId = transport.getCityId();
					String agencyId = transport.getAgencyId();
					int type = transport.getRouteType();
					
					if(firstTransport) //the first transport is always the closest. Might be getting more than 1 to prefill dropdown
						this.transportId = transportId;
					firstTransport = false;
						
					//If this route does not already exist in DB, getRouteCount() will assign 0
					int usageCount = transitDB.getTransportCount(transportId);
					
					transports[i][1] = String.valueOf(transportId);
					transports[i][0] = longName;
					
					if(count == 1) {
						line = longName;
					}
					
					if(transitDB.doesTransportExist(transportId)) {	
						transitDB.saveTransport(shortName, longName, transportId, agencyId, cityId, type, usageCount);
						System.out.println("transport process = " + ++i + " transport name " + transport.getLongName());
						
						GeomLineString geom = transport.getGeom();
						double[][] points = geom.getCoordinates();
						
						for(int m = 0; m < points.length; m++) {
							double[] point = points[m];
							int longitude = (int) (point[0] * 1000000);
							int latitude = (int) (point[1] * 1000000);
							
//							transitDB.savePolyline(areaId, latitude, longitude, transportId);
						}
					}
				}
				//This area has downloaded all of it's routes. Set download_flag to false so we don't download again unless out of date
				transitDB.updateDownloadFlag(areaId, false);
				jsonReader.endArray();
			}
		}
		jsonReader.close();
		return line;
	} */
	
/*	public void launchBarDialog(int max) {	
		int count = 1;
        barProgressDialog = new ProgressDialog(TransitSampling.this);
        barProgressDialog.setTitle("Saving Transports");
        barProgressDialog.setMessage("Progress " + count +"/" + max);
        barProgressDialog.setProgressStyle(barProgressDialog.STYLE_HORIZONTAL);
        barProgressDialog.setProgress(1);
        barProgressDialog.setMax(max);
        barProgressDialog.show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (barProgressDialog.getProgress() <= barProgressDialog.getMax()) {
                        Thread.sleep(2000);
                        updateBarHandler.post(new Runnable() {
                            public void run() {
                                barProgressDialog.incrementProgressBy(2);
                              }
                          });
                        if (barProgressDialog.getProgress() == barProgressDialog.getMax()) {
                            barProgressDialog.dismiss();
                        }
                    }
                } catch (Exception e) {
                }
            }
        }).start();
	} */
	
	public void getCity(final double latitude, final double longitude, final int limit, final boolean prefill, final String cityNameSearch) {
		busyIndicator.setVisibility(View.VISIBLE);
		cityAsyncTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String location = "";
				if(latitude != 0 && longitude != 0) {
					//if lat and long are 0 then an empty location will return all cities in database
					location = "lat=" + latitude + "&lng=" + longitude;
				}
				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getCity", "requesting with lat/lon = " + location);
				if(cityNameSearch != null) {
					location = "&search" + cityNameSearch;
				}
				
				try {
//					HttpParams httpParameters = new BasicHttpParams();
//					int timeoutConnection = 10000;
//					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//					int timeoutSocket = 10000;
//					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//
//					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
					String apiKey = MMCService.getApiKey(TransitSampling.this);
					String URL = getString(R.string.MMC_URL_LIN);
					
					String api = "/api/transit/city?"; //ie: lat=51&lng=-114&search=Calg";
					//ie: Tue, 22 Nov 2011 20:56:21 GMT
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");
					Calendar calendar = new GregorianCalendar();
					String date = simpleDateFormat.format(calendar.getTime());

//					HttpGet request = new HttpGet(URL + api + location + "&limit=" +  limit + "&apiKey=" + apiKey);
//
//					request.setHeader("Content-Type", "application/json; charset=utf-8");
//					request.setHeader("Date:", date);
//
//					System.out.println("prefill value = " + prefill);
//
//					HttpResponse response = httpClient.execute(request);
//					String responseContents = "";
//					try {
//						responseContents = EntityUtils.toString(response.getEntity());
//					} catch(Exception e) {
//						MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getEstimatedPrefillInfo", "Error responseContents " + e.getMessage());
//					}

					URL url = new URL(URL + api + location + "&limit=" +  limit + "&apiKey=" + apiKey);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(10000);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conn.setRequestProperty("Date:", date);
					conn.connect();
					String responseContents = WebReporter.readString(conn);
					return responseContents;
				} catch (Exception e) {
					MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getEstimatedPrefillInfo", "Error getting city " + e.getMessage());
				}
				
				return null;
			}
			
			@Override
			protected void onPostExecute(String responseContents) {
				busyIndicator.setVisibility(View.GONE);
				int cityId = 0;
				if(responseContents != null)
					cityId = readCityJson(responseContents, prefill);
				//Populate Line field with closest transport
				if(prefill) //prefilling to closest transport
					getTransports(latitude, longitude, 0, prefill, 0);
				else if(cityNameSearch != null && cityId != 0) { 
					getTransports(0, 0, 0, false, cityId);
				}
				cityAsyncTask = null;
			}			
		}.execute((Void[])null);
	}
	
	public int readCityJson(String response, final boolean prefill) {
		JSONObject json = null;
		JSONArray jsonArray;
		int cityId = 0;
		String cityName = null;
		String[] cities = null;
		
		try {
			json = new JSONObject(response);	
			String success = json.getString("success");
			
			if(success.equals("false")) {
				//Server request failed
				//TODO: handle this
				return 0;
			}
			
			jsonArray = json.getJSONArray("values");
			int total = json.getInt("count");
			cities = new String[total];
			
			for(int i = 0; i < total; i++) {
				//Either limit is 1 or first one is closest --ATM always limit =1
				//For now populate fields city with first city
				
				json = jsonArray.getJSONObject(i);
				cityName = json.getString("city_name");
				cityId = json.getInt("city_id");
//				double cityLat = json.getDouble("city_lat");
//				double cityLon = json.getDouble("city_lon");
				int areaId = json.getInt("area_id");
//				String lowerCityName = json.getString("lower_city_name");	
				
				if(transitDB.doesCityExist(cityId)) //returns true if it doesn't exist
					transitDB.saveCity(cityName, cityId, areaId);
				
				cities[i] = cityName;
			}
			
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, cities);
	    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	    	citySpinner.setAdapter(null);
	    	citySpinner.setAdapter(adapter);
//	    	citySpinner.setSelection(0);
	    	if(prefill) {
	    		citySpinner.setSelection(0);
	    		previousCity = cityName;
	    	}
			
			return cityId;
		}
		catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	public String readTransportFields(String response) throws JSONException {
		JSONObject json = null;
		JSONArray jsonArray = null;
		String line = null;
		boolean firstTransport = true;
		
		json = new JSONObject(response);	
		String success = json.getString("success");
		
		if(success.equals("false")) {
			return null;
		}
		
		jsonArray = json.getJSONArray("values");
		int total = json.getInt("count");
		
		if(0 == total) { //This can happen in prefill where we found the closest city but trains are more than 10 kms away, so call transport with lat/lon
			try {
				String cityName = citySpinner.getSelectedItem().toString();
				int cityId = transitDB.getCityId(cityName);
				if(cityId != 0) {
					//City was already saved in the DB
					getTransports(0,0,0,false, cityId);
					return null;
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		transports = new String[total][2];
//		transports = new String[total];

		for(int i = 0; i < total; i++) {
			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "readJsonStream", "Starting to process transports");
			json = jsonArray.getJSONObject(i);
			
			String shortName = json.getString("sname").trim();
			String longName = json.getString("trip_headsign").trim() + " - " + json.getString("lname").trim();
			int transportId = json.getInt("id");
			int cityId = json.getInt("city_id");
			String agencyId = json.getString("agency_id").trim();
			int type = json.getInt("route_type");
			
			if(firstTransport)  {//the first transport is always the closest. Might be getting more than 1 to prefill dropdown
				this.transportId = transportId;
				firstTransport = false;
				line = longName;
			}
						
			//If this route does not already exist in DB, getRouteCount() will assign 0
			int usageCount = transitDB.getTransportCount(transportId);
					
			transports[i][0] = longName;
			transports[i][1] = String.valueOf(transportId);;
//			transports[i] = longName;
			
			if(transitDB.doesTransportExist(transportId)) {	
				transitDB.saveTransport(shortName, longName, transportId, agencyId, cityId, type, usageCount);
				//System.out.println("transport process = " + ++i + " transport name " + longName);
			}
		}
		//This area has downloaded all of it's routes. Set download_flag to false so we don't download again unless out of date
		transitDB.updateDownloadFlag(areaId, false);
		
		return line;
	}
	
	public void getEstimatedPrefillInfo() {
		//Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		//Location lastKnownLocation = ReportManager.getInstance(getApplicationContext()).getLastKnownLocation();
		Location lastKnownLocation = locationRequest.getLocation();
		double latitude = 0;
		double longitude = 0;
		if(lastKnownLocation != null) {
			latitude = (lastKnownLocation.getLatitude());
			longitude = (lastKnownLocation.getLongitude());
		}
		
		//latitude = 51.0448;
		//longitude = -114.1027;
		//TODO remove this, for testing only
		
		//Populate City field with closest city
		getCity(latitude, longitude, 1, true, null);
//		//Populate Line field with closest transport
//		getTransports(latitude, longitude, 1);
//		//Populate start station with closest station
//		getStations(latitude, longitude, 1, false);
//		//Populate End station with first and last stations (for now show all until approved)
//		getStations(0, 0, 0, true);
	}
	
	public void getPolyline() { //g 8 b 5 r 4
//		boolean havePolyline = transitDB.getDownloadFlag(transportId);
//		if(havePolyline) {
//			MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getPolyline", "this transport has a polyline already: " + transportId);
//			return; 
//		}
		busyIndicator.setVisibility(View.VISIBLE);
		MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "getPolyline", "Requesting polyline");
		polylineAsyncTask = new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {	
				try {
//					HttpParams httpParameters = new BasicHttpParams();
//
//					int timeoutConnection = 10000;
//					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//					int timeoutSocket = 10000;
//					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//
//					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
					//String apiKey = "16fc03de-9c41-4a17-ae4c-2987d2bb32dc";
					String apiKey = MMCService.getApiKey(TransitSampling.this);;
					String URL = getString(R.string.MMC_URL_LIN);
					String api = "/api/transit/";
					String fields = "&fields=geom";

					String transport = "transitroute=" + transportId;
					//ie: Tue, 22 Nov 2011 20:56:21 GMT
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EE, dd MMM yyyy HH:mm:ss zzz");
					Calendar calendar = new GregorianCalendar();
					String date = simpleDateFormat.format(calendar.getTime());

//					HttpGet request = new HttpGet(URL + api + "transports?" + transport + "&apiKey=" + apiKey + fields);
//
//					request.setHeader("Content-Type", "application/json; charset=utf-8");
//					request.setHeader("Date:", date);
//
//					HttpResponse response = httpClient.execute(request);
//
//					HttpEntity responseEntity = response.getEntity();
//					InputStream in = responseEntity.getContent();

					URL url = new URL(URL + api + "transports?" + transport + "&apiKey=" + apiKey + fields);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setReadTimeout(10000);
					conn.setConnectTimeout(10000);
					conn.setRequestMethod("GET");
					conn.setDoInput(true);
					conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					conn.setRequestProperty("Date:", date);
					conn.connect();
					InputStream in = conn.getInputStream();
	
					readPolyline(in);

				} catch (Exception e) {
					MMCLogger.logToFile(MMCLogger.Level.ERROR, TAG, "getPolyline", "Exception ", e);
				}
				saveStationsToPolyline();
				return null;
			}
			
			@Override
			protected void onPostExecute(String line) {
				busyIndicator.setVisibility(View.GONE);
				polylineAsyncTask = null;
				if (departId > 0 && arrivalId > 0)
				{
					Intent intent = new Intent(TransitSampling.this, TransitSamplingMap.class);
					intent.putExtra("start", departId);
					intent.putExtra("stop", arrivalId);
					intent.putExtra("transport_id", transportId);
					startActivity(intent);
					TransitSampling.this.finish();
				}
				
			}			
		}.execute((Void[])null);
	}
	
	public void readPolyline(InputStream in) throws IOException {
		JsonReader jsonReader = new JsonReader(new InputStreamReader(in, "UTF-8"));
		jsonReader.setLenient(true);
		jsonReader.beginObject();
		Gson gson = new Gson();
		
		while (jsonReader.hasNext()) {
			String name = "";
			try {
				name = jsonReader.nextName();
				//System.out.println("name: " + name);
			} catch(Exception e) {
				jsonReader.skipValue();
			}
			
			if (name.equals("success")) {
				boolean success = jsonReader.nextBoolean();
				if(success == false) {
					jsonReader.endObject();
					jsonReader.close();
					return;
				}
			}
			else if(name.equals("values")) {
				MMCLogger.logToTransitFile(MMCLogger.Level.DEBUG, TAG, "readPolyline", "Starting to process polyline");
				jsonReader.beginArray();
				while (jsonReader.hasNext()) {
					Transports transport = gson.fromJson(jsonReader, Transports.class);
					GeomLineString geom = transport.getGeom();
//					double[][] points = geom.getCoordinates();
					polyline = geom.getCoordinates();
//					for(int m = 0; m < points.length; m++) {
//						double[] point = points[m];
//						int longitude = (int) (point[0] * 1000000);
//						int latitude = (int) (point[1] * 1000000);
//						
////						transitDB.savePolyline(latitude, longitude, transportId);
//						System.out.println("Saving polyline " + m + ": " + latitude + ", " + longitude);
//					}
				}
				//This area has downloaded all of it's routes. Set download_flag to false so we don't download again unless out of date
				transitDB.updateDownloadFlag(transportId, false);
				jsonReader.endArray();
			}
		}
		jsonReader.close();
	}
	
	public AsyncTask.Status returnCityAsyncTaskStatus() {
		if(cityAsyncTask != null)
			return cityAsyncTask.getStatus();
		else
			return null;
	}
	
	public AsyncTask.Status returnTransportAsyncTaskStatus() {
		if(transportAsyncTask != null)
			return transportAsyncTask.getStatus();
		else
			return null;
	}
	
	public AsyncTask.Status returnStationAsyncTaskStatus() {
		if(stationAsyncTask != null)
			return stationAsyncTask.getStatus();
		else
			return null;
	}
	
	public AsyncTask.Status returnPolylineAsyncTaskStatus() {
		if(polylineAsyncTask != null)
			return polylineAsyncTask.getStatus();
		else
			return null;
	}
	
	public boolean isAPICallInProgress() {
		if(returnCityAsyncTaskStatus() == AsyncTask.Status.RUNNING ||
			returnTransportAsyncTaskStatus() == AsyncTask.Status.RUNNING ||
			returnStationAsyncTaskStatus() == AsyncTask.Status.RUNNING ||
			returnPolylineAsyncTaskStatus() == AsyncTask.Status.RUNNING) {
			return true;
		}
		else {
			return false;
		}
	}
}
