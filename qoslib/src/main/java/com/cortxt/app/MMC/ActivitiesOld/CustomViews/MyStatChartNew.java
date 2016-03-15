package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.ActivitiesOld.CustomViews.Chart.Chart;
import com.cortxt.app.MMC.Reporters.ReportManager;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class MyStatChartNew extends StatChartNew {

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

	Time time = new Time(Time.getCurrentTimezone()); // This is a general time variable that will be constantly
														// "setToNow"
	// StatColumn[] columns = new StatColumn[3];
	// failed: #fbb03b, dropped: #ff0000, normal:#0099cc
	int[] colors = { Color.rgb(0xfb, 0xb0, 0x3b), Color.rgb(0xff, 0x00, 0x00), Color.rgb(0x00, 0x99, 0xcc) };
	// int[] colors2 = { Color.rgb(0xFF, 0x00, 0x00), Color.rgb(0xF7, 0x87, 0x00), Color.rgb(0x29, 0xAB, 0xE2) };

	Integer[] iSeries = new Integer[3];
	float yMax = 100, yInc = 25;
	String[] titles = new String[3];

	private static final int IS_DRAW = 2;
	private Paint mBgPaints = new Paint();
	private Paint mLinePaints = new Paint();
	private int mWidth;
	private int mHeight;
	private int mGapLeft;
	private int mGapTop;
	private int mBgColor;
	private float mStart;
	private float mSweep;
	int mTotalCalls = 0, iDropped = 0, iFailed = 0, iNormal = 0;
	private Paint mInnerPaint = new Paint();
	private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
	private Typeface fontLight, fontBold, fontRegular;
	float fontScaleFactor = 0.6f; // text scale factor for text inside donut chart 
	private boolean scaled = false;
	boolean isLandscape = false;
	// this changes thickness of the torus
	int torusWidth = 100;
	RectF mOvals = null;
	RectF mOvalsInner = null;
	float[] innerTextSize = {18f, 24f, 16f}; 
	float mVSpace = 20f;

	/*
	 * End private variables
	 * ==========================================================
	 * Start constructors
	 */
	public MyStatChartNew(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public MyStatChartNew(Context context, AttributeSet attrs) {
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
		CHART_LEFT_PADDING = 50;
		CHART_RIGHT_PADDING = 50;
		CHART_TO_SCREEN_RATIO = 0.65f;
		isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		if(isLandscape) {
			// ignore CHART_TO_SCREEN_RATIO -- for Tablet, XML layout tells size of this component
			CHART_TO_SCREEN_RATIO = -1;
			innerTextSize = new float[]{24f, 32f, 22f};
		}
		// int height = containerHeight; //getResources().getDisplayMetrics().heightPixels;
		// float density = getResources().getDisplayMetrics().density;
		// float h = height / density;
		// h = h - 165 - 85;
		// float ratio = h / 533;
		// CHART_MAX_HEIGHT = CHART_MIN_HEIGHT = (int)h;//(int) ((h - 250 * ratio));

		mTextPaint.setStyle(Paint.Style.FILL);
		mInnerPaint.setAntiAlias(true);
		int color = 0xffeaeaea;
		String customBackgroundColor = getResources().getString(R.string.CUSTOM_BACKGROUND_COLOR);
		if (customBackgroundColor.length() > 1) {
			// make inner of pie chart of same color as background
			color = Integer.parseInt(customBackgroundColor, 16) + (0xff000000);
		}
		mInnerPaint.setColor(color); // color to fill inner circle
		mInnerPaint.setShadowLayer(7, 0, 5, 0x32000000);

		fontLight = FontsUtil.getCustomFont(MmcConstants.font_Light, context);
		fontRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
		fontBold = FontsUtil.getCustomFont(MmcConstants.font_MEDIUM, context);
	}
	
	private void scaleContents() {
		mGapLeft = (int) (CHART_LEFT_PADDING * screenDensityScale);
		mGapTop = (int) (CHART_TOP_PADDING * screenDensityScale);
		int size = Math.min(chartWidth, chartHeight);
		mWidth = mHeight = size;
		mGapLeft += (chartWidth - size) / 2;
		if(isLandscape) {
			mGapTop = 0;
		} else {
			mGapTop += (chartHeight - size) / 2;
		}
		torusWidth = mWidth / 10;
		mOvals = new RectF(mGapLeft, mGapTop, mWidth + mGapLeft, mHeight + mGapTop);
		mOvalsInner = new RectF(mGapLeft + torusWidth, mGapTop + torusWidth, mWidth + mGapLeft - torusWidth, mHeight + mGapTop - torusWidth);
		//fontScaleFactor = Math.max(screenDensityScale * ((mWidth - torusWidth - torusWidth)/250f), 0.7f); // 300/480 * mWidth
		fontScaleFactor = Math.max(screenDensityScale * ((mWidth - torusWidth - torusWidth)/450f), 0.7f); // 300/480 * mWidth
		mVSpace = mOvalsInner.width() / 10;//20.0f * fontScaleFactor;
		for (int i = 0; i < innerTextSize.length; i++) {
			innerTextSize[i] = innerTextSize[i] * fontScaleFactor;
		}
		scaled = true;
	}

	private void processAttributeSet(Context context, AttributeSet attrs) {
	}

	/*
	 * Send the statistics to the chart
	 */
	@Override
	public void setStats(JSONObject stats) {
		super.setStats(stats);
		mStats = stats;
		try {
			JSONObject stat = mStats.getJSONObject("yourphone");
			iDropped = Integer.parseInt(stat.getString(ReportManager.StatsKeys.DROPPED_CALLS));
			iFailed = Integer.parseInt(stat.getString(ReportManager.StatsKeys.FAILED_CALLS));
			iNormal = Integer.parseInt(stat.getString(ReportManager.StatsKeys.NORMALLY_ENDED_CALLS));

			// TODO remove following 3 lines
/*			iFailed = 20;
			iDropped = 20;
			iNormal = 20;
*/
			mTotalCalls = iDropped + iFailed + iNormal;
			if (iDropped + iFailed + iNormal == 0)
				mTotalCalls = -1;

			iSeries[0] = 100 * iFailed / mTotalCalls;
			iSeries[1] = 100 * iDropped / mTotalCalls;
			iSeries[2] = mTotalCalls == -1 ? 100 * iNormal / mTotalCalls : 100 - iSeries[0] - iSeries[1];
		} catch (Exception e) {
		}
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

		// TODO remove following line
		// canvas.drawRect(0, 0, getWidth(), getHeight(), mBgPaints);
		
		if(!scaled) {
			scaleContents();
		}
//		setGeometry(size, size, gapLeft, gapTop, 0, 0);
		drawPieChart(canvas);
	}

	/*
	 * End over-ridden methods
	 * ========================================================
	 * Start helper methods
	 */

	/**
	 * This method takes the points stored in the signalTimeSeries variable and draws a series on the canvas.
	 * 
	 * @param canvas
	 * @param timeSeries
	 */
	private void drawPieChart(Canvas canvas) {
		paint.setStrokeWidth(CHART_SERIES_STROKE_WIDTH * screenDensityScale);
		paint.setShadowLayer(7, 0, 5, 0x32000000);
		float percent = animatePercentage();
		// ------------------------------------------------------
		// if (mState != IS_READY_TO_DRAW) return;
		canvas.drawColor(mBgColor);
		// ------------------------------------------------------
		mBgPaints.setAntiAlias(true);
		mBgPaints.setStyle(Paint.Style.FILL);
		mBgPaints.setColor(0x88FF0000);
		mBgPaints.setStrokeWidth(0.5f);
		// ------------------------------------------------------
		mLinePaints.setAntiAlias(true);
		mLinePaints.setStyle(Paint.Style.STROKE);
		mLinePaints.setColor(0xff000000);
		mLinePaints.setStrokeWidth(0.5f);
		// ------------------------------------------------------
		RectF mOvals = new RectF(mGapLeft, mGapTop, mWidth + mGapLeft, mHeight + mGapTop);
		// this changes thickness of the torus
		int torusWidth = mWidth / 10;
		RectF mOvalsInner = new RectF(mGapLeft + torusWidth, mGapTop + torusWidth, mWidth + mGapLeft - torusWidth, mHeight + mGapTop - torusWidth);
		// ------------------------------------------------------

		mStart = 0;
		float maxSweep = 360 * percent / 100f;
		for (int i = 0; i < 3; i++) {
			// Item = (PieItem) mDataArray.get(i);
			if (iSeries[i] == null)
				break;
			mBgPaints.setColor(colors[i]);
			// paint.setShader(shaders[i]);
			paint.setColor(colors[i]);
			mSweep = (float) 360 * ((float) iSeries[i] / (float) 100);
			if (mTotalCalls == -1 && i == 2) {
				mSweep = 360f;
			}
			if (mStart + mSweep > maxSweep)
				mSweep = maxSweep - mStart;
			if (mStart <= maxSweep && mSweep > 0) {
				// canvas.drawArc(mOvals, mStart-90, mSweep, true, mLinePaints);
				canvas.drawArc(mOvals, mStart - 90, mSweep, true, paint);
				mStart += mSweep;
			}
		}
		canvas.drawArc(mOvalsInner, -90, maxSweep, true, mInnerPaint);
		mLinePaints.setColor(0xffcccccc); // color for 1 pixel border of inner circle
		mLinePaints.setStrokeWidth(0);
		canvas.drawArc(mOvalsInner, -90, maxSweep, true, mLinePaints);

		// percent value after which donut's inner text should be drawn
		float drawTextAfter = 70;
		
		// draw calls info inside the chart
		if (percent >= drawTextAfter) {
			float opacity = 255 / (100 - drawTextAfter) * (percent - drawTextAfter);
			float fontScale = screenDensityScale;// * (mWidth/480f);
			float mVSpace = 20.0f * fontScaleFactor;// * (mWidth/480f);
			Rect b = new Rect();
			mTextPaint.setTypeface(fontLight);
			mTextPaint.setColor(0xff666666);
			mTextPaint.setAlpha((int)opacity);
			mTextPaint.setTextSize(innerTextSize[0]);
			mTextPaint.setTextAlign(Paint.Align.CENTER);
			// total calls
			float yc = mOvalsInner.centerY() - 10;
			canvas.drawText(context.getString(R.string.GenericText_Calls), mOvalsInner.right - mOvalsInner.width() / 2, yc - 2 * mVSpace, mTextPaint);
			mTextPaint.setTypeface(fontBold);
			mTextPaint.setTextSize(24.0f * fontScaleFactor);
			canvas.drawText("" + (mTotalCalls == -1 ? 0 : mTotalCalls), mOvalsInner.right - mOvalsInner.width() / 2, yc - mVSpace + 5f, mTextPaint);

			if (iSeries[0] == null) {
				iSeries[0] = 0;
			}
			if (iSeries[1] == null) {
				iSeries[1] = 0;
			}
			if (iSeries[2] == null) {
				iSeries[2] = 0;
			}

			String wideStr = "100% " + mTotalCalls + " " + context.getString(R.string.GenericText_Calls);
			mTextPaint.setTextSize(innerTextSize[2]);
			mTextPaint.getTextBounds(wideStr, 0, wideStr.length(), b);
			float leftOff = (mOvalsInner.width() - b.width()) / 2;

			b.setEmpty();
			mTextPaint.getTextBounds("100% ", 0, 5, b);
			mTextPaint.setTextAlign(Paint.Align.CENTER);
			// float xPc = mOvalsInner.right - (mOvalsInner.width()) / 2 - b.width() - mVSpace / 2;
			float xPc = mOvalsInner.left + leftOff;
			float yPc = mOvalsInner.centerY() - 15f;
			// percentage normal calls
			mTextPaint.setColor(0xff0098cb);
			mTextPaint.setAlpha((int)opacity);
			mTextPaint.setTypeface(fontBold);
			canvas.drawText(iSeries[2] + "%", xPc + b.width() / 2f, yPc + mVSpace, mTextPaint);
			// percentage failed calls
			mTextPaint.setColor(0xfffbb03b);
			canvas.drawText(iSeries[0] + "%", xPc + b.width() / 2f, yPc + 2 * mVSpace, mTextPaint);
			// percentage dropped calls
			mTextPaint.setColor(0xffff0000);
			mTextPaint.setAlpha((int)opacity);
			canvas.drawText(iSeries[1] + "%", xPc + b.width() / 2f, yPc + 3 * mVSpace, mTextPaint);

			mTextPaint.setTextAlign(Paint.Align.LEFT);
			mTextPaint.setColor(0xff666666);
			mTextPaint.setAlpha((int)opacity);
			// number of normal calls
			canvas.drawText(" " + iNormal + " " + context.getString(R.string.GenericText_Calls), xPc + b.width(), yPc + mVSpace, mTextPaint);
			// number of failed calls
			canvas.drawText(" " + iFailed + " " + context.getString(R.string.GenericText_Calls), xPc + b.width(), yPc + 2 * mVSpace, mTextPaint);
			// number of dropped calls
			canvas.drawText(" " + iDropped + " " + context.getString(R.string.GenericText_Calls), xPc + b.width(), yPc + 3 * mVSpace, mTextPaint);

		}
	}

	// --------------------------------------------------------------------------------------
	private void setGeometry(int width, int height, int GapLeft, int GapRight, int GapTop, int GapBottom) {
		mWidth = width;
		mHeight = height;
		mGapLeft = GapLeft;// (int) ((GapLeft + CHART_LEFT_PADDING) * screenDensityScale);
		// mGapRight = (int)((GapRight + CHART_RIGHT_PADDING) * screenDensityScale);
		mGapTop = GapTop;// (int) ((GapTop + CHART_TOP_PADDING) * screenDensityScale);
		// mGapBottom = (int)((GapBottom+ CHART_BOTTOM_PADDING) * screenDensityScale);
		fontScaleFactor = Math.max(screenDensityScale * (mWidth/480f), 0.7f);
	}

	// --------------------------------------------------------------------------------------
	public void setSkinParams(int bgColor) {
		mBgColor = bgColor;
	}

}
