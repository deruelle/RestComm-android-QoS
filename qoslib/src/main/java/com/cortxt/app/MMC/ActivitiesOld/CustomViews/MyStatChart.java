package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.format.Time;
import android.util.AttributeSet;

import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Chart.Chart;
import com.cortxt.app.MMC.Reporters.ReportManager;

public class MyStatChart extends StatChart {

        /*
         * ==========================================================
         * Start private variables
         */
        
        
        /**
         * Stroke width of the time series on the chart.
         */
        private static final float CHART_SERIES_STROKE_WIDTH = 1.3f;
        
        public static final float CHART_XAXIS_FONT_SIZE = 13.0f;
        /**
         * Tag for debugging
         */
        private static final String TAG = Chart.class.getSimpleName();
        

        
        
        Time time = new Time(Time.getCurrentTimezone());                //This is a general time variable that will be constantly "setToNow"
        //StatColumn[] columns = new StatColumn[3];        
        int[] colors = { Color.rgb(0x99, 0x20, 0x27), Color.rgb(0xFF, 0xD5, 0x61), Color.rgb(0x29, 0x66, 0xCC) };
        int[] colors2 = { Color.rgb(0xFF, 0x00, 0x00), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x29, 0xAB, 0xE2) };
        
        Integer[] iSeries = new Integer[3];
        float yMax = 100, yInc = 25;
        String[] titles = new String[3];
        
        
        private static final int IS_DRAW = 2;
    private Paint mBgPaints   = new Paint();
    private Paint mLinePaints = new Paint();
    private int   mWidth;
    private int   mHeight;
    private int   mGapLeft;
    private int   mGapTop;
    private int   mBgColor;
    private float mStart;
    private float mSweep;
    
        /*
         * End private variables
         * ==========================================================
         * Start constructors
         */
        public MyStatChart(Context context) {
                super(context);
                this.context = context;
                init();
        }
        
        public MyStatChart(Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                processAttributeSet(context, attrs);
                init();
        }
        
        private void init(){
                screenDensityScale = getContext().getResources().getDisplayMetrics().density;
                paint = new Paint();
                CHART_BOTTOM_PADDING = 40;
                CHART_LEFT_PADDING = 50;
                CHART_RIGHT_PADDING = 50;
//                int height = getResources().getDisplayMetrics().heightPixels;
//                float density = getResources().getDisplayMetrics().density;
//                float h = height/density;
//                float ratio=h/533;
//                CHART_MAX_HEIGHT = CHART_MIN_HEIGHT  = (int)((h - 250*ratio));
        }
        
        private void processAttributeSet(Context context, AttributeSet attrs){
        }
        
        /*
         * Send the statistics to the chart
         */
        @Override
        public void setStats (JSONObject stats)
        {
                super.setStats (stats);
                mStats = stats;
                try
                {
                        JSONObject stat = mStats.getJSONObject("yourphone");
                        int iDropped = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DROPPED_CALLS));
                        int iFailed = Integer.parseInt(stat.getString(ReportManager.StatsKeys.FAILED_CALLS));
                        int iNormal = Integer.parseInt(stat.getString(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS));
//                        iDropped=5;
//                        iFailed=3;
//                        iNormal=92;
                        
                        int iTotal = iDropped + iFailed + iNormal;
                        if (iDropped + iFailed + iNormal == 0)
                                iTotal = iNormal = 1;
                        
                        iSeries[0] = 100 * iDropped / iTotal;
                        iSeries[1] = 100 * iFailed / iTotal;
                        iSeries[2] = 100 - iSeries[0] - iSeries[1];
                }
                catch (Exception e )
                {}
                shaders = null;
        }
        
        /*
         * End Constructors
         * ===========================================================
         * Start over-ridden methods (from view)
         */

        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                paint.setAntiAlias(true);
                int gapLeft = (int) (CHART_LEFT_PADDING * screenDensityScale);
        		int gapTop = (int) (CHART_TOP_PADDING * screenDensityScale);
        		int size = Math.min(chartWidth, chartHeight);
        		gapLeft += (chartWidth -size) /2;
        		gapTop += (chartHeight -size) /2;
        		setGeometry(size, size, gapLeft, 0, gapTop, 0);
                setShaders ();
                drawEmptyChart(canvas);
                drawPieChart(canvas);
                
        }
        
        
        /*
         * End over-ridden methods
         * ========================================================
         * Start helper methods
         */
        
        /**
         * This method draws the empty chart. This consists of 
         * drawing the chart axes, the labels on the axes and the 
         * background grid.
         * @param canvas
         */
        private void drawEmptyChart(Canvas canvas){
                
        }
        
        /**
         * This method takes the points stored in the signalTimeSeries variable and
         * draws a series on the canvas.
         * @param canvas
         * @param timeSeries
         */
        private void drawPieChart(Canvas canvas){
                paint.setStrokeWidth(CHART_SERIES_STROKE_WIDTH * screenDensityScale);
                float percent = animatePercentage ();
                
        //------------------------------------------------------
        //if (mState != IS_READY_TO_DRAW) return;
        canvas.drawColor(mBgColor);
        //------------------------------------------------------
        mBgPaints.setAntiAlias(true);
        mBgPaints.setStyle(Paint.Style.FILL);
        mBgPaints.setColor(0x88FF0000);
        mBgPaints.setStrokeWidth(0.5f);
        //------------------------------------------------------
        mLinePaints.setAntiAlias(true);
        mLinePaints.setStyle(Paint.Style.STROKE);
        mLinePaints.setColor(0xff000000);
        mLinePaints.setStrokeWidth(0.5f);
        //------------------------------------------------------
        RectF mOvals = new RectF( mGapLeft, mGapTop, mWidth+mGapLeft, mHeight+mGapTop);
        //------------------------------------------------------
        mStart = 0; 
        float maxSweep = 360* percent/100f;
        for (int i = 0; i < 3; i++) {
            //Item = (PieItem) mDataArray.get(i);
                if (iSeries[i] == null) break;
            mBgPaints.setColor(colors[i]);
            paint.setShader(shaders[i]); 
            mSweep = (float) 360 * ( (float)iSeries[i] / (float)100 );
            if (mStart + mSweep > maxSweep)
                    mSweep = maxSweep - mStart;
            if (mStart <= maxSweep && mSweep > 0)
            {
                    canvas.drawArc(mOvals, mStart-90, mSweep, true, paint);
                    canvas.drawArc(mOvals, mStart-90, mSweep, true, mLinePaints);
                    mStart += mSweep;
            }
        }
       
        }
        
        /*
         * create the shaders for each chart element to avoid creating them on every frame of animation
         */
        private void setShaders ()
        {
                if (shaders != null)
                        return;
                shaders = new Shader[3];
                for (int i = 0; i < 3; i++) 
            shaders[i] = new LinearGradient(mGapLeft+mWidth/2, mGapTop, mWidth+mGapLeft, mGapTop+mWidth/2, colors2[i], colors[i], Shader.TileMode.MIRROR); 
                
        }
        
        //--------------------------------------------------------------------------------------
            public void setGeometry(int width, int height, int GapLeft, int GapRight, int GapTop, int GapBottom) {
                mWidth     = width;
                mHeight    = height;
                mGapLeft   = GapLeft;//(int)((GapLeft + CHART_LEFT_PADDING) * screenDensityScale);
                //mGapRight  = (int)((GapRight + CHART_RIGHT_PADDING) * screenDensityScale);
                mGapTop    = GapTop;//(int)((GapTop + CHART_TOP_PADDING/2) * screenDensityScale);
                //mGapBottom = (int)((GapBottom+ CHART_BOTTOM_PADDING) * screenDensityScale);
            }
            //--------------------------------------------------------------------------------------
            public void setSkinParams(int bgColor) {
                mBgColor   = bgColor;
            }
            
            
        
        
}