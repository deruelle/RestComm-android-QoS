package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Compare;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.UtilsOld.Carrier;

public class CallStatChart extends StatChart {

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
        private static final float XAXIS_STROKE_WIDTH = 1.67f;//2.67f;
        /**
         * Stroke width of the grid lines.
         */
        private static final float GRID_LINE_STROKE_WIDTH = 1.3f;
        /**
         * Stroke width of the time series on the chart.
         */
        private static final float CHART_SERIES_STROKE_WIDTH = 1.3f;

        private static final int BAR_SPACING = 50;

        public static final float CHART_XAXIS_FONT_SIZE = 12.0f;
        /**
         * Tag for debugging
         */
        private static final String TAG = CallStatChart.class.getSimpleName();

        Time time = new Time(Time.getCurrentTimezone());                //This is a general time variable that will be constantly "setToNow"
        StatColumn[] columns = new StatColumn[3];        
        // Bright end of gradients for the 3 bars
        int []flatcolors={Color.parseColor("#FF0000"),Color.parseColor("#FBB03B"),Color.parseColor("#0099CC"),Color.parseColor("#FF0000"),Color.parseColor("#FBB03B"),Color.parseColor("#0099CC"),Color.parseColor("#FF0000"),Color.parseColor("#FBB03B"),Color.parseColor("#0099CC")};

        // Bright end of gradients for the 3 bars
        int[] colors = { Color.rgb(0x99, 0x20, 0x27), Color.rgb(0xFF, 0xD5, 0x61), Color.rgb(0x29, 0x66, 0xCC) };
        // Dark end of gradients for the 3 bars
        int[] colors2 = { Color.rgb(0xFF, 0x00, 0x00), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x29, 0xAB, 0xE2) };

        float mYMax = 100;

        /*
         * End private variables
         * ==========================================================
         * Start constructors
         */
        public CallStatChart(Context context) {
                super(context);
                this.context = context;
                init();
        }

        public CallStatChart(Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                processAttributeSet(context, attrs);
                init();
        }

        private void init(){
                screenDensityScale = getContext().getResources().getDisplayMetrics().density;
                paint = new Paint();
                CHART_BOTTOM_PADDING = 85;
//                int height = getResources().getDisplayMetrics().heightPixels;
//                int width = getResources().getDisplayMetrics().widthPixels;
//                float density = getResources().getDisplayMetrics().density;
//                float h = height/density;
//                float w = width/density;
//                float ratio=h/533;
//                CHART_MAX_HEIGHT = CHART_MIN_HEIGHT  = (int)((h - 280*ratio));
        }

        private void processAttributeSet(Context context, AttributeSet attrs){
        }
        private boolean bZoom = false;
        private boolean bZoomAnimate = false;

        // Single tap causes a zoom-in animation usually from 100% to 25%
        @Override
        public void onSingleTap(float x, float y) 
        {
                if (y < screenDensityScale * 100)
                        return;  // ignore clicks on action bar
                bZoom = !bZoom;
                bZoomAnimate = true;
                bAnimating = true;
                animateStart = System.currentTimeMillis();
                animateEnd = System.currentTimeMillis() + 500;
                shaders = null;
                invalidate ();
        }
        /*
         * Compare Screen parent sends the json statistics to the chart
         */
        @Override
        public void setStats (JSONObject stats)
        {

                super.setStats (stats);
                if (stats == null)
                        return;
                mStats = stats;
                //String[] keys = {"yourphone", "yourcarrier", "allcarriers"};
                Carrier currentCarrier = ReportManager.getInstance(context.getApplicationContext()).getCurrentCarrier();
                String opid = "0";
                if (currentCarrier != null)
                        opid = currentCarrier.OperatorId;
                String[] keytitles = {"yourphone", "yourcarrier", "allcarriers"};
                String[] keys = {"yourphone", opid, "0"};
                int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
                
                int i;
                float max = 0;
                // fill columns[] with values for the 3 stacked columns (one each for yourphone, yourcarrier, allcarreirs)
                for (i=0; i<3; i++)
                {
                        try
                        {
                                int iDropped = 0, iFailed = 0, iNormal = 0;
                                String title = context.getString((int)Compare.STRINGS.get(keytitles[i]));
                                if (customTitles == 1)
                                	title = context.getString((int)Compare.CUST_STRINGS.get(keytitles[i]));

                                if (mStats != null && mStats.has(keys[i]))
                                {
                                        JSONObject stat = mStats.getJSONObject(keys[i]);
                                        iDropped = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DROPPED_CALLS));
                                        iFailed = Integer.parseInt(stat.getString(ReportManager.StatsKeys.FAILED_CALLS));
                                        iNormal = Integer.parseInt(stat.getString(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS));
//                                        iDropped=5;
//                                        iFailed=3;
//                                        iNormal=92;
                                        if (iDropped + iFailed + iNormal == 0)
                                                iNormal = 1;
                                        max = Math.max(max, (float)(iDropped + iFailed)*100.0f / (iDropped + iFailed + iNormal));
                                }
                                columns[i] = new StatColumn (title, iDropped, iFailed, iNormal);


                        }
                        catch (JSONException e)
                        {

                        }
                }
                mYMax = Math.max(25, Math.min(100, (int)((25+max)/25) * 25));
                shaders = null;
        }

        /*
         * End Constructors
         * ===========================================================
         * Start over-ridden methods (from view)
         */

        private float yMax = 100, yInc = 25;

        // Called by Android when screen is invalidated (such as during animation timer)
        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                paint.setAntiAlias(true);

                setShaders ();

                // Calculate the max Y range to draw
                // If its in the SingleTap zoom animation
                if (bAnimating && bZoomAnimate)
                {
                        float percent = animatePercentage ();
                        if (bZoom)
                                yMax = 100 - (100-mYMax)*percent/100;
                        else
                                yMax = mYMax + (100-mYMax)*percent/100;

                        if (!bAnimating)
                        {
                                bZoomAnimate = false;
                                invalidate ();
                        }
                }
                // normal drawing (yMax=100% unless zoomed in)
                else
                {
                        yMax = 100;
                        if (bZoom) yMax = mYMax;
                        yInc = yMax / 5;
                }

                // first draw the grid lines and labels
                drawEmptyChart(canvas);

                // then draw the 3 columns
                if(columns.length ==3)
                {
                        for (int i=0; i<3; i++)
                                drawColumn(canvas, i, columns[i]);
                }
                // finally, if zoomed, cut off the top of the graph with a zig-zag
                drawJags (canvas);
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
                                CHART_LEFT_PADDING * screenDensityScale + chartWidth, 
                                chartHeight + (CHART_TOP_PADDING - XAXIS_STROKE_WIDTH) * screenDensityScale, 
                                paint);

                //draw the grid lines parallel to the X-axis
                paint.setStrokeWidth(GRID_LINE_STROKE_WIDTH * screenDensityScale);
                float lineCount = 2; // yMax / yInc;
                float height = chartHeight;
                if (bAnimating && bZoomAnimate)
                {
                        if (bZoom)
                                height = chartHeight * 100 / yMax;
                        else
                                height = chartHeight * mYMax / yMax;
                }
                float gridLineOffset = height / (lineCount);        //distance between 2 consecutive grid lines
                for(int counter = 0; counter < lineCount; counter++){
                        canvas.drawLine(
                                        CHART_LEFT_PADDING * screenDensityScale, 
                                        CHART_TOP_PADDING * screenDensityScale + chartHeight - ((counter+1)*gridLineOffset), 
                                        CHART_LEFT_PADDING * screenDensityScale + chartWidth, 
                                        CHART_TOP_PADDING * screenDensityScale + chartHeight - ((counter+1)*gridLineOffset), 
                                        paint);
                }


                //draw the labels on the x axis
                paint.setColor(Color.GRAY);
                paint.setTextSize(CHART_XAXIS_FONT_SIZE * screenDensityScale);
                float distanceBtwLabels = chartWidth / (3);
                float labelXCoord = (CHART_LEFT_PADDING - XAXIS_LABEL_XOFFSET + BAR_SPACING*0) * screenDensityScale;
                float labelYCoord = chartHeight + (CHART_TOP_PADDING + XAXIS_LABEL_YOFFSET) * screenDensityScale;
                for (int i = 0; i < 3; i ++){
                        if (columns[i] != null)
                                canvas.drawText(columns[i].title, labelXCoord, labelYCoord, paint);
                        labelXCoord += distanceBtwLabels;
                }

                //draw the labels on the y axis, example from 20% to 100%
                paint.setColor(Color.parseColor("#666666"));
                Typeface customFont = FontsUtil.getCustomFont (MmcConstants.font_MEDIUM, context);
                //Typeface customFont=Typeface.createFromAsset(getContext().getAssets(), "fonts/"+MmcConstants.Omnes_ATT_MEDIUM);
                paint.setTypeface(customFont);
                paint.setTextSize(CHART_XAXIS_FONT_SIZE * screenDensityScale);
                distanceBtwLabels = gridLineOffset;
                labelXCoord = (CHART_LEFT_PADDING - 25) * screenDensityScale;
                labelYCoord = (CHART_TOP_PADDING + 5) * screenDensityScale + chartHeight ;
                for (int i = 0; i < 3; i ++){
                        Integer percent = (int)(yInc * (i*2.5));
                        canvas.drawText(percent.toString()+"%", labelXCoord, labelYCoord, paint);
                        labelYCoord -= distanceBtwLabels;
                }
        }

        /*
         * Draw jagged edges to bite the top off the bars if the zoom is not 100%
         */
        private void drawJags (Canvas canvas)
        {
                if (!bAnimating && yMax < 100)
                {
                        // Color dark grey to match the gradient near top of screen
                        paint.setColor(Color.parseColor("#FFFFFF"));
                        paint.setShader(null); 
                        float xcoord = (CHART_LEFT_PADDING + BAR_SPACING/2) * screenDensityScale;
                        float ycoord = CHART_TOP_PADDING * screenDensityScale + 1;
                        Path path = new Path();
                        path.moveTo(xcoord, ycoord);
                        for (int i=0; i<13; i++)
                        {
                                if ((i&1) > 0)
                                        ycoord = (CHART_TOP_PADDING + 12) * screenDensityScale;
                                else 
                                        ycoord = CHART_TOP_PADDING * screenDensityScale + 1;

                                path.lineTo(xcoord, ycoord);
                                xcoord += chartWidth / 12f;
                        }

                        path.close();

                        //now draw the path onto the canvas
                        canvas.drawPath(path, paint);
                }
        }
        /**
         * This method takes the data in the columns and draws a stacked bar on the canvas
         * this is called 3 times, once each for yourphone, yourcarrier and allcarriers
         */
        private void drawColumn(Canvas canvas, int index, StatColumn column){
                float xcoord = 0.0f;
                float ycoordMin = (CHART_TOP_PADDING + XAXIS_STROKE_WIDTH) * screenDensityScale;
                float ycoordBot = chartHeight + ycoordMin - XAXIS_STROKE_WIDTH * screenDensityScale * 2;
                float ycoordTop = ycoordBot;
                float barGap = BAR_SPACING * screenDensityScale;
                float barWidth = (chartWidth - barGap*3)/3;
                paint.setStrokeWidth(CHART_SERIES_STROKE_WIDTH * screenDensityScale);

                float percent = 100;
                if (bAnimating && !bZoomAnimate)
                        percent = animatePercentage ();
                if (column == null)
                        return;

                // get the x coordinate of the bar
                xcoord = (CHART_LEFT_PADDING * screenDensityScale + index *(barWidth+barGap)  + barGap) ;
                float redPercentTop=0;
         
                // Each stacked column consists of 3 sections: %dropped, %failed, and %normal calls
                for (int i=0; i<3; i++)
                {
                        float val = column.series[i];
                        val = ((float)(int)(val*10))/10;
                        String percentage=String.valueOf(val)+"%";
                        float height = Math.min (ycoordBot - ycoordMin, (chartHeight * val / yMax));
                        height = Math.min (height, (ycoordBot - ycoordMin) * percent / 100);
                        ycoordTop = ycoordBot - height;
                        //
                        paint.setShader(shaders[index*3+i]);
                        canvas.drawRect(new RectF(xcoord, ycoordTop, xcoord+barWidth, ycoordBot), paint);
                        float rectYPoint=0;
                        paint.setShader(null); 
                        paint.setColor(flatcolors[index*3+i]);
                        
                        if(val>=0 && val<=100 && i<2){
                                if(i==0){
                                        rectYPoint=ycoordBot-5*screenDensityScale;
                                        redPercentTop=rectYPoint-23*screenDensityScale;
                                }else if(i==1){
                                        rectYPoint=redPercentTop;
                                }
                                canvas.drawRect(new RectF((xcoord-(barGap-4*screenDensityScale)),(rectYPoint-20*screenDensityScale),(xcoord-(barGap-10*screenDensityScale)),(rectYPoint) ), paint);
                                paint.setColor(Color.rgb(0, 0, 0));
                                canvas.drawRect(new RectF((xcoord-(barGap-10*screenDensityScale)),(rectYPoint-20*screenDensityScale),(xcoord-4*screenDensityScale),(rectYPoint) ), paint);
                                paint.setColor(Color.parseColor("#FFFFFF"));
                                canvas.drawText(percentage,(xcoord-(barGap-12*screenDensityScale)),(rectYPoint-6*screenDensityScale), paint);
                        }
                        ycoordBot = ycoordTop - 1;
                }

        }

        /*
         * create the shaders for each chart element to avoid creating them on every frame of animation
         */
        protected void setShaders ()
        {
                if (shaders == null)
                {
                        shaders = new Shader[9];
                        yMax = 100;
                        if (bZoom) yMax = mYMax;
                        //make the initial translation to the starting point of the path (on the y axis).
                        for (int c=0; c<3; c++)
                        {
                                float ycoordMin = (CHART_TOP_PADDING + XAXIS_STROKE_WIDTH) * screenDensityScale;
                                float ycoordBot = chartHeight + ycoordMin - XAXIS_STROKE_WIDTH * screenDensityScale * 2;
                                float ycoordTop = ycoordBot;
                                for (int i=0; i<3; i++)
                                {
                                        int index = c*3 + i;
                                        float val = 0f;
                                        if (columns != null && c < columns.length && columns[c] != null && i < columns[c].series.length)
                                                val = columns[c].series[i];
                                        ycoordTop = Math.max (ycoordMin, ycoordBot - (chartHeight * val / yMax));
                                        shaders[index] = new LinearGradient(0, ycoordTop, 0, ycoordBot, colors2[i], colors[i], Shader.TileMode.CLAMP); 
                                        ycoordBot = ycoordTop - 1;
                                }
                        }
                }
        }

        class StatColumn
        {
                public String title;
                public static final int SERIES_DROPPED = 0; 
                public static final int SERIES_FAILED = 0; 
                public static final int SERIES_NORMAL = 0; 
                public float dropped = 0, failed = 0, normal = 0, total = 0;
                public float[] series = new float[3];

                public StatColumn (String title, int iDropped, int iFailed, int iNormal)
                {
                        this.title = title;
                        int iTotal = iDropped + iNormal + iFailed;
                        series[0] = dropped = 100.0f * iDropped / iTotal;
                        series[1] = failed = 100.0f * iFailed / iTotal;
                        series[2] = normal = 100.0f - dropped - failed;
                }
                public float getPercent ()
                {
                        return dropped + failed;
                }
        }

        /*
         * End helper classes
         * ================================================================
         */
}