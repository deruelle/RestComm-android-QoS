package com.cortxt.app.MMC.ActivitiesOld.CustomViews.Atomic;

import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View.MeasureSpec;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.LegendViewNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.StatChartNew;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Chart.Chart;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;
import com.cortxt.app.MMC.UtilsOld.Carrier;

public class SpeedStatChartAtomic extends StatChartNew {

	/*
	 * ========================================================== Start private
	 * variables
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
	 * The last label in the x axis is the "now" keyword. This is a larger word
	 * than the rest and thus requires an additional offset to fit into the
	 * screen
	 */
	private static final float XAXIS_STROKE_WIDTH = 1.67f;// 2.67f;
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

	// This is a general time variable that will be constantly "setToNow"
	Time time = new Time(Time.getCurrentTimezone());
	int[] flatcolors = { Color.parseColor("#0099CC"), Color.parseColor("#0099CC"), Color.parseColor("#FBB03B"), Color.parseColor("#FBB03B"), Color.parseColor("#FF0000"), Color.parseColor("#FF0000") };
	// Bright end of gradients for the 3 bars
	int[] colors = { Color.rgb(0x29, 0xAB, 0xE2), Color.rgb(0xFF, 0xD5, 0x61), Color.rgb(0xFF, 0x00, 0x00) };
	// Dark end of gradients for the 3 bars
	int[] colors2 = { Color.rgb(0x29, 0x66, 0xCC), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x99, 0x20, 0x27) };

	final int TOTAL_BAR_CHARTS = 6;
	Integer[] iSeries = new Integer[TOTAL_BAR_CHARTS];
	float yMax = 100, yInc = 25;

	// new colors
	private int[] newBarColors = { 0xff00a99d, 0xffed1e79, 0xff9e005d };
	// [0] for title Call Stats Benchmark, [1] for labels
	private int[] newTextColors = { 0xff5d5d5d, 0xff666666 };
	private int arrowsColor = 0xffffffff;

	private float[] newTextSize = { 24f * screenDensityScale, 18f * screenDensityScale, 16f * screenDensityScale, 16f * screenDensityScale };
	// context.getString((int) Compare.STRINGS.get("dataspeeds"));
	private String PAGE_TITLE = null;
	private String[] labels = { context.getString((int) CompareNew.STRINGS.get("yourphone")), context.getString((int) CompareNew.STRINGS.get("yourcarrier")),
			context.getString((int) CompareNew.STRINGS.get("allcarriers")) };

	private Rect textScale = new Rect();
	private boolean contentScaled = false;

	// Constants
	private Typeface robotoRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
	private Typeface robotoLight = FontsUtil.getCustomFont(MmcConstants.font_Light, context);
	private Typeface robotoMedium = FontsUtil.getCustomFont(MmcConstants.font_MEDIUM, context);

	private float separator = 6.0f * screenDensityScale;
	private float barWidth = 18.0f * screenDensityScale;
	private Rect speedTextRect = new Rect();
	private float maxSpeed = 0f;
	String downloadStr = "Download", uploadStr = "Upload";

	/*
	 * End private variables
	 * ========================================================== Start
	 * constructors
	 */
	public SpeedStatChartAtomic(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public SpeedStatChartAtomic(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	// initialize the values for the chart drawing
	private void init() {
		DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		screenDensityScale = dm.density;
		CHART_TO_SCREEN_RATIO = 0.8f;
		paint = new Paint();
		paint.setAntiAlias(true);
		CHART_LEFT_PADDING = 40;
		CHART_RIGHT_PADDING = 40;
		PAGE_TITLE = getTitle();
		if (TITLE_REFERENCE == null) {
			TITLE_REFERENCE = PAGE_TITLE;
		}
		downloadStr = getResources().getString(R.string.speedtest_download_speed);
		uploadStr = getResources().getString(R.string.speedtest_upload_speed);
		
		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
		if (customTitles == 1)
		{
			labels[0] = context.getString((int) CompareNew.CUST_STRINGS.get("yourphone"));
			labels[1] = context.getString((int) CompareNew.CUST_STRINGS.get("yourcarrier"));
			labels[2] = context.getString((int) CompareNew.CUST_STRINGS.get("allcarriers"));
		}
	}

	/**
	 * get title for this section
	 * 
	 * @return
	 */
	public String getTitle() {
		CompareNew compare = (CompareNew) context;
		return compare.getString("downloadspeed");

	}

	/*
	 * Compare Screen parent sends the json statistics to the chart
	 */
	@Override
	public void setStats(JSONObject stats) {
		super.setStats(stats);
		mStats = stats;
		if (stats == null)
			return;
		Carrier currentCarrier = ReportManager.getInstance(context.getApplicationContext()).getCurrentCarrier();
		String currOpid = "0";
		if (currentCarrier != null)
			currOpid = currentCarrier.OperatorId;
		String[] keys = { "yourphone", currOpid, "0" };
		int i;
		float max = 0;
		for (i = 0; i < 3; i++) {
			try {
				if (mStats != null && mStats.has(keys[i])) {
					JSONObject stat = mStats.getJSONObject(keys[i]);
					iSeries[2 * i] = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DOWNLOAD_SPEED_AVERAGE)) / 1000;
					iSeries[2 * i + 1] = Integer.parseInt(stat.getString(ReportManager.StatsKeys.UPLOAD_SPEED_AVERAGE)) / 1000;
					max = Math.max(max, iSeries[2 * i]);
					max = Math.max(max, iSeries[2 * i + 1]);
				} else {
					iSeries[2 * i] = 0;
					iSeries[2 * i + 1] = 0;
				}
			} catch (Exception e) {
				iSeries[2 * i] = 0;
				iSeries[2 * i + 1] = 0;
			}

			// TODO remove next 3
//			iSeries[2 * i] = 700;
//			iSeries[2 * i + 1] = 1100;
//			max = 1100;

		}

		maxSpeed = max;
		yMax = max * 5 / 4;
		yInc = yMax / 5;
		shaders = null;
	}

	/*
	 * Set heights relative to screen height for separators and bars fonts will
	 * take screen width into account as well
	 */
	private void setHeightRatio() {
		// width of unscaled column
		float colScale = newTextSize[1] + separator * 4 + barWidth * 2;

		float hRatio = (chartHeight/ colScale) * screenDensityScale;
		// top margin for labels e.g. 'Your Phone'
		separator = 6f * hRatio;

		// scale ratio for chart title
//		float scaledWidth = calcTextWidth(PAGE_TITLE, robotoRegular, 24f);
//		newTextSize[0] = calcFontSize(PAGE_TITLE, scaledWidth, 24f*hRatio, robotoRegular, 24f * hRatio);
		newTextSize[1] = calcFontSize(labels[1], chartWidth/3, 16f * hRatio, robotoLight, 16f * hRatio);

		// width of the horizontal bar
//		barWidth = 18f * hRatio;

		// let it scale by hRatio since its against bar which is also scaled by
		// same factor
		newTextSize[2] = barWidth = Math.min(14f * hRatio, newTextSize[1]);
		newTextSize[3] = Math.min(10f * hRatio, 16*screenDensityScale);

		contentScaled = true;
	}

	/*
	 * End Constructors
	 * =========================================================== Start
	 * over-ridden methods (from view)
	 */

	// Just some Android methods for it to determine the size of the Chart view
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int calculatedWidth = measureWidth(widthMeasureSpec);
		int calculatedHeight = measureHeight(heightMeasureSpec);
		setMeasuredDimension(calculatedWidth, calculatedHeight);
	}

	private int measureHeight(int heightMeasureSpec) {
		int specMode = MeasureSpec.getMode(heightMeasureSpec);
		int specSize = MeasureSpec.getSize(heightMeasureSpec);
		int measuredHeight = specSize;
		measuredHeight -= (CHART_BOTTOM_PADDING + CHART_TOP_PADDING) * screenDensityScale;
		if (screenDensityScale > 1.5)
			measuredHeight -= 20 * screenDensityScale;
		return measuredHeight;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (iSeries[0] == null) {
			// stats not populated yet
			return;
		}
		if (!contentScaled) {
			setHeightRatio();
		}
		float top = (CHART_TOP_PADDING + XAXIS_STROKE_WIDTH) * screenDensityScale;
		float left = CHART_LEFT_PADDING * screenDensityScale;

		// TODO remove next 2 lines
//		paint.setColor(0x33ff00ff);
//		canvas.drawRect(0, 0, chartWidth + CHART_RIGHT_PADDING, chartHeight + CHART_BOTTOM_PADDING, paint);

/*		Page title should be drawn by parent view
		// Paint settings for page title
		paint.setColor(newTextColors[0]);
		paint.setTextSize(newTextSize[0]); // 24
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTypeface(robotoRegular);
		textScale.setEmpty();

		// use TITLE_REFERENCE height for all titles to place them vertically
		// aligned
		paint.getTextBounds(TITLE_REFERENCE, 0, TITLE_REFERENCE.length(), textScale);

		top += textScale.height();
		canvas.drawText(PAGE_TITLE, left + chartWidth / 2, top, paint);

		// add gap b/w title and rest of chart elements
		top += separator;
*/
		paint.setTextAlign(Paint.Align.LEFT);

		if(getId() == R.id.stats_chart){
			// yourphone
			top = drawColumn(canvas, labels[0], iSeries[0], iSeries[1], newBarColors[0], left, top);
		} else if (getId() == R.id.stats_chart2){
			// allcarriers
			top = drawColumn(canvas, labels[2], iSeries[4], iSeries[5], newBarColors[2], left, top);
		}else if(getId() == R.id.stats_chart3){
			// yourcarrier
			top = drawColumn(canvas, labels[1], iSeries[2], iSeries[3], newBarColors[1], left, top);
		}
	}

	/*
	 * End over-ridden methods
	 * =========================================================== Start helper
	 * methods
	 */

	/**
	 * Draw a bar column
	 * 
	 * @param canvas
	 * @param label
	 * @param dSpeed
	 *            Download speed
	 * @param uSpeed
	 *            upload speed
	 * @param barColor
	 *            color of the bar
	 * @param left
	 *            left coordinate
	 * @param top
	 *            top coordinate
	 * @return bottom coordinate of vertically lowest element drawn
	 */
	private float drawColumn(Canvas canvas, String label, int dSpeed, int uSpeed, int barColor, float left, float top) {

		float animator = animatePercentage() / 100f;
		paint.setColor(newTextColors[1]);
		paint.setTextSize(newTextSize[1]); // 18
		paint.setTypeface(robotoLight);
		textScale.setEmpty();
		paint.getTextBounds(labels[0], 0, 2, textScale);

		// update y-coordinate for text label
		top += textScale.height() + separator;
		canvas.drawText(label, left, top, paint);

		// 'speed' text bounds
		getTextBounds(maxSpeed + "", robotoMedium, newTextSize[2], speedTextRect);
		
		float barXOffset = speedTextRect.width() + separator / 2 ;
		// Draw 'Download'
		paint.setTypeface(robotoMedium);
		paint.setTextSize(newTextSize[3]);
		textScale.setEmpty();
		paint.getTextBounds(downloadStr, 0, downloadStr.length(), textScale);
		float downloadStrHeight = textScale.height();
		top += downloadStrHeight + separator;
		canvas.drawText(downloadStr, left + barXOffset, top, paint);

		// update y-coordinate for first bar in the column
		top += separator / 2;

		// vertically center align text wit bar
		float textTopOffset = speedTextRect.height() + (barWidth - speedTextRect.height()) / 2;
		paint.setTypeface(robotoMedium);
		paint.setTextSize(newTextSize[2]); // 16
		canvas.drawText(dSpeed + "", left, top + textTopOffset, paint);
		
		// Draw bar for download speed
		paint.setColor(barColor);
		float dstUnit = (chartWidth - left - barXOffset) / (float) maxSpeed * animator;
		float right1 = left + barXOffset + (dSpeed * dstUnit);
		canvas.drawRect(left + barXOffset, top, right1, top + barWidth, paint);
		drawTriangle(canvas, left + barXOffset, top, barWidth / 2, false);

		// Draw 'Upload'
		top += barWidth + separator;
		paint.setTypeface(robotoMedium);
		paint.setTextSize(newTextSize[3]);
		paint.setColor(newTextColors[1]);
		top += downloadStrHeight;
		canvas.drawText(uploadStr, left + barXOffset, top, paint);

		// update y-coordinate for second bar in the column
		top += separator / 2;
		// draw speed for second bar
		paint.setTextSize(newTextSize[2]); // 16
		canvas.drawText(uSpeed + "", left, top + textTopOffset, paint);
		// draw second bar in this column
		paint.setColor(barColor);
		float right2 = left + barXOffset + (uSpeed * dstUnit);
		canvas.drawRect(left + barXOffset, top, right2, top + barWidth, paint);
		// draw indicator triangle for second bar in this column
		drawTriangle(canvas, left + barXOffset, top, barWidth / 2, true);

		// return bottom of vertically lowest element drawn (including separator
		// after the bar)
		return top + barWidth + separator;
	}

	/**
	 * Draws a white triangle at give <code>left</code> and <code>top</code>.
	 * Every side is equal to given <code>size</code>
	 * 
	 * @param canvas
	 * @param left
	 * @param top
	 * @param size
	 * @param down
	 */
	private void drawTriangle(Canvas canvas, float left, float top, float size, boolean down) {
		// top offset, so that triangle is vertically centered in bar
		top += size / 2f;
		// left offset
		left += size / 2f;
		// x1, y1, x2, y2, x3, y3
		float verts[] = { left, top, left + size, top, left + size / 2, top + size };
		if (down) {
			verts[0] = left + (size / 2);
			verts[1] = top;
			verts[2] = left;
			verts[3] = top + size;
			verts[4] = left + size;
			verts[5] = top + size;
		}

		// test white square
		// canvas.drawRect(left, top, left+size, top+size, paint);

		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

		paint.setStrokeWidth(1);
		paint.setColor(arrowsColor);
		paint.setStyle(Paint.Style.FILL_AND_STROKE);
		paint.setAntiAlias(true);

		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.moveTo(verts[0], verts[1]);
		path.lineTo(verts[2], verts[3]);
		path.lineTo(verts[4], verts[5]);
		path.close();

		canvas.drawPath(path, paint);
	}

	/*
	 * ========================================================
	 */
}
