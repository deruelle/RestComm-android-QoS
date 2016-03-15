package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;

import com.cortxt.app.MMC.R;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class TransitCustomAdapter extends BaseAdapter implements OnClickListener, UndoBarController.UndoListener {
	          
	private Activity activity;
	private ArrayList data;
	private static LayoutInflater inflater = null;
	public Resources resource;
	private UndoBarController mUndoBarController;
	private TransitItineraries itineraries = null;
	private TransitItineraries recentlyDeletedItinerary = null;
	          
	public TransitCustomAdapter(Activity activity, ArrayList data, Resources resource) {
	    this.activity = activity;
		this.data = data;
		this.resource = resource;
		this.inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		mUndoBarController = new UndoBarController(activity.findViewById(R.id.undobar), this);
	}
	      
    public int getCount() {
    	if(data.size() <= 0)
    		return 1;
    	return data.size();
    }
  
    public Object getItem(int position) {
    	return position;
    }
  
    public long getItemId(int position) {
    	return position;
    }
      
    public static class ViewHolder{
    	public TextView lineLabelTextView;
    	public TextView lineTextView;
    	public TextView fromLabelTextView;
    	public TextView fromTextView;
    	public TextView toLabelTextView;
    	public TextView toTextView;
    	public View view;
    	public Button trashcanImageView;
    }
  
    public View getView(final int position, View convertView, ViewGroup parent) {
      
    	View view = convertView;
    	ViewHolder holder;
      
    	if(convertView == null) {
    		view = inflater.inflate(R.layout.transit_main_listview_item_new, null);
    		holder = new ViewHolder();
    		holder.lineLabelTextView = (TextView) view.findViewById(R.id.lineLabelTextView);
    		holder.fromLabelTextView = (TextView) view.findViewById(R.id.fromLabelTextView);
    		holder.fromTextView = (TextView) view.findViewById(R.id.fromTextView);
    		holder.toLabelTextView = (TextView) view.findViewById(R.id.toLabelTextView);
    		holder.toTextView = (TextView) view.findViewById(R.id.toTextView);
    		holder.lineTextView = (TextView) view.findViewById(R.id.lineNameTextView);
    		holder.view = (View) view.findViewById(R.id.view);
    		holder.trashcanImageView = (Button) view.findViewById(R.id.trashcanImageView);
    		
    		holder.trashcanImageView.setOnClickListener(new View.OnClickListener() {
    			@Override
                public void onClick(View v) {
                   deleteItinerary(position);
                }
            });
    		
    		view.setTag(holder);
    		try {
    			holder.lineLabelTextView.setVisibility(View.GONE);
    			holder.fromLabelTextView.setVisibility(View.GONE);
    			holder.fromTextView.setVisibility(View.GONE);
    			holder.toLabelTextView.setVisibility(View.GONE);
    			holder.toTextView.setVisibility(View.GONE);
    			holder.lineTextView.setVisibility(View.GONE);
    			holder.view.setVisibility(View.GONE);
    			holder.trashcanImageView.setVisibility(View.GONE);
    		} catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
    	else 
    		holder = (ViewHolder) view.getTag();
    	
    	if(data.size() > 0) {
    		itineraries = null;
    		itineraries = (TransitItineraries) data.get(position);
    		holder.lineTextView.setText(itineraries.getLine());
    		holder.fromTextView.setText(itineraries.getDepartName());
    		holder.toTextView.setText(itineraries.getArriveName());
    		view.setOnClickListener(new OnItemClickListener(position));
    		try {
    			holder.lineLabelTextView.setVisibility(View.VISIBLE);
    			holder.lineTextView.setVisibility(View.VISIBLE);
    			holder.fromLabelTextView.setVisibility(View.VISIBLE);
    			holder.fromTextView.setVisibility(View.VISIBLE);
    			holder.toLabelTextView.setVisibility(View.VISIBLE);
    			holder.toTextView.setVisibility(View.VISIBLE);
    			holder.view.setVisibility(View.VISIBLE);
    			holder.trashcanImageView.setVisibility(View.VISIBLE);
    		} catch(Exception e) {
    			e.printStackTrace();
    		}
    	}
    	return view;
    }
          
    @Override
    public void onClick(View view) {
//    	 System.out.println("CustomAdapter Row clicked");
    }
    
    public void deleteItinerary(int position) {
    	try {
    		mUndoBarController.showUndoBar(false, activity.getString(R.string.transitsampling_itinerary_deleted_msg), null);
    		recentlyDeletedItinerary = (TransitItineraries) data.get(position);
	    	((TransitSamplingMain) activity).deleteItinerary(position);
//	    	((TransitSamplingMain) activity).refresh();
	    	this.notifyDataSetChanged();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    private class OnItemClickListener implements OnClickListener {           
    	private int mPosition;
              
    	OnItemClickListener(int position){
    		mPosition = position;
    	}
              
    	@Override
    	public void onClick(View view) {
    		TransitSamplingMain main = (TransitSamplingMain) activity;
    		main.onItemClick(mPosition);

//    		 ViewHolder holder = new ViewHolder();
//    		 try {
//		         holder.lineLabelTextView.setTextColor(Color.rgb(red, green, blue));
//		    	 holder.fromLabelTextView.setVisibility(View.GONE);
//		    	 holder.fromTextView.setVisibility(View.GONE);
//		    	 holder.toLabelTextView.setVisibility(View.GONE);
//		    	 holder.toTextView.setVisibility(View.GONE);
//		    	 holder.lineTextView.setVisibility(View.GONE);
//		    	 holder.view.setVisibility(View.GONE);
//	         } catch(Exception e) {
//	        	 e.printStackTrace();
//	         }
    	}               
    }

	@Override
	public void onUndo(Parcelable token) {
		if(recentlyDeletedItinerary != null) {
			((TransitSamplingMain) activity).saveItinerary(recentlyDeletedItinerary);
			((TransitSamplingMain) activity).refresh();
			this.notifyDataSetChanged();
			recentlyDeletedItinerary = null;
		}
	}

	public boolean isVisible() {
		return mUndoBarController.isVisible();
	} 
	
	public void hideUndoBar() {
		mUndoBarController.hideUndoBar(true);
	} 
}
