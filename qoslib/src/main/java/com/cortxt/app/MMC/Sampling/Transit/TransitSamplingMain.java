package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.Activities.EventHistory;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.Utils.ScalingUtility;
import com.cortxt.app.MMC.UtilsOld.PreferenceKeys;

public class TransitSamplingMain extends MMCTrackedActivityOld {
	
	private TextView itineraryTextView;
	private ImageView transitBgImageView;
	private TextView noItinerariesTextView;
	private TextView addItineraryTextView;
	private RelativeLayout undoBar;
	private View dummyView;
	
//	private String[][] itineraryIds;
	private List<Itinerary> itineraries = new ArrayList<Itinerary>();
	private ListView transitListView;
	private TransitCustomAdapter adapter;
	private ArrayList<TransitItineraries> CustomListViewArray = new ArrayList<TransitItineraries>();
	private TransitDatabaseReadWriteNew transitDB;
	private List<Itinerary> deletedItinerary = new ArrayList<Itinerary>();;
	
	private final int RESULT_OK = 1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_main, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);
		
		Dashboard.customizeTitleBar(this, view, R.string.transitsampling_main_title, R.string.transitcustom_main_title);
		
		itineraryTextView = (TextView) view.findViewById(R.id.itineraryTextView);
		transitBgImageView = (ImageView) view.findViewById(R.id.transitBgImageView);
		noItinerariesTextView = (TextView) view.findViewById(R.id.noItinerariesTextView);
		addItineraryTextView = (TextView) view.findViewById(R.id.addItineraryTextView);
		undoBar = (RelativeLayout) view.findViewById(R.id.undobar);
		dummyView = (View) view.findViewById(R.id.dummyView);
//		FontsUtil.applyFontToTextView(MmcConstants.font_MEDIUM, itineraryTextView, this);
		
		transitListView = (ListView) view.findViewById(R.id.transitListView);
		
		transitDB = new	TransitDatabaseReadWriteNew(this);
		showSavedItineraries();
		
        Resources resources = getResources();
        adapter = new TransitCustomAdapter(this, CustomListViewArray, resources);
        transitListView.setAdapter(adapter);
        
        dummyView.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				if(hasFocus) {
					if(adapter.isVisible())
						adapter.hideUndoBar();
				}
			}
		});
	}
	
	public void deleteItinerary(int position) {
		Itinerary itinerary = itineraries.get(position);
		String itineraryId = itinerary.getItineraryId();
		itineraries.remove(position);
		deletedItinerary.add(itinerary);
		CustomListViewArray.remove(position);
		transitDB.deleteItinerary(itineraryId);
		
		if(itineraries.size() == 0) {
			refresh();
			showSavedItineraries();
		}
	}
	
	public void saveItinerary(TransitItineraries itinerary) {
		if(deletedItinerary.size() > 0) {
			Itinerary deleted = deletedItinerary.get(deletedItinerary.size()-1);
			int transportId = Integer.valueOf(deleted.getTransportId());
			String itineraryId = deleted.getItineraryId();
			String[] stopsIds = itineraryId.split(","); // itineraryId = departId,arriveId
			Integer departId = Integer.valueOf(stopsIds[0]);
			Integer arrivalId = Integer.valueOf(stopsIds[1]);
			transitDB.saveItinerary(transportId, arrivalId, departId);
			
			String line = transitDB.getTransportName(transportId);
			String departName = transitDB.getStopName(stopsIds[0]);
			String arriveName = transitDB.getStopName(stopsIds[1]);
			
			TransitItineraries TI = new TransitItineraries(line, departId, arrivalId, departName, arriveName);
			CustomListViewArray.add(TI);
			
			deletedItinerary.remove(deleted);
		}
	}
	    
    public void onItemClick(int position) {
    	try {
    		TransitItineraries selectedItinerary = (TransitItineraries) CustomListViewArray.get(position); 
    		Itinerary itinerary = itineraries.get(position);
    		int transportId = Integer.valueOf(itinerary.getTransportId());
//   	 	String itineraryId = itineraryIds[position][0];
    	 
//    	 	ArrayList<TransitInfo> stations = transitDB.getTransportStopsFromDB(transportId);
    	
    		startSamplingItinerary(selectedItinerary.getDepart(), selectedItinerary.getArrive(), transportId);
    	} catch(Exception e) {
    		e.printStackTrace();
    		if(itineraries.size() == 0)
    			refresh();
    			showSavedItineraries();
    	}
     }
	
	public void showSavedItineraries() {
		try {
			String[][] tempItineraries = transitDB.getSavedItineraryIds();  //[itinerary_id][route_id]
			
			for(int i = 0; i < tempItineraries.length; i++) {
				String itinerary = tempItineraries[i][0];
				String transport = tempItineraries[i][1];
				
				Itinerary it = new Itinerary(itinerary, transport);
				itineraries.add(it);
			}
	
			if(itineraries.size() > 0) {
				showItineraryMessages(false);
			}
			else { //In case we ever add delete functionality to itineraries, then this will be needed
				showItineraryMessages(true);
			}
			
			for(int i = 0; i < itineraries.size(); i ++) {
				Itinerary it = itineraries.get(i);
				
				String[] stopsIds = it.getItineraryId().split(","); // itineraryId = departId,arriveId
				int transportId = Integer.valueOf(it.getTransportId());
				Integer departId = Integer.valueOf(stopsIds[0]);
				Integer arrivalId = Integer.valueOf(stopsIds[1]);
				
				String line = transitDB.getTransportName(transportId);
				String departName = transitDB.getStopName(stopsIds[0]);
				String arriveName = transitDB.getStopName(stopsIds[1]);
				
				TransitItineraries itinerary = new TransitItineraries(line, departId, arrivalId, departName, arriveName);
				CustomListViewArray.add(itinerary);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/** this will hide/show background and no itinerary messages **/
	public void showItineraryMessages(boolean show) {
		int view = View.GONE;
		if(show)
			view = View.VISIBLE;
		transitBgImageView.setVisibility(view);
		noItinerariesTextView.setVisibility(view);
		addItineraryTextView.setVisibility(view);
	}
	
	public void onExitClicked(View view) {
		this.finish();
	}
	
	public void onSettings(View view) {
		Intent intent = new Intent(this, TransitSamplingSettings.class);
		startActivity(intent);
	}
	
	public void onHistory(View view) {
		Intent intent = new Intent(this, EventHistory.class);
		intent.putExtra("fromTransit", RESULT_OK);
		startActivity(intent);
	}
	
	public void onAdd(View view) {
		Intent intent = new Intent(this, TransitSampling.class);
		startActivity(intent);
		this.finish();
	}
	
	public void onLibrary(View view) {
		Intent intent = new Intent(this, TransitSamplingLibrary.class);
		startActivity(intent);
		this.finish();
	}
	
	public void refresh() {
//		adapter.notifyDataSetChanged(); //not working?!
//		//workaround
		Resources resources = getResources();
////		adapter = new TransitCustomAdapter(this, CustomListViewArray, resources);
//		showSavedItineraries();
		
//  	  adapter = new CustomListAdapter(getActivity(), feed);
  	  adapter = new TransitCustomAdapter(this, CustomListViewArray, resources);
  	  transitListView.setAdapter(adapter);
      adapter.notifyDataSetChanged();
	}
	
	public void startSamplingItinerary(Integer stationFrom, Integer stationTo, int transportId) {
//		TransitInfoWrapper wrapper = new TransitInfoWrapper(stations);
		Intent intent = new Intent(this, TransitSamplingMap.class);
//		intent.putExtra("stops", wrapper);
		intent.putExtra("start", stationFrom);
		intent.putExtra("stop", stationTo);
		intent.putExtra("transport_id", transportId);
//		startActivityForResult(intent, RESULT_OK);
		startActivity(intent);
		this.finish();
	}
	
	public class Itinerary {
		public String itineraryId;
		public String transportId;
		
		public Itinerary(String itineraryId, String transportId) {
			this.itineraryId = itineraryId;
			this.transportId = transportId;
		}
		
		public void setItineraryId(String itineraryId) {
			this.itineraryId = itineraryId;
		}
		
		public String getItineraryId() {
			return this.itineraryId;
		}
		
		public void setTransportId(String transportId) {
			this.transportId = transportId;
		}
		
		public String getTransportId() {
			return this.transportId;
		}
	}
}
