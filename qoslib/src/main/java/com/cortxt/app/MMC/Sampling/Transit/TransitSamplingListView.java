package com.cortxt.app.MMC.Sampling.Transit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cortxt.app.MMC.Activities.MMCActivity;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Dashboard;
import com.cortxt.app.MMC.ActivitiesOld.MMCTrackedActivityOld;
import com.cortxt.app.MMC.Utils.ScalingUtility;

public class TransitSamplingListView extends MMCTrackedActivityOld {

	private ListView transitListView;
	private final int RESULT_ERROR = 2;
	private int type = 0;
	private String[] info = null;
	private int index = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.transit_sampling_listview, null, false);
		ScalingUtility.getInstance(this).scaleView(view);
		setContentView(view);

        MMCActivity.customizeTitleBar(this, view, R.string.transitsampling_listview_title, R.string.transitcustom_listview_title);
		
		transitListView = (ListView) findViewById(R.id.transitListView);
		
		Intent intent = getIntent();
//		Bundle intentExtras = intent.getExtras();
		if(intent.hasExtra("stops")) {	
			info = intent.getStringArrayExtra("stops");
//			info = (String[][]) intentExtras.getSerializable("stops");
		}
		//stationtype here refers to starting or stopping station
		if(intent.hasExtra("stationtype"))
				type = intent.getIntExtra("stationtype", 0); 
		if(info == null || type == 0)
			return;
		
//		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, info);
		final ListViewAdapter adapter = new ListViewAdapter(this, R.layout.custom_spinner, info);
		transitListView.setAdapter(adapter); 
	            
		transitListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				index = position;
				
				adapter.setSelected(position);
				adapter.notifyDataSetChanged();
			}
		}); 
	}
	
	public void onContinue(View view) {
		String stop = info[index];
		Intent returnIntent = new Intent();
		returnIntent.putExtra("stop", stop);
		setResult(type, returnIntent); 
		TransitSamplingListView.this.finish(); 
	}
	
	public void onExit(View view) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra("type", type);
		setResult(RESULT_ERROR, returnIntent); 
		TransitSamplingListView.this.finish(); 
	}
	
	private class ListViewAdapter extends ArrayAdapter<String> {
		
		int selectedItem = -1;
		String[] stations;
		 
        public ListViewAdapter(Context context, int txtViewResourceId, String[] objects) {
            super(context, txtViewResourceId, objects);
            stations = objects;
        }

        public void setSelected(int selectedItem) {
        	this.selectedItem = selectedItem;
        }
        
        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = getLayoutInflater();
            View listView = inflater.inflate(R.layout.transit_menu_listview_item, viewGroup, false);
         
//            ImageView menuImageView = (ImageView) listView.findViewById(R.id.menuImageView);
//            if(sampled[position] == 1)
//            	spinnerImageView.setImageResource(R.drawable.train_station_icon_ts);
//            else
//            	spinnerImageView.setImageResource(android.R.color.transparent);
            
            TextView textView = (TextView) listView.findViewById(R.id.menuTextView);
            textView.setText(stations[position]);
            
            if(position == selectedItem) {
            	//Blue background
            	textView.setTextColor(Color.rgb(255, 255, 255));
            	listView.setBackgroundColor(Color.rgb(54, 180, 227));
//            	checkImageView.setVisibility(View.VISIBLE);
        	}
        	else {
        		listView.setBackgroundColor(Color.rgb(255, 255, 255));
        		textView.setTextColor(Color.rgb(31, 31, 31));
        	}

            return listView;
        }
    }
}
