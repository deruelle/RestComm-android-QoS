package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Compare;


public class StatChartView extends StatView {

        private static final String TAG = NerdView.class.getSimpleName();
        private StatChart mChartView;
        private LegendView legendView;
        // progress dialog
    private ProgressDialog progressDialog;
        
        /*
         * End private variables
         * ========================================================
         * Start constructors (and their helper methods)
         */
        public StatChartView (Context context) {
                super(context);
                this.context = context;
                init();
                
        }

        public StatChartView (Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                init();
        }
        
        
        @Override
        public void init() {
                mChartView = (StatChart) findViewById(R.id.stats_chart);
                //mLoadingIndicator = (ProgressBar) findViewById(R.id.stats_loadingindicator);
                legendView = (LegendView) findViewById(R.id.stats_legend);
        }
        
        public int getIndex (int index)
        {
                return statIndex;
        }
        
        @Override
        public void setIndex (int index)
        {
                statIndex = index;
        }
        @Override
        public void reload ()
        {
                //mLoadingIndicator.setVisibility(View.VISIBLE);
                loadStatPage ();
        }
        @Override
        public void show ()
        {
                mChartView.beginAnimation(800, 800, mHandler);
        }
        @Override
        public void onSingleTap(MotionEvent e) 
        {
                Rect outRect = new Rect ();
                mChartView.getHitRect(outRect);
                mChartView.onSingleTap (e.getX()-outRect.left, e.getY()-outRect.top);
                
        }
        @Override
        public void setParent (Compare parent)
        {
                super.setParent(parent);
                
        }
        private void loadStatPage ()
        {
                loadStats ();
                if (legendView != null)
                        legendView.setStatScreen (statIndex);
        }
        @Override
        public void loadStats ()
        {
                mChartView.setStats(mCompare.getStats());
                try{
                JSONObject yourStats = mCompare.getStats().getJSONObject("yourphone");
                if (legendView != null)
                        legendView.setPieLegend(yourStats);} catch (Exception e) {}
                hideLoadingIndicator();
        }
}