package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import com.cortxt.app.MMC.Activities.Compare;

public abstract class StatView extends LinearLayout {

        //protected ProgressBar mLoadingIndicator;
        protected Compare mCompare;
        protected int statIndex = 0;
        protected Context context;
        protected Handler mHandler = new Handler();
        
        public StatView (Context context) {
                super(context);
                this.context = context;
                
        }

        public StatView (Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                
        }
        public void init ()
        {
        
        }
        public int getIndex (int index)
        {
                return statIndex;
        }
        public void setIndex (int index)
        {
                statIndex = index;
        }
        public void show ()
        {
        }
        
        public void reload ()
        {
        }
        
        public void onSingleTap(MotionEvent e) 
        {
                
        }
        public void setParent (Compare parent)
        {
                mCompare = parent;
                //mLoadingIndicator.setVisibility(View.VISIBLE);
        }
        
        public void loadStats ()
        {
        }
        
        public void hideLoadingIndicator() {
                mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                                //mLoadingIndicator.setVisibility(View.GONE);
                        }
                });
        }
}