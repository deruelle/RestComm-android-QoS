package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import java.text.NumberFormat;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.UtilsOld.Carrier;

public class CallStatChartNew extends StatChartNew {

	/*
	 * ==========================================================
	 * Start private variables
	 */

	/**
	 * This is the distance between the x axis and the time labels
	 */
	private static final int XAXIS_LABEL_YOFFSET = 14;
	/**
	 * This is the distance by which all the labels on the x axis are moved left.
	 */
	private static final int XAXIS_LABEL_XOFFSET = 0;
	private static final float XAXIS_STROKE_WIDTH = 1.67f;// 2.67f;
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
	private static final String TAG = CallStatChartNew.class.getSimpleName();

	Time time = new Time(Time.getCurrentTimezone()); // This is a general time variable that will be constantly
														// "setToNow"
	StatColumn[] columns = new StatColumn[3];
	// Bright end of gradients for the 3 bars
	int[] flatcolors = { Color.parseColor("#FF0000"), Color.parseColor("#FBB03B"), Color.parseColor("#0099CC"), Color.parseColor("#FF0000"), Color.parseColor("#FBB03B"), Color.parseColor("#0099CC"), Color.parseColor("#FF0000"), Color.parseColor("#FBB03B"), Color.parseColor("#0099CC") };

	// Bright end of gradients for the 3 bars
	int[] colors = { Color.rgb(0x99, 0x20, 0x27), Color.rgb(0xFF, 0xD5, 0x61), Color.rgb(0x29, 0x66, 0xCC) };
	// Dark end of gradients for the 3 bars
	int[] colors2 = { Color.rgb(0xFF, 0x00, 0x00), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x29, 0xAB, 0xE2) };

	float mYMax = 100;

	// new colors
	// [0] for dropped calls, [1] for failed calls, [2] for normal calls
	int[] newBarColors = { 0xffff0000, 0xfffbb03b, 0xff0099cc };
	// new text colors -- [0] for title Call Stats Benchmark, [1] for labels
	int[] newTextColors = { 0xff5d5d5d, 0xff666666 };

	/* Text sizes for title, lable and percentages -- note that this might change in setHeightRatio depending on screen height.
	 * [0] for title, [1] for chart labels, [2] for percentages
	 */
	float[] newTextSize = { 24f * screenDensityScale, 18f * screenDensityScale, 16f * screenDensityScale };

	// fonts
	private Typeface robotoRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
	private Typeface robotoLight = FontsUtil.getCustomFont(MmcConstants.font_Light, context);
	private Typeface robotoMedium = FontsUtil.getCustomFont(MmcConstants.font_MEDIUM, context);

	// width and distances
	// space between label and bar -- applies to bar and percentages below bar as well.
	float separator = 6f * screenDensityScale;
	float barWidth = 18f * screenDensityScale; // width of the horizontal bar
	float squareWidth = 12f * screenDensityScale; // width/height of small color squares below bar chart
	float sqrToLbl = 5 * screenDensityScale; // distance from color square to next-to-right percentage label

	private String PAGE_TITLE = context.getString((int) CompareNew.STRINGS.get("callstats"));
	private Rect textScale = new Rect(); // rect to measure text bounds
	NumberFormat nf = NumberFormat.getInstance(); // format percentage

	// flag to check if content is scaled to height
	private boolean contentScaled = false;
	private Rect percentLblRect = new Rect(); // rect to measure percentage text bounds
	boolean isLandscape = false;
	
	/*
	 * End private variables
	 * ==========================================================
	 * Start constructorsfailedPercent
	 */
	public CallStatChartNew(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public CallStatChartNew(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		processAttributeSet(context, attrs);
		init();
	}

	private void init() {
		DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		screenDensityScale = dm.density;
		paint = new Paint();
		paint.setAntiAlias(true);
		nf.setMaximumFractionDigits(1);
		CHART_LEFT_PADDING = 40; // overrides StatChart padding
		CHART_RIGHT_PADDING = 40; // overrides StatChart padding
		CHART_TO_SCREEN_RATIO = 0.65f;
		isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		if(isLandscape) {
			CHART_TO_SCREEN_RATIO = -1;
		}
		if (TITLE_REFERENCE == null) {
			TITLE_REFERENCE = PAGE_TITLE;
		}
	}

	private void processAttributeSet(Context context, AttributeSet attrs) {
	}

	/*
	 * Compare Screen parent sends the json statistics to the chart
	 */

	@Override
	public void setStats(JSONObject stats) {

		super.setStats(stats);
		if (stats == null)
			return;
		mStats = stats;
		Carrier currentCarrier = ReportManager.getInstance(context.getApplicationContext()).getCurrentCarrier();
		String opid = "0";
		if (currentCarrier != null)
			opid = currentCarrier.OperatorId;
		String[] keytitles = { "yourphone", "yourcarrier", "allcarriers" };
		String[] keys = { "yourphone", opid, "0" };
		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
		int i;
		float max = 0;
		// fill columns[] with values for the 3 stacked columns (one each for yourphone, yourcarrier, allcarreirs)
		for (i = 0; i < 3; i++) {
			try {
				int iDropped = 0, iFailed = 0, iNormal = 0;
				String title = context.getString((int) CompareNew.STRINGS.get(keytitles[i]));
				if (customTitles == 1)
					title = context.getString((int) CompareNew.CUST_STRINGS.get(keytitles[i]));

				if (mStats != null && mStats.has(keys[i])) {
					JSONObject stat = mStats.getJSONObject(keys[i]);
					iDropped = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DROPPED_CALLS));
					iFailed = Integer.parseInt(stat.getString(ReportManager.StatsKeys.FAILED_CALLS));
					iNormal = Integer.parseInt(stat.getString(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS));
					if (iDropped + iFailed + iNormal == 0)
						iNormal = 1;
					max = Math.max(max, (float) (iDropped + iFailed) * 100.0f / (iDropped + iFailed + iNormal));
				} else {
					// we don't have stats
					iNormal = 1;
				}
				columns[i] = new StatColumn(title, iDropped, iFailed, iNormal);
			} catch (JSONException e) {
				e.printStackTrace();

			}
		}
		mYMax = Math.max(25, Math.min(100, (int) ((25 + max) / 25) * 25));
		shaders = null;
	}

	/*
	 * Set heights relative to screen height for texts, separators and bars
	 */
	private void setHeightRatio() {
		chartHeight = getHeight();
//		float titleHeight = (chartHeight/10f);
		// colSpace space available on screen for width of the horizontal column -- 3 columns in total
		float colSpace = chartHeight/3.5f;
		if(isLandscape) {
			colSpace = chartHeight / 4f;
		}
		// colScale is width of horizontal column if it were not restricted by screen size
		float colScale = newTextSize[1] + separator*4 + barWidth + squareWidth;
		
		float hRatio = (colSpace / colScale) * screenDensityScale;
		separator = 6f * hRatio; // vertical space
		barWidth = 18f * hRatio; // width of the horizontal bar
		squareWidth = Math.round(12f * hRatio); // width/height of small color squares below bar chart
		newTextSize[2] = 16f * hRatio;

//		float scaledWidth = calcTextWidth(PAGE_TITLE, robotoRegular, 24f);
//		newTextSize[0] = calcFontSize(PAGE_TITLE, scaledWidth, titleHeight, robotoRegular, 24f * hRatio);
		if(isLandscape) {
			newTextSize[1] = calcFontSize(columns[1].title, chartWidth/2, 16f*hRatio, robotoLight, 16f * hRatio);
		} else {
			newTextSize[1] = calcFontSize(columns[1].title, chartWidth/2, 18f*hRatio, robotoLight, 18f * hRatio);
		}
		// get text bounds for percentage label
		percentLblRect.setEmpty();
		getTextBounds("99.9%", robotoMedium, newTextSize[2], percentLblRect);

		
		float w = (percentLblRect.width() + squareWidth  + sqrToLbl*4) * 3;
		float r = chartWidth/w;
		if(r<1){
			squareWidth *= r;
			newTextSize[2] = squareWidth;
			sqrToLbl *= r; 
		}
		
		contentScaled = true;
	}

	/*
	 * End Constructors
	 * ===========================================================
	 * Start over-ridden methods (from view)
	 */



	// Called by Android when screen is invalidated (such as during animation timer)
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if(columns[0] == null){
			// stats not populated -- wait for it
			return;
		}
		if (!contentScaled) {
			setHeightRatio();
		}
		float top = (CHART_TOP_PADDING + XAXIS_STROKE_WIDTH) * screenDensityScale;
		float left = CHART_LEFT_PADDING * screenDensityScale;
		
		//TODO remove next 2 lines
//		paint.setColor(Color.GRAY);
//		canvas.drawRect(left, top, left+chartWidth, top+chartHeight, paint);
		
		// Paint settings for title 'Call Stats Benchmark'
/*		Don't draw the title here... it should be drawn by parent view
		paint.setColor(newTextColors[0]);
		paint.setTextSize(newTextSize[0]); // 24
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTypeface(robotoRegular);
		textScale.setEmpty();

		paint.getTextBounds(TITLE_REFERENCE, 0, TITLE_REFERENCE.length(), textScale);
		top += textScale.height();
		canvas.drawText(PAGE_TITLE, left + chartWidth / 2, top, paint);
*/
		// All other items are drawn left aligned
		paint.setTextAlign(Paint.Align.LEFT);
		top += separator;

		// draw chart for 'Your Phone'
		top = drawBarChart(canvas, columns[0], left, top);

		// draw chart for 'Your Carrier'
		top = drawBarChart(canvas, columns[1], left, top); // draw second column

		// draw chart for 'All Carriers'
		top = drawBarChart(canvas, columns[2], left, top); // draw third column
	}

	/*
	 * End over-ridden methods
	 * ===========================================================
	 * Start helper methods
	 */


	private float drawBarChart(Canvas canvas, StatColumn column, float left, float top) {
		// draw text for label i.e. 'Your Phone', 'Your Carrier' and 'All Carriers'
		paint.setTypeface(robotoLight);
		paint.setColor(newTextColors[1]);
		paint.setTextSize(newTextSize[1]); // 18
		textScale.setEmpty();
		paint.getTextBounds(columns[0].title, 0, 2, textScale);
		top += textScale.height() + separator; // update y-coord for text label
		canvas.drawText(column.title, left, top, paint);

		top += separator; // update y-coord for bar

		// dstPercent determines horizontal distance for each call category -- to calculate width of the 3 sections of
		// the bar
		float dstPercent = chartWidth / column.total;
		// animate drawing the bar -- increasing/extending from left to right
		float percent = animatePercentage();
		float animDstPercent = dstPercent * percent / 100f;
		float dRight = left + (column.dropped * animDstPercent); // right end of bar for dropped calls
		float fRight = dRight + (column.failed * animDstPercent); // right end of bar for failed calls
		float nRight = fRight + (column.normal * animDstPercent); // right end of bar for normal calls

		// bar for dropped calls
		paint.setColor(newBarColors[0]);
		canvas.drawRect(left, top, dRight, top + barWidth, paint);

		if (percent > 75f) {
			drawPercentage(canvas, column.series[0], left, top + barWidth);
		}

		// failed calls
		paint.setColor(newBarColors[1]);
		canvas.drawRect(dRight, top, fRight, top + barWidth, paint);
		if (percent > 85f) {
			float sqrLeft = left + percentLblRect.width() + squareWidth + sqrToLbl*4;
			drawPercentage(canvas, column.series[1], sqrLeft, top + barWidth);
		}

		// normal calls
		paint.setColor(newBarColors[2]);
		canvas.drawRect(fRight, top, nRight, top + barWidth, paint);
		if (percent > 95) {
			float sqrLeft = left + (percentLblRect.width() + squareWidth  + sqrToLbl*4) * 2;
			drawPercentage(canvas, column.series[2], sqrLeft, top + barWidth);
		}
		top += barWidth + separator; // bottom coordinate of bar + separator
		top += squareWidth + separator; // bottom coordinate of color square + separator
		return top;
	}

	private void drawPercentage(Canvas canvas, float percent, float left, float top) {
		top += separator;
		paint.setTypeface(robotoMedium);
		paint.setTextSize(newTextSize[2]); // 16
//		Log.e(TAG, (int)left + " : " + squareWidth);
		canvas.drawRect((int)left, (int)top, (int)left + squareWidth, (int)top + squareWidth, paint);

		// draw percentage label
		left += squareWidth + sqrToLbl;
		paint.setColor(newTextColors[1]);
		canvas.drawText(nf.format(percent) + "%", left, top + squareWidth, paint);
//		canvas.drawText("99.9%", left, top + squareWidth, paint);
	}

	/*
	 * End helper methods
	 * ================================================================
	 */
	
	class StatColumn {
		public String title;
		public static final int SERIES_DROPPED = 0;
		public static final int SERIES_FAILED = 0;
		public static final int SERIES_NORMAL = 0;
		public float dropped = 0, failed = 0, normal = 1, total = 0;
		public float[] series = new float[3];

		public StatColumn(String title, int iDropped, int iFailed, int iNormal) {
			this.title = title;
			this.dropped = iDropped;
			this.failed = iFailed;
			this.normal = iNormal;
			int iTotal = iDropped + iNormal + iFailed;
			this.total = iTotal;
			series[0] = (100.0f * iDropped) / iTotal;
			series[1] = (100.0f * iFailed) / iTotal;
			series[2] = 100.0f - series[0] - series[1];
		}

		public float getPercent() {
			return dropped + failed;
		}
	}
}
