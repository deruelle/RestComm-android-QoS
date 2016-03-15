package com.cortxt.app.MMC.Sampling.Transit;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.MotionEvent;

public class TransitDrawerLayout extends DrawerLayout {

	public TransitDrawerLayout(Context context) {
		super(context);
		
	}
	
	@Override
	public boolean onInterceptTouchEvent (MotionEvent ev) {
		
		if(ev != null) {
			
		}
		
		return false;
	}
	

}
