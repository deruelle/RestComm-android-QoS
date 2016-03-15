package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader;
import android.text.format.Time;
import android.util.AttributeSet;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Chart.Chart;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.UtilsOld.Carrier;
public class SpeedStatChart extends StatChart {

        /*
         * ==========================================================
         * Start private variables
         */


        /**
         * This is the distance between the x axis and the time labels
         */
        private static final int XAXIS_LABEL_YOFFSET = 14;
        /**
         * This is the distance by which all the labels on the x axis are moved
         * left.
         */
        private static final int XAXIS_LABEL_XOFFSET = 0;
        /**
         * The last label in the x axis is the "now" keyword. This is 
         * a larger word than the rest and thus requires an additional offset
         * to fit into the screen
         */
        private static final float XAXIS_STROKE_WIDTH = 1.67f;//2.67f;
        /**
         * Stroke width of the time series on the chart.
         */
        private static final float CHART_SERIES_STROKE_WIDTH = 1.3f;

        private static final int BAR_SPACING = 10;

        public static final float CHART_XAXIS_FONT_SIZE = 13.0f;
        /**
         * Tag for debugging
         */
        private static final String TAG = Chart.class.getSimpleName();

        Time time = new Time(Time.getCurrentTimezone());                //This is a general time variable that will be constantly "setToNow"
        int []flatcolors={Color.parseColor("#0099CC"),Color.parseColor("#0099CC"),Color.parseColor("#FBB03B"),Color.parseColor("#FBB03B"),Color.parseColor("#FF0000"),Color.parseColor("#FF0000")};
        // Bright end of gradients for the 3 bars
        int[] colors = { Color.rgb(0x29, 0xAB, 0xE2), Color.rgb(0xFF, 0xD5, 0x61), Color.rgb(0xFF, 0x00, 0x00) }; // Color.rgb(0x39, 0xB5, 0x4A)};
        // Dark end of gradients for the 3 bars
        int[] colors2 = { Color.rgb(0x29, 0x66, 0xCC), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x99, 0x20, 0x27) };//Color.rgb(0x39, 0x66, 0x4A) };
                
        final int TOTAL_BAR_CHARTS=6;
        Integer[] iSeries = new Integer[TOTAL_BAR_CHARTS];
        float yMax = 100, yInc = 25;


        /*
         * End private variables
         * ==========================================================
         * Start constructors
         */
        public SpeedStatChart(Context context) {
                super(context);
                this.context = context;
                init();
        }

        public SpeedStatChart(Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                init();
        }

        // initialize the values for the chart drawing
        private void init(){
                screenDensityScale = getContext().getResources().getDisplayMetrics().density;
                paint = new Paint();
                CHART_LEFT_PADDING = 30;
                CHART_RIGHT_PADDING = 30;
                CHART_BOTTOM_PADDING = 85;
//                int height = getResources().getDisplayMetrics().heightPixels;
//                float density = getResources().getDisplayMetrics().density;
//                float h = height/density;
//                float ratio=h/533;
//                CHART_MAX_HEIGHT = CHART_MIN_HEIGHT  = (int)((h - 280*ratio));
        }

        /*
         * Compare Screen parent sends the json statistics to the chart
         */
        @Override
        public void setStats (JSONObject stats)
        {
                super.setStats (stats);
                mStats = stats;
                if (stats == null)
                        return;
                Carrier currentCarrier = ReportManager.getInstance(context.getApplicationContext()).getCurrentCarrier();
                String currOpid = "0";
                if (currentCarrier != null)
                        currOpid = currentCarrier.OperatorId;
                //String[] keytitles = {"yourphone", "yourcarrier", "allcarriers"};
                String[] keys = {"yourphone", currOpid, "0"};
                int i;
                float max = 0;
                for (i=0; i<3; i++)
                {
                        try
                        {
                                if (mStats != null && mStats.has(keys[i]))
                                {
                                        JSONObject stat = mStats.getJSONObject(keys[i]);
                                        iSeries[2*i] = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DOWNLOAD_SPEED_AVERAGE))/1000;
                                        iSeries[2*i+1] = Integer.parseInt(stat.getString(ReportManager.StatsKeys.UPLOAD_SPEED_AVERAGE))/1000;                
                                        max = Math.max(max, iSeries[2*i]);
                                        max = Math.max(max, iSeries[2*i+1]);
                                }
                        }
                        catch (Exception e)
                        {

                        }        
                }        
                yMax = max * 5/4;
                yInc = yMax / 5;
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
                setShaders ();  // only creates the gradients on the first time called
                drawEmptyChart(canvas);  // draw labels and axis etc
                drawBars(canvas);  // draw the 3 bars themselves
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
                //draw the x axis
                paint.setColor(Color.GRAY);
                //paint.setPathEffect(new DashPathEffect(new float[]{ 5.0f, 5.0f }, 0.0f));
                paint.setStrokeWidth(XAXIS_STROKE_WIDTH * screenDensityScale);
                paint.setStyle(Style.FILL);
                paint.setShader(null);
                canvas.drawLine(
                                CHART_LEFT_PADDING * screenDensityScale, 
                                chartHeight + (CHART_TOP_PADDING - XAXIS_STROKE_WIDTH) * screenDensityScale, 
                                (CHART_LEFT_PADDING+10) * screenDensityScale + chartWidth, 
                                chartHeight + (CHART_TOP_PADDING - XAXIS_STROKE_WIDTH) * screenDensityScale, 
                                paint);


        }

        /**
         * This method takes the points stored in the signalTimeSeries variable and
         * draws a series on the canvas.
         * @param canvas
         * @param timeSeries
         */
        private void drawBars(Canvas canvas){
                float xcoord = 0.0f;
                float ycoordMin = (CHART_TOP_PADDING - XAXIS_STROKE_WIDTH) * screenDensityScale;
                float ycoordBot = chartHeight + ycoordMin;
                float ycoordTop = ycoordBot;
                float barGap = BAR_SPACING * screenDensityScale;
                float barWidth = (chartWidth - barGap*(TOTAL_BAR_CHARTS-1))/TOTAL_BAR_CHARTS;
                paint.setStrokeWidth(CHART_SERIES_STROKE_WIDTH * screenDensityScale);
                float percent = animatePercentage ();

                paint.setColor(Color.GRAY);
                if (bAnimating && percent < 99)
                        paint.setAntiAlias(false);
                else
                        paint.setAntiAlias(true);
                paint.setTextSize(13.0f * screenDensityScale);

                float distanceBtwLabels = chartWidth / (TOTAL_BAR_CHARTS);
                float labelXCoord = (CHART_LEFT_PADDING - XAXIS_LABEL_XOFFSET + BAR_SPACING/2) * screenDensityScale;
                float labelYCoord = chartHeight + (CHART_TOP_PADDING + XAXIS_LABEL_YOFFSET) * screenDensityScale;
                Bitmap bitmap=null;
                BitmapFactory.Options mNoScale = new BitmapFactory.Options();
                mNoScale.inScaled = false;
                int iconId=0;
                // draw 3 simple bars, speeds for yourphone, yourcarrier and allcarriers
                for (int i=0; i<iSeries.length; i++)
                {
                        if(iSeries[i]!=null){
                                xcoord = (CHART_LEFT_PADDING * screenDensityScale + i *(barWidth+barGap)  + barGap) ;
                                // y coordinate scales by percent while animating
                                ycoordTop = Math.max (ycoordMin, ycoordBot - (chartHeight * iSeries[i] * percent / yMax / 100));

                                //paint.setColor(flatcolors[i]);
                                paint.setShader(shaders[i/2]); 
                                canvas.drawRect(new RectF(xcoord, ycoordTop, xcoord+barWidth, ycoordBot), paint); 
                                paint.setColor(Color.GRAY); 
                                paint.setShader(null); 

                                Integer speed = (int)(iSeries[i] * percent / 100f);
                                float leftPoint=(float) (xcoord+(barWidth-(speed.toString().length()*7.5*screenDensityScale))/2);
                                canvas.drawText(speed.toString(), leftPoint, labelYCoord, paint);
                                labelXCoord += distanceBtwLabels;

                                if(i%2==1){
                                        iconId=R.drawable.arrow_speed_up;
                                }else{
                                        iconId=R.drawable.arrow_speed_down;
                                }

                                bitmap= BitmapFactory.decodeResource(context.getResources(),iconId, mNoScale);
                                float left=xcoord+(barWidth-bitmap.getWidth())/2;
                                float top=ycoordBot-(8*screenDensityScale+bitmap.getHeight());
                                canvas.drawBitmap(bitmap, left, top, null);
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
                float ycoordMin = (CHART_TOP_PADDING - XAXIS_STROKE_WIDTH) * screenDensityScale;
                float ycoordBot = chartHeight + ycoordMin;
                float ycoordTop = ycoordBot;
                for (int i=0; i<3; i++)
                {
                        if (iSeries[i] == null) continue;
                        ycoordTop = Math.max (ycoordMin, ycoordBot - (chartHeight * iSeries[i] / yMax));
                        shaders[i] = new LinearGradient(0, ycoordTop, 0, ycoordBot, colors[i], colors2[i], Shader.TileMode.CLAMP); 
                }
        }

}