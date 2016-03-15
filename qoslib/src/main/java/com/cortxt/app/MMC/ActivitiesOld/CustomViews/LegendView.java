package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.Compare;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class LegendView extends View{

        private static final int TOP_PADDING = 20;
        //private static final int BOTTOM_PADDING = 2;
        private static final float STROKE_WIDTH = 2.0f;
        public static final float FONT_SIZE = 16.0f;

        private int myWidth = 0;
        //private int myHeight = 0;
        private int statIndex = 0;
        private int myCallsDropped = 0, myCallsFailed = 0, myCallsNormal = 0;
        private float density;
        private Context context;
        private static final String TAG = NerdView.class.getSimpleName();
        private float backgroundHeight = 60;
        private Bitmap[] icons = new Bitmap[6];
        private String[]colorsPaint={"#FF0000","#FBB03B","#0098CB","#5D5D5D","#666666"};
        String locationOne="En un area de 1 Km alrededor de";
        String locationTwo="Carrera 9,Bogota,Colombia";
        String date="Mayo 16,2012 3:46PM";
        Typeface customFont;
        private Paint paint = null;
        /*
         * End private variables
         * ========================================================
         * Start constructors (and their helper methods)
         */
        public LegendView (Context context) {
                super(context);
                this.context = context;
                init();
        }

        public LegendView (Context context, AttributeSet attrs){
                super(context, attrs);
                this.context = context;
                init();
                processAttributeSet(attrs);
        }

        private void init() {
                density = getContext().getResources().getDisplayMetrics().density;
                myWidth= getContext().getResources().getDisplayMetrics().widthPixels;
                BitmapFactory.Options mNoScale = new BitmapFactory.Options();
                mNoScale.inScaled = false;
                icons[0] = BitmapFactory.decodeResource(context.getResources(),R.drawable.call_blue_icon, mNoScale);
                icons[1] = BitmapFactory.decodeResource(context.getResources(),R.drawable.call_failed_yellow_icon, mNoScale);
                icons[2] = BitmapFactory.decodeResource(context.getResources(),R.drawable.call_dropped_yellow_icon, mNoScale);
                icons[3] = BitmapFactory.decodeResource(context.getResources(),R.drawable.download_blue_icon, mNoScale);
                icons[4] = BitmapFactory.decodeResource(context.getResources(),R.drawable.download_yellow_icon, mNoScale);
                icons[5] = BitmapFactory.decodeResource(context.getResources(),R.drawable.download_red_icon, mNoScale);
                if (density != 1.5)
                {
                        for (int i=0; i<6; i++)
                                icons[i] = Bitmap.createScaledBitmap(icons[i], (int)(32.0*density/1.5), (int)(32.0*density/1.5), true);

                }
                customFont = FontsUtil.getCustomFont (MmcConstants.font_Regular, context);
                paint = new Paint();
                paint.setStrokeWidth(STROKE_WIDTH * density);
                paint.setStyle(Style.FILL);
                paint.setShader(null);
                paint.setTextSize(FONT_SIZE * density);
                paint.setAntiAlias(true);
                paint.setTypeface(customFont);
        }

        private void processAttributeSet(AttributeSet attrs) {

        }

        @Override
        protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);

                float top = TOP_PADDING * density;

                String[] labels = new String[3];        
                float labelX;
                float labelY=0;
                int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
                
                if(Compare.STATS_URLS[statIndex].equals( Compare.DOWNLOADSPEED_PAGE_URL)){
                        labels[0] = context.getString(R.string.mystats_yourphone);
                        labels[1] = context.getString(R.string.mystats_yourcarrier);
                        labels[2] = context.getString(R.string.mystats_allcarriers);
                        if (customTitles == 1)
                        {
                        	labels[0] = context.getString(R.string.mystats_custom_yourphone);
                            labels[1] = context.getString(R.string.mystats_custom_yourcarrier);
                            labels[2] = context.getString(R.string.mystats_custom_allcarriers);
                        }
                }
                else{
                        labels[0] = context.getString(R.string.mystats_droppedcalls);
                        labels[1] = context.getString(R.string.mystats_failedcalls);
                        labels[2] = context.getString(R.string.mystats_normalcalls);
                        if (customTitles == 1)
                        {
                        	labels[0] = context.getString(R.string.mystats_custom_droppedcalls);
                            labels[1] = context.getString(R.string.mystats_custom_failedcalls);
                            labels[2] = context.getString(R.string.mystats_custom_normalcalls);
                        }
                        
                        if (Compare.STATS_URLS[statIndex].equals(Compare.MYCALLSTATS_PAGE_URL)){
                                int iTotal = myCallsDropped + myCallsFailed + myCallsNormal;
                                if (myCallsDropped + myCallsFailed + myCallsNormal == 0)
                                        iTotal = myCallsNormal = 1;
                                
                                // we need only labels -- no need for following
//                                int dropPercent = 100 * myCallsDropped / iTotal;
//                                int failPercent = 100 * myCallsFailed / iTotal;
//                                int normalPercent = 100 * myCallsNormal / iTotal;
//                                labels[0] = normalPercent+"% "+labels[0];
//                                labels[1] = failPercent+"% "+labels[1];
//                                labels[2] = dropPercent+"% "+labels[2];
                        }
                }
                for (int i=0; i<3; i++){
                        int left=(int) ((25 + 100*i)*density);
                        int topPoint=(int) (top);
                        int rightPoint=(int) (left+(16*density));
                        int bottomPoint=(int) (topPoint+(16*density));
//                        Rect iconRect = new Rect(left,topPoint, rightPoint ,bottomPoint); 
                        if(Compare.STATS_URLS[statIndex].equals(Compare.DOWNLOADSPEED_PAGE_URL))
                                paint.setColor(Color.parseColor(colorsPaint[2-i]));
                        else
                                paint.setColor(Color.parseColor(colorsPaint[i]));
                        canvas.drawRect(left, topPoint, rightPoint, bottomPoint, paint);
                        paint.setColor(Color.parseColor(colorsPaint[3]));
                        // we don't have '%' in the labels now -- don't know what's the intention behind the following
//                        int percentIndex=labels[i].indexOf("%");
//                        if(Compare.STATS_URLS[statIndex].equals(Compare.MYCALLSTATS_PAGE_URL)){
//                                labelX=rightPoint+(16*density)/percentIndex;
//                        }else{
//                                labelX=rightPoint+20*density;
//                        }
                        labelX = rightPoint + 5 * density;
                        labelY=top + 15 * density;
                        String split[] = labels[i].split(" ");
                        String topPart = split[0];
                        String bottomPart = "";
                        for (int j=1;j<split.length-1; j++)
                                topPart += " " + split[j];
                        if (split.length > 1)
                                bottomPart = split[split.length-1];
                        canvas.drawText(topPart,labelX,labelY, paint);
                        canvas.drawText(bottomPart,labelX,labelY + 20*density, paint);
                }

        }

        /*
         * Draw different legend according to which stat screen index is selected
         * Delay the change slightly when changing to screens that show a legend
         */
        public void setStatScreen (final int statInd)
        {
                statIndex = statInd;
                /*
                if (MyStats.STATS_URLS[statIndex].equals(MyStats.MYCALLSTATS_PAGE_URL))
                        statIndex = statInd;
                else
                postDelayed(new Runnable() {
                  @Override
                  public void run() {
                          statIndex = statInd;
                          LegendView.this.invalidate();
                  }
                }, 1000);
                 */
                this.invalidate();
        }

        public void setPieLegend (JSONObject yourStats)
        {
                try{
                        myCallsDropped = yourStats.getInt("droppedCalls");
                        myCallsFailed = yourStats.getInt("failedCalls");
                        myCallsNormal = yourStats.getInt("normallyEndedCalls");
                }
                catch (Exception e) {}
                //statIndex = 0;
                this.invalidate();
        }


                @Override
                protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                        //myHeight = h - (int)((TOP_PADDING + BOTTOM_PADDING) * density);
                        //myWidth = w - (int)((LEFT_PADDING + RIGHT_PADDING) * density);
                        super.onSizeChanged(w, h, oldw, oldh);
                }

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
                }

        private int measureHeight (int heightMeasureSpec){
                int measuredHeight = (int)(backgroundHeight* density);
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
                        result = (int) myWidth;
                        if (specMode == MeasureSpec.AT_MOST) {
                                // Respect AT_MOST value if that was what is called for by measureSpec
                                result = Math.min(result, specSize);
                        }
                }

                return result;
        }
}