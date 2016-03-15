package com.cortxt.app.MMC.ActivitiesOld.CustomViews;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import com.cortxt.app.MMC.R;
import com.cortxt.app.MMC.Activities.CompareNew;
import com.cortxt.app.MMC.Utils.FontsUtil;
import com.cortxt.app.MMC.Utils.MmcConstants;

public class LegendViewNew extends View {

	/**
	 * CHART_HEIGHT is set by StatChart. This view needs it to scale its color
	 * squares relatively smaller than StatChart views.
	 */
	public static float CHART_HEIGHT = 0f;
	private int TOP_PADDING = 0;
	// private static final int BOTTOM_PADDING = 2;
	private static final float STROKE_WIDTH = 2.0f;
	public static final float FONT_SIZE = 16.0f;

	private static int LEFT_PADDING = 40;
	private static int RIGHT_PADDING = 40;

	private int viewWidth = 0;
	private float viewHeight = 60;
	private int statIndex = 0;
	private int myCallsDropped = 0, myCallsFailed = 0, myCallsNormal = 0;
	private float density;
	private Context context;
	private static final String TAG = NerdView.class.getSimpleName();
	// private Bitmap[] icons = new Bitmap[6];
	private int[] newColors = { 0xffff0000, 0xfffbb03b, 0xff0098cb, 0xff5d5d5d, 0xff666666 };
	String locationOne = "En un area de 1 Km alrededor de";
	String locationTwo = "Carrera 9,Bogota,Colombia";
	String date = "Mayo 16,2012 3:46PM";
	Typeface robotoRegular = null;
	private Paint paint = null;
	String[] labels = null;

	Rect textScaleRect = new Rect(); // rect to get text bounds in
	float fontSize = 0; // font size for legend view, to be calculated once in
						// calcFontSize
	float legendWidth = 0f; // width of view available to draw legend (after
							// excluding PADDINGS)
	private float sqrSize = 8f;
	private boolean scaled = false;
	private float separator = 5f;

	private boolean isTablet = false;

	/*
	 * End private variables
	 * ======================================================== Start
	 * constructors (and their helper methods)
	 */
	public LegendViewNew(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public LegendViewNew(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
		processAttributeSet(attrs);
	}

	private void init() {
		isTablet = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
		density = dm.density;
		viewWidth = dm.widthPixels;
		legendWidth = viewWidth - (LEFT_PADDING + RIGHT_PADDING) * density;
		// viewHeight = 30f * density;
		viewHeight = Math.max(0.05f * dm.heightPixels, 60f);
		labels = new String[3];
		int customTitles = getResources().getInteger(R.integer.CUSTOM_COMPARENAMES);
		if (customTitles == 1) {
			labels[0] = getContext().getString((int) CompareNew.CUST_STRINGS.get("droppedcalls"));
			labels[1] = getContext().getString((int) CompareNew.CUST_STRINGS.get("failedcalls"));
			labels[2] = getContext().getString((int) CompareNew.CUST_STRINGS.get("normalcalls"));
		} else {
			labels[0] = getContext().getString(R.string.mystats_droppedcalls);
			labels[1] = getContext().getString(R.string.mystats_failedcalls);
			labels[2] = getContext().getString(R.string.mystats_normalcalls);
		}
		// labels[0] = "Dropped Calls"; labels[1] = "Failed Calls"; labels[2] =
		// "Normal Calls";
		BitmapFactory.Options mNoScale = new BitmapFactory.Options();
		mNoScale.inScaled = false;
		// icons[0] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.call_blue_icon, mNoScale);
		// icons[1] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.call_failed_yellow_icon,
		// mNoScale);
		// icons[2] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.call_dropped_yellow_icon,
		// mNoScale);
		// icons[3] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.download_blue_icon, mNoScale);
		// icons[4] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.download_yellow_icon, mNoScale);
		// icons[5] = BitmapFactory.decodeResource(context.getResources(),
		// R.drawable.download_red_icon, mNoScale);
		// if (density != 1.5) {
		// for (int i = 0; i < 6; i++)
		// icons[i] = Bitmap.createScaledBitmap(icons[i], (int) (32.0 * density
		// / 1.5), (int) (32.0 * density / 1.5),
		// true);
		// }
		robotoRegular = FontsUtil.getCustomFont(MmcConstants.font_Regular, context);
		paint = new Paint();
		paint.setStrokeWidth(STROKE_WIDTH * density);
		paint.setStyle(Style.FILL);
		paint.setShader(null);
		paint.setTextSize(FONT_SIZE * density);
		paint.setAntiAlias(true);
		paint.setTypeface(robotoRegular);
	}

	private void processAttributeSet(AttributeSet attrs) {

	}

	/**
	 * Other StatChart squares use a certain ratio to calculate their size. We
	 * need legend squares to be relatively smaller than those other squares.
	 * This method calculates that ratio.
	 */
	private void scaleContents() {
		float space = (CHART_HEIGHT - (CHART_HEIGHT / 10f) - (6f * density)) / 4f;
		float scale = (18 + 18 + 12 + 6 * 4) * density;
		float ratio = (space / (float) scale) * density;
		fontSize = sqrSize = Math.max(Math.round(10f * ratio), 9f);
		if (isTablet) {
			TOP_PADDING = 5;
			space = (getHeight() - TOP_PADDING * density) / 3;
			ratio = (space / (float) scale) * density;
			separator = fontSize = sqrSize = Math.max(12f * ratio, 9f);
			// center the drawing vertically
			// TOP_PADDING = (int) (getHeight() - (sqrSize * 6)) / 2;
			TOP_PADDING = 70;
		}
		scaled = true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (!scaled) {
			scaleContents();
		}
		// TODO remove next 2
		// paint.setColor(0x22ff00ff);
		// canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

		if (isTablet) {
			drawLegendColumn(canvas);
		} else {
			drawLegendRow(canvas);
		}

	}

	private void drawLegendRow(Canvas c) {
		// each legend item gets 1/3
		float itemSpace = (legendWidth - 2 * separator) / 3f;
		for (int i = 0; i < 3; i++) {
			float left = LEFT_PADDING * density + (itemSpace + separator) * i;
			paint.setColor(newColors[i]);
			drawLegendItem(c, left, TOP_PADDING * density, labels[i], itemSpace);
		}
	}

	private void drawLegendItem(Canvas c, float left, float top, String text, float pWidth) {
		String[] str = text.split(" ");
		float dst = pWidth / 10f;
		float textSpace = pWidth - sqrSize - dst;
		// use "Failed" in calcFontSize as a standard.. to keep font size same
		// for all
		String stStr = labels[0].split(" ")[0];

		paint.setTextSize(fontSize);
		paint.setTypeface(robotoRegular);
		textScaleRect.setEmpty();
		paint.getTextBounds(str[0], 0, str[0].length(), textScaleRect);
		float textWidth = textScaleRect.width();

		float sqLeft = left;
		if (text == labels[2]) {
			sqLeft = left + textSpace - textWidth;
		} else if (text == labels[1]) {
			sqLeft = left + (textSpace - textWidth) / 2f;
		}
		c.drawRect((int) sqLeft, (int) top, (int) sqLeft + sqrSize, (int) top + sqrSize, paint);

		// shift left-coordinate to right for drawing text next to squares
		left += dst + sqrSize;
		// again use "Failed" as a standard.. to keep fonts at same y-position
		textScaleRect.setEmpty();
		paint.getTextBounds(stStr, 0, stStr.length(), textScaleRect);
		float heightFailed = textScaleRect.height(); // height of text 'Failed'
		paint.setColor(newColors[3]);
		Paint.Align align = Paint.Align.LEFT;
		if (text == labels[2]) {
			align = Paint.Align.RIGHT;
			left = left + textSpace;
		} else if (text == labels[1]) {
			align = Paint.Align.CENTER;
			left = left + textSpace / 2f;
		}
		paint.setTextAlign(align);
		// draw 'Normal', 'Failed', 'Dropped'
		c.drawText(str[0], left, top + heightFailed, paint);

		top += heightFailed;
		paint.setTextAlign(Paint.Align.LEFT);
		if (text == labels[2]) {
			left -= textWidth;
		} else if (text == labels[1]) {
			left -= textWidth / 2f;
		}
		// draw 'Calls'
		c.drawText(str[1], left, top + heightFailed + 6 * density, paint);
	}

	private void drawLegendColumn(Canvas c) {
		// each legend item gets 1/3
		float itemSpace = (legendWidth - 2 * separator) / 3f;
		float top = TOP_PADDING * density;
		float left = LEFT_PADDING * density;
		for (int i = 0; i < 3; i++) {
			paint.setColor(newColors[i]);
			top = drawLegendItem2(c, left, top, labels[i], itemSpace);
		}
	}

	private float drawLegendItem2(Canvas c, float left, float top, String text, float pWidth) {
		String[] str = text.split(" ");
		float dst = pWidth / 10f;
		// use "Failed" in calcFontSize as a standard.. to keep font size same
		// for all
		String stStr = labels[1].split(" ")[0];

		paint.setTextSize(fontSize);
		paint.setTypeface(robotoRegular);
		textScaleRect.setEmpty();
		paint.getTextBounds(stStr, 0, stStr.length(), textScaleRect);
		float heightFailed = textScaleRect.height(); // height of text 'Failed'

		// Log.e(TAG, (int)sqLeft + " : " + sqrSize);
		c.drawRect((int) left, (int) top, (int) left + sqrSize, (int) top + sqrSize, paint);

		// shift left-coordinate to right for drawing text next to squares
		left += dst + sqrSize;
		paint.setColor(newColors[3]);
		top += heightFailed;
		// Draw 'Failed', 'Dropped', 'Normal'
		c.drawText(text, left, top, paint);
		return top + separator + separator;
	}

	/*
	 * Draw different legend according to which stat screen index is selected
	 * Delay the change slightly when changing to screens that show a legend
	 */
	public void setStatScreen(final int statInd) {
		statIndex = statInd;
		/*
		 * if
		 * (MyStats.STATS_URLS[statIndex].equals(MyStats.MYCALLSTATS_PAGE_URL))
		 * statIndex = statInd; else postDelayed(new Runnable() {
		 * 
		 * @Override public void run() { statIndex = statInd;
		 * LegendView.this.invalidate(); } }, 1000);
		 */
		this.invalidate();
	}

	public void setPieLegend(JSONObject yourStats) {
		try {
			myCallsDropped = yourStats.getInt("droppedCalls");
			myCallsFailed = yourStats.getInt("failedCalls");
			myCallsNormal = yourStats.getInt("normallyEndedCalls");
		} catch (Exception e) {
		}
		// statIndex = 0;
		this.invalidate();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// myHeight = h - (int)((TOP_PADDING + BOTTOM_PADDING) * density);
		// myWidth = w - (int)((LEFT_PADDING + RIGHT_PADDING) * density);
		legendWidth = w - ((LEFT_PADDING + RIGHT_PADDING) * density);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
	}

	private int measureHeight(int heightMeasureSpec) {
		if (isTablet) {
			return (int) (getResources().getDisplayMetrics().heightPixels / 2);
		}
		int measuredHeight = (int) (viewHeight * density);
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
			// get the size of the image
			result = (int) viewWidth;
			if (specMode == MeasureSpec.AT_MOST) {
				// Respect AT_MOST value if that was what is called for by
				// measureSpec
				result = Math.min(result, specSize);
			}
		}

		return result;
	}
}
