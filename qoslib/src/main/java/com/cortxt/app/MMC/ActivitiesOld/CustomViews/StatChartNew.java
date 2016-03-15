package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;

public class StatChartNew extends View {
	
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
	protected int CHART_TOP_PADDING = 10 + 0; // added height of tabs (50)
	
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
	
	protected int CHART_BOTTOM_PADDING = 5;
	protected JSONObject mStats; //the actual statistics
	protected Context context; 
	protected Shader[] shaders = null;
	
	/**
	 * This is the scale with which all lengths get multiplied by to make sure the charts work
	 * on all kinds of screen resolutions.
	 */
	protected float screenDensityScale;
	
	//other variables
	protected Paint paint;	//This is the multi-purpose paint object used by all the methods.

	/**
	 * we use height of TITLE_REFERENCE to place all titles at same y position i.e. for 'All Calls', 'Rankings' and 'Speeds'
	 */
	protected static String TITLE_REFERENCE = null;
	
	/**
	 * Width of the chart
	 */
	protected int chartWidth;
	/**
	 * Height of the chart
	 */
	protected int chartHeight;
	protected int containerHeight;
	protected float CHART_TO_SCREEN_RATIO = 0.72f;
	/**
	 * This is the timespan (in seconds) that the chart is supposed to cover.
	 * By default, it is 4*60 seconds.
	 */
	
	public StatChartNew(Context context) {
		super(context);
		this.context = context;
		//View v = ((Compare)context).findViewById(R.id.compare_container);
		//containerHeight = v.getHeight();
		init();
	}
	
	public StatChartNew(Context context, AttributeSet attrs){
		super(context, attrs);
		this.context = context;
//		TITLE_REFERENCE = context.getString((int) Compare.STRINGS.get("dataspeeds"));
		TITLE_REFERENCE = context.getString((int) CompareNew.STRINGS.get("callstats"));
		//View v = ((Compare)context).findViewById(R.id.compare_container);
		//containerHeight = v.getHeight();
		init();
	}
	
	public int getContainerHeight ()
	{
		View v = ((CompareNew)this.context).findViewById(R.id.compare_container);
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
		DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		screenDensityScale = dm.density;
		if(isTallScreen(context)){
			CHART_TOP_PADDING = 20;
			CHART_TO_SCREEN_RATIO = 0.68f;
		}
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
		chartHeight = h - (int)((CHART_TOP_PADDING + CHART_BOTTOM_PADDING) * screenDensityScale);
		chartWidth = Math.max(w - (int)((CHART_LEFT_PADDING + CHART_RIGHT_PADDING) * screenDensityScale), (int)(CHART_MIN_WIDTH * screenDensityScale));
		LegendViewNew.CHART_HEIGHT = chartHeight;
		super.onSizeChanged(w, h, oldw, oldh);
	}
	
	private int measureHeight (int heightMeasureSpec){
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);
		int measuredHeight = specSize; // Math.max(specSize, (int)((CHART_MIN_HEIGHT + CHART_BOTTOM_PADDING + CHART_TOP_PADDING) * screenDensityScale));
		// some times we may need to ignore CH_TO_SCREEN_RATIO, so we set it to -1 when the view component is initialized
		if(CHART_TO_SCREEN_RATIO != -1)
			measuredHeight = (int) (CHART_TO_SCREEN_RATIO * getResources().getDisplayMetrics().heightPixels);
		// subtract the size of the legend if there is one
		measuredHeight -= (CHART_BOTTOM_PADDING + CHART_TOP_PADDING)*screenDensityScale;
		if (screenDensityScale > 1.5)
			measuredHeight -= 20*screenDensityScale;
		//measuredHeight = Math.min(measuredHeight, (int)(CHART_MAX_HEIGHT * screenDensityScale));
		return measuredHeight;
	}
	
	
	protected int measureWidth(int widthMeasureSpec) {
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
	/**
	 * Calculates smaller font size for a given target width to fit that text in.
	 * @param text text string to calculate text for
	 * @param targetWidth width that the text should fit in
	 * @param paint a plain Paint object. This method will set text size and typeface.
	 * @param tFace TypeFace to use for Paint object.
	 * @param size preferred initial size
	 * @return same or smaller font size for the given text so that it fits into given target width
	 */
	protected float calcFontSize(String text, float targetWidth, float targetHeight, Typeface tFace, float size) {
		paint.setTextSize(size);
		paint.setTypeface(tFace);
		r.setEmpty();
		paint.getTextBounds(text, 0, text.length(), r);
		if(r.width() <= targetWidth && r.height() <= targetHeight){
			return size;
		}
		return calcFontSize(text, targetWidth, targetHeight, tFace, size-2);
	}
	private Rect r = new Rect();
	/**
	 * calculate text width, keeping 360 pixels screen as a base factor.
	 * It finds how wide a text will be on 360 pixels wide screen and then applies a ratio of chart width on current screen to chart width on a 360 pixels wide screen  
	 * @param text text string to calculate width for
	 * @param tFace Typeface to use for the given text
	 * @param size This is font size of the text for the 360 pixels screen e.g. 24f for page title
	 * @return width of text after applying a scale factor 
	 */
	protected float calcTextWidth(String text, Typeface tf, float size){
		// chart width in case of 360 pixels wide screen
		float chartStandardWidth = calculateChartWidth(360*screenDensityScale);
		float scaleFactor = chartWidth/chartStandardWidth;
		Paint paint = new Paint();
		paint.setTypeface(tf);
		paint.setTextSize(size*screenDensityScale); // text size for title
		Rect r = new Rect();
		paint.getTextBounds(text, 0, text.length(), r);
		return r.width() * scaleFactor;
	}
	
	/**
	 * @param w arbitrary screen width to calculate chart size for
	 * @return chart width for given screen width
	 */
	protected float calculateChartWidth(float w) {
		return w - ((CHART_LEFT_PADDING*2) * screenDensityScale);
	}

	/**
	 * 
	 * @param text text string to get height of
	 * @param tf Typeface to apply to the text
	 * @param size size of the text
	 * @param r rectangle to save the text bounds
	 */
	
	protected void getTextBounds(String text, Typeface tf, float size, Rect r){
		Paint p = new Paint();
		p.setTextSize(size);
		p.setTypeface(tf);
		r.setEmpty();
		p.getTextBounds(text, 0, text.length(), r);
	}

	/**
	 * 
	 * @return whether the screen's height is more than its width
	 */
	public static boolean isTallScreen(Context c){
		// might need a little more effort here
		DisplayMetrics dm = c.getResources().getDisplayMetrics();
		float r = (float)dm.heightPixels / (float)dm.widthPixels;
		if(r <= 1f){
			// its square-ish screen
			return false;
		}
		return true;
	}
}
