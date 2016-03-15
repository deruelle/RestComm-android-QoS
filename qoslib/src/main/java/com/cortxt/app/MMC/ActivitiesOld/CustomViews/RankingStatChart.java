package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.AttributeSet;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ServicesOld.MMCPhoneStateListenerOld;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MMCLogger;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class RankingStatChart extends StatChart {

        /*
         * ==========================================================
         * Start private variables
         */
        
        private static int BAR_SPACING = 10;
        
        public static final float TITLE_FONT_SIZE = 16.0f;
        public static float RANK_FONT_SIZE = 14.0f;
        /**
         * Tag for debugging
         */
        private static final String TAG = RankingStatChart.class.getSimpleName();
        
        Time time = new Time(Time.getCurrentTimezone());                //This is a general time variable that will be constantly "setToNow"
        StatCategory[] categories = new StatCategory[4];        
        int[] colors = { Color.argb(0xFF,  0x00, 0x66, 0xCC), Color.argb(0xFF, 0x99, 0x99, 0x99), Color.argb(0xCC, 0x99, 0x99, 0x99), Color.argb(0x99, 0x99, 0x99, 0x99), Color.argb(0x60, 0x99, 0x99, 0x99), Color.rgb( 0x21, 0x54, 0x91) };
        int[] colors2 = { Color.argb(0, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.argb(0x00, 0x00, 0x00, 0x00), Color.rgb( 0x0, 0x0, 0x0) };
        
        float mYMax = 100;
        float[] colsX = new float[5];
        float[] rowsY = new float[4];
        private Bitmap[] icons = new Bitmap[4];
        int iCategories = 4, iRankings = 3;
    	private boolean bGradient = false;
        /*
         * End private variables
         * ==========================================================
         * Start constructors
         */
        public RankingStatChart(Context context) {
                super(context);
                this.context = context;
                init();
        }
        
        public RankingStatChart(Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                init();
        }
        
        private void init(){
                screenDensityScale = getContext().getResources().getDisplayMetrics().density;
                paint = new Paint();
                CHART_LEFT_PADDING = 15;
                CHART_RIGHT_PADDING = 15;
                //CHART_MIN_HEIGHT = 390;
                //CHART_MAX_HEIGHT = 400;
                CHART_TOP_PADDING = 0;
                CHART_BOTTOM_PADDING = 0;
                BAR_SPACING = 10;
                //int height = getResources().getDisplayMetrics().heightPixels;
                float density = getResources().getDisplayMetrics().density;
                //float h = height/density;
                //h = h - 165 - 85;
                //CHART_MAX_HEIGHT = CHART_MIN_HEIGHT = (int)h;//360;//(int)((h - (50+84))); // *density));
                //density = getContext().getResources().getDisplayMetrics().density;
                BitmapFactory.Options mNoScale = new BitmapFactory.Options();
                mNoScale.inScaled = false;
                icons[0] = BitmapFactory.decodeResource(context.getResources(),R.drawable.gold_ribbon, mNoScale);
                icons[1] = BitmapFactory.decodeResource(context.getResources(),R.drawable.gold_ribbon, mNoScale);
                icons[2] = BitmapFactory.decodeResource(context.getResources(),R.drawable.gold_ribbon, mNoScale);
                icons[3] = BitmapFactory.decodeResource(context.getResources(),R.drawable.gold_ribbon, mNoScale);
//                if (density != 1.5)
//                {
//                        for (int i=0; i<4; i++)
//                                icons[i] = Bitmap.createScaledBitmap(icons[i], (int)(51.0*density/1.5), (int)(64.0*density/1.5), true);
//                
//                }
        }

        @Override
        public void onSingleTap(float x, float y) 
        {
                
        }
        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                int i = 0;
                chartHeight -= 9*screenDensityScale;
                
                if (screenDensityScale < 1.5)
                {
                	iRankings = 3;
                	RANK_FONT_SIZE = 12f;
                }
                if (screenDensityScale > 1.5)
                        chartHeight -= 10*screenDensityScale;
                for (i=0; i<iCategories; i++)
                {
                        rowsY[i] = (chartHeight) * i / iCategories + 6*screenDensityScale;
                }
                colsX[0] = CHART_LEFT_PADDING * screenDensityScale;
                colsX[1] = CHART_LEFT_PADDING * screenDensityScale + chartWidth * 47 / 100;
                colsX[2] = colsX[1] + 12.0f * screenDensityScale;
                colsX[3] = colsX[1] + 65.0f * screenDensityScale;
                colsX[4] = CHART_LEFT_PADDING * screenDensityScale + chartWidth;
        }
        
        /*
         * Compare Screen parent sends the json statistics to this view
         * 
         */
        @Override
        public void setStats (JSONObject stats)
        {
                super.setStats (stats);
                if (stats == null)
                        return;
                mStats = stats;
                // For Dataspeed rankings, detects if the phone is currently in 2G,3G coverage etc
                int networkType = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType();
                int speedTier = MMCPhoneStateListenerOld.getNetworkGeneration(networkType);
                
                String speedkey = "3G";
                switch (speedTier)
                {
                        case 1: case 2: speedkey = "2G"; break;
                        case 5: speedkey = "LTE"; break;
                }
                // Now for speed statistics, it will adjust the key for the type of network
                // these are keys in the main JSON statistics object
                String[] keys = {"covService", "dropPercent", "download" + speedkey, "upload" + speedkey};
                // these are the titles for the 4 categories of statistics
                String[] titles = {context.getString(R.string.rankings_coverage),context.getString(R.string.rankings_dropped),
                                        speedkey + " " + context.getString(R.string.rankings_download), speedkey + " " + context.getString(R.string.rankings_upload)};
                
                int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
                if (customTitles == 1)
                {
                        String[] custTitles = {context.getString(R.string.rankings_custom_coverage),context.getString(R.string.rankings_custom_dropped),
                                speedkey + " " + context.getString(R.string.rankings_custom_download), speedkey + " " + context.getString(R.string.rankings_custom_upload)};
                        titles = custTitles;
                }
                boolean[] descending = {true, false, true, true};
                int i;
                
                // Build the 4 statistics categories ready to draw the 4 sections of the rankings screen
                for (i=0; i<iCategories; i++)
                {
                        categories[i] = (new StatCategory(mStats, keys[i], titles[i], descending[i]));
                }
                shaders = null;
                invalidate ();
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
                float percent = animatePercentage ();
                setShaders ();
                
                Typeface customFont = FontsUtil.getCustomFont (MmcConstants.font_MEDIUM, context);
                paint.setTypeface(customFont);
                //paint.setColor(Color.rgb(255, 0, 0));
                //RectF rect = new RectF(0, 0, chartWidth, chartHeight); 
                //canvas.drawRect(rect, paint); 
                
                // Draw the 4 main sections (categories) of the rankings view
                if(categories != null)
                {
                        for (int i=0; i<iCategories; i++)
                                drawCategory(canvas, i, categories[i], percent);
                }
        }
        
        /*
         * End over-ridden methods
         * ========================================================
         * Start helper methods
         */
        
        /**
         * Called to draw each of the 4 categories of the rankings view
         * This will draw a title and the top carrier logo on the left
         * And draw the top 5 ranking of stats and carrier names on the right
         * This also animates the 5 rankings as the percent grows from 0-100%
         * The animation causes each of the 5 rows to be drawn in gradually from left to right and top to bottom
         */
        private void drawCategory(Canvas canvas, int index, StatCategory category, float percent){
                float padding = BAR_SPACING * screenDensityScale;
                int i = 0;
                try
                {
                        float sectionHeight = rowsY[1] - rowsY[0];// - padding;
                        float titleHeight = sectionHeight / 5;
                        float rankHeight = (sectionHeight-titleHeight)/(iRankings+1);
                        StatCategory cat = categories[index];
                        if (cat == null)
                                return;
                        float density = screenDensityScale;
                        
                        if (bGradient)
                        {
                                paint.setShader(shaders[0]); 
                                canvas.drawRect(new RectF(colsX[0], rowsY[index], colsX[2], rowsY[index] + titleHeight), paint); 
                        }
                        
                        paint.setAntiAlias(true);
                        paint.setTextAlign(Align.LEFT);
                        // min. rect of text
                    Rect textBounds = new Rect();
                    Rect currTextBounds = new Rect();
                    paint.getTextBounds(categories[0].title, 0, categories[0].title.length(), textBounds);
                        
                        paint.setShader(null); 
                        //String screenColor = (this.getResources().getString(R.string.SCREEN_COLOR));
                        int grayColor = 0xffaaaaaa;
                        //grayColor = Math.max(0, grayColor - 0x202020);
                        //grayColor += (0xff000000);
                        
                        paint.setColor(Color.WHITE);
                        
                        //RectF logoRect = new RectF(colsX[0], rowsY[index]+titleHeight, colsX[1], rowsY[index] + sectionHeight - titleHeight); 
                        //canvas.drawRect(logoRect, paint); 
                        
                        // Draws the carrier logo for the top carrier in the current category
                        if (cat.bestlogo != null)
                        {
                                float boxH = sectionHeight - titleHeight*2;
                                float boxW = colsX[1] - colsX[0];
                                float w = (float)cat.bestlogo.getWidth() * screenDensityScale / 1.5f;
                                float h = (float)cat.bestlogo.getHeight() * screenDensityScale / 1.5f;
                                float drawW = 0.0f;
                                float drawH = 0.0f;
                                float boxRatio = boxW / boxH;
                                float imgRatio = w / h;
                                // Carrier logo may require some aspect ratio scaling because the online carrier logo images have different sizes and shapes
                                if (imgRatio > boxRatio)
                                {
                                        drawW = 8*boxW/10;
                                        if (Math.abs(drawW-w) < boxW/6)
                                                drawW = w;
                                        drawH = drawW/imgRatio;
                                }
                                else
                                {
                                        drawH = 8*boxH/10;
                                        if (Math.abs(drawH-h) < boxH/6)
                                                drawH = h;
                                        drawW = drawH*imgRatio;
                                }        
                                Bitmap scaledBitmap = Bitmap.createScaledBitmap(cat.bestlogo, (int)drawW, (int)drawH, true);
                                RectF logoRect = new RectF(colsX[0]+(boxW-drawW)/2, rowsY[index]+titleHeight+(boxH-drawH)/2, colsX[0]+(boxW+drawW)/2, rowsY[index] +titleHeight + (boxH+drawH)/2); 
                                
                                canvas.drawBitmap(scaledBitmap, null, logoRect, paint);
                        }
                        String headingColor = getContext().getResources().getString(R.string.HEADING_COLOR);
                		headingColor = headingColor.length() > 0 ? headingColor : "000000";
                		int hdrColor = Integer.parseInt(headingColor, 16) + (0xff000000);
                		
                        paint.setColor(hdrColor);
                        paint.setTextSize(TITLE_FONT_SIZE * screenDensityScale);
                        canvas.drawText(cat.title, colsX[0] + 40*screenDensityScale, rowsY[index] + 2*screenDensityScale + textBounds.height()-textBounds.bottom, paint);
                        
                        // draws the gold icon representing the given category
                        icons[0].getWidth();
                        icons[0].getHeight();
                        RectF iconRect = new RectF(colsX[0]-11f*0*density, rowsY[index]-1*density, colsX[0]-11f*0*density + icons[index].getWidth(), rowsY[index]-1*density + icons[index].getHeight()); 
                        canvas.drawBitmap(icons[index], null, iconRect, paint);
                        paint.setTextSize(RANK_FONT_SIZE * screenDensityScale);
                        paint.getTextBounds(categories[0].title, 0, categories[0].title.length(), textBounds);
                        
                        // start drawing the right side portion with the 5 rows of listed carriers
                        int currItem = -1, currSize = 0;
                        float currWidth = 0;
                        // while animating, one item at a time will be drawn with a partial size that grows
                        if (percent < 100)
                        {
                                if (percent >= 45)
                                        currWidth = 1;
                                currItem  = (int)percent/20;
                                currSize = (int)(percent - 20*((int)percent/20)) / 2;
                        }
                        
                        // draw the 5 boxes with name and stat values along the right side of the view
                        for (i=0; i<(iRankings); i++)
                        {
                                if (i >= percent/20)  // to animate the top 5 carriers one by one as percent increases to 100
                                        break;
                                Ranking ranking = null;
                                if (category.rankings.size() > i)
                                        ranking = category.rankings.get(i);
                                if (ranking == null)
                                        continue;
                                int nameLength = ranking.name.length();
                                if (bGradient)
                                {        paint.setShader(shaders[i*3+1]); 
                                        RectF rect = new RectF(colsX[1], rowsY[index]+titleHeight+i*rankHeight, colsX[2], rowsY[index]+titleHeight+(i+1)*rankHeight); 
                                        canvas.drawRect(rect, paint); 
                                        paint.setShader(shaders[i*3+2]); 
                                
                                        // While animating each row in, it draws the gradient and text for the current item from left to right
                                        // so a given row may have a variable width while the animation is being drawn
                                        if (i == currItem)
                                        {
                                                nameLength = Math.min(currSize, nameLength);
                                                paint.getTextBounds(ranking.name, 0, nameLength, currTextBounds);
                                                currWidth = currTextBounds.right - currTextBounds.left;
                                        }
                                        // but normally each row is a fixed width
                                        else
                                                currWidth = (colsX[4] - colsX[2]);
                                        // now draw the gradient backgound bar for this row, with the currWidth        
                                        rect = new RectF(colsX[2], rowsY[index]+titleHeight+i*rankHeight, colsX[2] + currWidth, rowsY[index]+titleHeight+(i+1)*rankHeight); 
                                        canvas.drawRect(rect, paint); 
                                        paint.setShader(null); 
                                }
                                
                                // draw the rank number of each carrier (from '1' to '5)
                                paint.setColor(grayColor);
                                
                                canvas.drawText(Integer.toString(i+1), colsX[1] + 2*screenDensityScale, rowsY[index]+titleHeight+i*rankHeight + 2*screenDensityScale + textBounds.height()-textBounds.bottom, paint);
                                paint.setColor(Color.BLACK);
                                if (ranking != null)
                                {
                                        String val = "";
                                        // draw the value of the statistic to the left of the name (or '-' if not applicable)
                                        if (ranking.value == -1 || ranking.value == 99999999 || (ranking.value == 0 && index > 1))
                                                val = " -";
                                        else 
                                        {
                                                if (ranking.value == 100f)
                                                        val = "100";
                                                else
                                                        val = Float.toString(ranking.value); //canvas.drawText(Float.toString(ranking.value), colsX[2] + 2*screenDensityScale, rowsY[index]+titleHeight+i*rankHeight + 2*screenDensityScale + textBounds.height()-textBounds.bottom, paint);
                                                if (index < 2)
                                                        val += "%";
                                                
                                        }
                                        paint.setTextSize(TITLE_FONT_SIZE * screenDensityScale);
                                        canvas.drawText(val, colsX[2] + 2*screenDensityScale, rowsY[index]+titleHeight+i*rankHeight + 2*screenDensityScale + textBounds.height()-textBounds.bottom, paint);
                                        
                                        paint.setTextSize(RANK_FONT_SIZE * screenDensityScale);
                                        // Draw the name of the carrier (this also uses a variable length while its animating each line in)
                                        canvas.drawText(ranking.name.substring(0, nameLength), colsX[3] + 2*screenDensityScale, rowsY[index]+titleHeight+i*rankHeight + 2*screenDensityScale + textBounds.height()-textBounds.bottom, paint);
                                }
                                
                                if (i<iRankings)
                                {
                                        paint.setColor(grayColor);
                                        float space = screenDensityScale*3;
                                        if (screenDensityScale < 1.5)
                                        	space = 0;
                                        canvas.drawLine(colsX[2] - 4*screenDensityScale, rowsY[index]+titleHeight+(i+1)*rankHeight - 2*space, colsX[4], rowsY[index]+titleHeight+(i+1)*rankHeight - 2*space, paint);
                                }
                                
                        }
                        canvas.drawLine(colsX[0] - 4*screenDensityScale, rowsY[index]+sectionHeight - 2*screenDensityScale, colsX[4], rowsY[index]+sectionHeight - 2*screenDensityScale, paint);
                }
                catch (Exception e)
                {
                        e.getMessage();
                }
                 
        }
        
        /*
         * create the shaders for each chart element to avoid creating them on every frame of animation
         */
        protected void setShaders ()
        {
                if (shaders == null)
                {
                        float right = 0, left = 0;
                        
                        shaders = new Shader[18];
                        shaders[0] = new LinearGradient(colsX[0], 0, colsX[1], 0, colors[5], colors2[5], Shader.TileMode.CLAMP); 
                        //make the initial translation to the starting point of the path (on the y axis).
                        for (int i=0; i<6; i++)
                        {
                                for (int j=0; j<3; j++)
                                {
                                        right = j<2 ? colsX[j+1] : colsX[4];
                                        left = j<2 ? colsX[j] : colsX[2];
                                        if (j>0)
                                                shaders[i*3+j] = new LinearGradient(left, 0, right, 0, colors[i], colors2[i], Shader.TileMode.CLAMP); 
                                }
                        }
                        
                }
        }
        
        
        /*
         * End public methods
         * ===============================================================
         * Start helper classes
         */
        
        
        /*
         * There are 4 statistic categories on the ranking screen:
         * Coverage %, Download Speed, Upload Speed, and Dropped Calls
         * Each category has a sorted list of the rankings for up to 5 carriers
         */
        class StatCategory
        {
                public String title;
                public static final int SERIES_DROPPED = 0; 
                public static final int SERIES_FAILED = 0; 
                public static final int SERIES_NORMAL = 0; 
                public float dropped = 0, failed = 0, normal = 0, total = 0;
                public float[] series = new float[3];
                public Bitmap bestlogo = null;
                // the ranking are a sorted list of the 5 carriers values for this statistic category
                public List<Ranking> rankings = new ArrayList<Ranking>(5);
                
                // Constructor extracts the statistics for the 5 carriers from the JSON statistics object
                // and sorts them to give a top 5 ranking, ready to be drawn  
                public StatCategory (JSONObject json, String statkey, String title, boolean descending)
                {
                        this.title = title;
                        //String[] carrkeys = {"yourcarrier", "carrier0", "carrier1", "carrier2", "carrier3"};
                        //for (int i=0; i<5; i++)
                        //{
                        //        if (json.has(carrkeys[i]))
                        Iterator<String> iter = json.keys();
                    while (iter.hasNext()) {
                        String key = iter.next();
                        if (key.length() > 10)
                        {
                                try
                                        {
                                                JSONObject stats = json.getJSONObject(key);
                                                if (stats.has(statkey))
                                                {
                                                        float value = 0.0f;
                                                        if (statkey.equals("dropPercent") || statkey.equals("covService"))
                                                                value = (float)(int)(stats.getDouble(statkey)*10)/10f;
                                                        else
                                                                value = (float)(stats.getInt(statkey) / 100000)/10f;
                                                        rankings.add ( new Ranking (stats.getString("operator"), value, stats.getString("logo"), descending));
                                                }
                                        }
                                        catch (Exception e)
                                        {
                                        }
                                }
                        }
                        Collections.sort(rankings);// Sorts the array list
                        if (rankings.size() > 0 && rankings.get(0) != null && rankings.get(0).logo.length() > 0)
                        {
                                String logoPath = RankingStatChart.this.context.getApplicationContext().getFilesDir() + rankings.get(0).logo;
                                try
                                {
                                        bestlogo = BitmapFactory.decodeFile(logoPath);
                                }
                                catch (OutOfMemoryError e)
                                {
                                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "OutOfMemoryError loading logo " + logoPath);        
                                }
                                catch (Exception e)
                                {
                                        MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "error loading logo " + logoPath, e);        
                                }
                        }
                        else
                                MMCLogger.logToFile(MMCLogger.Level.ERROR, "StatCategory", "StatCategory", "no logo ");        
                }
        }
        
        class Ranking implements Comparable<Ranking>
        {
                public float value;
                public String name;
                public String logo;
                public boolean descending;
                public Ranking (String _name, float _value, String _logo, boolean _descending)
                {
                        name = _name;
                        value = _value;
                        logo = _logo;
                        descending = _descending;
                }
                
                // Overriding the compareTo method
                public int compareTo(Ranking r){
                        if (descending)
                                return (int)(r.value*100 - value*100);
                        else
                        {
                                if (value == -1)
                                        return 1;
                                else if (r.value == -1)
                                        return -1;
                                return (int)(value*100 - r.value*100);
                        }
                }

                
        }
        
        
        /*
         * End helper classes
         * ================================================================
         */
}