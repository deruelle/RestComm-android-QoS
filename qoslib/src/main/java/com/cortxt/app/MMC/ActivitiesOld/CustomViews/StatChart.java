package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Compare;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class StatChart extends View {
        
        /**
         * Distance between the left edge of the chart and the left edge of
         * the container.
         */
        protected int CHART_LEFT_PADDING = 50;
        /**
         * Distance between the right edge of the chart and the right edge of
         * the container.
         */
        protected int CHART_RIGHT_PADDING = 35;
        /**
         * Distance between the top edge of the chart and the top edge of
         * the container.
         * The extra space between these 2 is used for the icons.
         */
        protected int CHART_TOP_PADDING = 20 + 0; // added height of tabs (50)
        
        /**
         * The minimum height that the chart (excluding the paddings)
         * can have.
         */
        protected int CHART_MIN_HEIGHT = 240;
        /**
         * The minimum width that the chart (excluding the paddings)
         * can have.
         */
        protected int CHART_MIN_WIDTH = 200;
        /**
         * The maximum height that the whole view can have
         */
        protected int CHART_MAX_HEIGHT = 320;
        /**
         * The maximum width that the whole view can have
         * if the view is in portrait mode.
         */
        protected final int CHART_MAX_WIDTH_PORTRAIT = 267;
        /**
         * The maximum width that the whole view can have
         * if the view is in landscape mode.
         */
        protected final int CHART_MAX_WIDTH_LANDSCAPE = 400;
        /**
         * Stroke width of the x axis.
         */
        
        protected int CHART_BOTTOM_PADDING = 30-10;
        protected JSONObject mStats; //the actual statistics
        protected Context context; 
        protected Shader[] shaders = null;
        
        /**
         * This is the scale with which all lengths get multiplied by to make sure the charts work
         * on all kinds of screen resolutions.
         */
        protected float screenDensityScale;
        
        //other variables
        protected Paint paint;        //This is the multi-purpose paint object used by all the methods.
                                        
        /**
         * Width of the chart
         */
        protected int chartWidth;
        /**
         * Height of the chart
         */
        protected int chartHeight;
        protected int containerHeight;
        /**
         * This is the timespan (in seconds) that the chart is supposed to cover.
         * By default, it is 4*60 seconds.
         */
        
        public StatChart(Context context) {
                super(context);
                this.context = context;
                //View v = ((Compare)context).findViewById(R.id.compare_container);
                //containerHeight = v.getHeight();
                init();
        }
        
        public StatChart(Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                //View v = ((Compare)context).findViewById(R.id.compare_container);
                //containerHeight = v.getHeight();
                init();
        }
        
        public int getContainerHeight ()
        {
                View v = ((Compare)this.context).findViewById(R.id.compare_container);
                if (v != null)
                        containerHeight = v.getHeight();
                return containerHeight;
        }
        
        protected boolean bAnimating = false;
        protected long animateStart = 0, animateEnd = 0;
        // Called by the Compare screen parent to start an animation
        // This simple animation just causes invalidate to call repeatedly to redraw the graph 
        // over a given duration, increasing the percentage to 100%
        public void beginAnimation (final long delay, final long duration, Handler handler)
        {
                if (!bAnimating)
                {
                        bAnimating = true;
                        animateStart = System.currentTimeMillis() + delay;
                        animateEnd = System.currentTimeMillis() + duration + delay;
                        if (handler == null || delay == 0)
                        {
                                animateStart = System.currentTimeMillis();
                                animateEnd = System.currentTimeMillis() + duration;
                                invalidate ();
                        }
                        else
                        handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                        animateStart = System.currentTimeMillis();
                                        animateEnd = System.currentTimeMillis() + duration;
                                        invalidate ();
                                        
                                }}, delay);
                }
        }
        
        // Called by the draw/ondraw function to get a new percentage for as long as the animation
        // is in progress
        protected float animatePercentage ()
        {
                if (bAnimating)
                {
                        if (System.currentTimeMillis() < animateStart)
                                return 0f;
                        float percent = (System.currentTimeMillis() - animateStart) * 100.0f / (animateEnd - animateStart);
                        
                        if (System.currentTimeMillis() > animateEnd)
                        {
                                bAnimating = false;
                                return 100f;
                        }
                        invalidate ();
                        return percent;
                }
                return 100f;
        }
        
        private void init(){
                screenDensityScale = getContext().getResources().getDisplayMetrics().density;
                paint = new Paint();
        }
        
        /*
         * Compare Screen parent sends the json statistics to the chart
         */
        public void setStats (JSONObject stats)
        {
                mStats = stats;
                invalidate ();
        }
        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
        }
        
        // Just some Android methods for it to determine the size of the Chart view
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int calculatedWidth = measureWidth(widthMeasureSpec);
                int calculatedHeight = measureHeight(heightMeasureSpec);
                setMeasuredDimension(calculatedWidth, calculatedHeight);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                //chartHeight = Math.max(h - (int)((CHART_TOP_PADDING + CHART_BOTTOM_PADDING) * screenDensityScale), (int)(CHART_MIN_HEIGHT * screenDensityScale));
                chartHeight = h - (int)((CHART_TOP_PADDING*2) * screenDensityScale);
                chartWidth = Math.max(w - (int)((CHART_LEFT_PADDING + CHART_RIGHT_PADDING) * screenDensityScale), (int)(CHART_MIN_WIDTH * screenDensityScale));
                super.onSizeChanged(w, h, oldw, oldh);
        }
        
        private int measureHeight (int heightMeasureSpec){
                int specMode = MeasureSpec.getMode(heightMeasureSpec);
                int specSize = MeasureSpec.getSize(heightMeasureSpec);
                int measuredHeight = specSize; // Math.max(specSize, (int)((CHART_MIN_HEIGHT + CHART_BOTTOM_PADDING + CHART_TOP_PADDING) * screenDensityScale));
                // subtract the size of the legend if there is one
                measuredHeight -= (CHART_BOTTOM_PADDING + CHART_TOP_PADDING)*screenDensityScale;
                if (screenDensityScale > 1.5)
                    measuredHeight -= 20*screenDensityScale;
                //else if (screenDensityScale < 1)
                //	measuredHeight += 20*screenDensityScale;
                //measuredHeight = Math.min(measuredHeight, (int)(CHART_MAX_HEIGHT * screenDensityScale));
                return measuredHeight;
        }
        
        
        private int measureWidth(int widthMeasureSpec) {
                int result = 0;
                int specMode = MeasureSpec.getMode(widthMeasureSpec);
                int specSize = MeasureSpec.getSize(widthMeasureSpec);
                
                if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize;
        } else {
            //get the size of the image
                result = (int) CHART_MAX_WIDTH_PORTRAIT;
            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = Math.min(result, specSize);
            }
        }

        return result;
        }
        
        public void onSingleTap(float x, float y) 
        {
                
        }
        
        
}