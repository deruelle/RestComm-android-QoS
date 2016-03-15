package com.cortxt.app.MMC.Sampling.Transit;

import java.util.ArrayList;
import java.util.Arrays;

import com.cortxt.app.MMC.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DrawerListAdapter extends BaseAdapter {
	
	private Context context;
    private ArrayList<DrawerItem> drawerItems;
     
    public DrawerListAdapter(Context context, ArrayList<DrawerItem> drawerItems){
        this.context = context;
        this.drawerItems = drawerItems;
    }
 
    @Override
    public int getCount() {
        return drawerItems.size();
    }
 
    @Override
    public Object getItem(int position) {       
        return drawerItems.get(position);
    }
 
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    public void setFinished(int position) {
    	drawerItems.get(position).setImage(R.drawable.check_mark_ok);
    }
    
    public void setProblem(int position) {
    	drawerItems.get(position).setProblemImage(R.drawable.alert_sample_grey);
    }
    
    public void setCurrent(int position) {
    	drawerItems.get(position).setProblemImage(R.drawable.train_station_icon_ts);
    }
 
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            view = mInflater.inflate(R.layout.transit_sampling_map_drawer_item, null);
        }
          
        ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        ImageView problemImageView = (ImageView) view.findViewById(R.id.problemImageView);

        int image = drawerItems.get(position).getImage();
        if(image == 0) {
//        	imageView.getBackground().setAlpha(0);
        } else {
        	imageView.setBackground(context.getResources().getDrawable(image));
        	imageView.setImageResource(image);        
        }
         
        textView.setText(drawerItems.get(position).getText());
        
        int problem = drawerItems.get(position).getProblemImage();
        if(problem == 0) {
//        	problemImageView.getBackground().setAlpha(0);
        } else {
        	problemImageView.setBackground(context.getResources().getDrawable(problem));
        }
        
        return view;
    }
}
