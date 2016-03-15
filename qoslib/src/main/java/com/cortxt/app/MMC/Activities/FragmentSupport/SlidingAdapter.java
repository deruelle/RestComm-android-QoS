package com.cortxt.app.MMC.Activities.FragmentSupport;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Utils.MMCLogger;

public class SlidingAdapter extends FragmentStatePagerAdapter {

	List<SlidingFragment> slidingViews = null;
	int[] ids = { R.layout.new_dashboard_icons1, R.layout.new_dashboard_icons2, R.layout.new_dashboard_icons3 };
	private int count = 0;
	boolean isFullWidth = false;

	public SlidingAdapter(FragmentManager fm) {
		super(fm);
		
		slidingViews = new ArrayList<SlidingFragment>();
		count = ids.length;

		for (int i = 0; i < ids.length; i++) {
			SlidingFragment f = new SlidingFragment();
			Bundle b = new Bundle();
			b.putInt("layoutId", ids[i]);
			f.setArguments(b);
			slidingViews.add(i, f);
		}
	}
	
	public void setFullWidth(boolean full) {
		isFullWidth = full;
	}
	
	@Override
	public float getPageWidth(int position) {
		if(isFullWidth)
			return 1f;
		return 0.96f;
	}

	@Override
	public int getCount() {
		return count;
	}
	
	@Override
	public Fragment getItem(int arg0) {
		return slidingViews.get(arg0);
	}

	
	@Override
	public int getItemPosition(Object object) {
		return POSITION_NONE;
	}
	
	/**
	 * Hide second page from Dashboard
	 */
	public void removeSecondScreen() {
		if(slidingViews.size() <= 1) {
			Log.e(getClass().getSimpleName(), "Can NOT remove second");
			return;
		}
		slidingViews.remove(1);
		count = 1;
	}

	/**
	 * When Sampling icon needs to be hidden, which is the only icon on page 3 of Dashboard, we hide the whole page
	 */
	public void removeThirdScreen() {
		if(slidingViews.size() <= 2) {
			Log.e(getClass().getSimpleName(), "Can NOT remove third");
			return;
		}
		slidingViews.remove(2);
		count = 2;
	}
}